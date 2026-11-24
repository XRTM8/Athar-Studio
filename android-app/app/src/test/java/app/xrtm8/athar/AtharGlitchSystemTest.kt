package app.xrtm8.athar

import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.GlitchEffectState
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Glitch, RGB Split & Analog CRT Engine state and serialization.
 */
class AtharGlitchSystemTest {

    @Test
    fun testGlitchEffectStateDefaults() {
        val defaultState = GlitchEffectState()
        assertFalse("Default glitch state should not be enabled", defaultState.isEnabled)
        assertTrue("Default glitch state should report isDefault", defaultState.isDefault)
        assertEquals(0f, defaultState.rgbSplit, 0.001f)
        assertEquals(0f, defaultState.sliceJitter, 0.001f)
        assertEquals(0f, defaultState.scanlines, 0.001f)
        assertEquals(0f, defaultState.noise, 0.001f)
        assertEquals(0f, defaultState.pixelate, 0.001f)
        assertEquals(1337L, defaultState.seed)
    }

    @Test
    fun testGlitchEffectStateActivation() {
        val activeRgb = GlitchEffectState(rgbSplit = 12f)
        assertTrue("State with rgbSplit > 0.5f should be enabled", activeRgb.isEnabled)
        assertFalse(activeRgb.isDefault)

        val activeJitter = GlitchEffectState(sliceJitter = 50f)
        assertTrue("State with sliceJitter > 0.5f should be enabled", activeJitter.isEnabled)

        val activeScanlines = GlitchEffectState(scanlines = 30f)
        assertTrue("State with scanlines > 0.5f should be enabled", activeScanlines.isEnabled)

        val activeNoise = GlitchEffectState(noise = 25f)
        assertTrue("State with noise > 0.5f should be enabled", activeNoise.isEnabled)

        val activePixelate = GlitchEffectState(pixelate = 15f)
        assertTrue("State with pixelate > 0.5f should be enabled", activePixelate.isEnabled)
    }

    @Test
    fun testGlitchSerializationRoundtrip() {
        val originalGlitch = GlitchEffectState(
            rgbSplit = 42.5f,
            sliceJitter = 65.0f,
            scanlines = 80.0f,
            noise = 20.0f,
            pixelate = 15.0f,
            seed = 777888L
        )

        val canvas = CanvasState(
            glitchEffectState = originalGlitch
        )

        val json = AtharProjectSerializer.serializeCanvasState(canvas, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        val restoredGlitch = restored.glitchEffectState
        assertTrue("Restored glitch state should be enabled", restoredGlitch.isEnabled)
        assertEquals(42.5f, restoredGlitch.rgbSplit, 0.01f)
        assertEquals(65.0f, restoredGlitch.sliceJitter, 0.01f)
        assertEquals(80.0f, restoredGlitch.scanlines, 0.01f)
        assertEquals(20.0f, restoredGlitch.noise, 0.01f)
        assertEquals(15.0f, restoredGlitch.pixelate, 0.01f)
        assertEquals(777888L, restoredGlitch.seed)
    }

    @Test
    fun testGlitchBackwardCompatibilityWithLegacyProjects() {
        // Legacy JSON without any glitch fields
        val legacyJson = org.json.JSONObject("""
            {
                "canvas": {
                    "aspect_ratio": "STORY_9_16",
                    "background_dim": 0.25,
                    "film_grain": 0.0
                },
                "layers": []
            }
        """.trimIndent())

        val restored = AtharProjectSerializer.deserializeCanvasState(legacyJson)
        val glitch = restored.glitchEffectState
        assertFalse("Legacy project should have disabled glitch state by default", glitch.isEnabled)
        assertTrue(glitch.isDefault)
        assertEquals(1337L, glitch.seed)
    }

    @Test
    fun testOpenTypeFieldsSerializationRoundtrip() {
        val textLayer = app.xrtm8.athar.model.TextLayer(
            rawText = "خط عربي أصيل",
            displayText = "خط عربي أصيل",
            stylisticSet = 3,
            enableSwash = true,
            enableDiscretionaryLigatures = true,
            wordSpacing = 14.5f
        )
        val canvas = CanvasState(
            layers = listOf(textLayer),
            activeLayerId = textLayer.id
        )

        val json = AtharProjectSerializer.serializeCanvasState(canvas, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        val restoredLayer = restored.activeLayer
        assertEquals(3, restoredLayer.stylisticSet)
        assertTrue(restoredLayer.enableSwash)
        assertTrue(restoredLayer.enableDiscretionaryLigatures)
        assertEquals(14.5f, restoredLayer.wordSpacing, 0.01f)
    }
}
