package app.xrtm8.athar.core

import android.util.Log
import app.xrtm8.athar.model.KashidaMode

object AtharNativeEngine {
    private const val TAG = "AtharNativeEngine"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("athar_core")
            isLoaded = true
            Log.i(TAG, "Athar native Rust core loaded successfully. Version: ${nativeGetVersion()}")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library athar_core not found or failed to load. Fallback pure-engine enabled.", e)
            isLoaded = false
        } catch (e: Throwable) {
            Log.w(TAG, "Error initializing athar_core. Fallback pure-engine enabled.", e)
            isLoaded = false
        }
    }

    fun isNativeReady(): Boolean = isLoaded

    fun getEngineVersion(): String {
        return if (isLoaded) {
            try {
                nativeGetVersion()
            } catch (e: Throwable) {
                "1.5.2-pure"
            }
        } else {
            "1.5.2-pure"
        }
    }

    fun applyKashida(
        text: String,
        stretchLevel: Int,
        mode: KashidaMode = KashidaMode.SMART,
        targetIndex: Int = -1
    ): String {
        if (stretchLevel <= 0 || text.isEmpty()) {
            return text
        }
        return PureArabicKashidaEngine.stretchText(text, stretchLevel, mode, targetIndex)
    }

    fun findOpportunities(text: String): List<PureArabicKashidaEngine.KashidaOpportunity> {
        return PureArabicKashidaEngine.findOpportunities(text)
    }

    fun getKashidaOpportunities(text: String): String {
        if (!isLoaded || text.isEmpty()) return "[]"
        return try {
            nativeFindOpportunitiesJson(text)
        } catch (e: Throwable) {
            "[]"
        }
    }

    // JNI Native Declarations
    @JvmStatic
    private external fun nativeGetVersion(): String

    @JvmStatic
    private external fun nativeApplyKashida(text: String, stretchLevel: Int, targetIndex: Long): String

    @JvmStatic
    private external fun nativeFindOpportunitiesJson(text: String): String
}

/**
 * Authentic, robust Arabic Calligraphic Elongation Engine.
 * Implements classical Arabic calligraphy rules:
 * - Proper priority weighting (Seen/Sheen, Saad/Daad, Jeem/Haa/Khaa, etc.)
 * - Golden Pre-Final connection elevation (highest aesthetic priority for letters before the final letter)
 * - Avoidance of awkward non-calligraphic stretch points (e.g. Meem before Jeem/Haa/Khaa, Lam-Alef)
 * - Full Tashkeel / Harakat / Quranic marks preservation attached directly to base glyphs
 * - Supports SMART (single optimal calligraphic point per word), ALL (distributed multi-point elongation),
 *   and CUSTOM (user-selected opportunity by global index).
 */
object PureArabicKashidaEngine {

    data class KashidaOpportunity(
        val globalIndex: Int,
        val wordIndex: Int,
        val tokenIndex: Int,
        val baseChar: Char,
        val nextChar: Char,
        val displayLabel: String,
        val priority: Int,
        val isRecommended: Boolean
    )

    private val TASHKEEL = setOf(
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652', // Harakat & Tanween
        '\u0653', '\u0654', '\u0655', '\u0670', // Maddah, Hamza, Dagger Alef
        '\u06D6', '\u06D7', '\u06D8', '\u06D9', '\u06DA', '\u06DB', '\u06DC', '\u06DF',
        '\u06E0', '\u06E1', '\u06E2', '\u06E3', '\u06E4', '\u06E7', '\u06E8', '\u06EA',
        '\u06EB', '\u06EC', '\u06ED' // Quranic marks
    )

    private val DUAL_JOINING = setOf(
        'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ',
        'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'ي', 'ى', 'ئ', 'ـ'
    )

    private val RIGHT_JOINING = setOf(
        'ا', 'أ', 'إ', 'آ', 'ٱ', 'د', 'ذ', 'ر', 'ز', 'و', 'ؤ', 'ة'
    )

    private fun isTashkeel(c: Char): Boolean = c in TASHKEEL

    fun getCalligraphicPriority(currChar: Char, nextChar: Char, isPreFinal: Boolean): Int {
        // Disallowed connections in Arabic calligraphy:
        // 1. Lam-Alef is a composite ligature, cannot be stretched with basic tatweel
        if (currChar == 'ل' && nextChar in setOf('ا', 'أ', 'إ', 'آ', 'ٱ')) {
            return -1
        }
        // 2. Meem followed by Jeem/Haa/Khaa:
        // In Arabic calligraphy, Meem stacks on Jeem/Haa/Khaa without intermediate elongation.
        if (currChar == 'م' && nextChar in setOf('ج', 'ح', 'خ')) {
            return 1 // Severely discouraged, lowest priority
        }

        var score = when (currChar) {
            'س', 'ش' -> 15 // Classical Seen / Sheen Kashida
            'ص', 'ض' -> 14 // After Saad / Daad tooth
            'ط', 'ظ' -> 13
            'ج', 'ح', 'خ' -> 12 // Calligraphic Jeem/Haa/Khaa
            'ف', 'ق' -> 11
            'ع', 'غ' -> 10
            'ب', 'ت', 'ث', 'ن', 'ي', 'ئ' -> 9
            'ك', 'ل' -> 7
            'م' -> 5
            'ه' -> 3
            else -> 4
        }

        // Golden Pre-Final rule in Arabic calligraphy:
        // Elongating the letter immediately before the final connected glyph creates
        // structural balance and visual harmony (e.g. مَجْـــــد, أَثَـــــر, جَمِيـــــل).
        if (isPreFinal) {
            score += 10
        }

        return score
    }

    data class ArabicToken(
        val char: Char,
        var diacritics: String = ""
    )

    fun tokenizeWord(word: String): List<ArabicToken> {
        val tokens = ArrayList<ArabicToken>()
        var i = 0
        while (i < word.length) {
            val c = word[i]
            if (isTashkeel(c)) {
                if (tokens.isNotEmpty()) {
                    tokens.last().diacritics += c
                } else {
                    tokens.add(ArabicToken(c))
                }
                i++
            } else {
                val sb = StringBuilder()
                var j = i + 1
                while (j < word.length && isTashkeel(word[j])) {
                    sb.append(word[j])
                    j++
                }
                tokens.add(ArabicToken(c, sb.toString()))
                i = j
            }
        }
        return tokens
    }

    fun findOpportunities(text: String): List<KashidaOpportunity> {
        if (text.isEmpty()) return emptyList()

        val opps = ArrayList<KashidaOpportunity>()
        var globalId = 0
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

        for ((wIdx, word) in words.withIndex()) {
            val tokens = tokenizeWord(word)
            if (tokens.size < 2) continue

            for (idx in 0 until tokens.size - 1) {
                val currC = tokens[idx].char
                val nextC = tokens[idx + 1].char
                if (currC in DUAL_JOINING && (nextC in DUAL_JOINING || nextC in RIGHT_JOINING)) {
                    val isPreFinal = (idx == tokens.size - 2)
                    val p = getCalligraphicPriority(currC, nextC, isPreFinal)
                    if (p > 0) {
                        val isRec = p >= 15
                        val star = if (isRec) " ★" else ""
                        val label = "${currC}ـ (${word})$star"
                        opps.add(
                            KashidaOpportunity(
                                globalIndex = globalId,
                                wordIndex = wIdx,
                                tokenIndex = idx,
                                baseChar = currC,
                                nextChar = nextC,
                                displayLabel = label,
                                priority = p,
                                isRecommended = isRec
                            )
                        )
                        globalId++
                    }
                }
            }
        }
        return opps
    }

    fun stretchText(
        text: String,
        level: Int,
        mode: KashidaMode = KashidaMode.SMART,
        targetGlobalIndex: Int = -1
    ): String {
        if (level <= 0 || text.isEmpty()) return text

        val opps = findOpportunities(text)
        if (opps.isEmpty()) return text

        val activeMode = if (mode == KashidaMode.CUSTOM && (targetGlobalIndex < 0 || targetGlobalIndex >= opps.size)) {
            KashidaMode.SMART
        } else {
            mode
        }

        val tatweel = "ـ".repeat(level)
        var currentGlobalOppIndex = 0
        val lines = text.split("\n")
        val resultLines = ArrayList<String>(lines.size)

        for (line in lines) {
            val words = line.split(" ")
            val resultWords = ArrayList<String>(words.size)

            for (word in words) {
                if (word.length < 2) {
                    resultWords.add(word)
                    continue
                }

                val tokens = tokenizeWord(word)
                if (tokens.size < 2) {
                    resultWords.add(word)
                    continue
                }

                val wordCands = ArrayList<Pair<Int, Int>>() // Pair(tokenIndex, priority)
                val wordOppIndices = ArrayList<Int>()

                for (idx in 0 until tokens.size - 1) {
                    val currC = tokens[idx].char
                    val nextC = tokens[idx + 1].char
                    if (currC in DUAL_JOINING && (nextC in DUAL_JOINING || nextC in RIGHT_JOINING)) {
                        val isPreFinal = (idx == tokens.size - 2)
                        val p = getCalligraphicPriority(currC, nextC, isPreFinal)
                        if (p > 0) {
                            wordCands.add(Pair(idx, p))
                            wordOppIndices.add(currentGlobalOppIndex)
                            currentGlobalOppIndex++
                        }
                    }
                }

                if (wordCands.isEmpty()) {
                    resultWords.add(word)
                    continue
                }

                val selectedTokenIndices = HashSet<Int>()

                when (activeMode) {
                    KashidaMode.SMART -> {
                        val bestTokenIdx = wordCands.maxByOrNull { it.second }?.first
                        if (bestTokenIdx != null) {
                            selectedTokenIndices.add(bestTokenIdx)
                        }
                    }
                    KashidaMode.ALL -> {
                        for (cand in wordCands) {
                            selectedTokenIndices.add(cand.first)
                        }
                    }
                    KashidaMode.CUSTOM -> {
                        for (i in wordCands.indices) {
                            if (wordOppIndices[i] == targetGlobalIndex) {
                                selectedTokenIndices.add(wordCands[i].first)
                            }
                        }
                    }
                }

                val sb = StringBuilder()
                for (idx in tokens.indices) {
                    sb.append(tokens[idx].char)
                    sb.append(tokens[idx].diacritics)
                    if (idx in selectedTokenIndices) {
                        sb.append(tatweel)
                    }
                }
                resultWords.add(sb.toString())
            }

            resultLines.add(resultWords.joinToString(" "))
        }

        return resultLines.joinToString("\n")
    }
}
