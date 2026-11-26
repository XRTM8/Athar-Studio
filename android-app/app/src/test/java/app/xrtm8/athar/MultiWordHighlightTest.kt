package app.xrtm8.athar

import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.TextSpanStyle
import app.xrtm8.athar.typography.RichTextSpanMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class MultiWordHighlightTest {

    @Test
    fun multipleNonOverlappingWordsCanBeHighlightedIndependently() {
        val text = "كن في الجانب الذي فيه الله"
        // Word 1: "كن" (0..2), Word 3: "الجانب" (6..12), Word 6: "الله" (22..26)
        var spans = emptyList<TextSpanStyle>()
        spans = RichTextSpanMapper.upsertSpan(
            spans,
            TextSpanStyle(start = 0, end = 2, highlightColorLong = 0xFFE5C07BL)
        )
        spans = RichTextSpanMapper.upsertSpan(
            spans,
            TextSpanStyle(start = 6, end = 12, highlightColorLong = 0xFF9E1B32L)
        )
        spans = RichTextSpanMapper.upsertSpan(
            spans,
            TextSpanStyle(start = 22, end = 26, fontFamily = ArabicFontFamily.AMIRI, highlightColorLong = 0xFF1F6F54L)
        )

        assertEquals(3, spans.size)
        assertEquals("كن", text.substring(spans[0].start, spans[0].end))
        assertEquals("الجانب", text.substring(spans[1].start, spans[1].end))
        assertEquals("الله", text.substring(spans[2].start, spans[2].end))
        assertEquals(0xFFE5C07BL, spans[0].highlightColorLong)
        assertEquals(0xFF9E1B32L, spans[1].highlightColorLong)
        assertEquals(0xFF1F6F54L, spans[2].highlightColorLong)
    }

    @Test
    fun multipleSpansMapAccuratelyThroughKashida() {
        val raw = "كن ذا أثر"
        val display = "كــن ذا أَثَـــر" // tatweels in word 1 and word 3
        val spans = listOf(
            TextSpanStyle(start = 0, end = 2, highlightColorLong = 0xFFE5C07BL),
            TextSpanStyle(start = 6, end = 9, highlightColorLong = 0xFF9E1B32L)
        )
        val mapped = RichTextSpanMapper.mapRawSpansToDisplaySpans(raw, display, spans)
        assertEquals(2, mapped.size)
        assertEquals("كــن", display.substring(mapped[0].start, mapped[0].end))
    }
}
