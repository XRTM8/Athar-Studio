package app.xrtm8.athar

import app.xrtm8.athar.typography.ArabicParagraphJustifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicParagraphJustifierTest {

    @Test
    fun testSingleLineRemainsUntouched() {
        val singleLine = "أَثَــرٌ يَبْقَى"
        val result = ArabicParagraphJustifier.justifyLines(
            text = singleLine,
            measureText = { it.length * 15f }
        )
        assertEquals(singleLine, result)
    }

    @Test
    fun testEmptyOrBlankTextRemainsUntouched() {
        assertEquals("", ArabicParagraphJustifier.justifyLines("", measureText = { it.length * 15f }))
        assertEquals("   ", ArabicParagraphJustifier.justifyLines("   ", measureText = { it.length * 15f }))
    }

    @Test
    fun testMultiLineDistributionInsertsTatweel() {
        val multiLine = "أثر\nأثر في قلب الزمان الجميل"
        val justified = ArabicParagraphJustifier.justifyLines(
            text = multiLine,
            measureText = { it.length * 15f }
        )

        assertTrue("Justified text must contain newline", justified.contains("\n"))
        val lines = justified.split("\n")
        assertEquals(2, lines.size)
        assertTrue("First line should receive tatweel extension", lines[0].contains("ـ"))
    }

    @Test
    fun testBalanceArabicBlockConsolidation() {
        val multiLine = "أثرٌ جميلٌ يبقى\nفي قلوب الناس أجمعين"
        val balanced = ArabicParagraphJustifier.balanceArabicBlock(multiLine)
        assertTrue("Balanced text must contain newline", balanced.contains("\n"))
        val lines = balanced.split("\n")
        assertEquals(2, lines.size)
        assertTrue("Should distribute kashida to shorter line", lines[0].contains("ـ"))
    }
}
