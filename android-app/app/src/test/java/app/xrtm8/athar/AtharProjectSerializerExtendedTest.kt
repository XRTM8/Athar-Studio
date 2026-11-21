package app.xrtm8.athar

import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.project.AtharProjectSerializer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtharProjectSerializerExtendedTest {

    @Test
    fun testDefaultStateRoundTrip() {
        val defaultState = CanvasState()
        val json = AtharProjectSerializer.serializeCanvasState(defaultState, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        assertEquals(defaultState.rawText, restored.rawText)
        assertEquals(defaultState.displayText, restored.displayText)
        assertEquals(defaultState.stretchLevel, restored.stretchLevel)
        assertEquals(defaultState.fontSize, restored.fontSize, 0.001f)
        assertEquals(defaultState.isTransparentBg, restored.isTransparentBg)
    }

    @Test
    fun testEmptyJsonFallbackToSafeDefaults() {
        val emptyJson = JSONObject()
        val fallback = AtharProjectSerializer.deserializeCanvasState(emptyJson)
        assertNotNull(fallback)
        assertEquals("أَثَــر", fallback.rawText)
        assertEquals(3, fallback.stretchLevel)
    }

    @Test
    fun testBackwardCompatibilityMissingFields() {
        // Simulates an older project json without glass backdrop or photo adjustments
        val legacyJsonStr = """
            {
                "version": 1,
                "raw_text": "تراث قديم",
                "stretch_level": 2,
                "font_size": 48.0,
                "selected_font": "AMIRI",
                "selected_effect": "LIQUID_CHROME",
                "is_transparent_bg": true
            }
        """.trimIndent()

        val legacyJson = JSONObject(legacyJsonStr)
        val restored = AtharProjectSerializer.deserializeCanvasState(legacyJson)
        assertEquals("تراث قديم", restored.rawText)
        assertEquals(2, restored.stretchLevel)
        assertEquals(48f, restored.fontSize, 0.001f)
        assertEquals(ArabicFontFamily.AMIRI, restored.selectedFont)
        assertEquals(VisualEffect.LIQUID_CHROME, restored.selectedEffect)
        assertTrue(restored.isTransparentBg)
        // Ensure unpopulated fields safely get defaults
        assertFalse(restored.enableGlassBackdrop)
        assertEquals(0f, restored.backgroundBlur, 0.001f)
    }

    @Test
    fun testAllKashidaModesSerialization() {
        for (mode in KashidaMode.entries) {
            val state = CanvasState(rawText = "اختبار", kashidaMode = mode)
            val json = AtharProjectSerializer.serializeCanvasState(state, false)
            val restored = AtharProjectSerializer.deserializeCanvasState(json)
            assertEquals("Kashida mode $mode must persist correctly", mode, restored.kashidaMode)
        }
    }

    @Test
    fun testAllVisualEffectsSerialization() {
        for (effect in VisualEffect.entries) {
            val state = CanvasState(rawText = "أثر", selectedEffect = effect)
            val json = AtharProjectSerializer.serializeCanvasState(state, false)
            val restored = AtharProjectSerializer.deserializeCanvasState(json)
            assertEquals("VisualEffect $effect must persist correctly", effect, restored.selectedEffect)
        }
    }

    @Test
    fun testAllAspectRatiosSerialization() {
        for (ratio in CanvasAspectRatio.entries) {
            val state = CanvasState(rawText = "أبعاد", aspectRatio = ratio)
            val json = AtharProjectSerializer.serializeCanvasState(state, false)
            val restored = AtharProjectSerializer.deserializeCanvasState(json)
            assertEquals("Aspect ratio $ratio must persist correctly", ratio, restored.aspectRatio)
        }
    }

    @Test
    fun testFullPhotoAdjustmentsAndGlassBackdropIntegrity() {
        val state = CanvasState(
            rawText = "تصميم فوتوغرافي متقدم",
            displayText = "تصميم فوتوغرافي متقدم",
            backgroundDim = 0.65f,
            backgroundBlur = 24.5f,
            backgroundBrightness = -0.15f,
            backgroundContrast = 1.45f,
            backgroundSaturation = 0.85f,
            backgroundVignette = 0.75f,
            enableGlassBackdrop = true,
            glassBackdropOpacity = 0.88f,
            glassBackdropColorLong = 0xEE121824L,
            glassBackdropBorderColorLong = 0xAAFFD700L,
            glassBackdropCornerRadius = 32f,
            glassBackdropPadding = 36f,
            textAlign = TextAlignOption.LEFT,
            lineSpacingMultiplier = 1.8f,
            letterSpacing = 0.12f,
            rotationAngle = -45f,
            scaleFactor = 2.5f,
            normOffsetX = -0.35f,
            normOffsetY = 0.45f
        )

        val json = AtharProjectSerializer.serializeCanvasState(state, hasBackgroundPhoto = true)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        assertEquals(state.backgroundDim, restored.backgroundDim, 0.001f)
        assertEquals(state.backgroundBlur, restored.backgroundBlur, 0.001f)
        assertEquals(state.backgroundBrightness, restored.backgroundBrightness, 0.001f)
        assertEquals(state.backgroundContrast, restored.backgroundContrast, 0.001f)
        assertEquals(state.backgroundSaturation, restored.backgroundSaturation, 0.001f)
        assertEquals(state.backgroundVignette, restored.backgroundVignette, 0.001f)

        assertTrue(restored.enableGlassBackdrop)
        assertEquals(state.glassBackdropOpacity, restored.glassBackdropOpacity, 0.001f)
        assertEquals(state.glassBackdropColorLong, restored.glassBackdropColorLong)
        assertEquals(state.glassBackdropBorderColorLong, restored.glassBackdropBorderColorLong)
        assertEquals(state.glassBackdropCornerRadius, restored.glassBackdropCornerRadius, 0.001f)
        assertEquals(state.glassBackdropPadding, restored.glassBackdropPadding, 0.001f)

        assertEquals(TextAlignOption.LEFT, restored.textAlign)
        assertEquals(state.rotationAngle, restored.rotationAngle, 0.001f)
        assertEquals(state.scaleFactor, restored.scaleFactor, 0.001f)
        assertEquals(state.normOffsetX, restored.normOffsetX, 0.001f)
        assertEquals(state.normOffsetY, restored.normOffsetY, 0.001f)
    }
}
