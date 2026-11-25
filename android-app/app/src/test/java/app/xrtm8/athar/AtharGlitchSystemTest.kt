package app.xrtm8.athar

import app.xrtm8.athar.core.AtharGlitchEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.GlitchEffectState
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Glitch, RGB Split & Analog CRT Engine state, serialization, and buffer math.
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
    fun testGlitchScanlineNormalizationAndAlphaPreservation() {
        // Verify alpha=0 stays 0 and rgbSplit doesn't darken opaque edges near transparent gaps
        val w = 32
        val h1 = 108
        val h2 = 216
        val src1 = IntArray(w * h1) { idx ->
            val x = idx % w
            if (x < 4) 0x00000000 else 0xFFC8C8C8.toInt()
        }
        val out1 = IntArray(src1.size)
        val state = GlitchEffectState(rgbSplit = 60f, scanlines = 80f)
        AtharGlitchEngine.applyGlitchToBuffer(src1, out1, w, h1, state)

        for (y in 0 until h1) {
            assertEquals("Transparent pixel must stay transparent", 0, out1[y * w])
            val opaqueEdge = out1[y * w + 4]
            assertEquals(0xFF, (opaqueEdge ushr 24) and 0xFF)
            assertTrue("Edge pixel should not pull black from transparent gap", ((opaqueEdge shr 16) and 0xFF) > 50)
        }

        // Verify normalized scanline dark-row ratio matches between h1 and h2 (2x scale)
        val src2 = IntArray(w * h2) { 0xFFC8C8C8.toInt() }
        val out2 = IntArray(src2.size)
        AtharGlitchEngine.applyGlitchToBuffer(src2, out2, w, h2, GlitchEffectState(scanlines = 100f))

        val darkRatio1 = (0 until h1).count { ((out1[it * w + 16] shr 16) and 0xFF) < 180 }.toFloat() / h1
        val darkRatio2 = (0 until h2).count { ((out2[it * w + 16] shr 16) and 0xFF) < 180 }.toFloat() / h2
        assertEquals("Scanline density must remain invariant across resolutions", darkRatio1, darkRatio2, 0.05f)
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
