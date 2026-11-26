package app.xrtm8.athar

import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.CustomGradientState
import app.xrtm8.athar.model.CustomGradientStop
import app.xrtm8.athar.model.PatternState
import app.xrtm8.athar.model.PatternType
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverhaulProFeaturesTest {

    @Test
    fun customGradientOuterGlowAndPatternRoundTrip() {
        val grad = CustomGradientState(
            isEnabled = true,
            stops = listOf(
                CustomGradientStop(0xFFE5C07BL, 0f),
                CustomGradientStop(0xFF9E1B32L, 0.5f),
                CustomGradientStop(0xFF1E3A5FL, 1f)
            ),
            angle = 135f,
            isRadial = true
        )
        val layer = TextLayer(
            id = "l_pro",
            rawText = "أثر برو",
            displayText = "أثر برو",
            customGradient = grad,
            outerGlowRadius = 24f,
            outerGlowColorLong = 0xFF38BDF8L,
            glassBackdropPaddingV = 14f
        )
        val state = CanvasState(
            layers = listOf(layer),
            patternState = PatternState(
                type = PatternType.MICRO_GRID,
                opacity = 0.45f,
                scale = 1.5f,
                customColorLong = 0xFFE5C07BL,
                rotationDegrees = 35f
            )
        )
        val json = AtharProjectSerializer.serializeCanvasState(state, hasBackgroundPhoto = false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)
        val rLayer = restored.layers.first()
        assertTrue(rLayer.customGradient.isValid)
        assertEquals(3, rLayer.customGradient.stops.size)
        assertEquals(135f, rLayer.customGradient.angle, 0.001f)
        assertTrue(rLayer.customGradient.isRadial)
        assertEquals(24f, rLayer.outerGlowRadius, 0.001f)
        assertEquals(0xFF38BDF8L, rLayer.outerGlowColorLong)
        assertEquals(14f, rLayer.glassBackdropPaddingV ?: 0f, 0.001f)
        assertEquals(0xFFE5C07BL, restored.patternState.customColorLong)
        assertEquals(35f, restored.patternState.rotationDegrees, 0.001f)
    }
}
