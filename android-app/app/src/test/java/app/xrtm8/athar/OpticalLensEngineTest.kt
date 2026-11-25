package app.xrtm8.athar

import app.xrtm8.athar.core.AtharFilmGrainEngine
import app.xrtm8.athar.core.AtharOpticalLensEngine
import app.xrtm8.athar.core.FilmPresetEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.FilmGrainState
import app.xrtm8.athar.model.FilmPreset
import app.xrtm8.athar.model.OpticalLensState
import app.xrtm8.athar.model.OpticalLensType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pure-JVM unit tests for FilmPresetEngine v2 (3D LUT), AtharOpticalLensEngine,
 * and AtharFilmGrainEngine v2.
 */
class OpticalLensEngineTest {

    @Test
    fun testCanvasStateAndOpticalLensDefaults() {
        val defaultState = CanvasState()
        assertEquals(1.0f, defaultState.filmPresetIntensity, 0.001f)
        assertEquals(OpticalLensType.NONE, defaultState.opticalLensState.type)
        assertFalse(defaultState.opticalLensState.isEnabled)
        assertTrue(defaultState.opticalLensState.isDefault)

        val activeLens = OpticalLensState(type = OpticalLensType.STARBURST_4PT, intensity = 0.75f)
        assertTrue(activeLens.isEnabled)
        assertFalse(activeLens.isDefault)
    }

    @Test
    fun testFilmLutBakingIdentityBlendingAndAlphaPreservation() {
        val lutCount = FilmPresetEngine.LUT_SIZE * FilmPresetEngine.LUT_SIZE * FilmPresetEngine.LUT_SIZE
        FilmPreset.entries.forEach { preset ->
            val lut = FilmPresetEngine.getOrBakeLut(preset)
            assertEquals("LUT size for $preset must be 17^3", lutCount, lut.size)
        }

        val sample = 0x80B4783CL.toInt() // semi-transparent warm pixel
        assertEquals(sample, FilmPresetEngine.evaluateFilmLutRgb(sample, FilmPreset.NONE, 1.0f))
        assertEquals(sample, FilmPresetEngine.evaluateFilmLutRgb(sample, FilmPreset.KODAK_PORTRA_400, 0.0f))

        val full = FilmPresetEngine.evaluateFilmLutRgb(sample, FilmPreset.KODAK_PORTRA_400, 1.0f)
        assertEquals("Alpha byte must be preserved", 0x80, (full ushr 24) and 0xFF)

        val half = FilmPresetEngine.evaluateFilmLutRgb(sample, FilmPreset.KODAK_PORTRA_400, 0.5f)
        val origR = (sample shr 16) and 0xFF
        val fullR = (full shr 16) and 0xFF
        val halfR = (half shr 16) and 0xFF
        val expectedMid = (origR + fullR) / 2
        assertTrue("50% intensity should blend cleanly", abs(halfR - expectedMid) <= 2)

        // Buffer test with transparent pixels
        val buf = intArrayOf(0x00123456, 0xFF406080.toInt())
        FilmPresetEngine.applyFilmLutToBuffer(buf, FilmPreset.SLIDE_CHROME_100, 1.0f)
        assertEquals("Fully transparent pixel must remain untouched", 0x00123456, buf[0])
        assertEquals(0xFF, (buf[1] ushr 24) and 0xFF)
    }

    @Test
    fun testFilmLutSplitToneAndMonochromeStocks() {
        // Teal & Orange must cool darks and warm highlights (not shift globally)
        val darkShadow = 0xFF242424.toInt()
        val tealShadow = FilmPresetEngine.evaluateFilmLutRgb(darkShadow, FilmPreset.CINEMATIC_TEAL_ORANGE, 1.0f)
        val sR = (tealShadow shr 16) and 0xFF
        val sB = tealShadow and 0xFF
        assertTrue("Teal & Orange shadows must lean cool/blue (B=$sB > R=$sR)", sB > sR)

        val brightHighlight = 0xFFDCDCDC.toInt()
        val orangeHigh = FilmPresetEngine.evaluateFilmLutRgb(brightHighlight, FilmPreset.CINEMATIC_TEAL_ORANGE, 1.0f)
        val hR = (orangeHigh shr 16) and 0xFF
        val hB = orangeHigh and 0xFF
        assertTrue("Teal & Orange highlights must lean warm/orange (R=$hR > B=$hB)", hR > hB)

        // Monochrome stocks must produce exact neutral R == G == B
        val vibrant = 0xFFE05028.toInt()
        listOf(FilmPreset.KODAK_TRI_X, FilmPreset.LEICA_MONOCHROME, FilmPreset.STREET_MONO_GR).forEach { mono ->
            val out = FilmPresetEngine.evaluateFilmLutRgb(vibrant, mono, 1.0f)
            val r = (out shr 16) and 0xFF
            val g = (out shr 8) and 0xFF
            val b = out and 0xFF
            assertEquals("$mono must output R == G", r, g)
            assertEquals("$mono must output G == B", g, b)
        }
    }

    @Test
    fun testStarburst4PtProducesOrthogonalPurpleCyanRays() {
        val w = 128
        val h = 128
        val src = IntArray(w * h) { 0xFF050508.toInt() }
        // Place crisp specular highlight dot at (64, 64)
        for (dy in -1..1) {
            for (dx in -1..1) {
                src[(64 + dy) * w + (64 + dx)] = 0xFFFFFFFF.toInt()
            }
        }

        val state = OpticalLensState(
            type = OpticalLensType.STARBURST_4PT,
            intensity = 0.90f,
            spread = 0.65f,
            angleDegrees = 45f
        )
        val overlay = AtharOpticalLensEngine.buildStarburst4PtBuffer(src, w, h, state, monochrome = false)

        // Ray along +45 deg (x+8, y+8) and -45 deg (x+8, y-8) must be brightly illuminated
        val diag1 = overlay[(64 + 8) * w + (64 + 8)]
        val diag2 = overlay[(64 - 8) * w + (64 + 8)]
        val offAxis = overlay[64 * w + (64 + 10)]

        val diag1Luma = (diag1 shr 16 and 0xFF) + (diag1 shr 8 and 0xFF) + (diag1 and 0xFF)
        val diag2Luma = (diag2 shr 16 and 0xFF) + (diag2 shr 8 and 0xFF) + (diag2 and 0xFF)
        val offLuma = (offAxis shr 16 and 0xFF) + (offAxis shr 8 and 0xFF) + (offAxis and 0xFF)

        assertTrue("45-deg ray must be bright ($diag1Luma)", diag1Luma > 60)
        assertTrue("-45-deg ray must be bright ($diag2Luma)", diag2Luma > 60)
        assertTrue("Off-axis pixel should stay dark compared to rays ($offLuma vs $diag1Luma)", offLuma < diag1Luma / 4)

        // Outer tip of ray must show purple/blue diffraction (B > G)
        val outerTip = overlay[(64 + 12) * w + (64 + 12)]
        val tipG = (outerTip shr 8) and 0xFF
        val tipB = outerTip and 0xFF
        assertTrue("Outer diffraction tip should shift toward purple/cyan (B=$tipB >= G=$tipG)", tipB >= tipG)
    }

    @Test
    fun testHalationLeakAndMonochromeSuppression() {
        val w = 96
        val h = 96
        val src = IntArray(w * h) { 0xFF000000.toInt() }
        for (dy in -3..3) {
            for (dx in -3..3) {
                src[(48 + dy) * w + (48 + dx)] = 0xFFFFFFFF.toInt()
            }
        }

        val state = OpticalLensState(
            type = OpticalLensType.HALATION_LEAK,
            intensity = 0.85f,
            spread = 0.60f,
            angleDegrees = 35f
        )
        val colorHalation = AtharOpticalLensEngine.buildHalationLeakBuffer(src, w, h, state, monochrome = false)
        val nearby = colorHalation[48 * w + (48 + 6)]
        val r = (nearby shr 16) and 0xFF
        val g = (nearby shr 8) and 0xFF
        val b = nearby and 0xFF
        assertTrue("Halation scatter around white spot must be warm crimson/amber (R=$r > G=$g > B=$b)", r > g && g > b)

        val monoHalation = AtharOpticalLensEngine.buildHalationLeakBuffer(src, w, h, state, monochrome = true)
        val monoNearby = monoHalation[48 * w + (48 + 6)]
        assertEquals((monoNearby shr 16) and 0xFF, (monoNearby shr 8) and 0xFF)
        assertEquals((monoNearby shr 8) and 0xFF, monoNearby and 0xFF)
    }

    @Test
    fun testPrismDiffractionGeneratesSpectralSeparation() {
        val w = 96
        val h = 96
        val src = IntArray(w * h) { 0xFF000000.toInt() }
        for (dy in -2..2) {
            for (dx in -2..2) {
                src[(48 + dy) * w + (48 + dx)] = 0xFFFFFFFF.toInt()
            }
        }
        val state = OpticalLensState(
            type = OpticalLensType.PRISM_DIFFRACTION,
            intensity = 0.85f,
            spread = 0.65f,
            angleDegrees = 0f
        )
        val prism = AtharOpticalLensEngine.buildPrismDiffractionBuffer(src, w, h, state, monochrome = false)
        val leftBand = prism[48 * w + (48 - 7)]
        val rightBand = prism[48 * w + (48 + 7)]
        assertTrue("Prism dispersion should differ spectrally between opposite sides", leftBand != rightBand)
        assertTrue("Prism dispersion should produce non-zero alpha", ((leftBand ushr 24) and 0xFF) > 0)
    }

    @Test
    fun testSoftWideOrganicVignetteAndMiniDvBleed() {
        val w = 64
        val h = 64
        // Checkerboard to measure contrast softening
        val checker = IntArray(w * h) { idx ->
            val x = idx % w
            val y = idx / w
            if (x == 0 && y == 0) 0x00000000 // transparent corner pixel
            else if ((x + y) % 2 == 0) 0xFFE6E6E6.toInt() else 0xFF282828.toInt()
        }
        val softState = OpticalLensState(type = OpticalLensType.SOFT_WIDE, intensity = 0.9f, spread = 0.7f)
        AtharOpticalLensEngine.applySoftWideToBuffer(checker, w, h, softState, referenceWidthPx = 64)

        assertEquals("Transparent pixel must stay transparent", 0, checker[0])
        val centerDiff = abs(((checker[32 * w + 32] shr 8) and 0xFF) - ((checker[32 * w + 33] shr 8) and 0xFF))
        val cornerDiff = abs(((checker[2 * w + 2] shr 8) and 0xFF) - ((checker[2 * w + 3] shr 8) and 0xFF))
        assertTrue("Center must remain sharper than soft outer corners (centerDiff=$centerDiff > cornerDiff=$cornerDiff)", centerDiff > cornerDiff)

        // Organic vignette symmetry & center preservation
        val flat = IntArray(w * h) { 0xFFC8C8C8.toInt() }
        AtharOpticalLensEngine.applyOrganicVignetteToBuffer(flat, w, h, intensity = 0.80f, spread = 0.50f)
        val centerLuma = (flat[32 * w + 32] shr 16) and 0xFF
        val topLeftLuma = (flat[0] shr 16) and 0xFF
        val bottomRightLuma = (flat[(h - 1) * w + (w - 1)] shr 16) and 0xFF
        assertTrue("Vignette center ($centerLuma) must be brighter than corner ($topLeftLuma)", centerLuma > topLeftLuma + 40)
        assertEquals("Vignette must be symmetric across opposite corners", topLeftLuma, bottomRightLuma)

        // MiniDV chroma bleed
        val step = IntArray(w * h) { idx ->
            if ((idx % w) < 32) 0xFFE02020.toInt() else 0xFF2020E0.toInt()
        }
        AtharOpticalLensEngine.applyMiniDvChromaBleedToBuffer(step, w, h, referenceWidthPx = 1080)
        val boundaryLeftB = step[10 * w + 31] and 0xFF
        assertTrue("MiniDV bleed should soften horizontal chroma across boundary ($boundaryLeftB > 0x20)", boundaryLeftB > 0x28)
    }

    @Test
    fun testFilmGrainPreservesAlphaAndMidtoneBellCurve() {
        val pixels = intArrayOf(
            0x00808080,               // transparent midtone -> untouched
            0x80808080.toInt(),       // semi-transparent midtone -> preserves 0x80 alpha
            0xFF000000.toInt(),       // pure black -> bell curve = 0 (clean)
            0xFFFFFFFF.toInt()        // pure white -> bell curve = 0 (clean)
        )
        val grainState = FilmGrainState(grain = 80f, size = 50f, roughness = 50f)
        AtharFilmGrainEngine.applyGrainToBuffer(pixels, width = 4, height = 1, state = grainState, referenceWidthPx = 1080)

        assertEquals(0x00808080, pixels[0])
        assertEquals("Semi-transparent alpha must be preserved", 0x80, (pixels[1] ushr 24) and 0xFF)
        assertEquals("Pure black shadows must stay clean", 0xFF000000.toInt(), pixels[2])
        assertEquals("Pure white highlights must stay clean", 0xFFFFFFFF.toInt(), pixels[3])
    }
}
