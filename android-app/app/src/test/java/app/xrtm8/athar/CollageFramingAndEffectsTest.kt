package app.xrtm8.athar

import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.CollageCell
import app.xrtm8.athar.model.CollageLayoutType
import app.xrtm8.athar.model.CollageState
import app.xrtm8.athar.project.AtharProjectSerializer
import app.xrtm8.athar.ui.AtharStudioViewModel
import org.junit.Assert.*
import org.junit.Test

class CollageFramingAndEffectsTest {

    @Test
    fun testCollageCellFramingOffsetAndScale() {
        val vm = AtharStudioViewModel()
        vm.setCollageLayout(CollageLayoutType.DIPTYCH_V)

        val state = vm.canvasState.value.collageState
        assertTrue(state.isEnabled)
        assertEquals(2, state.cells.size)

        val cell0 = state.cells[0]
        val cellId = cell0.id

        // Default framing values
        assertEquals(0f, cell0.photoOffsetX, 0.001f)
        assertEquals(0f, cell0.photoOffsetY, 0.001f)
        assertEquals(1.0f, cell0.photoScale, 0.001f)

        // Adjust vertical and horizontal pan
        vm.setCollageCellPhotoOffset(cellId, 0.45f, -0.65f)
        val updatedCell = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(0.45f, updatedCell.photoOffsetX, 0.001f)
        assertEquals(-0.65f, updatedCell.photoOffsetY, 0.001f)

        // Verify clamping in [-1f, 1f]
        vm.setCollageCellPhotoOffset(cellId, 2.5f, -3.0f)
        val clampedCell = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(1.0f, clampedCell.photoOffsetX, 0.001f)
        assertEquals(-1.0f, clampedCell.photoOffsetY, 0.001f)

        // Adjust scale / zoom
        vm.setCollageCellPhotoScale(cellId, 2.35f)
        val scaledCell = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(2.35f, scaledCell.photoScale, 0.001f)

        // Verify scale clamping in [0.5f, 4.0f]
        vm.setCollageCellPhotoScale(cellId, 10.0f)
        val clampedScaleMax = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(4.0f, clampedScaleMax.photoScale, 0.001f)

        vm.setCollageCellPhotoScale(cellId, 0.1f)
        val clampedScaleMin = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(0.5f, clampedScaleMin.photoScale, 0.001f)
    }

    @Test
    fun testCollageCellFramingResetAndRemove() {
        val vm = AtharStudioViewModel()
        vm.setCollageLayout(CollageLayoutType.QUAD_GRID)

        val state = vm.canvasState.value.collageState
        assertEquals(4, state.cells.size)
        val cellId = state.cells[1].id

        vm.setCollageCellPhotoOffset(cellId, 0.8f, -0.4f)
        vm.setCollageCellPhotoScale(cellId, 2.5f)

        // Reset framing
        vm.resetCollageCellFraming(cellId)
        val resetCell = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(0f, resetCell.photoOffsetX, 0.001f)
        assertEquals(0f, resetCell.photoOffsetY, 0.001f)
        assertEquals(1.0f, resetCell.photoScale, 0.001f)

        // Remove photo
        vm.removeCollageCellPhoto(cellId)
        val removedCell = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertNull(removedCell.imageUri)
        assertEquals(0f, removedCell.photoOffsetX, 0.001f)
        assertEquals(0f, removedCell.photoOffsetY, 0.001f)
        assertEquals(1.0f, removedCell.photoScale, 0.001f)
    }

    @Test
    fun testCollageFramingUndoRedo() {
        val vm = AtharStudioViewModel()
        vm.setCollageLayout(CollageLayoutType.DIPTYCH_H)

        val cellId = vm.canvasState.value.collageState.cells[0].id

        // Framing modification 1
        vm.setCollageCellPhotoOffset(cellId, 0.25f, 0.50f)
        vm.setCollageCellPhotoScale(cellId, 1.8f)
        vm.commitHistoryCheckpoint()

        val afterMod1 = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(0.25f, afterMod1.photoOffsetX, 0.001f)
        assertEquals(0.50f, afterMod1.photoOffsetY, 0.001f)
        assertEquals(1.8f, afterMod1.photoScale, 0.001f)

        // Framing modification 2
        vm.setCollageCellPhotoOffset(cellId, -0.70f, -0.30f)
        vm.setCollageCellPhotoScale(cellId, 2.8f)
        vm.commitHistoryCheckpoint()

        val afterMod2 = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(-0.70f, afterMod2.photoOffsetX, 0.001f)
        assertEquals(-0.30f, afterMod2.photoOffsetY, 0.001f)
        assertEquals(2.8f, afterMod2.photoScale, 0.001f)

        // Undo -> restores modification 1
        vm.undo()
        val afterUndo = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(0.25f, afterUndo.photoOffsetX, 0.001f)
        assertEquals(0.50f, afterUndo.photoOffsetY, 0.001f)
        assertEquals(1.8f, afterUndo.photoScale, 0.001f)

        // Redo -> restores modification 2
        vm.redo()
        val afterRedo = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(-0.70f, afterRedo.photoOffsetX, 0.001f)
        assertEquals(-0.30f, afterRedo.photoOffsetY, 0.001f)
        assertEquals(2.8f, afterRedo.photoScale, 0.001f)
    }

    @Test
    fun testCollageSerializationRoundTripWithFraming() {
        val cell1 = CollageCell(
            id = "c1",
            leftNorm = 0f,
            topNorm = 0f,
            rightNorm = 1f,
            bottomNorm = 0.5f,
            photoOffsetX = -0.35f,
            photoOffsetY = 0.65f,
            photoScale = 2.2f
        )
        val cell2 = CollageCell(
            id = "c2",
            leftNorm = 0f,
            topNorm = 0.5f,
            rightNorm = 1f,
            bottomNorm = 1f,
            photoOffsetX = 0.50f,
            photoOffsetY = -0.20f,
            photoScale = 1.4f
        )

        val collageState = CollageState(
            layout = CollageLayoutType.DIPTYCH_H,
            cells = listOf(cell1, cell2),
            spacingDp = 8f,
            cornerRadiusDp = 12f,
            frameColorLong = 0xFF2A080FL,
            activeCellId = "c1"
        )

        val canvasState = CanvasState(collageState = collageState)
        val json = AtharProjectSerializer.serializeCanvasState(canvasState, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        assertTrue(restored.collageState.isEnabled)
        assertEquals(CollageLayoutType.DIPTYCH_H, restored.collageState.layout)
        assertEquals(2, restored.collageState.cells.size)
        assertEquals(8f, restored.collageState.spacingDp, 0.001f)
        assertEquals(12f, restored.collageState.cornerRadiusDp, 0.001f)
        assertEquals(0xFF2A080FL, restored.collageState.frameColorLong)

        val restoredC1 = restored.collageState.cells[0]
        assertEquals("c1", restoredC1.id)
        assertEquals(-0.35f, restoredC1.photoOffsetX, 0.001f)
        assertEquals(0.65f, restoredC1.photoOffsetY, 0.001f)
        assertEquals(2.2f, restoredC1.photoScale, 0.001f)

        val restoredC2 = restored.collageState.cells[1]
        assertEquals("c2", restoredC2.id)
        assertEquals(0.50f, restoredC2.photoOffsetX, 0.001f)
        assertEquals(-0.20f, restoredC2.photoOffsetY, 0.001f)
        assertEquals(1.4f, restoredC2.photoScale, 0.001f)
    }

    @Test
    fun testCommitCollageCellFramingAndUndoRedo() {
        val vm = AtharStudioViewModel()
        vm.setCollageLayout(CollageLayoutType.DIPTYCH_V)

        val cellId = vm.canvasState.value.collageState.cells[0].id
        vm.commitCollageCellFraming(cellId, 0.40f, -0.60f, 2.2f)

        val committed = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(0.40f, committed.photoOffsetX, 0.001f)
        assertEquals(-0.60f, committed.photoOffsetY, 0.001f)
        assertEquals(2.2f, committed.photoScale, 0.001f)

        // Undo -> restores initial state
        vm.undo()
        val undone = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(0f, undone.photoOffsetX, 0.001f)
        assertEquals(0f, undone.photoOffsetY, 0.001f)
        assertEquals(1.0f, undone.photoScale, 0.001f)

        // Redo -> restores committed state
        vm.redo()
        val redone = vm.canvasState.value.collageState.cells.first { it.id == cellId }
        assertEquals(0.40f, redone.photoOffsetX, 0.001f)
        assertEquals(-0.60f, redone.photoOffsetY, 0.001f)
        assertEquals(2.2f, redone.photoScale, 0.001f)
    }

    @Test
    fun testCanvasHitTesterCollageCellDetection() {
        val cellLeft = CollageCell("cell_left", leftNorm = 0f, topNorm = 0f, rightNorm = 0.5f, bottomNorm = 1f)
        val cellRight = CollageCell("cell_right", leftNorm = 0.5f, topNorm = 0f, rightNorm = 1f, bottomNorm = 1f)

        val state = CanvasState(
            collageState = CollageState(
                layout = CollageLayoutType.DIPTYCH_V,
                cells = listOf(cellLeft, cellRight)
            )
        )

        val canvasW = 1000f
        val canvasH = 1000f

        // Tap on left cell (x = 250, y = 500)
        val hitLeft = app.xrtm8.athar.ui.canvas.CanvasHitTester.findHitCollageCell(250f, 500f, canvasW, canvasH, state)
        assertEquals("cell_left", hitLeft)

        // Tap on right cell (x = 750, y = 500)
        val hitRight = app.xrtm8.athar.ui.canvas.CanvasHitTester.findHitCollageCell(750f, 500f, canvasW, canvasH, state)
        assertEquals("cell_right", hitRight)

        // When collage is disabled
        val disabledState = CanvasState(collageState = CollageState(layout = CollageLayoutType.NONE))
        assertNull(app.xrtm8.athar.ui.canvas.CanvasHitTester.findHitCollageCell(250f, 500f, canvasW, canvasH, disabledState))
    }
}
