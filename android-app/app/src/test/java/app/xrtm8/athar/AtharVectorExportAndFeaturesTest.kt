package app.xrtm8.athar

import app.xrtm8.athar.export.AtharSvgExporter
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.project.AtharProjectSerializer
import app.xrtm8.athar.ui.dialogs.DiacriticCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Vector SVG Exporter, A4 print ratio, smart refit math, and diacritics palette.
 */
class AtharVectorExportAndFeaturesTest {

    @Test
    fun testSvgGenerationValidStructure() {
        val state = CanvasState(
            aspectRatio = CanvasAspectRatio.STORY_9_16,
            solidBgColorLong = 0xFF12141A,
            layers = listOf(
                TextLayer(
                    id = "layer_1",
                    rawText = "أثر للإبداع والتصميم",
                    displayText = "أَثَـرٌ لِلإِبْدَاعِ وَالتَّصْمِيمِ",
                    fontSize = 64f,
                    scaleFactor = 1.2f,
                    rotationAngle = 15f,
                    normOffsetX = 0.05f,
                    normOffsetY = -0.1f,
                    textAlign = TextAlignOption.CENTER,
                    shadowRadius = 12f,
                    shadowDx = 0f,
                    shadowDy = 4f,
                    shadowColorLong = 0x88000000,
                    customTextColorLong = 0xFFE5C07B
                )
            )
        )

        val svg = AtharSvgExporter.generateSvgString(state)
        assertNotNull(svg)
        assertTrue(svg.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(svg.contains("""<svg xmlns="http://www.w3.org/2000/svg""""))
        assertTrue(svg.contains("""viewBox="0 0 1080 1920""""))
        assertTrue(svg.contains("""<rect width="100%" height="100%" fill="#12141a" />"""))
        assertTrue(svg.contains("""<filter id="shadow_layer_1""""))
        assertTrue(svg.contains("""<feDropShadow"""))
        assertTrue(svg.contains("""direction="rtl""""))
        assertTrue(svg.contains("""text-anchor="middle""""))
        assertTrue(svg.contains("أَثَـرٌ لِلإِبْدَاعِ وَالتَّصْمِيمِ"))
        assertTrue(svg.endsWith("</svg>\n") || svg.endsWith("</svg>"))
    }

    @Test
    fun testSvgXmlSpecialCharacterEscaping() {
        val state = CanvasState(
            layers = listOf(
                TextLayer(
                    rawText = "نص مع علامات & < > ' \" خاصة",
                    displayText = "نص مع علامات & < > ' \" خاصة"
                )
            )
        )

        val svg = AtharSvgExporter.generateSvgString(state)
        assertTrue("Ampersand should be escaped", svg.contains("&amp;"))
        assertTrue("Less-than should be escaped", svg.contains("&lt;"))
        assertTrue("Greater-than should be escaped", svg.contains("&gt;"))
        assertTrue("Double quotes should be escaped", svg.contains("&quot;"))
        assertTrue("Single quotes should be escaped", svg.contains("&apos;"))
    }

    @Test
    fun testA4AspectRatioDimensions() {
        val a4 = CanvasAspectRatio.PRINT_A4
        assertEquals("مستند A4 (طباعة)", a4.label)
        assertNotNull(a4.ratio)
        val expectedHeight = (1080f / a4.ratio!!).toInt()
        assertEquals(1527, expectedHeight)

        val state = CanvasState(aspectRatio = a4)
        val svg = AtharSvgExporter.generateSvgString(state)
        assertTrue(svg.contains("""viewBox="0 0 1080 1527""""))
    }

    @Test
    fun testA4SerializationRoundtrip() {
        val state = CanvasState(
            aspectRatio = CanvasAspectRatio.PRINT_A4,
            layers = listOf(TextLayer(rawText = "مشروع طباعة A4"))
        )
        val json = AtharProjectSerializer.serializeCanvasState(state, false)
        assertTrue(json.toString().contains("PRINT_A4"))

        val restored = AtharProjectSerializer.deserializeCanvasState(json)
        assertEquals(CanvasAspectRatio.PRINT_A4, restored.aspectRatio)
        assertEquals("مشروع طباعة A4", restored.layers.first().rawText)
    }

    @Test
    fun testSmartRefitViewportHeightFactor() {
        val oldRatio = CanvasAspectRatio.STORY_9_16 // ratio 9/16 = 0.5625 -> height 1920
        val newRatio = CanvasAspectRatio.LANDSCAPE_16_9 // ratio 16/9 = 1.7778 -> height 607.5

        val oldH = 1080f / oldRatio.ratio!!
        val newH = 1080f / newRatio.ratio!!
        val factor = newH / oldH

        // factor = 607.5 / 1920 = ~0.316
        assertTrue("Landscape height factor should be less than 0.5", factor < 0.5f)

        // Initial layer placed at normOffsetY = -0.35 (near top of 9:16 screen)
        val initialNormY = -0.35f
        val adaptedNormY = (initialNormY * factor).coerceIn(-0.42f, 0.42f)
        // In landscape, normY is scaled inward so it doesn't get clipped off-screen
        assertTrue(adaptedNormY > -0.35f)
        assertTrue(adaptedNormY > -0.15f)
    }

    @Test
    fun testDiacriticCategoriesIntegrity() {
        val categories = DiacriticCategory.entries
        assertEquals(4, categories.size)
        assertTrue(categories.contains(DiacriticCategory.TASHKEEL))
        assertTrue(categories.contains(DiacriticCategory.QURANIC))
        assertTrue(categories.contains(DiacriticCategory.WAQF))
        assertTrue(categories.contains(DiacriticCategory.ORNAMENTS))
    }
}
