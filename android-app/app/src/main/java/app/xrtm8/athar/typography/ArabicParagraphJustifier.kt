package app.xrtm8.athar.typography

import android.graphics.Paint
import app.xrtm8.athar.core.PureArabicKashidaEngine
import kotlin.math.roundToInt

/**
 * Unified Arabic Typographic Paragraph Justification & Block Balancing Engine.
 * 
 * Provides dual calligraphic alignment modes for Arabic poetry, quotes, and multiline text:
 * 1. Pixel-Precise Justification (`justifyLines`): Equalizes line pixel widths using actual font metrics.
 * 2. Optical Weight Block Balancing (`balanceArabicBlock`): Balances line weights into a harmonious block.
 * 
 * 100% authentic calligraphy: respects Seen/Sheen, Saad/Daad, and pre-final priority rules,
 * preserving diacritics and ligatures without distortion.
 */
object ArabicParagraphJustifier {

    /**
     * Justifies all lines in a multiline paragraph to match the widest line's pixel length.
     */
    fun justifyLines(
        text: String,
        paint: Paint? = null,
        measureText: ((String) -> Float)? = null
    ): String {
        if (!text.contains("\n") || text.isBlank()) return text

        val rawLines = text.split("\n")
        if (rawLines.size < 2) return text

        val measurer: (String) -> Float = measureText ?: { str ->
            val w = try { paint?.measureText(str) ?: 0f } catch (_: Throwable) { 0f }
            if (w > 0f) w else (str.length * 15f)
        }

        val widths = rawLines.map { measurer(it) }
        val maxWidth = widths.maxOrNull() ?: return text
        if (maxWidth <= 0f) return text

        val tatweelWidth = measurer("ـ").let { if (it <= 0.1f) 14f else it }

        val justifiedLines = rawLines.mapIndexed { index, line ->
            val curWidth = widths[index]
            val deficit = maxWidth - curWidth

            if (deficit <= (tatweelWidth * 0.5f) || line.isBlank()) {
                line
            } else {
                val neededCount = (deficit / tatweelWidth).toInt().coerceIn(1, 35)
                distributeKashidas(line, neededCount)
            }
        }

        return justifiedLines.joinToString("\n")
    }

    /**
     * Balances lines of multiline Arabic text to achieve an optical rectangular block.
     * Uses font paint metrics when available, falling back to anatomical letter weighting.
     */
    fun balanceArabicBlock(
        text: String,
        paint: Paint? = null,
        measureText: ((String) -> Float)? = null
    ): String {
        val lines = text.split("\n")
        if (lines.size <= 1) return text

        // Clean existing tatweels to measure true base weight
        val cleanedLines = lines.map { it.replace("ـ", "") }

        val measurer: ((String) -> Float)? = if (paint != null || measureText != null) {
            measureText ?: { str ->
                val w = try { paint?.measureText(str) ?: 0f } catch (_: Throwable) { 0f }
                if (w > 0f) w else computeAnatomicalWeight(str)
            }
        } else null

        val weights = cleanedLines.map { measurer?.invoke(it) ?: computeAnatomicalWeight(it) }
        val maxWeight = weights.maxOrNull() ?: return text
        if (maxWeight <= 0f) return text

        val unitWeight = if (measurer != null) {
            val tw = measurer("ـ")
            if (tw > 0.1f) tw else 14f
        } else 0.85f

        val balancedLines = ArrayList<String>(cleanedLines.size)
        for (i in cleanedLines.indices) {
            val line = cleanedLines[i]
            val currentW = weights[i]
            val deficit = maxWeight - currentW

            if (deficit <= (unitWeight * 0.5f) || line.trim().isEmpty()) {
                balancedLines.add(line)
                continue
            }

            val neededKashidas = (deficit / unitWeight).roundToInt().coerceIn(1, 25)
            balancedLines.add(distributeKashidas(line, neededKashidas))
        }

        return balancedLines.joinToString("\n")
    }

    /**
     * Optical letter weighting based on Arabic glyph anatomy.
     */
    private fun computeAnatomicalWeight(line: String): Float {
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
     * High-precision calligraphic kashida allocator across genuine ligatures.
     */
    private fun distributeKashidas(line: String, totalKashidas: Int): String {
        val opps = PureArabicKashidaEngine.findOpportunities(line)
        if (opps.isEmpty()) return line

        val countPerOpp = IntArray(opps.size)
        val sortedIndices = opps.indices.sortedByDescending { opps[it].priority }

        var remaining = totalKashidas
        var round = 0
        val maxPerOpp = kotlin.math.max(8, (totalKashidas / opps.size) + 4)
        while (remaining > 0 && round < 30) {
            var allocatedThisRound = 0
            for (idx in sortedIndices) {
                if (remaining <= 0) break
                if (countPerOpp[idx] < maxPerOpp) {
                    countPerOpp[idx]++
                    remaining--
                    allocatedThisRound++
                }
            }
            if (allocatedThisRound == 0) break
            round++
        }

        val words = line.split(" ")
        val resultWords = ArrayList<String>(words.size)
        var oppCounter = 0

        for (word in words) {
            val tokens = PureArabicKashidaEngine.tokenizeWord(word)
            if (tokens.size < 2) {
                resultWords.add(word)
                continue
            }

            val sb = StringBuilder()
            for (i in tokens.indices) {
                sb.append(tokens[i].char)
                sb.append(tokens[i].suffix)

                if (i < tokens.size - 1) {
                    if (oppCounter < opps.size &&
                        opps[oppCounter].baseChar == tokens[i].char &&
                        opps[oppCounter].nextChar == tokens[i + 1].char
                    ) {
                        val kCount = countPerOpp[oppCounter]
                        if (kCount > 0) {
                            sb.append("ـ".repeat(kCount))
                        }
                        oppCounter++
                    }
                }
            }
            resultWords.add(sb.toString())
        }

        return resultWords.joinToString(" ")
    }
}
