package app.xrtm8.athar

import app.xrtm8.athar.core.PureArabicKashidaEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KashidaEngineTest {

    @Test
    fun testSingleWordKashida() {
        val original = "أثر"
        val stretched = PureArabicKashidaEngine.stretchText(original, 2)
        assertEquals("أثــر", stretched)
    }

    @Test
    fun testKashidaWithTashkeelPreserved() {
        val original = "أَثَـر"
        val stretched = PureArabicKashidaEngine.stretchText(original, 3)
        // Ensure Tashkeel (Fatha on Alif and Thaa) is preserved intact
        assertTrue(stretched.contains("أَ"))
        assertTrue(stretched.contains("ثَ"))
        assertTrue(stretched.contains("ـ"))
        assertTrue(stretched.endsWith("ر"))
        assertFalse(stretched.contains("ثـَ")) // Diacritic must stay on base letter, not after tatweel
    }

    @Test
    fun testMultiWordKashidaBalanced() {
        val text = "كُنْ ذَا أَثَـرٍ جَمِيل"
        val stretched = PureArabicKashidaEngine.stretchText(text, 2)
        val words = stretched.split(" ")
        assertEquals(4, words.size)
        // "كُنْ" should be elongated
        assertTrue(words[0].contains("ـ"))
        // "ذا" has no dual-joining letters, should remain "ذَا"
        assertEquals("ذَا", words[1])
        // "أَثَـرٍ" should be elongated
        assertTrue(words[2].contains("ـ"))
        // "جَمِيل" should be elongated
        assertTrue(words[3].contains("ـ"))
    }

    @Test
    fun testZeroStretchReturnsOriginal() {
        val original = "إبداع عربي أصيل"
        val result = PureArabicKashidaEngine.stretchText(original, 0)
        assertEquals(original, result)
    }

    @Test
    fun testNonArabicTextSafe() {
        val text = "Athar Studio 2026! 100%"
        val result = PureArabicKashidaEngine.stretchText(text, 4)
        assertEquals(text, result)
    }

    @Test
    fun testMultiLinePreserved() {
        val text = "أثر يبقى\nوذكرى تدوم"
        val stretched = PureArabicKashidaEngine.stretchText(text, 2)
        val lines = stretched.split("\n")
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("ـ"))
        assertTrue(lines[1].contains("ـ"))
    }

    @Test
    fun testEmojiHandling() {
        val text = "أثر رائع ✨ 🌟"
        val stretched = PureArabicKashidaEngine.stretchText(text, 2)
        assertTrue(stretched.contains("✨"))
        assertTrue(stretched.contains("🌟"))
    }

    @Test
    fun testEmptyAndSingleCharSafe() {
        assertEquals("", PureArabicKashidaEngine.stretchText("", 3))
        assertEquals("أ", PureArabicKashidaEngine.stretchText("أ", 3))
        assertEquals(" ", PureArabicKashidaEngine.stretchText(" ", 3))
    }
}
