package app.xrtm8.athar

import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.model.KashidaMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PureArabicKashidaExtendedTest {

    @Test
    fun testLamAlefLigatureNeverStretched() {
        // In Arabic typography, Lam-Alef (لا / لإ / لأ / لآ / لٱ) is an indivisible ligature.
        // It must NEVER be split by a Tatweel/Kashida.
        val text1 = "لا"
        val stretched1 = PureArabicKashidaEngine.stretchText(text1, 4, KashidaMode.ALL)
        assertEquals("لا", stretched1)

        val text2 = "الإسلام"
        val opps = PureArabicKashidaEngine.findOpportunities(text2)
        // Ensure no opportunity has baseChar == 'ل' and nextChar in ('ا', 'إ', 'أ', 'آ', 'ٱ')
        for (opp in opps) {
            val isLamAlef = opp.baseChar == 'ل' && opp.nextChar in setOf('ا', 'إ', 'أ', 'آ', 'ٱ')
            assertFalse("Lam-Alef ligature must not be stretched", isLamAlef)
        }
    }

    @Test
    fun testTeethModeElongation() {
        // Seen and Sheen have teeth that calligraphers traditionally stretch.
        val text = "شَمْس"
        val stretched = PureArabicKashidaEngine.stretchText(text, 3, KashidaMode.TEETH)
        // Must stretch after Sheen
        assertTrue(stretched.startsWith("شَـــم"))
    }

    @Test
    fun testPreFinalModeElongation() {
        // In PRE_FINAL mode, the letter immediately preceding the final letter is stretched.
        val text = "أثر جميل"
        val stretched = PureArabicKashidaEngine.stretchText(text, 2, KashidaMode.PRE_FINAL)
        assertTrue(stretched.contains("أثــر"))
        assertTrue(stretched.contains("جميــل"))
    }

    @Test
    fun testInitialModeElongation() {
        // In INITIAL mode, the first letter connection is stretched.
        val text = "جميل"
        val stretched = PureArabicKashidaEngine.stretchText(text, 2, KashidaMode.INITIAL)
        assertTrue(stretched.startsWith("جــم"))
    }

    @Test
    fun testMedialModeElongation() {
        // In MEDIAL mode, connections in the middle of words are targeted.
        val text = "مستقبل"
        val stretched = PureArabicKashidaEngine.stretchText(text, 2, KashidaMode.MEDIAL)
        assertTrue(stretched.contains("ـ"))
    }

    @Test
    fun testMultilinePreservation() {
        val multiline = "أثر\nجميل\nيبقى"
        val stretched = PureArabicKashidaEngine.stretchText(multiline, 2, KashidaMode.SMART)
        val lines = stretched.split("\n")
        assertEquals(3, lines.size)
        assertEquals("أثــر", lines[0])
        assertTrue(lines[1].contains("ـ"))
        assertTrue(lines[2].contains("ـ"))
    }

    @Test
    fun testSingleLetterAndEmptyEdgeCases() {
        assertEquals("", PureArabicKashidaEngine.stretchText("", 4))
        assertEquals("و", PureArabicKashidaEngine.stretchText("و", 4))
        assertEquals("ب", PureArabicKashidaEngine.stretchText("ب", 4))
        assertEquals("123", PureArabicKashidaEngine.stretchText("123", 4))
    }
}
