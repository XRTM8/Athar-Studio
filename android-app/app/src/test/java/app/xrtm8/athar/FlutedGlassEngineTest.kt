package app.xrtm8.athar

import app.xrtm8.athar.core.AtharFlutedGlassEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.FlutedGlassState
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlutedGlassEngineTest {

    @Test
    fun zeroIntensityLeavesPixelsUnchanged() {
        val w = 64
        val h = 16
        val src = IntArray(w * h) { idx -> 0xFF000000.toInt() or ((idx * 17) and 0xFFFFFF) }
        val out = IntArray(w * h)
        AtharFlutedGlassEngine.applyFlutedGlassToBuffer(src, out, w, h, FlutedGlassState(intensity = 0f))
        assertArrayEquals(src, out)
    }

    @Test
    fun partialCoverageLeavesUncoveredRegionUntouched() {
        val w = 100
        val h = 10
        val src = IntArray(w * h) { idx ->
            val x = idx % w
            0xFF000000.toInt() or ((x * 2) shl 16) or ((x * 2) shl 8) or (x * 2)
        }
        val out = IntArray(w * h)
        val state = FlutedGlassState(
            intensity = 0.8f,
            ribCount = 20,
            specular = 0.6f,
            coverage = 0.5f,
            coverageFromStart = true
        )
        AtharFlutedGlassEngine.applyFlutedGlassToBuffer(src, out, w, h, state)

        // Pixels past coverage (x >= 52) must remain identical to source
        for (y in 0 until h) {
            for (x in 52 until w) {
                assertEquals("Pixel at ($x,$y) should be untouched", src[y * w + x], out[y * w + x])
            }
        }
        // Pixels inside coverage should differ due to refraction/specular
        var anyDiff = false
        for (x in 0 until 48) {
            if (src[x] != out[x]) {
                anyDiff = true
                break
            }
        }
        assertTrue("Refracted region should differ from source", anyDiff)
    }

    @Test
    fun flutedGlassStateRoundTripsThroughSerializer() {
        val original = CanvasState(
            flutedGlassState = FlutedGlassState(
                intensity = 0.72f,
                ribCount = 36,
                specular = 0.8f,
                coverage = 0.6f,
                coverageFromStart = false,
                angleDegrees = 15f,
                chromaticDispersion = 0.45f,
                edgeSoftness = 0.30f
            )
        )
        val json = AtharProjectSerializer.serializeCanvasState(original, hasBackgroundPhoto = true)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)
        assertEquals(original.flutedGlassState.intensity, restored.flutedGlassState.intensity, 0.001f)
        assertEquals(original.flutedGlassState.ribCount, restored.flutedGlassState.ribCount)
        assertEquals(original.flutedGlassState.specular, restored.flutedGlassState.specular, 0.001f)
        assertEquals(original.flutedGlassState.coverage, restored.flutedGlassState.coverage, 0.001f)
        assertFalse(restored.flutedGlassState.coverageFromStart)
        assertEquals(original.flutedGlassState.angleDegrees, restored.flutedGlassState.angleDegrees, 0.001f)
        assertEquals(original.flutedGlassState.chromaticDispersion, restored.flutedGlassState.chromaticDispersion, 0.001f)
        assertEquals(original.flutedGlassState.edgeSoftness, restored.flutedGlassState.edgeSoftness, 0.001f)
    }
}
