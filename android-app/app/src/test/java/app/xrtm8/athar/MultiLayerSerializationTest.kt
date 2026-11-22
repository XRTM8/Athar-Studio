package app.xrtm8.athar

import app.xrtm8.athar.model.*
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiLayerSerializationTest {

    @Test
    fun testMultiLayerRoundTrip() {
        val layer1 = TextLayer(
            id = "layer_title",
            name = "العنوان",
            rawText = "أَثَرٌ يَبْقَى",
            displayText = "أَثَـــرٌ يَبْقَى",
            fontSize = 72f,
            selectedFont = ArabicFontFamily.GULZAR,
            selectedEffect = VisualEffect.ROYAL_GOLD,
            normOffsetX = 0f,
            normOffsetY = -0.2f
        )
        val layer2 = TextLayer(
            id = "layer_subtitle",
            name = "العنوان الفرعي",
            rawText = "تصميم عربي أصيل",
            displayText = "تصميم عربي أصيل",
            fontSize = 28f,
            selectedFont = ArabicFontFamily.LALEZAR,
            selectedEffect = VisualEffect.MINIMAL_LUXURY,
            normOffsetX = 0f,
            normOffsetY = 0.15f
        )
        val originalState = CanvasState(
            layers = listOf(layer1, layer2),
            activeLayerId = "layer_subtitle",
            filmPreset = FilmPreset.FUJI_CLASSIC_CHROME,
            gradientScrim = CanvasGradientScrim(
                isEnabled = true,
                direction = CanvasGradientDirection.BOTTOM_TO_TOP,
                opacity = 0.8f
            ),
            bgPhotoOffsetX = 0.12f,
            bgPhotoOffsetY = -0.05f,
            bgPhotoScale = 1.35f
        )

        val json = AtharProjectSerializer.serializeCanvasState(originalState, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        assertEquals(2, restored.layers.size)
        assertEquals("layer_subtitle", restored.activeLayerId)
        assertEquals(FilmPreset.FUJI_CLASSIC_CHROME, restored.filmPreset)
        assertTrue(restored.gradientScrim.isEnabled)
        assertEquals(CanvasGradientDirection.BOTTOM_TO_TOP, restored.gradientScrim.direction)
        assertEquals(0.8f, restored.gradientScrim.opacity, 0.001f)
        assertEquals(0.12f, restored.bgPhotoOffsetX, 0.001f)
        assertEquals(-0.05f, restored.bgPhotoOffsetY, 0.001f)
        assertEquals(1.35f, restored.bgPhotoScale, 0.001f)

        // Check individual layers
        val r1 = restored.layers.find { it.id == "layer_title" }
        assertNotNull(r1)
        assertEquals("أَثَرٌ يَبْقَى", r1!!.rawText)
        assertEquals(ArabicFontFamily.GULZAR, r1.selectedFont)
        assertEquals(72f, r1.fontSize, 0.001f)

        val r2 = restored.layers.find { it.id == "layer_subtitle" }
        assertNotNull(r2)
        assertEquals("تصميم عربي أصيل", r2!!.rawText)
        assertEquals(ArabicFontFamily.LALEZAR, r2.selectedFont)
    }

    @Test
    fun testLegacySingleLayerDeserialization() {
        val legacyJson = org.json.JSONObject().apply {
            put("raw_text", "تراث قديم")
            put("selected_font", "AMIRI")
            put("font_size", 42.0)
            put("stretch_level", 2)
        }

        val restored = AtharProjectSerializer.deserializeCanvasState(legacyJson)
        assertEquals(1, restored.layers.size)
        assertEquals("تراث قديم", restored.rawText)
        assertEquals(ArabicFontFamily.AMIRI, restored.selectedFont)
        assertEquals(42f, restored.fontSize, 0.001f)
    }

    @Test
    fun testCustomFontSerializationRoundTrip() {
        val customLayer = TextLayer(
            id = "layer_custom",
            name = "نص بخط مخصص",
            rawText = "خط الرقعة الحديث",
            customFontPath = "/data/user/0/app.xrtm8.athar/files/custom_fonts/my_ruqaa.ttf",
            customFontName = "my_ruqaa"
        )
        val state = CanvasState(layers = listOf(customLayer), activeLayerId = "layer_custom")
        val json = AtharProjectSerializer.serializeCanvasState(state, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        val layer = restored.layers.first()
        assertEquals("/data/user/0/app.xrtm8.athar/files/custom_fonts/my_ruqaa.ttf", layer.customFontPath)
        assertEquals("my_ruqaa", layer.customFontName)
        assertEquals(layer.customFontPath, restored.customFontPath)
    }
}
