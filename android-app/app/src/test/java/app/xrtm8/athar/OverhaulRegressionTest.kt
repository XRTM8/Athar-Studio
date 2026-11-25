package app.xrtm8.athar

import app.xrtm8.athar.core.AtharOpticalLensEngine
import app.xrtm8.athar.model.*
import app.xrtm8.athar.project.AtharProjectSerializer
import app.xrtm8.athar.ui.AtharStudioViewModel
import app.xrtm8.athar.updater.AtharUpdateManager
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class OverhaulRegressionTest {

    @Test
    fun newAndImageProjectsStartWithCleanHistoryAndDirtyTracking() {
        val vm = AtharStudioViewModel()
        vm.setText("نص مبدئي")
        assertTrue(vm.canUndo.value)
        assertTrue(vm.hasUnsavedChanges())

        vm.createNewProject(CanvasAspectRatio.SQUARE_1_1)
        assertFalse("Fresh project must clear undo stack", vm.canUndo.value)
        assertFalse("Fresh project must clear redo stack", vm.canRedo.value)
        assertFalse("Fresh project must not be dirty", vm.hasUnsavedChanges())
        assertNull(vm.currentProjectId)

        vm.setText("طبقة معدلة")
        assertTrue(vm.hasUnsavedChanges())
        assertTrue(vm.canUndo.value)
        vm.undo()
        assertEquals("", vm.canvasState.value.rawText)
        assertTrue(vm.canRedo.value)
        vm.redo()
        assertEquals("طبقة معدلة", vm.canvasState.value.rawText)
    }

    @Test
    fun sliderCheckpointCoalescesDragIntoSingleUndoStep() {
        val vm = AtharStudioViewModel()
        vm.createNewProject()
        vm.setBackgroundBlur(5f)
        vm.setBackgroundBlur(12f)
        vm.setBackgroundBlur(20f)
        assertFalse("Uncommitted drag must not push undo entries yet", vm.canUndo.value)

        vm.commitHistoryCheckpoint()
        assertTrue(vm.canUndo.value)
        assertEquals(20f, vm.canvasState.value.backgroundBlur, 0.001f)

        vm.undo()
        assertEquals(0f, vm.canvasState.value.backgroundBlur, 0.001f)
    }

    @Test
    fun splitProjectSerializerPreservesAllLayersAndEffects() {
        val layer1 = TextLayer(
            id = "l1",
            rawText = "سطر أول",
            displayText = "سطر أول",
            fontWeight = 700,
            stylisticSet = 2,
            enableSwash = true,
            wordSpacing = 12f,
            gradientPreset = LuxuryGradientPreset.ANDALUSIAN_GOLD
        )
        val layer2 = TextLayer(
            id = "l2",
            rawText = "سطر ثانٍ",
            displayText = "سطر ثانٍ",
            isCurved = true,
            curveAngle = -45f,
            blendMode = LayerBlendMode.SCREEN
        )
        val state = CanvasState(
            layers = listOf(layer1, layer2),
            activeLayerId = "l2",
            aspectRatio = CanvasAspectRatio.PORTRAIT_4_5,
            filmPreset = FilmPreset.KODAK_PORTRA_400,
            filmPresetIntensity = 0.72f,
            opticalLensState = OpticalLensState(OpticalLensType.HALATION_LEAK, 0.55f, 0.65f, 30f),
            maskState = AtharMaskState(isEnabled = true, activeMaskId = "proc_french_window", softness = 0.4f, warmth = 0.7f),
            collageState = CollageState(
                layout = CollageLayoutType.DIPTYCH_V,
                cells = listOf(
                    CollageCell("c0", null, 0f, 0f, 0.5f, 1f, photoOffsetX = 0.2f, photoScale = 1.8f, photoRotationDeg = 90, isFlippedH = true)
                )
            )
        )

        val json = AtharProjectSerializer.serializeCanvasState(state, hasBackgroundPhoto = false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        assertEquals(2, restored.layers.size)
        assertEquals("l2", restored.activeLayerId)
        assertEquals(700, restored.layers[0].fontWeight)
        assertEquals(2, restored.layers[0].stylisticSet)
        assertTrue(restored.layers[0].enableSwash)
        assertEquals(12f, restored.layers[0].wordSpacing, 0.001f)
        assertEquals(LayerBlendMode.SCREEN, restored.layers[1].blendMode)
        assertEquals(FilmPreset.KODAK_PORTRA_400, restored.filmPreset)
        assertEquals(0.72f, restored.filmPresetIntensity, 0.001f)
        assertEquals(OpticalLensType.HALATION_LEAK, restored.opticalLensState.type)
        assertEquals("proc_french_window", restored.maskState.activeMaskId)
        assertEquals(0.4f, restored.maskState.softness, 0.001f)
        assertEquals(CollageLayoutType.DIPTYCH_V, restored.collageState.layout)
        assertEquals(90, restored.collageState.cells.single().photoRotationDeg)
        assertTrue(restored.collageState.cells.single().isFlippedH)
    }

    @Test
    fun softWideDownscaledPathPreservesCenterSharpnessOnLargeBuffer() {
        val w = 760
        val h = 760
        val checker = IntArray(w * h) { idx ->
            val x = idx % w
            val y = idx / w
            if ((x + y) % 2 == 0) 0xFFE6E6E6.toInt() else 0xFF282828.toInt()
        }
        val state = OpticalLensState(type = OpticalLensType.SOFT_WIDE, intensity = 0.9f, spread = 0.7f)
        AtharOpticalLensEngine.applySoftWideToBuffer(checker, w, h, state, referenceWidthPx = 1080)

        val cx = w / 2
        val cy = h / 2
        val centerDiff = abs(((checker[cy * w + cx] shr 8) and 0xFF) - ((checker[cy * w + cx + 1] shr 8) and 0xFF))
        val cornerDiff = abs(((checker[6 * w + 6] shr 8) and 0xFF) - ((checker[6 * w + 7] shr 8) and 0xFF))
        assertTrue("Center ($centerDiff) must remain sharper than soft outer corners ($cornerDiff)", centerDiff > cornerDiff)
    }

    @Test
    fun updateRepositoryUrlMatchesGitOrigin() {
        assertEquals("XRTM8/Athar-Studio", AtharUpdateManager.GITHUB_REPO)
        assertEquals("https://github.com/XRTM8/Athar-Studio", AtharUpdateManager.REPO_URL)
    }
}
