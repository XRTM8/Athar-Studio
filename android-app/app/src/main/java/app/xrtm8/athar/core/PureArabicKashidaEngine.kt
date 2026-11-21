package app.xrtm8.athar.core

import app.xrtm8.athar.model.KashidaMode

/**
 * Authentic, robust Arabic Calligraphic Elongation Engine.
 * Implements classical Arabic calligraphy rules:
 * - Proper priority weighting (Seen/Sheen, Saad/Daad, Jeem/Haa/Khaa, etc.)
 * - Golden Pre-Final connection elevation (highest aesthetic priority for letters before the final letter)
 * - Avoidance of awkward non-calligraphic stretch points (e.g. Meem before Jeem/Haa/Khaa, Lam-Alef)
 * - Full Tashkeel / Harakat / Quranic marks preservation attached directly to base glyphs
 * - Supports 7 authentic modes: SMART, PRE_FINAL, INITIAL, MEDIAL, TEETH, ALL, and CUSTOM.
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

    private val DUAL_JOINING = setOf(
        'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ',
        'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'ي', 'ئ'
    )

    private val RIGHT_JOINING = setOf(
        'ا', 'أ', 'إ', 'آ', 'ٱ', 'د', 'ذ', 'ر', 'ز', 'و', 'ؤ', 'ة', 'ى'
    )

    private fun isArabicMark(c: Char): Boolean =
        (c in '\u0600'..'\u06FF' || c in '\u08A0'..'\u08FF') &&
            Character.getType(c) == Character.NON_SPACING_MARK.toInt()

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
        val suffix: String = ""
    )

    fun tokenizeWord(word: String): List<ArabicToken> {
        val tokens = ArrayList<ArabicToken>()
        var i = 0
        while (i < word.length) {
            val base = word[i++]
            val start = i
            while (i < word.length && (isArabicMark(word[i]) || word[i] == 'ـ')) i++
            tokens.add(ArabicToken(base, word.substring(start, i)))
        }
        return tokens
    }

    fun findOpportunities(text: String): List<KashidaOpportunity> {
        if (text.isEmpty()) return emptyList()

        val opps = ArrayList<KashidaOpportunity>()
        var globalId = 0
        val lines = text.split("\n")

        for (line in lines) {
            val words = line.split(" ")
            for ((wIdx, word) in words.withIndex()) {
                if (word.length < 2) continue

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

        val activeMode = if (mode == KashidaMode.CUSTOM &&
            (targetGlobalIndex < 0 || targetGlobalIndex >= findOpportunities(text).size)
        ) {
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
                    KashidaMode.PRE_FINAL -> {
                        val preFinalCand = wordCands.find { it.first == tokens.size - 2 }?.first ?: wordCands.last().first
                        selectedTokenIndices.add(preFinalCand)
                    }
                    KashidaMode.INITIAL -> {
                        val initialCand = wordCands.find { it.first == 0 }?.first ?: wordCands.first().first
                        selectedTokenIndices.add(initialCand)
                    }
                    KashidaMode.MEDIAL -> {
                        val medialCands = wordCands.filter { it.first > 0 && it.first < tokens.size - 2 }
                        if (medialCands.isNotEmpty()) {
                            val bestMedial = medialCands.maxByOrNull { it.second }?.first
                            if (bestMedial != null) selectedTokenIndices.add(bestMedial)
                        } else {
                            selectedTokenIndices.add(wordCands[wordCands.size / 2].first)
                        }
                    }
                    KashidaMode.TEETH -> {
                        val teethChars = setOf('س', 'ش', 'ص', 'ض', 'ب', 'ت', 'ث', 'ن', 'ي', 'ئ')
                        val teethCands = wordCands.filter { tokens[it.first].char in teethChars }
                        if (teethCands.isNotEmpty()) {
                            for (c in teethCands) {
                                selectedTokenIndices.add(c.first)
                            }
                        } else {
                            val best = wordCands.maxByOrNull { it.second }?.first
                            if (best != null) selectedTokenIndices.add(best)
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
                    sb.append(tokens[idx].suffix)
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
