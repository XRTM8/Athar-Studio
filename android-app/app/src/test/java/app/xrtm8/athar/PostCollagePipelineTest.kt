package app.xrtm8.athar

import app.xrtm8.athar.core.AtharBackgroundPipeline
import app.xrtm8.athar.core.AtharColorMixEngine
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.core.CollageCoverFitMath
import app.xrtm8.athar.core.CollageLayoutCalculator
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ChannelAdjustment
import app.xrtm8.athar.model.CinematicGlowState
import app.xrtm8.athar.model.CollageLayoutType
import app.xrtm8.athar.model.ColorMixChannel
import app.xrtm8.athar.model.ColorMixState
import app.xrtm8.athar.model.FilmGrainState
import app.xrtm8.athar.model.FilmPreset
import app.xrtm8.athar.model.GlitchEffectState
import app.xrtm8.athar.model.PatternState
import app.xrtm8.athar.model.PatternType
import app.xrtm8.athar.ui.AtharStudioViewModel
import org.junit.Assert.*
import org.junit.Test

class PostCollagePipelineTest {

    @Test
    fun coverFitNeverDistortsOrLeavesGapsAcrossAspectRatios() {
        val tfAt1x = CollageCoverFitMath.computeCoverTransform(
            srcW = 1600f, srcH = 900f,
            dstLeft = 0f, dstTop = 0f, dstRight = 500f, dstBottom = 1000f,
            offsetX = 0f, offsetY = 0f, scale = 0.5f
        )
        assertEquals(1000f, tfAt1x.dispH, 0.01f)
        assertTrue(tfAt1x.dispW > 500f)
        assertEquals(0f, tfAt1x.maxSlackPxY, 0.01f)
        assertTrue(tfAt1x.maxSlackPxX > 0f)

        val tfLeft = CollageCoverFitMath.computeCoverTransform(
            srcW = 1600f, srcH = 900f,
            dstLeft = 100f, dstTop = 50f, dstRight = 600f, dstBottom = 1050f,
            offsetX = -1f, offsetY = 0f, scale = 1.0f
        )
        assertEquals(100f, tfLeft.centerX - tfLeft.dispW * 0.5f, 0.01f)

        val tfRight = CollageCoverFitMath.computeCoverTransform(
            srcW = 1600f, srcH = 900f,
            dstLeft = 100f, dstTop = 50f, dstRight = 600f, dstBottom = 1050f,
            offsetX = 1f, offsetY = 0f, scale = 1.0f
        )
        assertEquals(600f, tfRight.centerX + tfRight.dispW * 0.5f, 0.01f)
    }

    @Test
    fun coverFitSupports90DegreeRotationWithoutGaps() {
        val tf = CollageCoverFitMath.computeCoverTransform(
            srcW = 1600f, srcH = 900f,
            dstLeft = 0f, dstTop = 0f, dstRight = 500f, dstBottom = 1000f,
            offsetX = 0f, offsetY = 1f, scale = 1.5f, rotationDeg = 90
        )
        assertEquals(90, tf.normalizedRotDeg)
        assertTrue(tf.dispW >= 500f)
        assertTrue(tf.dispH >= 1000f)
        assertEquals(1000f, tf.centerY + tf.dispH * 0.5f, 0.01f)
    }

    @Test
    fun oneToOneFingerPanAvoidsPhantomDriftOnZeroSlackAxis() {
        val step = CollageCoverFitMath.stepFramingTransform(
            srcW = 1600f, srcH = 900f,
            dstLeft = 0f, dstTop = 0f, dstRight = 500f, dstBottom = 1000f,
            currentOffsetX = 0f, currentOffsetY = 0f, currentScale = 1.0f,
            panDeltaX = 50f, panDeltaY = 80f, zoomDelta = 1.0f
        )
        assertTrue(step.offsetX < 0f)
        assertEquals(0f, step.offsetY, 0.001f)
        assertEquals(1.0f, step.scale, 0.001f)
    }

    @Test
    fun triptychColumnsPartitionWidthExact() {
        val cells = CollageLayoutCalculator.generateCellsForLayout(CollageLayoutType.TRIPTYCH_COLS)
        assertEquals(3, cells.size)
        assertEquals(0f, cells[0].leftNorm, 0.0001f)
        assertEquals(cells[0].rightNorm, cells[1].leftNorm, 0.0001f)
        assertEquals(cells[1].rightNorm, cells[2].leftNorm, 0.0001f)
        assertEquals(1f, cells[2].rightNorm, 0.0001f)
    }

    @Test
    fun stackBlurPreservesTransparentPixelsWithoutBlackHalo() {
        val w = 8
        val h = 8
        val pix = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                pix[y * w + x] = if (x < 4) 0xFFFFFFFF.toInt() else 0x00000000
            }
        }
        AtharImageProcessor.stackBlurPixels(pix, w, h, 2)

        assertEquals(0, (pix[7] ushr 24) and 0xFF)
        val mid = pix[4]
        val midA = (mid ushr 24) and 0xFF
        val midR = (mid ushr 16) and 0xFF
        val midG = (mid ushr 8) and 0xFF
        val midB = mid and 0xFF
        assertTrue(midA in 1..254)
        assertTrue("Expected bright white RGB without dark halo, got ($midR,$midG,$midB)", midR >= 250 && midG >= 250 && midB >= 250)
    }

    @Test
    fun colorMixSkipsZeroAlphaAndProcessesOpaquePixels() {
        val state = ColorMixState(
            isEnabled = true,
            channelAdjustments = mapOf(ColorMixChannel.RED to ChannelAdjustment(hueShift = 30f)),
            isolatedChannel = ColorMixChannel.BLUE
        )
        val pixels = intArrayOf(
            0x00FF1122,
            0xFFFF0000.toInt()
        )
        AtharColorMixEngine.processPixelArray(pixels, pixels.size, state)
        assertEquals(0x00FF1122, pixels[0])
        val r = (pixels[1] shr 16) and 0xFF
        val g = (pixels[1] shr 8) and 0xFF
        val b = pixels[1] and 0xFF
        assertEquals(r, g)
        assertEquals(g, b)
    }

    @Test
    fun pipelineDensityPredicatesAndBeforeAfterResetMatchUnifiedContract() {
        assertEquals(1.0f, AtharBackgroundPipeline.canvasDensity(380f), 0.001f)
        assertEquals(2160f / 380f, AtharBackgroundPipeline.canvasDensity(2160f), 0.001f)

        val defaultState = CanvasState()
        assertFalse(AtharBackgroundPipeline.hasHeavyCollageCompositeEffects(defaultState))
        assertFalse(AtharBackgroundPipeline.hasAnyPixelEffects(defaultState))

        val edited = defaultState.copy(
            filmPreset = FilmPreset.KODAK_PORTRA_400,
            colorMixState = ColorMixState(isEnabled = true, isolatedChannel = ColorMixChannel.RED),
            cinematicGlowState = CinematicGlowState(glow = 20f),
            filmGrainState = FilmGrainState(grain = 20f),
            glitchEffectState = GlitchEffectState(rgbSplit = 20f),
            patternState = PatternState(type = PatternType.SCANLINES)
        )
        assertTrue(AtharBackgroundPipeline.hasHeavyCollageCompositeEffects(edited))

        val raw = AtharBackgroundPipeline.createBeforeCompareState(edited)
        assertFalse(AtharBackgroundPipeline.hasHeavyCollageCompositeEffects(raw))
        assertEquals(FilmPreset.NONE, raw.filmPreset)
        assertEquals(PatternType.NONE, raw.patternState.type)
        assertEquals(0f, raw.backgroundDim, 0.001f)
    }

    @Test
    fun replacingCellPhotoResetsFramingAndClampsBgScaleToMin1x() {
        val vm = AtharStudioViewModel()
        vm.setCollageLayout(CollageLayoutType.DIPTYCH_V)
        val cellId = vm.canvasState.value.collageState.cells.first().id

        vm.commitCollageCellFraming(cellId, 0.5f, -0.5f, 2.5f)
        vm.rotateCollageCell90(cellId)
        vm.toggleCollageCellFlipH(cellId)
        assertEquals(90, vm.canvasState.value.collageState.cells.first().photoRotationDeg)
        assertTrue(vm.canvasState.value.collageState.cells.first().isFlippedH)

        vm.setCollageCellPhoto(cellId, null)
        val replaced = vm.canvasState.value.collageState.cells.first()
        assertEquals(0f, replaced.photoOffsetX, 0.001f)
        assertEquals(0f, replaced.photoOffsetY, 0.001f)
        assertEquals(1.0f, replaced.photoScale, 0.001f)
        assertEquals(0, replaced.photoRotationDeg)
        assertFalse(replaced.isFlippedH)

        vm.setBgPhotoScale(0.2f)
        assertEquals(1.0f, vm.canvasState.value.bgPhotoScale, 0.001f)
        vm.commitPhotoFraming(0f, 0f, 0.4f)
        assertEquals(1.0f, vm.canvasState.value.bgPhotoScale, 0.001f)
    }
}
