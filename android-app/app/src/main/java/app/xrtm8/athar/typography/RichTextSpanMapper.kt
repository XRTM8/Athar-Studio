package app.xrtm8.athar.typography

import app.xrtm8.athar.model.TextSpanStyle
import kotlin.math.max
import kotlin.math.min

/**
 * Pure utilities for managing [TextSpanStyle] ranges across:
 * - Arabic word-boundary expansion (prevents breaking Arabic letter connections)
 * - Deterministic index mapping from rawText -> displayText (with Kashida tatweel)
 * - Span adjustment on text edits & clean span merging
 */
object RichTextSpanMapper {

    private const val TATWEEL = '\u0640'

    /**
     * Expands [start, end) to whole word boundaries when the selection falls inside
     * an Arabic letter sequence so font overrides never break cursive joining.
     */
    fun expandToWordBounds(text: String, start: Int, end: Int): IntRange {
        if (text.isEmpty()) return 0..0
        var s = start.coerceIn(0, text.length)
        var e = end.coerceIn(s, text.length)
        if (s == e) {
            if (s < text.length && !text[s].isWhitespace()) {
                while (s > 0 && !text[s - 1].isWhitespace()) s--
                while (e < text.length && !text[e].isWhitespace()) e++
            }
            return s..e
        }
        while (s > 0 && !text[s - 1].isWhitespace() && !text[s].isWhitespace()) {
            s--
        }
        while (e < text.length && !text[e - 1].isWhitespace() && !text[e].isWhitespace()) {
            e++
        }
        return s..e
    }

    /**
     * Maps [TextSpanStyle] indices from [rawText] to [displayText] (which may contain
     * inserted Kashida tatweel characters or justification spaces).
     */
    fun mapRawSpansToDisplaySpans(
        rawText: String,
        displayText: String,
        rawSpans: List<TextSpanStyle>
    ): List<TextSpanStyle> {
        if (rawSpans.isEmpty() || rawText.isEmpty() || displayText.isEmpty()) return emptyList()
        if (rawText == displayText) {
            return rawSpans.mapNotNull { span ->
                val s = span.start.coerceIn(0, displayText.length)
                val e = span.end.coerceIn(s, displayText.length)
                if (e > s && span.hasAnyStyle) span.copy(start = s, end = e) else null
            }
        }

        val rawLen = rawText.length
        val dispLen = displayText.length
        val rawToDispStart = IntArray(rawLen + 1)
        val rawToDispEnd = IntArray(rawLen + 1)

        var r = 0
        var d = 0
        while (r < rawLen && d < dispLen) {
            val rc = rawText[r]
            val dc = displayText[d]
            if (rc == dc) {
                rawToDispStart[r] = d
                r++
                d++
                rawToDispEnd[r] = d
            } else if (dc == TATWEEL && rc != TATWEEL) {
                d++
            } else if (dc == ' ' && rc != ' ') {
                d++
            } else {
                rawToDispStart[r] = d
                r++
                d++
                rawToDispEnd[r] = d
            }
        }
        while (r < rawLen) {
            rawToDispStart[r] = dispLen
            r++
            rawToDispEnd[r] = dispLen
        }
        rawToDispStart[rawLen] = dispLen

        return rawSpans.mapNotNull { span ->
            val rs = span.start.coerceIn(0, rawLen)
            val re = span.end.coerceIn(rs, rawLen)
            if (re <= rs || !span.hasAnyStyle) return@mapNotNull null
            val ds = rawToDispStart[rs].coerceIn(0, dispLen)
            val de = rawToDispEnd[re].coerceIn(ds, dispLen)
            if (de > ds) span.copy(start = ds, end = de) else null
        }
    }

    /**
     * Updates [spans] when [oldText] is edited into [newText] using common prefix/suffix diffing.
     */
    fun adjustSpansOnTextEdit(
        oldText: String,
        newText: String,
        spans: List<TextSpanStyle>
    ): List<TextSpanStyle> {
        if (spans.isEmpty() || oldText == newText) return spans
        if (newText.isEmpty()) return emptyList()

        val minLen = min(oldText.length, newText.length)
        var prefix = 0
        while (prefix < minLen && oldText[prefix] == newText[prefix]) prefix++

        var oldSuffix = oldText.length
        var newSuffix = newText.length
        while (oldSuffix > prefix && newSuffix > prefix && oldText[oldSuffix - 1] == newText[newSuffix - 1]) {
            oldSuffix--
            newSuffix--
        }

        val delta = newText.length - oldText.length
        return spans.mapNotNull { span ->
            if (!span.isValid) return@mapNotNull null
            val s = span.start
            val e = span.end
            when {
                // Completely before edit
                e <= prefix -> span
                // Completely after edit
                s >= oldSuffix -> span.copy(
                    start = (s + delta).coerceIn(0, newText.length),
                    end = (e + delta).coerceIn(0, newText.length)
                )
                // Edit happened inside or overlapping the span
                else -> {
                    val ns = min(s, prefix).coerceIn(0, newText.length)
                    val ne = max(ns, e + delta).coerceIn(ns, newText.length)
                    if (ne > ns) span.copy(start = ns, end = ne) else null
                }
            }
        }
    }

    /**
     * Merges [incoming] into [existing] by replacing exact-match ranges or inserting cleanly.
     */
    fun upsertSpan(existing: List<TextSpanStyle>, incoming: TextSpanStyle): List<TextSpanStyle> {
        if (!incoming.isValid) return existing
        val result = mutableListOf<TextSpanStyle>()
        var merged = false
        for (span in existing) {
            if (span.start == incoming.start && span.end == incoming.end) {
                if (incoming.hasAnyStyle) result.add(incoming)
                merged = true
            } else if (span.end <= incoming.start || span.start >= incoming.end) {
                result.add(span)
            } else {
                // Split non-overlapping parts of the existing span
                if (span.start < incoming.start) {
                    result.add(span.copy(end = incoming.start))
                }
                if (span.end > incoming.end) {
                    result.add(span.copy(start = incoming.end))
                }
            }
        }
        if (!merged && incoming.hasAnyStyle) {
            result.add(incoming)
        }
        return result.sortedBy { it.start }
    }
}
