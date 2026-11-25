package app.xrtm8.athar

import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
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

    @Test
    fun testBoxWidthRatioSerialization() {
        val state = CanvasState(
            layers = listOf(
                TextLayer(
                    id = "l1",
                    rawText = "نص طويل ممتد للتجربة",
                    boxWidthRatio = 0.65f
                )
            )
        )
        val json = AtharProjectSerializer.serializeCanvasState(state, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)
        assertEquals(0.65f, restored.activeLayer.boxWidthRatio, 0.001f)
    }

    @Test
    fun testV281MasksAndScrimCompatibility() {
        // 1. Legacy v2.7.0 JSON without masks or scrim
        val legacyJson = JSONObject("""
            {
                "version": 2,
                "raw_text": "مشروع قديم",
                "stretch_level": 3
            }
        """.trimIndent())
        val legacyRestored = AtharProjectSerializer.deserializeCanvasState(legacyJson)
        assertFalse(legacyRestored.gradientScrim.isEnabled)
        assertFalse(legacyRestored.maskState.isEnabled)
        assertEquals(1.0f, legacyRestored.maskState.contrast, 0.001f)
        assertFalse(legacyRestored.maskState.isInverted)
        org.junit.Assert.assertNull(legacyRestored.maskState.tintColorLong)

        // 2. Full v2.8.1 state round-trip
        val v281State = CanvasState(
            gradientScrim = app.xrtm8.athar.model.CanvasGradientScrim(
                isEnabled = true,
                direction = app.xrtm8.athar.model.CanvasGradientDirection.LEFT_TO_RIGHT,
                colorLong = 0xFF0A1128L,
                opacity = 0.85f,
                midpoint = 0.45f
            ),
            maskState = app.xrtm8.athar.model.AtharMaskState(
                isEnabled = true,
                activeMaskId = "window_shadow",
                opacity = 0.90f,
                contrast = 1.75f,
                isInverted = true,
                tintColorLong = 0xFFFFD54FL,
                isLuminanceMode = true,
                scale = 1.6f,
                offsetX = -0.2f,
                offsetY = 0.3f,
                rotationDegrees = 90
            )
        )
        val json = AtharProjectSerializer.serializeCanvasState(v281State, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        // Verify Scrim
        assertTrue(restored.gradientScrim.isEnabled)
        assertEquals(app.xrtm8.athar.model.CanvasGradientDirection.LEFT_TO_RIGHT, restored.gradientScrim.direction)
        assertEquals(0xFF0A1128L, restored.gradientScrim.colorLong)
        assertEquals(0.85f, restored.gradientScrim.opacity, 0.001f)
        assertEquals(0.45f, restored.gradientScrim.midpoint, 0.001f)

        // Verify Mask 2.0
        assertTrue(restored.maskState.isEnabled)
        assertEquals("window_shadow", restored.maskState.activeMaskId)
        assertEquals(app.xrtm8.athar.model.ProceduralMaskType.WINDOW_BLINDS, restored.maskState.proceduralType)
        assertEquals(0.90f, restored.maskState.opacity, 0.001f)
        assertEquals(1.75f, restored.maskState.contrast, 0.001f)
        assertTrue(restored.maskState.isInverted)
        assertEquals(0xFFFFD54FL, restored.maskState.tintColorLong)
        assertTrue(restored.maskState.isLuminanceMode)
        assertEquals(1.6f, restored.maskState.scale, 0.001f)
        assertEquals(-0.2f, restored.maskState.offsetX, 0.001f)
        assertEquals(0.3f, restored.maskState.offsetY, 0.001f)
        assertEquals(90, restored.maskState.rotationDegrees)
    }

    @Test
    fun testV211FilmLensMaskFontWeightAndCollageTransformRoundTrip() {
        val state = CanvasState(
            layers = listOf(
                TextLayer(
                    id = "l_alex",
                    rawText = "أثر تصميم",
                    selectedFont = ArabicFontFamily.ALEXANDRIA,
                    fontWeight = 300,
                    stylisticSet = 2,
                    enableSwash = true,
                    enableDiscretionaryLigatures = true,
                    wordSpacing = 8f
                )
            ),
            activeLayerId = "l_alex",
            filmPreset = app.xrtm8.athar.model.FilmPreset.DREAM_GLOW,
            filmPresetIntensity = 0.72f,
            opticalLensState = app.xrtm8.athar.model.OpticalLensState(
                type = app.xrtm8.athar.model.OpticalLensType.HALATION_LEAK,
                intensity = 0.85f,
                spread = 0.60f,
                angleDegrees = 120f
            ),
            maskState = app.xrtm8.athar.model.AtharMaskState(
                isEnabled = true,
                activeMaskId = "golden_hour_beams",
                softness = 0.68f,
                warmth = 0.82f
            ),
            collageState = app.xrtm8.athar.model.CollageState(
                layout = app.xrtm8.athar.model.CollageLayoutType.DIPTYCH_V,
                cells = listOf(
                    app.xrtm8.athar.model.CollageCell(
                        id = "c1",
                        leftNorm = 0f, topNorm = 0f, rightNorm = 0.5f, bottomNorm = 1f,
                        photoRotationDeg = 270,
                        isFlippedH = true,
                        isFlippedV = true
                    )
                )
            )
        )

        val json = AtharProjectSerializer.serializeCanvasState(state, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        assertEquals(300, restored.activeLayer.fontWeight)
        assertEquals(ArabicFontFamily.ALEXANDRIA, restored.activeLayer.selectedFont)
        assertEquals(app.xrtm8.athar.model.FilmPreset.DREAM_GLOW, restored.filmPreset)
        assertEquals(0.72f, restored.filmPresetIntensity, 0.001f)
        assertEquals(app.xrtm8.athar.model.OpticalLensType.HALATION_LEAK, restored.opticalLensState.type)
        assertEquals(0.85f, restored.opticalLensState.intensity, 0.001f)
        assertEquals(0.60f, restored.opticalLensState.spread, 0.001f)
        assertEquals(120f, restored.opticalLensState.angleDegrees, 0.001f)
        assertEquals(0.68f, restored.maskState.softness, 0.001f)
        assertEquals(0.82f, restored.maskState.warmth, 0.001f)
        assertEquals(270, restored.collageState.cells.first().photoRotationDeg)
        assertTrue(restored.collageState.cells.first().isFlippedH)
        assertTrue(restored.collageState.cells.first().isFlippedV)

        // Safe fallback on unknown enum / legacy root font_weight
        val legacyWithUnknown = JSONObject("""{"raw_text":"اختبار","font_weight":700,"lens_type":"FUTURE_UNKNOWN_LENS"}""")
        val fallback = AtharProjectSerializer.deserializeCanvasState(legacyWithUnknown)
        assertEquals(700, fallback.activeLayer.fontWeight)
        assertEquals(app.xrtm8.athar.model.OpticalLensType.NONE, fallback.opticalLensState.type)
        assertEquals(1.0f, fallback.filmPresetIntensity, 0.001f)
        assertEquals(0.25f, fallback.maskState.softness, 0.001f)
        assertEquals(0.35f, fallback.maskState.warmth, 0.001f)
    }
}
