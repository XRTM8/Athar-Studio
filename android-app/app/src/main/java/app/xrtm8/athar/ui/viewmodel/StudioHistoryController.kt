package app.xrtm8.athar.ui.viewmodel

import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.ArrayDeque

sealed interface StudioUiEvent {
    data class ShareFile(
        val file: File,
        val mimeType: String,
        val chooserTitle: String
    ) : StudioUiEvent
}

/**
 * Thread-safe undo/redo history and dirty-session state holder for the studio editor.
 */
internal class StudioHistoryController {
    private val lock = Any()
    private val _canvasState = MutableStateFlow(CanvasState())
    val canvasState: StateFlow<CanvasState> = _canvasState.asStateFlow()

    private val undoStack = ArrayDeque<CanvasState>()
    private val redoStack = ArrayDeque<CanvasState>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var sliderDragInitialState: CanvasState? = null
    @Volatile
    var isDirty: Boolean = false
        private set

    fun current(): CanvasState = _canvasState.value

    fun markSaved() {
        synchronized(lock) { isDirty = false }
    }

    fun markSliderChange() {
        synchronized(lock) {
            if (sliderDragInitialState == null) {
                sliderDragInitialState = _canvasState.value
            }
        }
    }

    fun commitCheckpoint() {
        synchronized(lock) {
            val initial = sliderDragInitialState
            val current = _canvasState.value
            if (initial != null && initial != current) {
                undoStack.addLast(initial)
                if (undoStack.size > 100) undoStack.removeFirst()
                redoStack.clear()
                _canUndo.value = true
                _canRedo.value = false
                isDirty = true
            }
            sliderDragInitialState = null
        }
    }

    fun pushHistory(newState: CanvasState) {
        synchronized(lock) {
            if (_canvasState.value != newState) {
                undoStack.addLast(_canvasState.value)
                if (undoStack.size > 100) undoStack.removeFirst()
                redoStack.clear()
                _canUndo.value = true
                _canRedo.value = false
                _canvasState.value = newState
                isDirty = true
            }
        }
    }

    fun resetSession(newState: CanvasState) {
        synchronized(lock) {
            _canvasState.value = newState
            sliderDragInitialState = null
            undoStack.clear()
            redoStack.clear()
            _canUndo.value = false
            _canRedo.value = false
            isDirty = false
        }
    }

    fun updateState(markDirty: Boolean = true, transform: (CanvasState) -> CanvasState) {
        synchronized(lock) {
            _canvasState.update(transform)
            if (markDirty) isDirty = true
        }
    }

    fun updateActiveLayer(markDirty: Boolean = true, transform: (TextLayer) -> TextLayer) {
        synchronized(lock) {
            val current = _canvasState.value
            val activeId = current.activeLayer.id
            val updatedLayers = current.layers.map { layer ->
                if (layer.id == activeId) transform(layer) else layer
            }
            _canvasState.value = current.copy(layers = updatedLayers)
            if (markDirty) isDirty = true
        }
    }

    fun pushActiveLayerHistory(transform: (TextLayer) -> TextLayer) {
        synchronized(lock) {
            val current = _canvasState.value
            val activeId = current.activeLayer.id
            val updatedLayers = current.layers.map { layer ->
                if (layer.id == activeId) transform(layer) else layer
            }
            pushHistory(current.copy(layers = updatedLayers))
        }
    }

    fun undo() {
        synchronized(lock) {
            if (undoStack.isNotEmpty()) {
                val previous = undoStack.removeLast()
                redoStack.addLast(_canvasState.value)
                _canvasState.value = previous
                _canUndo.value = undoStack.isNotEmpty()
                _canRedo.value = true
                isDirty = true
            }
        }
    }

    fun redo() {
        synchronized(lock) {
            if (redoStack.isNotEmpty()) {
                val next = redoStack.removeLast()
                undoStack.addLast(_canvasState.value)
                _canvasState.value = next
                _canUndo.value = true
                _canRedo.value = redoStack.isNotEmpty()
                isDirty = true
            }
        }
    }
}
