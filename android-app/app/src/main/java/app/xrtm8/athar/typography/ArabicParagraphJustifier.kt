package app.xrtm8.athar.typography

import android.graphics.Paint
import app.xrtm8.athar.core.PureArabicKashidaEngine

/**
 * Intelligent Calligraphic Arabic Paragraph Justifier.
 * Equalizes the visual pixel width of all lines in multiline Arabic text
 * by proportionally distributing Tatweel (ـ) across genuine calligraphic ligatures.
 * Does not alter single-line texts or destroy letter shapes.
 */
object ArabicParagraphJustifier {

    fun justifyLines(
        text: String,
        paint: Paint? = null,
        measureText: ((String) -> Float)? = null
    ): String {
        if (!text.contains("\n") || text.isBlank()) return text

        val rawLines = text.split("\n")
        if (rawLines.size < 2) return text

        val measurer: (String) -> Float = measureText ?: { str ->
            val w = paint?.measureText(str) ?: 0f
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
                balanceLine(line, neededCount)
            }
        }

        return justifiedLines.joinToString("\n")
    }

    private fun balanceLine(line: String, totalTatweelToAdd: Int): String {
        val opps = PureArabicKashidaEngine.findOpportunities(line)
        if (opps.isEmpty()) return line

        val countPerOpp = IntArray(opps.size)
        val sortedIndices = opps.indices.sortedByDescending { opps[it].priority }

        var remaining = totalTatweelToAdd
        while (remaining > 0) {
            for (idx in sortedIndices) {
                if (remaining <= 0) break
                countPerOpp[idx]++
                remaining--
            }
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
                sb.append(tokens[i].diacritics)

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
