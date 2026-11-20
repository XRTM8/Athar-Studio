package app.xrtm8.athar.core

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Smart Typographic Block Balance & Optical Justifier Engine.
 * 
 * Solves uneven, jagged line lengths in Arabic poetry, quotes, and multi-line titles.
 * Calculates the visual weight deficit of each line and mathematically distributes
 * authentic Arabic kashidas (ـ) across the highest-priority calligraphic positions
 * so the entire text block forms a majestic, rectangular justified composition.
 */
object SmartTypographyBalancingEngine {

    /**
     * Balances all lines of a multi-line Arabic text to achieve optical rectangular justification.
     */
    fun balanceArabicBlock(text: String): String {
        val lines = text.split("\n")
        if (lines.size <= 1) return text

        // 1. Clean existing tatweels to measure baseline width
        val cleanedLines = lines.map { it.replace("ـ", "") }

        // 2. Measure optical typographic visual weight of each line
        val weights = cleanedLines.map { computeLineWeight(it) }
        val maxWeight = weights.maxOrNull() ?: return text

        if (maxWeight <= 0f) return text

        // 3. Justify shorter lines using weighted kashida distribution
        val balancedLines = ArrayList<String>()

        for (i in cleanedLines.indices) {
            val line = cleanedLines[i]
            val currentW = weights[i]
            val deficit = maxWeight - currentW

            if (deficit <= 0.8f || line.trim().isEmpty()) {
                // Already at max width or empty line
                balancedLines.add(line)
                continue
            }

            // Estimate how many kashida units (ـ) are needed to fill the deficit
            // 1 kashida unit contributes ~0.8f to line weight
            val neededKashidas = (deficit / 0.85f).roundToInt().coerceIn(1, 12)

            val justifiedLine = justifySingleLine(line, neededKashidas)
            balancedLines.add(justifiedLine)
        }

        return balancedLines.joinToString("\n")
    }

    /**
     * Calculates optical letter weight accounting for Arabic glyph anatomy.
     */
    private fun computeLineWeight(line: String): Float {
        var w = 0f
        for (c in line) {
            w += when (c) {
                'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ك', 'ف', 'ق' -> 1.35f
                'ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'ع', 'غ', 'ل', 'م', 'ن', 'ه', 'ي', 'ى' -> 1.0f
                'ا', 'أ', 'إ', 'آ', 'ٱ', 'د', 'ذ', 'ر', 'ز', 'و', 'ؤ', 'ء' -> 0.60f
                ' ' -> 0.50f
                in '\u064B'..'\u065F' -> 0f // Diacritics take no horizontal advance
                else -> 0.85f
            }
        }
        return w
    }

    /**
     * Distributes the target number of kashidas across authentic calligraphic opportunities in a line.
     */
    private fun justifySingleLine(line: String, totalKashidas: Int): String {
        val words = line.split(" ")
        val opportunities = ArrayList<OpportunityRef>()

        // Discover all allowable kashida locations across words
        for (wIdx in words.indices) {
            val word = words[wIdx]
            val tokens = PureArabicKashidaEngine.tokenizeWord(word)
            if (tokens.size < 2) continue

            for (tIdx in 0 until tokens.size - 1) {
                val curr = tokens[tIdx].char
                val next = tokens[tIdx + 1].char
                val isPreFinal = (tIdx == tokens.size - 2)

                val priority = PureArabicKashidaEngine.getCalligraphicPriority(curr, next, isPreFinal)
                if (priority > 0) {
                    opportunities.add(OpportunityRef(wIdx, tIdx, priority))
                }
            }
        }

        if (opportunities.isEmpty()) {
            return line
        }

        // Sort opportunities by calligraphic priority descending
        opportunities.sortByDescending { it.priority }

        // Distribute kashidas among top opportunities
        val allocations = IntArray(opportunities.size)
        var remaining = totalKashidas
        var round = 0

        while (remaining > 0 && round < 4) {
            for (idx in opportunities.indices) {
                if (remaining <= 0) break
                // Cap per position to keep calligraphy natural
                if (allocations[idx] < 4) {
                    allocations[idx]++
                    remaining--
                }
            }
            round++
        }

        // Reconstruct line with allocated kashidas
        val reconstructedWords = ArrayList<String>()

        for (wIdx in words.indices) {
            val word = words[wIdx]
            val tokens = PureArabicKashidaEngine.tokenizeWord(word)

            val sb = java.lang.StringBuilder()
            for (tIdx in tokens.indices) {
                val tok = tokens[tIdx]
                sb.append(tok.char)
                sb.append(tok.diacritics)

                // Check if this position has kashidas allocated
                val oppIdx = opportunities.indexOfFirst { it.wordIndex == wIdx && it.tokenIndex == tIdx }
                if (oppIdx != -1) {
                    val count = allocations[oppIdx]
                    repeat(count) {
                        sb.append('ـ')
                    }
                }
            }
            reconstructedWords.add(sb.toString())
        }

        return reconstructedWords.joinToString(" ")
    }

    private data class OpportunityRef(
        val wordIndex: Int,
        val tokenIndex: Int,
        val priority: Int
    )
}
