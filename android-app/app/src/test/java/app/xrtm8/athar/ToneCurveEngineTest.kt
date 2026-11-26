package app.xrtm8.athar

import app.xrtm8.athar.core.AtharToneCurveEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ToneCurveState
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneCurveEngineTest {

    @Test
    fun defaultToneCurveLeavesBufferUnchanged() {
        val src = intArrayOf(0xFF102030.toInt(), 0xFF808080.toInt(), 0xFFF0E0D0.toInt())
        val out = IntArray(src.size)
        AtharToneCurveEngine.applyToBuffer(src, out, ToneCurveState())
        assertArrayEquals(src, out)
    }

    @Test
    fun toneCurveShapesShadowsAndHighlights() {
        val src = intArrayOf(0xFF404040.toInt(), 0xFFC0C0C0.toInt())
        val out = IntArray(src.size)
        val state = ToneCurveState(shadows = -0.3f, highlights = 0.3f)
        AtharToneCurveEngine.applyToBuffer(src, out, state)
        val shadowLuma = (out[0] ushr 16) and 0xFF
        val highlightLuma = (out[1] ushr 16) and 0xFF
        assertTrue("Shadows should be darkened", shadowLuma < 0x40)
        assertTrue("Highlights should be lifted", highlightLuma > 0xC0)
    }

    @Test
    fun toneCurveRoundTripsThroughSerializer() {
        val original = CanvasState(
            toneCurveState = ToneCurveState(
                blacks = 0.12f,
                shadows = -0.18f,
                highlights = 0.24f,
                whites = -0.08f,
                splitShadowHue = 210f,
                splitShadowSat = 0.25f,
                splitHighlightHue = 45f,
                splitHighlightSat = 0.30f
            )
        )
        val json = AtharProjectSerializer.serializeCanvasState(original, hasBackgroundPhoto = false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)
        assertEquals(0.12f, restored.toneCurveState.blacks, 0.001f)
        assertEquals(-0.18f, restored.toneCurveState.shadows, 0.001f)
        assertEquals(0.24f, restored.toneCurveState.highlights, 0.001f)
        assertEquals(-0.08f, restored.toneCurveState.whites, 0.001f)
        assertEquals(210f, restored.toneCurveState.splitShadowHue, 0.001f)
        assertEquals(0.25f, restored.toneCurveState.splitShadowSat, 0.001f)
        assertEquals(45f, restored.toneCurveState.splitHighlightHue, 0.001f)
        assertEquals(0.30f, restored.toneCurveState.splitHighlightSat, 0.001f)
    }
}
