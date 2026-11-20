package app.xrtm8.athar.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.xrtm8.athar.core.AtharNativeEngine
import app.xrtm8.athar.export.AtharExportManager
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.project.AtharProjectManager
import app.xrtm8.athar.project.AtharProjectMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque

class AtharStudioViewModel : ViewModel() {

    private val _canvasState = MutableStateFlow(CanvasState())
    val canvasState: StateFlow<CanvasState> = _canvasState.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    // Undo / Redo History Stacks
    private val undoStack = ArrayDeque<CanvasState>()
    private val redoStack = ArrayDeque<CanvasState>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _projectsList = MutableStateFlow<List<AtharProjectMeta>>(emptyList())
    val projectsList: StateFlow<List<AtharProjectMeta>> = _projectsList.asStateFlow()

    private var kashidaDebounceJob: Job? = null

    init {
        updateKashidaInternal(
            _canvasState.value.rawText,
            _canvasState.value.stretchLevel,
            _canvasState.value.kashidaMode,
            _canvasState.value.kashidaTargetIndex
        )
    }

    private fun pushHistory(newState: CanvasState) {
        if (_canvasState.value != newState) {
            undoStack.addLast(_canvasState.value)
            if (undoStack.size > 25) undoStack.removeFirst()
            redoStack.clear()
            _canUndo.value = true
            _canRedo.value = false
            _canvasState.value = newState
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeLast()
            redoStack.addLast(_canvasState.value)
            _canvasState.value = previous
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.addLast(_canvasState.value)
            _canvasState.value = next
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun setText(newText: String) {
        pushHistory(_canvasState.value.copy(rawText = newText))
        scheduleKashidaUpdate(newText, _canvasState.value.stretchLevel, _canvasState.value.kashidaMode, _canvasState.value.kashidaTargetIndex)
    }

    fun setStretchLevel(newLevel: Int) {
        if (newLevel == _canvasState.value.stretchLevel) return
        pushHistory(_canvasState.value.copy(stretchLevel = newLevel))
        scheduleKashidaUpdate(_canvasState.value.rawText, newLevel, _canvasState.value.kashidaMode, _canvasState.value.kashidaTargetIndex)
    }

    fun setKashidaMode(mode: KashidaMode) {
        if (mode == _canvasState.value.kashidaMode) return
        pushHistory(_canvasState.value.copy(kashidaMode = mode))
        scheduleKashidaUpdate(_canvasState.value.rawText, _canvasState.value.stretchLevel, mode, _canvasState.value.kashidaTargetIndex)
    }

    fun setKashidaTargetIndex(targetIndex: Int) {
        pushHistory(_canvasState.value.copy(kashidaTargetIndex = targetIndex, kashidaMode = KashidaMode.CUSTOM))
        scheduleKashidaUpdate(_canvasState.value.rawText, _canvasState.value.stretchLevel, KashidaMode.CUSTOM, targetIndex)
    }

    fun setEffect(effect: VisualEffect) {
        pushHistory(_canvasState.value.copy(selectedEffect = effect, customTextColorLong = null))
    }

    fun setFont(font: ArabicFontFamily) {
        pushHistory(_canvasState.value.copy(selectedFont = font))
    }

    fun setCustomColor(colorLong: Long?) {
        pushHistory(_canvasState.value.copy(customTextColorLong = colorLong))
    }

    fun setStrokeWidth(width: Float) {
        pushHistory(_canvasState.value.copy(strokeWidth = width.coerceIn(0f, 16f)))
    }

    fun setStrokeColor(colorLong: Long) {
        pushHistory(_canvasState.value.copy(strokeColorLong = colorLong))
    }

    fun setShadowRadius(radius: Float) {
        pushHistory(_canvasState.value.copy(shadowRadius = radius.coerceIn(0f, 60f)))
    }

    fun setShadowDx(dx: Float) {
        pushHistory(_canvasState.value.copy(shadowDx = dx.coerceIn(-30f, 30f)))
    }

    fun setShadowDy(dy: Float) {
        pushHistory(_canvasState.value.copy(shadowDy = dy.coerceIn(-30f, 40f)))
    }

    fun setShadowColor(colorLong: Long) {
        pushHistory(_canvasState.value.copy(shadowColorLong = colorLong))
    }

    fun setLetterSpacing(spacing: Float) {
        pushHistory(_canvasState.value.copy(letterSpacing = spacing.coerceIn(-0.05f, 0.35f)))
    }

    fun setTextOpacity(opacity: Float) {
        pushHistory(_canvasState.value.copy(textOpacity = opacity.coerceIn(0.1f, 1.0f)))
    }

    fun toggleBold() {
        pushHistory(_canvasState.value.copy(isBold = !_canvasState.value.isBold))
    }

    fun toggleItalic() {
        pushHistory(_canvasState.value.copy(isItalic = !_canvasState.value.isItalic))
    }

    fun toggleUnderline() {
        pushHistory(_canvasState.value.copy(isUnderline = !_canvasState.value.isUnderline))
    }

    fun toggleLock() {
        _canvasState.update { it.copy(isLocked = !it.isLocked) }
    }

    fun toggleGridGuides() {
        _canvasState.update { it.copy(showGridGuides = !it.showGridGuides) }
    }

    fun setSolidBgColor(colorLong: Long?) {
        pushHistory(_canvasState.value.copy(solidBgColorLong = colorLong, isTransparentBg = false))
    }

    fun setRotationAngle(angle: Float) {
        _canvasState.update { it.copy(rotationAngle = angle) }
    }

    fun rotateStep(deltaAngle: Float) {
        val newAngle = ((_canvasState.value.rotationAngle + deltaAngle) % 360f)
        pushHistory(_canvasState.value.copy(rotationAngle = newAngle))
    }

    fun setTextAlign(alignment: TextAlignOption) {
        pushHistory(_canvasState.value.copy(textAlign = alignment))
    }

    fun setLineSpacing(multiplier: Float) {
        pushHistory(_canvasState.value.copy(lineSpacingMultiplier = multiplier.coerceIn(1.0f, 2.5f)))
    }

    fun setAspectRatio(ratio: CanvasAspectRatio) {
        pushHistory(_canvasState.value.copy(aspectRatio = ratio))
    }

    fun setBackgroundImageUri(uri: Uri?) {
        pushHistory(_canvasState.value.copy(backgroundImageUri = uri))
    }

    fun setBackgroundDim(dim: Float) {
        _canvasState.update { it.copy(backgroundDim = dim.coerceIn(0f, 0.95f)) }
    }

    fun setBackgroundGrayscale(enabled: Boolean) {
        pushHistory(_canvasState.value.copy(backgroundGrayscale = enabled))
    }

    fun setTransparentBg(enabled: Boolean) {
        pushHistory(_canvasState.value.copy(isTransparentBg = enabled))
    }

    fun setFontSize(size: Float) {
        _canvasState.update { it.copy(fontSize = size) }
    }

    fun setTextBehindSubject(enabled: Boolean) {
        pushHistory(_canvasState.value.copy(isTextBehindSubject = enabled))
    }

    fun updateTransform(normX: Float, normY: Float, scale: Float, rotation: Float) {
        _canvasState.update {
            it.copy(
                normOffsetX = normX,
                normOffsetY = normY,
                scaleFactor = scale,
                rotationAngle = rotation
            )
        }
    }

    fun commitTransform(normX: Float, normY: Float, scale: Float, rotation: Float) {
        pushHistory(
            _canvasState.value.copy(
                normOffsetX = normX,
                normOffsetY = normY,
                scaleFactor = scale,
                rotationAngle = rotation
            )
        )
    }

    fun alignCenter() {
        pushHistory(_canvasState.value.copy(normOffsetX = 0f, normOffsetY = 0f, rotationAngle = 0f))
    }

    fun alignTop() {
        pushHistory(_canvasState.value.copy(normOffsetX = 0f, normOffsetY = -0.28f, rotationAngle = 0f))
    }

    fun alignBottom() {
        pushHistory(_canvasState.value.copy(normOffsetX = 0f, normOffsetY = 0.28f, rotationAngle = 0f))
    }

    fun snapToPosition(normX: Float, normY: Float, textAlign: TextAlignOption? = null) {
        val updated = _canvasState.value.copy(
            normOffsetX = normX,
            normOffsetY = normY,
            textAlign = textAlign ?: _canvasState.value.textAlign
        )
        pushHistory(updated)
    }

    fun nudgeOffset(deltaNormX: Float, deltaNormY: Float) {
        val newX = (_canvasState.value.normOffsetX + deltaNormX).coerceIn(-0.45f, 0.45f)
        val newY = (_canvasState.value.normOffsetY + deltaNormY).coerceIn(-0.45f, 0.45f)
        pushHistory(_canvasState.value.copy(normOffsetX = newX, normOffsetY = newY))
    }

    fun resetCanvasTransform() {
        pushHistory(
            _canvasState.value.copy(
                normOffsetX = 0f,
                normOffsetY = 0f,
                scaleFactor = 1.0f,
                rotationAngle = 0f
            )
        )
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    fun loadProjectsList(context: Context) {
        viewModelScope.launch {
            _projectsList.value = AtharProjectManager.listProjects(context)
        }
    }

    fun saveCurrentProject(context: Context, title: String? = null, onSaved: ((AtharProjectMeta) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val meta = AtharProjectManager.saveProject(context, _canvasState.value, title)
                loadProjectsList(context)
                onSaved?.invoke(meta)
                _exportMessage.value = "تم حفظ المشروع في مسوداتي بنجاح"
            } catch (e: Exception) {
                _exportMessage.value = "فشل حفظ المشروع: ${e.message}"
            }
        }
    }

    fun exportProjectAsAthar(context: Context, title: String? = null) {
        viewModelScope.launch {
            try {
                val meta = AtharProjectManager.saveProject(context, _canvasState.value, title)
                val file = AtharProjectManager.getExportableProjectFile(context, meta.id, meta.title)
                val uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة مشروع أَثَــر (.athar)"))
                _exportMessage.value = "تم تجهيز ملف المشروع (.athar) للمشاركة"
            } catch (e: Exception) {
                _exportMessage.value = "فشل تصدير المشروع: ${e.message}"
            }
        }
    }

    fun openProjectById(context: Context, projectId: String) {
        viewModelScope.launch {
            try {
                val state = AtharProjectManager.loadProjectById(context, projectId)
                pushHistory(state)
                _exportMessage.value = "تم فتح المشروع بنجاح"
            } catch (e: Exception) {
                _exportMessage.value = "فشل فتح المشروع: ${e.message}"
            }
        }
    }

    fun openProjectFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("تعذر قراءة الملف")
                val state = stream.use { AtharProjectManager.loadProjectFromStream(context, it) }
                pushHistory(state)
                _exportMessage.value = "تم استيراد المشروع بنجاح"
            } catch (e: Exception) {
                _exportMessage.value = "فشل استيراد المشروع: ${e.message}"
            }
        }
    }

    fun deleteProject(context: Context, projectId: String) {
        viewModelScope.launch {
            AtharProjectManager.deleteProject(context, projectId)
            loadProjectsList(context)
        }
    }

    private fun scheduleKashidaUpdate(
        text: String,
        level: Int,
        mode: KashidaMode = _canvasState.value.kashidaMode,
        targetIndex: Int = _canvasState.value.kashidaTargetIndex
    ) {
        kashidaDebounceJob?.cancel()
        kashidaDebounceJob = viewModelScope.launch(Dispatchers.Default) {
            updateKashidaInternal(text, level, mode, targetIndex)
        }
    }

    private fun updateKashidaInternal(
        text: String,
        level: Int,
        mode: KashidaMode = _canvasState.value.kashidaMode,
        targetIndex: Int = _canvasState.value.kashidaTargetIndex
    ) {
        val stretched = AtharNativeEngine.applyKashida(text, level, mode, targetIndex)
        _canvasState.update { it.copy(displayText = stretched) }
    }

    fun exportDesign(
        context: Context,
        resolution: ExportResolution = ExportResolution.HD_1080P,
        isTransparent: Boolean = false,
        isStickerTrimmed: Boolean = true,
        onShareRequested: Boolean = true
    ) {
        if (_isExporting.value) return

        viewModelScope.launch {
            _isExporting.value = true
            val result = AtharExportManager.exportDesign(
                context = context,
                state = _canvasState.value,
                resolution = resolution,
                isTransparent = isTransparent,
                isStickerTrimmed = isStickerTrimmed,
                onShareRequested = onShareRequested
            )
            _isExporting.value = false
            if (result.isSuccess) {
                _exportMessage.value = result.getOrNull()
            } else {
                _exportMessage.value = "فشل التصدير: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}
