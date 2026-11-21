package app.xrtm8.athar

import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.CinematicGlowState
import app.xrtm8.athar.model.FilmGrainState
import app.xrtm8.athar.model.GlowStyle
import app.xrtm8.athar.model.PatternBlend
import app.xrtm8.athar.model.PatternState
import app.xrtm8.athar.model.PatternType
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.*
import org.junit.Test

class CinematicEffectsAndPatternTest {

    @Test
    fun testPatternStateDefaultsAndEnablement() {
        val defaultPattern = PatternState()
        assertEquals(PatternType.NONE, defaultPattern.type)
        assertFalse(defaultPattern.isEnabled)
        assertEquals(0.30f, defaultPattern.opacity, 0.001f)
        assertEquals(1.0f, defaultPattern.scale, 0.001f)
        assertEquals(PatternBlend.OVERLAY, defaultPattern.blendMode)
        assertTrue(defaultPattern.isLightTint)

        val activeScanlines = defaultPattern.copy(type = PatternType.SCANLINES, opacity = 0.5f)
        assertTrue(activeScanlines.isEnabled)
    }

    @Test
    fun testCinematicGlowStateDefaultsAndEnablement() {
        val defaultGlow = CinematicGlowState()
        assertEquals(GlowStyle.MOTION, defaultGlow.style)
        assertEquals(0f, defaultGlow.glow, 0.001f)
        assertEquals(70f, defaultGlow.range, 0.001f)
        assertEquals(65f, defaultGlow.spread, 0.001f)
        assertEquals(70f, defaultGlow.warmth, 0.001f)
        assertFalse(defaultGlow.isEnabled)
        assertTrue(defaultGlow.isDefault)

        val activeGlow = defaultGlow.copy(glow = 31f)
        assertTrue(activeGlow.isEnabled)
        assertFalse(activeGlow.isDefault)
    }

    @Test
    fun testFilmGrainStateDefaultsAndEnablement() {
        val defaultGrain = FilmGrainState()
        assertEquals(0f, defaultGrain.grain, 0.001f)
        assertEquals(47f, defaultGrain.size, 0.001f)
        assertEquals(50f, defaultGrain.roughness, 0.001f)
        assertFalse(defaultGrain.isEnabled)
        assertTrue(defaultGrain.isDefault)

        val activeGrain = defaultGrain.copy(grain = 40f)
        assertTrue(activeGrain.isEnabled)
        assertFalse(activeGrain.isDefault)
    }

    @Test
    fun testFullProjectSerializationAndRoundTrip() {
        val originalState = CanvasState(
            patternState = PatternState(
                type = PatternType.SCANLINES,
                opacity = 0.45f,
                scale = 1.8f,
                blendMode = PatternBlend.SOFT_LIGHT,
                isLightTint = false
            ),
            cinematicGlowState = CinematicGlowState(
                style = GlowStyle.MOTION,
                glow = 31f,
                range = 70f,
                spread = 65f,
                warmth = 70f
            ),
            filmGrainState = FilmGrainState(
                grain = 40f,
                size = 47f,
                roughness = 50f
            ),
            collageState = app.xrtm8.athar.model.CollageState(
                layout = app.xrtm8.athar.model.CollageLayoutType.DIPTYCH_V,
                cells = app.xrtm8.athar.core.CollageLayoutCalculator.generateCellsForLayout(app.xrtm8.athar.model.CollageLayoutType.DIPTYCH_V),
                spacingDp = 12f,
                cornerRadiusDp = 16f,
                frameColorLong = 0xFF2A080FL
            )
        )

        val json = AtharProjectSerializer.serializeCanvasState(originalState, hasBackgroundPhoto = true)
        val deserialized = AtharProjectSerializer.deserializeCanvasState(json, backgroundUri = null)

        // Verify Pattern
        assertEquals(PatternType.SCANLINES, deserialized.patternState.type)
        assertEquals(0.45f, deserialized.patternState.opacity, 0.01f)
        assertEquals(1.8f, deserialized.patternState.scale, 0.01f)
        assertEquals(PatternBlend.SOFT_LIGHT, deserialized.patternState.blendMode)
        assertFalse(deserialized.patternState.isLightTint)

        // Verify Cinematic Glow
        assertEquals(GlowStyle.MOTION, deserialized.cinematicGlowState.style)
        assertEquals(31f, deserialized.cinematicGlowState.glow, 0.01f)
        assertEquals(70f, deserialized.cinematicGlowState.range, 0.01f)
        assertEquals(65f, deserialized.cinematicGlowState.spread, 0.01f)
        assertEquals(70f, deserialized.cinematicGlowState.warmth, 0.01f)
        assertTrue(deserialized.cinematicGlowState.isEnabled)

        // Verify Film Grain
        assertEquals(40f, deserialized.filmGrainState.grain, 0.01f)
        assertEquals(47f, deserialized.filmGrainState.size, 0.01f)
        assertEquals(50f, deserialized.filmGrainState.roughness, 0.01f)
        assertTrue(deserialized.filmGrainState.isEnabled)

        // Verify Collage
        assertEquals(app.xrtm8.athar.model.CollageLayoutType.DIPTYCH_V, deserialized.collageState.layout)
        assertEquals(2, deserialized.collageState.cells.size)
        assertEquals(12f, deserialized.collageState.spacingDp, 0.01f)
        assertEquals(16f, deserialized.collageState.cornerRadiusDp, 0.01f)
        assertEquals(0xFF2A080FL, deserialized.collageState.frameColorLong)
        assertTrue(deserialized.collageState.isEnabled)
    }
}
