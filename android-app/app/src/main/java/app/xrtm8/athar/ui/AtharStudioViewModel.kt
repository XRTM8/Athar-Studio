package app.xrtm8.athar.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.xrtm8.athar.core.AtharPaletteExtractor
import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.export.AtharExportManager
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasGradientDirection
import app.xrtm8.athar.model.CanvasGradientScrim
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.CustomFont
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.FilmPreset
import app.xrtm8.athar.model.GlassBackdropPreset
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.LayerBlendMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.project.AtharProjectManager
import app.xrtm8.athar.project.AtharProjectMeta
import app.xrtm8.athar.typography.ArabicParagraphJustifier
import app.xrtm8.athar.typography.AtharFontManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.UUID

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

    private val _extractedPalette = MutableStateFlow<AtharPaletteExtractor.ExtractedPalette?>(null)
    val extractedPalette: StateFlow<AtharPaletteExtractor.ExtractedPalette?> = _extractedPalette.asStateFlow()

    private val _isAmoledWhite = MutableStateFlow(false)
    val isAmoledWhite: StateFlow<Boolean> = _isAmoledWhite.asStateFlow()

    private val _customFonts = MutableStateFlow<List<CustomFont>>(emptyList())
    val customFonts: StateFlow<List<CustomFont>> = _customFonts.asStateFlow()

    private var copiedLayerStyle: TextLayer? = null
    private val _hasCopiedStyle = MutableStateFlow(false)
    val hasCopiedStyle: StateFlow<Boolean> = _hasCopiedStyle.asStateFlow()

    fun initTheme(context: Context) {
        try {
            val sp = context.getSharedPreferences("athar_prefs", Context.MODE_PRIVATE)
            _isAmoledWhite.value = sp.getBoolean("pref_amoled_white", false)
        } catch (_: Exception) {}
    }

    fun toggleAmoledWhite(context: Context) {
        val next = !_isAmoledWhite.value
        _isAmoledWhite.value = next
        try {
            val sp = context.getSharedPreferences("athar_prefs", Context.MODE_PRIVATE)
            sp.edit().putBoolean("pref_amoled_white", next).apply()
        } catch (_: Exception) {}
    }

    private var kashidaDebounceJob: Job? = null
    private var sliderDragInitialState: CanvasState? = null

    init {
        updateKashidaInternal(
            _canvasState.value.rawText,
            _canvasState.value.stretchLevel,
            _canvasState.value.kashidaMode,
            _canvasState.value.kashidaTargetIndex
        )
    }

    private fun markSliderChange() {
        if (sliderDragInitialState == null) {
            sliderDragInitialState = _canvasState.value
        }
    }

    fun commitHistoryCheckpoint() {
        val initial = sliderDragInitialState
        val current = _canvasState.value
        if (initial != null && initial != current) {
            undoStack.addLast(initial)
            if (undoStack.size > 100) undoStack.removeFirst()
            redoStack.clear()
            _canUndo.value = true
            _canRedo.value = false
        }
        sliderDragInitialState = null
    }

    private fun pushHistory(newState: CanvasState) {
        if (_canvasState.value != newState) {
            undoStack.addLast(_canvasState.value)
            if (undoStack.size > 100) undoStack.removeFirst()
            redoStack.clear()
            _canUndo.value = true
            _canRedo.value = false
            _canvasState.value = newState
        }
    }

    private fun updateActiveLayer(transform: (TextLayer) -> TextLayer) {
        val current = _canvasState.value
        val activeId = current.activeLayer.id
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == activeId) transform(layer) else layer
        }
        _canvasState.update { it.copy(layers = updatedLayers) }
    }

    private fun pushActiveLayerHistory(transform: (TextLayer) -> TextLayer) {
        val current = _canvasState.value
        val activeId = current.activeLayer.id
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == activeId) transform(layer) else layer
        }
        pushHistory(current.copy(layers = updatedLayers))
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

    // ==========================================
    // Multi-Layer Architecture Operations
    // ==========================================

    fun addTextLayer(initialText: String = "نص جديد") {
        val current = _canvasState.value
        val newLayer = TextLayer(
            id = UUID.randomUUID().toString(),
            name = "نص ${current.layers.size + 1}",
            rawText = initialText,
            displayText = initialText,
            fontSize = 44f,
            normOffsetX = 0f,
            normOffsetY = (current.layers.size * 0.08f).coerceAtMost(0.35f)
        )
        val newLayers = current.layers + newLayer
        pushHistory(current.copy(layers = newLayers, activeLayerId = newLayer.id))
    }

    fun duplicateActiveLayer() {
        val current = _canvasState.value
        val active = current.activeLayer
        val dup = active.copy(
            id = UUID.randomUUID().toString(),
            name = "${active.name} (نسخة)",
            normOffsetX = (active.normOffsetX + 0.04f).coerceAtMost(0.4f),
            normOffsetY = (active.normOffsetY + 0.04f).coerceAtMost(0.4f)
        )
        val newLayers = current.layers + dup
        pushHistory(current.copy(layers = newLayers, activeLayerId = dup.id))
    }

    fun removeActiveLayer() {
        val current = _canvasState.value
        if (current.layers.size <= 1) {
            pushActiveLayerHistory { it.copy(rawText = "", displayText = "") }
            return
        }
        val activeId = current.activeLayer.id
        val remaining = current.layers.filter { it.id != activeId }
        val nextActive = remaining.lastOrNull()?.id ?: ""
        pushHistory(current.copy(layers = remaining, activeLayerId = nextActive))
    }

    fun moveActiveLayerUp() {
        val current = _canvasState.value
        val activeId = current.activeLayer.id
        val index = current.layers.indexOfFirst { it.id == activeId }
        if (index >= 0 && index < current.layers.size - 1) {
            val mutable = current.layers.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(index + 1, item)
            pushHistory(current.copy(layers = mutable))
        }
    }

    fun moveActiveLayerDown() {
        val current = _canvasState.value
        val activeId = current.activeLayer.id
        val index = current.layers.indexOfFirst { it.id == activeId }
        if (index > 0) {
            val mutable = current.layers.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(index - 1, item)
            pushHistory(current.copy(layers = mutable))
        }
    }

    fun selectLayer(layerId: String) {
        if (_canvasState.value.activeLayerId != layerId) {
            _canvasState.update { it.copy(activeLayerId = layerId) }
        }
    }

    // ==========================================
    // Active Text Layer Operations
    // ==========================================

    fun setText(newText: String) {
        pushActiveLayerHistory { it.copy(rawText = newText) }
        val active = _canvasState.value.activeLayer
        scheduleKashidaUpdate(newText, active.stretchLevel, active.kashidaMode, active.kashidaTargetIndex)
    }

    fun setStretchLevel(newLevel: Int) {
        if (newLevel == _canvasState.value.stretchLevel) return
        pushActiveLayerHistory { it.copy(stretchLevel = newLevel) }
        val active = _canvasState.value.activeLayer
        scheduleKashidaUpdate(active.rawText, newLevel, active.kashidaMode, active.kashidaTargetIndex)
    }

    fun setKashidaMode(mode: KashidaMode) {
        if (mode == _canvasState.value.kashidaMode) return
        pushActiveLayerHistory { it.copy(kashidaMode = mode) }
        val active = _canvasState.value.activeLayer
        scheduleKashidaUpdate(active.rawText, active.stretchLevel, mode, active.kashidaTargetIndex)
    }

    fun setKashidaTargetIndex(targetIndex: Int) {
        pushActiveLayerHistory { it.copy(kashidaTargetIndex = targetIndex, kashidaMode = KashidaMode.CUSTOM) }
        val active = _canvasState.value.activeLayer
        scheduleKashidaUpdate(active.rawText, active.stretchLevel, KashidaMode.CUSTOM, targetIndex)
    }

    fun justifyActiveLayerLines(context: Context) {
        val active = _canvasState.value.activeLayer
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = AtharFontManager.getTypeface(context, active.selectedFont, active.customFontPath)
            textSize = active.fontSize * 3f
            letterSpacing = active.letterSpacing
        }
        val justified = ArabicParagraphJustifier.justifyLines(active.rawText, paint)
        if (justified != active.displayText) {
            pushActiveLayerHistory { it.copy(displayText = justified) }
        }
    }

    fun setEffect(effect: VisualEffect) {
        val mappedPreset = when (effect) {
            VisualEffect.ROYAL_GOLD -> app.xrtm8.athar.model.LuxuryGradientPreset.ANDALUSIAN_GOLD
            VisualEffect.LIQUID_CHROME -> app.xrtm8.athar.model.LuxuryGradientPreset.TITANIUM_CHROME
            VisualEffect.CYBER_NEON -> app.xrtm8.athar.model.LuxuryGradientPreset.CYBER_EMERALD
            VisualEffect.FROSTED_GLASS -> app.xrtm8.athar.model.LuxuryGradientPreset.FROSTED_PEARL
            else -> app.xrtm8.athar.model.LuxuryGradientPreset.NONE
        }
        val customColor = when (effect) {
            VisualEffect.MINIMAL_LUXURY -> 0xFFFFFFFF
            VisualEffect.CINEMATIC_SHADOW -> 0xFFF2F2F2
            else -> null
        }
        pushActiveLayerHistory {
            it.copy(
                selectedEffect = effect,
                gradientPreset = mappedPreset,
                customTextColorLong = customColor
            )
        }
    }

    fun setFont(font: ArabicFontFamily) {
        pushActiveLayerHistory { it.copy(selectedFont = font, customFontPath = null, customFontName = null) }
    }

    fun setCustomColor(colorLong: Long?) {
        pushActiveLayerHistory {
            it.copy(
                customTextColorLong = colorLong,
                gradientPreset = if (colorLong != null) app.xrtm8.athar.model.LuxuryGradientPreset.NONE else it.gradientPreset
            )
        }
    }

    fun setStrokeWidth(width: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(strokeWidth = width.coerceIn(0f, 20f)) }
    }

    fun setStrokeColor(colorLong: Long) {
        pushActiveLayerHistory { it.copy(strokeColorLong = colorLong) }
    }

    fun setShadowRadius(radius: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(shadowRadius = radius.coerceIn(0f, 60f)) }
    }

    fun setShadowDx(dx: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(shadowDx = dx.coerceIn(-30f, 30f)) }
    }

    fun setShadowDy(dy: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(shadowDy = dy.coerceIn(-30f, 40f)) }
    }

    fun setShadowColor(colorLong: Long) {
        pushActiveLayerHistory { it.copy(shadowColorLong = colorLong) }
    }

    fun setLetterSpacing(spacing: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(letterSpacing = spacing.coerceIn(-0.05f, 0.35f)) }
    }

    fun setTextOpacity(opacity: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(textOpacity = opacity.coerceIn(0.1f, 1.0f)) }
    }

    fun toggleBold() {
        pushActiveLayerHistory { it.copy(isBold = !it.isBold) }
    }

    fun toggleItalic() {
        pushActiveLayerHistory { it.copy(isItalic = !it.isItalic) }
    }

    fun toggleUnderline() {
        pushActiveLayerHistory { it.copy(isUnderline = !it.isUnderline) }
    }

    fun toggleLock() {
        updateActiveLayer { it.copy(isLocked = !it.isLocked) }
    }

    fun toggleGridGuides() {
        _canvasState.update { it.copy(showGridGuides = !it.showGridGuides) }
    }

    fun setRotationAngle(angle: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(rotationAngle = (angle % 360f + 360f) % 360f) }
    }

    fun setScale(scale: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(scaleFactor = scale.coerceIn(0.3f, 4.0f)) }
    }

    fun setTextAlign(alignment: TextAlignOption) {
        pushActiveLayerHistory { it.copy(textAlign = alignment) }
    }

    fun setLineSpacing(multiplier: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(lineSpacingMultiplier = multiplier.coerceIn(1.0f, 2.5f)) }
    }

    fun setBoxWidthRatio(ratio: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(boxWidthRatio = ratio.coerceIn(0.30f, 1.0f)) }
    }

    fun toggleGlassBackdrop(enabled: Boolean) {
        pushActiveLayerHistory { it.copy(enableGlassBackdrop = enabled) }
    }

    fun setGlassBackdropOpacity(opacity: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(glassBackdropOpacity = opacity.coerceIn(0.1f, 0.98f)) }
    }

    fun setGlassBackdropCornerRadius(radius: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(glassBackdropCornerRadius = radius.coerceIn(4f, 48f)) }
    }

    fun setGlassBackdropPadding(padding: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(glassBackdropPadding = padding.coerceIn(8f, 56f)) }
    }

    fun setGlassBackdropPreset(preset: GlassBackdropPreset) {
        pushActiveLayerHistory {
            it.copy(
                enableGlassBackdrop = true,
                glassBackdropColorLong = preset.bgArgb,
                glassBackdropBorderColorLong = preset.strokeArgb,
                glassBackdropOpacity = preset.opacity
            )
        }
    }

    fun setFontSize(size: Float) {
        updateActiveLayer { it.copy(fontSize = size.coerceIn(6f, 100f)) }
    }

    fun updateTransform(normX: Float, normY: Float, scale: Float, rotation: Float) {
        updateActiveLayer {
            it.copy(
                normOffsetX = normX,
                normOffsetY = normY,
                scaleFactor = scale,
                rotationAngle = rotation
            )
        }
    }

    fun commitTransform(normX: Float, normY: Float, scale: Float, rotation: Float) {
        pushActiveLayerHistory {
            it.copy(
                normOffsetX = normX,
                normOffsetY = normY,
                scaleFactor = scale,
                rotationAngle = rotation
            )
        }
    }

    fun alignCenter() {
        pushActiveLayerHistory { it.copy(normOffsetX = 0f, normOffsetY = 0f, rotationAngle = 0f) }
    }

    fun alignTop() {
        pushActiveLayerHistory { it.copy(normOffsetX = 0f, normOffsetY = -0.28f, rotationAngle = 0f) }
    }

    fun alignBottom() {
        pushActiveLayerHistory { it.copy(normOffsetX = 0f, normOffsetY = 0.28f, rotationAngle = 0f) }
    }

    fun snapToPosition(normX: Float, normY: Float, textAlign: TextAlignOption? = null) {
        pushActiveLayerHistory {
            it.copy(
                normOffsetX = normX,
                normOffsetY = normY,
                textAlign = textAlign ?: it.textAlign
            )
        }
    }

    fun nudgeOffset(deltaNormX: Float, deltaNormY: Float) {
        pushActiveLayerHistory {
            it.copy(
                normOffsetX = (it.normOffsetX + deltaNormX).coerceIn(-0.45f, 0.45f),
                normOffsetY = (it.normOffsetY + deltaNormY).coerceIn(-0.45f, 0.45f)
            )
        }
    }

    fun resetCanvasTransform() {
        pushActiveLayerHistory {
            it.copy(
                normOffsetX = 0f,
                normOffsetY = 0f,
                scaleFactor = 1.0f,
                rotationAngle = 0f
            )
        }
    }

    // ==========================================
    // Global Canvas & Photo Background Operations
    // ==========================================

    fun setAspectRatio(ratio: CanvasAspectRatio) {
        pushHistory(_canvasState.value.copy(aspectRatio = ratio))
    }

    fun setSolidBgColor(colorLong: Long?) {
        pushHistory(_canvasState.value.copy(solidBgColorLong = colorLong, isTransparentBg = false))
    }

    fun setBackgroundImageUri(uri: Uri?) {
        pushHistory(_canvasState.value.copy(backgroundImageUri = uri, bgPhotoOffsetX = 0f, bgPhotoOffsetY = 0f, bgPhotoScale = 1.0f))
    }

    fun setBackgroundDim(dim: Float) {
        markSliderChange()
        _canvasState.update { it.copy(backgroundDim = dim.coerceIn(0f, 0.95f)) }
    }

    fun setBackgroundBlur(blur: Float) {
        markSliderChange()
        _canvasState.update { it.copy(backgroundBlur = blur.coerceIn(0f, 35f)) }
    }

    fun setBackgroundBrightness(brightness: Float) {
        markSliderChange()
        _canvasState.update { it.copy(backgroundBrightness = brightness.coerceIn(-0.5f, 0.5f)) }
    }

    fun setBackgroundContrast(contrast: Float) {
        markSliderChange()
        _canvasState.update { it.copy(backgroundContrast = contrast.coerceIn(0.5f, 1.8f)) }
    }

    fun setBackgroundSaturation(saturation: Float) {
        markSliderChange()
        _canvasState.update { it.copy(backgroundSaturation = saturation.coerceIn(0f, 2.0f)) }
    }

    fun setBackgroundVignette(vignette: Float) {
        markSliderChange()
        _canvasState.update { it.copy(backgroundVignette = vignette.coerceIn(0f, 1.0f)) }
    }

    fun setBackgroundGrayscale(enabled: Boolean) {
        pushHistory(_canvasState.value.copy(backgroundGrayscale = enabled))
    }

    fun setTransparentBg(enabled: Boolean) {
        pushHistory(_canvasState.value.copy(isTransparentBg = enabled))
    }

    // ==========================================
    // Directional Gradient Scrim Operations
    // ==========================================

    fun setGradientScrimEnabled(enabled: Boolean) {
        pushHistory(_canvasState.value.copy(
            gradientScrim = _canvasState.value.gradientScrim.copy(isEnabled = enabled)
        ))
    }

    fun setGradientScrimDirection(direction: CanvasGradientDirection) {
        pushHistory(_canvasState.value.copy(
            gradientScrim = _canvasState.value.gradientScrim.copy(direction = direction)
        ))
    }

    fun setGradientScrimColor(colorLong: Long) {
        pushHistory(_canvasState.value.copy(
            gradientScrim = _canvasState.value.gradientScrim.copy(colorLong = colorLong)
        ))
    }

    fun setGradientScrimOpacity(opacity: Float) {
        markSliderChange()
        _canvasState.update {
            it.copy(gradientScrim = it.gradientScrim.copy(opacity = opacity.coerceIn(0f, 1f)))
        }
    }

    fun setGradientScrimMidpoint(midpoint: Float) {
        markSliderChange()
        _canvasState.update {
            it.copy(gradientScrim = it.gradientScrim.copy(midpoint = midpoint.coerceIn(0.1f, 0.9f)))
        }
    }

    // ==========================================
    // Photo Framing Pan & Zoom Operations
    // ==========================================

    fun toggleFramingMode(enabled: Boolean) {
        _canvasState.update { it.copy(isFramingMode = enabled) }
    }

    fun commitPhotoFraming(normX: Float, normY: Float, scale: Float) {
        pushHistory(_canvasState.value.copy(
            bgPhotoOffsetX = normX.coerceIn(-1f, 1f),
            bgPhotoOffsetY = normY.coerceIn(-1f, 1f),
            bgPhotoScale = scale.coerceIn(0.5f, 4.0f)
        ))
    }

    fun setBgPhotoOffset(normX: Float, normY: Float) {
        markSliderChange()
        _canvasState.update {
            it.copy(
                bgPhotoOffsetX = normX.coerceIn(-1f, 1f),
                bgPhotoOffsetY = normY.coerceIn(-1f, 1f)
            )
        }
    }

    fun setBgPhotoScale(scale: Float) {
        markSliderChange()
        _canvasState.update {
            it.copy(bgPhotoScale = scale.coerceIn(0.5f, 4.0f))
        }
    }

    // ==========================================
    // Film Camera Presets Operations
    // ==========================================

    fun setFilmPreset(preset: FilmPreset) {
        pushHistory(_canvasState.value.copy(filmPreset = preset))
    }

    // ==========================================
    // Blend Modes & Curved Path Operations
    // ==========================================

    fun setBlendMode(mode: LayerBlendMode) {
        pushActiveLayerHistory { it.copy(blendMode = mode) }
    }

    fun toggleCurvedText(enabled: Boolean) {
        pushActiveLayerHistory { it.copy(isCurved = enabled) }
    }

    fun setCurveAngle(angle: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(curveAngle = angle.coerceIn(-180f, 180f)) }
    }

    // ==========================================
    // Color Palette Extraction Operations
    // ==========================================

    fun loadExtractedPalette(context: Context) {
        val uri = _canvasState.value.backgroundImageUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@launch
                val palette = AtharPaletteExtractor.extractPalette(bmp)
                bmp.recycle()
                withContext(Dispatchers.Main) {
                    _extractedPalette.value = palette
                }
            } catch (e: Exception) {
                // Ignore gracefully
            }
        }
    }

    fun autoBalanceTypographicBlock() {
        val active = _canvasState.value.activeLayer
        if (!active.displayText.contains("\n")) return

        val balanced = ArabicParagraphJustifier.balanceArabicBlock(active.displayText)
        pushActiveLayerHistory { it.copy(displayText = balanced) }
    }

    // ==========================================
    // Project & Export Management
    // ==========================================

    fun createNewProject(
        aspectRatio: CanvasAspectRatio = CanvasAspectRatio.STORY_9_16,
        initialText: String = ""
    ) {
        val newLayer = TextLayer(
            id = "layer_primary",
            name = "نص 1",
            rawText = initialText,
            displayText = initialText
        )
        val newState = CanvasState(
            layers = listOf(newLayer),
            activeLayerId = "layer_primary",
            aspectRatio = aspectRatio,
            solidBgColorLong = null,
            backgroundImageUri = null,
            backgroundDim = 0.25f,
            backgroundBlur = 0f
        )
        _canvasState.value = newState
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
    }

    fun createNewProjectWithImage(
        context: Context,
        imageUri: Uri,
        aspectRatio: CanvasAspectRatio = CanvasAspectRatio.STORY_9_16
    ) {
        createNewProject(aspectRatio, initialText = "")
        setBackgroundImageUri(imageUri)
        loadExtractedPalette(context)
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

    fun shareProjectById(context: Context, projectId: String) {
        viewModelScope.launch {
            try {
                val proj = _projectsList.value.find { it.id == projectId }
                val file = AtharProjectManager.getExportableProjectFile(context, projectId, proj?.title ?: "مشروع_أثر")
                val uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة مشروع أَثَــر (.athar)"))
                _exportMessage.value = "تم تجهيز ملف المشروع للمشاركة"
            } catch (e: Exception) {
                _exportMessage.value = "فشل مشاركة المشروع: ${e.message}"
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

    fun renameProject(context: Context, projectId: String, newTitle: String) {
        viewModelScope.launch {
            val success = AtharProjectManager.renameProject(context, projectId, newTitle)
            if (success) {
                loadProjectsList(context)
                _exportMessage.value = "تمت إعادة تسمية المشروع بنجاح"
            } else {
                _exportMessage.value = "تعذرت إعادة تسمية المشروع"
            }
        }
    }

    // ==========================================
    // Custom Font Import & Management
    // ==========================================

    fun loadCustomFonts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = AtharFontManager.listCustomFonts(context)
            _customFonts.value = list
        }
    }

    fun importCustomFont(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val font = AtharFontManager.importFontFromUri(context, uri)
            if (font != null) {
                loadCustomFonts(context)
                setCustomFont(font)
                _exportMessage.value = "تم استيراد الخط: ${font.name}"
            } else {
                _exportMessage.value = "تعذر استيراد الخط، تأكد من صحة الملف (.ttf / .otf)"
            }
        }
    }

    fun deleteCustomFont(context: Context, fontId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = AtharFontManager.deleteCustomFont(context, fontId)
            if (success) {
                loadCustomFonts(context)
                // If active layer was using this font, reset it
                val active = _canvasState.value.activeLayer
                if (active.customFontPath?.contains(fontId) == true) {
                    setCustomFont(null)
                }
                _exportMessage.value = "تم حذف الخط المخصص"
            }
        }
    }

    fun setCustomFont(customFont: CustomFont?) {
        pushActiveLayerHistory {
            it.copy(
                customFontPath = customFont?.filePath,
                customFontName = customFont?.name
            )
        }
    }

    // ==========================================
    // Copy / Paste Layer Styling
    // ==========================================

    fun copyActiveLayerStyle() {
        val active = _canvasState.value.activeLayer
        copiedLayerStyle = active.copy()
        _hasCopiedStyle.value = true
        _exportMessage.value = "تم نسخ تنسيق الطبقة"
    }

    fun pasteStyleToActiveLayer() {
        val style = copiedLayerStyle ?: return
        pushActiveLayerHistory { target ->
            target.copy(
                selectedFont = style.selectedFont,
                customFontPath = style.customFontPath,
                customFontName = style.customFontName,
                selectedEffect = style.selectedEffect,
                fontSize = style.fontSize,
                customTextColorLong = style.customTextColorLong,
                strokeWidth = style.strokeWidth,
                strokeColorLong = style.strokeColorLong,
                shadowRadius = style.shadowRadius,
                shadowDx = style.shadowDx,
                shadowDy = style.shadowDy,
                shadowColorLong = style.shadowColorLong,
                textAlign = style.textAlign,
                lineSpacingMultiplier = style.lineSpacingMultiplier,
                letterSpacing = style.letterSpacing,
                textOpacity = style.textOpacity,
                isBold = style.isBold,
                isItalic = style.isItalic,
                isUnderline = style.isUnderline,
                enableGlassBackdrop = style.enableGlassBackdrop,
                glassBackdropOpacity = style.glassBackdropOpacity,
                glassBackdropColorLong = style.glassBackdropColorLong,
                glassBackdropBorderColorLong = style.glassBackdropBorderColorLong,
                glassBackdropCornerRadius = style.glassBackdropCornerRadius,
                glassBackdropPadding = style.glassBackdropPadding,
                blendMode = style.blendMode,
                gradientPreset = style.gradientPreset,
                gradientAngle = style.gradientAngle,
                isRadialGradient = style.isRadialGradient,
                isHollowText = style.isHollowText
            )
        }
        _exportMessage.value = "تم تطبيق التنسيق على الطبقة"
    }

    private fun scheduleKashidaUpdate(
        text: String,
        level: Int,
        mode: KashidaMode,
        targetIndex: Int
    ) {
        kashidaDebounceJob?.cancel()
        kashidaDebounceJob = viewModelScope.launch(Dispatchers.Default) {
            updateKashidaInternal(text, level, mode, targetIndex)
        }
    }

    private fun updateKashidaInternal(
        text: String,
        level: Int,
        mode: KashidaMode,
        targetIndex: Int
    ) {
        val stretched = PureArabicKashidaEngine.stretchText(text, level, mode, targetIndex)
        updateActiveLayer { it.copy(displayText = stretched) }
    }

    // ==========================================
    // Multi-Stop Gradients, Hollow Text, 3-Way Color Grading
    // ==========================================

    fun setLuxuryGradient(preset: app.xrtm8.athar.model.LuxuryGradientPreset) {
        pushActiveLayerHistory { it.copy(gradientPreset = preset, customTextColorLong = null) }
    }

    fun setGradientAngle(angle: Float) {
        markSliderChange()
        updateActiveLayer { it.copy(gradientAngle = angle % 360f) }
    }

    fun setGradientRadial(isRadial: Boolean) {
        pushActiveLayerHistory { it.copy(isRadialGradient = isRadial) }
    }

    fun setHollowText(isHollow: Boolean) {
        pushActiveLayerHistory { it.copy(isHollowText = isHollow) }
    }

    fun setColorGradingEnabled(enabled: Boolean) {
        pushHistory(_canvasState.value.copy(colorGradingEnabled = enabled))
    }

    fun setColorGradingLift(r: Float, g: Float, b: Float) {
        markSliderChange()
        _canvasState.update { it.copy(liftRed = r.coerceIn(-0.5f, 0.5f), liftGreen = g.coerceIn(-0.5f, 0.5f), liftBlue = b.coerceIn(-0.5f, 0.5f)) }
    }

    fun setColorGradingGamma(r: Float, g: Float, b: Float) {
        markSliderChange()
        _canvasState.update { it.copy(gammaRed = r.coerceIn(-0.5f, 0.5f), gammaGreen = g.coerceIn(-0.5f, 0.5f), gammaBlue = b.coerceIn(-0.5f, 0.5f)) }
    }

    fun setColorGradingGain(r: Float, g: Float, b: Float) {
        markSliderChange()
        _canvasState.update { it.copy(gainRed = r.coerceIn(-0.5f, 0.5f), gainGreen = g.coerceIn(-0.5f, 0.5f), gainBlue = b.coerceIn(-0.5f, 0.5f)) }
    }

    fun resetColorGrading() {
        pushHistory(_canvasState.value.copy(
            liftRed = 0f, liftGreen = 0f, liftBlue = 0f,
            gammaRed = 0f, gammaGreen = 0f, gammaBlue = 0f,
            gainRed = 0f, gainGreen = 0f, gainBlue = 0f
        ))
    }

    // ==========================================
    // 8-Channel Lightroom Color Mix
    // ==========================================

    fun setColorMixEnabled(enabled: Boolean) {
        pushHistory(_canvasState.value.copy(
            colorMixState = _canvasState.value.colorMixState.copy(isEnabled = enabled)
        ))
    }

    fun setColorMixChannel(channel: app.xrtm8.athar.model.ColorMixChannel, hue: Float, sat: Float, lum: Float) {
        markSliderChange()
        val cur = _canvasState.value.colorMixState
        val map = cur.channelAdjustments.toMutableMap()
        map[channel] = app.xrtm8.athar.model.ChannelAdjustment(
            hueShift = hue.coerceIn(-50f, 50f),
            satShift = sat.coerceIn(-100f, 100f),
            lumShift = lum.coerceIn(-100f, 100f)
        )
        _canvasState.update { it.copy(colorMixState = cur.copy(isEnabled = true, channelAdjustments = map)) }
    }

    fun setIsolatedColorChannel(channel: app.xrtm8.athar.model.ColorMixChannel?) {
        pushHistory(_canvasState.value.copy(
            colorMixState = _canvasState.value.colorMixState.copy(isEnabled = true, isolatedChannel = channel, monochromeChannel = null)
        ))
    }

    fun setMonochromeColorChannel(channel: app.xrtm8.athar.model.ColorMixChannel?) {
        pushHistory(_canvasState.value.copy(
            colorMixState = _canvasState.value.colorMixState.copy(isEnabled = true, monochromeChannel = channel, isolatedChannel = null)
        ))
    }

    fun resetColorMixChannel(channel: app.xrtm8.athar.model.ColorMixChannel) {
        val cur = _canvasState.value.colorMixState
        val map = cur.channelAdjustments.toMutableMap()
        map.remove(channel)
        pushHistory(_canvasState.value.copy(colorMixState = cur.copy(channelAdjustments = map)))
    }

    fun resetAllColorMix() {
        pushHistory(_canvasState.value.copy(
            colorMixState = app.xrtm8.athar.model.ColorMixState()
        ))
    }

    fun resetPhotoAdjustments() {
        pushHistory(_canvasState.value.copy(
            backgroundBlur = 0f,
            backgroundBrightness = 0f,
            backgroundContrast = 1.0f,
            backgroundSaturation = 1.0f,
            backgroundVignette = 0f,
            backgroundDim = 0.25f
        ))
    }

    fun exportDesign(
        context: Context,
        resolution: ExportResolution = ExportResolution.HD_1080P,
        isTransparent: Boolean = false,
        isStickerTrimmed: Boolean = true,
        onShareRequested: Boolean = false
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
