package app.xrtm8.athar

import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.TextSpanStyle
import app.xrtm8.athar.project.AtharProjectSerializer
import app.xrtm8.athar.typography.RichTextSpanMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextSpansTest {

    @Test
    fun expandToWordBoundsPreventsBreakingArabicWord() {
        val text = "كن ذا أثر جميل"
        // Select inside "أثر" (index 7..8)
        val range = RichTextSpanMapper.expandToWordBounds(text, 7, 8)
        assertEquals("أثر", text.substring(range.first, range.last))
    }

    @Test
    fun mapRawSpansToDisplaySpansTracksKashidaExpansion() {
        val raw = "كن أثر"
        val display = "كـــن أثر" // 3 tatweels inserted inside "كن"
        val rawSpans = listOf(
            TextSpanStyle(start = 3, end = 6, fontFamily = ArabicFontFamily.AMIRI, highlightColorLong = 0xFFE5C07BL)
        )
        val mapped = RichTextSpanMapper.mapRawSpansToDisplaySpans(raw, display, rawSpans)
        assertEquals(1, mapped.size)
        assertEquals("أثر", display.substring(mapped[0].start, mapped[0].end))
    }

    @Test
    fun adjustSpansOnTextEditShiftsSubsequentSpans() {
        val oldText = "سلام جميل"
        val newText = "سلام رائع جميل"
        val spans = listOf(TextSpanStyle(start = 5, end = 9, highlightColorLong = 0xFF9E1B32L))
        val adjusted = RichTextSpanMapper.adjustSpansOnTextEdit(oldText, newText, spans)
        assertEquals(1, adjusted.size)
        assertEquals("جميل", newText.substring(adjusted[0].start, adjusted[0].end))
    }

    @Test
    fun textSpansRoundTripThroughProjectSerializer() {
        val span = TextSpanStyle(
            start = 0,
            end = 4,
            fontFamily = ArabicFontFamily.AMIRI,
            scaleMultiplier = 1.25f,
            baselineShift = -0.08f,
            textColorLong = 0xFF0A0A0AL,
            highlightColorLong = 0xFFE5C07BL,
            highlightAlpha = 0.9f,
            highlightCornerRadius = 12f,
            highlightPaddingH = 8f,
            highlightPaddingV = 4f
        )
        val state = CanvasState(
            layers = listOf(TextLayer(id = "l1", rawText = "أثرٌ جميل", displayText = "أثرٌ جميل", textSpans = listOf(span)))
        )
        val json = AtharProjectSerializer.serializeCanvasState(state, hasBackgroundPhoto = false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)
        val restoredSpans = restored.layers.first().textSpans
        assertEquals(1, restoredSpans.size)
        assertEquals(ArabicFontFamily.AMIRI, restoredSpans[0].fontFamily)
        assertEquals(1.25f, restoredSpans[0].scaleMultiplier, 0.001f)
        assertEquals(0xFFE5C07BL, restoredSpans[0].highlightColorLong)
        assertEquals(12f, restoredSpans[0].highlightCornerRadius, 0.001f)
        assertTrue(restoredSpans[0].hasHighlight)
    }
}
