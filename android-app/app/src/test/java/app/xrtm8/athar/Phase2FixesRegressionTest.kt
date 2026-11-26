package app.xrtm8.athar

import app.xrtm8.athar.ui.AtharStudioViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2FixesRegressionTest {

    @Test
    fun `toggleLayerVisibility toggles active layer and supports undo`() {
        val vm = AtharStudioViewModel()
        assertTrue(vm.canvasState.value.activeLayer.isVisible)

        vm.toggleLayerVisibility()
        assertFalse(vm.canvasState.value.activeLayer.isVisible)
        assertTrue(vm.canUndo.value)

        vm.undo()
        assertTrue(vm.canvasState.value.activeLayer.isVisible)
    }

    @Test
    fun `renameLayer updates layer name and ignores blank input`() {
        val vm = AtharStudioViewModel()
        val id = vm.canvasState.value.activeLayer.id
        val originalName = vm.canvasState.value.activeLayer.name

        vm.renameLayer(id, "   ")
        assertEquals(originalName, vm.canvasState.value.activeLayer.name)

        vm.renameLayer(id, "عنوان رئيسي")
        assertEquals("عنوان رئيسي", vm.canvasState.value.activeLayer.name)

        vm.undo()
        assertEquals(originalName, vm.canvasState.value.activeLayer.name)
    }
}
