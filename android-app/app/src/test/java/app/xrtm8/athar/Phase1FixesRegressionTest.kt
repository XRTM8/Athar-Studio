package app.xrtm8.athar

import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.ParagraphBalanceMode
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.project.AtharProjectSerializer
import app.xrtm8.athar.ui.AtharStudioViewModel
import app.xrtm8.athar.ui.components.CanvasTransformHelper
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1FixesRegressionTest {

    @Test
    fun `setRotationAngle normalizes to -180 to 180 range`() {
        val vm = AtharStudioViewModel()
        vm.setRotationAngle(-45f)
        assertEquals(-45f, vm.canvasState.value.rotationAngle, 0.01f)

        vm.setRotationAngle(270f)
        assertEquals(-90f, vm.canvasState.value.rotationAngle, 0.01f)

        vm.setRotationAngle(-270f)
        assertEquals(90f, vm.canvasState.value.rotationAngle, 0.01f)

        vm.setRotationAngle(180f)
        assertEquals(180f, vm.canvasState.value.rotationAngle, 0.01f)
    }

    @Test
    fun `setFontSize records undo history checkpoint`() {
        val vm = AtharStudioViewModel()
        val initialSize = vm.canvasState.value.fontSize
        assertFalse(vm.canUndo.value)

        vm.setFontSize(72f)
        vm.commitHistoryCheckpoint()

        assertEquals(72f, vm.canvasState.value.fontSize, 0.01f)
        assertTrue(vm.canUndo.value)

        vm.undo()
        assertEquals(initialSize, vm.canvasState.value.fontSize, 0.01f)
    }

    @Test
    fun `canvasTransformHelper clamps scale and normalizes rotation to -180_180`() {
        val res = CanvasTransformHelper.applyTransform(
            pan = Offset.Zero,
            zoom = 10f,
            rotate = -200f,
            canvasW = 1000f,
            canvasH = 1000f,
            normX = 0f,
            normY = 0f,
            scale = 1f,
            rotation = 0f,
            onSnapHaptic = {}
        )
        assertEquals(4.0f, res.scale, 0.01f)
        assertEquals(160f, res.rotation, 0.01f)
    }

    @Test
    fun `paragraphBalanceMode survives stretchLevel changes and serialization`() {
        val vm = AtharStudioViewModel()
        vm.setText("سبحان الله\nالحمد لله")
        vm.autoBalanceTypographicBlock(null)
        assertEquals(ParagraphBalanceMode.BALANCE, vm.canvasState.value.activeLayer.paragraphBalanceMode)

        vm.setStretchLevel(4)
        vm.commitHistoryCheckpoint()
        assertEquals(ParagraphBalanceMode.BALANCE, vm.canvasState.value.activeLayer.paragraphBalanceMode)

        val json = AtharProjectSerializer.serializeCanvasState(vm.canvasState.value, hasBackgroundPhoto = false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)
        assertEquals(ParagraphBalanceMode.BALANCE, restored.activeLayer.paragraphBalanceMode)

        vm.setStretchLevel(0)
        assertEquals(ParagraphBalanceMode.NONE, vm.canvasState.value.activeLayer.paragraphBalanceMode)
    }
}
