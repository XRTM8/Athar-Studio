package app.xrtm8.athar.core

import android.util.Log

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

    fun applyKashida(text: String, stretchLevel: Int, targetIndex: Long = -1L): String {
        if (stretchLevel <= 0 || text.isEmpty()) {
            return text
        }
        // If native engine is loaded and targetIndex is specified, try native
        if (isLoaded && targetIndex >= 0) {
            try {
                return nativeApplyKashida(text, stretchLevel, targetIndex)
            } catch (e: Throwable) {
                Log.e(TAG, "Error in nativeApplyKashida, using robust pure engine", e)
            }
        }
        // Use our high-precision multi-word calligraphic Arabic engine
        return PureArabicKashidaEngine.stretchText(text, stretchLevel)
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
 * Never truncates text, preserves all Tashkeel / Harakat / Quranic marks,
 * and harmoniously balances kashida across all words in phrases and sentences.
 */
object PureArabicKashidaEngine {

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

    private fun getCalligraphicPriority(c: Char): Int = when (c) {
        'س', 'ش' -> 10
        'ص', 'ض' -> 9
        'ع', 'غ', 'ف', 'ق' -> 8
        'ط', 'ظ' -> 7
        'ب', 'ت', 'ث', 'ن', 'ي', 'ئ' -> 6
        'ك', 'ل' -> 5
        'م', 'ه' -> 4
        else -> 2
    }

    private data class ArabicToken(
        val char: Char,
        var diacritics: String = ""
    )

    fun stretchWord(word: String, level: Int): String {
        if (level <= 0 || word.length < 2) return word

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

        if (tokens.size < 2) return word

        // Find candidate join points between dual-joining letter and next connecting letter
        var bestIndex = -1
        var highestPriority = -1

        for (idx in 0 until tokens.size - 1) {
            val currC = tokens[idx].char
            val nextC = tokens[idx + 1].char
            if (currC in DUAL_JOINING && (nextC in DUAL_JOINING || nextC in RIGHT_JOINING)) {
                val priority = getCalligraphicPriority(currC)
                if (priority > highestPriority) {
                    highestPriority = priority
                    bestIndex = idx
                }
            }
        }

        if (bestIndex == -1) return word

        val tatweel = "ـ".repeat(level)
        val result = StringBuilder()
        for (idx in tokens.indices) {
            result.append(tokens[idx].char)
            result.append(tokens[idx].diacritics)
            if (idx == bestIndex) {
                result.append(tatweel)
            }
        }
        return result.toString()
    }

    fun stretchText(text: String, level: Int): String {
        if (level <= 0 || text.isEmpty()) return text

        val lines = text.split("\n")
        val stretchedLines = lines.map { line ->
            val words = line.split(" ")
            words.joinToString(" ") { word ->
                stretchWord(word, level)
            }
        }
        return stretchedLines.joinToString("\n")
    }
}
