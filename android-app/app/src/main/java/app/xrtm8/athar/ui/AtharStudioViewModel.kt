package app.xrtm8.athar.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.xrtm8.athar.core.AtharNativeEngine
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.project.AtharProjectManager
import app.xrtm8.athar.project.AtharProjectMeta
import app.xrtm8.athar.typography.AtharFontManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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

    private fun decodeSampledBitmap(context: Context, uri: Uri, targetW: Int, targetH: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            var inSampleSize = 1
            val halfH = options.outHeight / 2
            val halfW = options.outWidth / 2
            while ((halfH / inSampleSize) >= targetH && (halfW / inSampleSize) >= targetW) {
                inSampleSize *= 2
            }
            val finalOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, finalOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cropTransparentBitmap(src: Bitmap, paddingPx: Int = 48): Bitmap {
        val width = src.width
        val height = src.height
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        val rowPixels = IntArray(width)
        for (y in 0 until height) {
            src.getPixels(rowPixels, 0, width, 0, y, width, 1)
            var rowHasVisible = false
            for (x in 0 until width) {
                val alpha = (rowPixels[x] ushr 24) and 0xFF
                if (alpha > 12) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    rowHasVisible = true
                }
            }
            if (rowHasVisible) {
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }

        if (maxX < minX || maxY < minY) {
            return src
        }

        val cropLeft = (minX - paddingPx).coerceAtLeast(0)
        val cropTop = (minY - paddingPx).coerceAtLeast(0)
        val cropRight = (maxX + paddingPx).coerceAtMost(width - 1)
        val cropBottom = (maxY + paddingPx).coerceAtMost(height - 1)
        val cropW = (cropRight - cropLeft + 1).coerceAtLeast(1)
        val cropH = (cropBottom - cropTop + 1).coerceAtLeast(1)

        return Bitmap.createBitmap(src, cropLeft, cropTop, cropW, cropH)
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
            val currentState = _canvasState.value

            val exportResult = withContext(Dispatchers.IO) {
                try {
                    val width = resolution.width
                    val height = when (currentState.aspectRatio) {
                        CanvasAspectRatio.STORY_9_16 -> (width * 16f / 9f).toInt()
                        CanvasAspectRatio.SQUARE_1_1 -> width
                        CanvasAspectRatio.PORTRAIT_4_5 -> (width * 5f / 4f).toInt()
                        CanvasAspectRatio.FREE -> (width * 16f / 9f).toInt()
                    }

                    val effectiveTransparent = isTransparent || currentState.isTransparentBg
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)

                    if (!effectiveTransparent) {
                        var photoDrawn = false
                        val bgUri = currentState.backgroundImageUri
                        if (bgUri != null) {
                            val srcBitmap = decodeSampledBitmap(context, bgUri, width, height)
                            if (srcBitmap != null) {
                                val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                                if (currentState.backgroundGrayscale) {
                                    val cm = ColorMatrix().apply { setSaturation(0f) }
                                    bgPaint.colorFilter = ColorMatrixColorFilter(cm)
                                }
                                val srcW = srcBitmap.width.toFloat()
                                val srcH = srcBitmap.height.toFloat()
                                val srcRatio = srcW / srcH
                                val dstRatio = width.toFloat() / height.toFloat()

                                val srcRect = if (srcRatio > dstRatio) {
                                    val newW = (srcH * dstRatio).toInt()
                                    val xOffset = ((srcW - newW) / 2).toInt()
                                    Rect(xOffset, 0, xOffset + newW, srcH.toInt())
                                } else {
                                    val newH = (srcW / dstRatio).toInt()
                                    val yOffset = ((srcH - newH) / 2).toInt()
                                    Rect(0, yOffset, srcW.toInt(), yOffset + newH)
                                }
                                val dstRect = Rect(0, 0, width, height)
                                canvas.drawBitmap(srcBitmap, srcRect, dstRect, bgPaint)
                                photoDrawn = true
                                srcBitmap.recycle()
                            }
                        }

                        if (!photoDrawn) {
                            if (currentState.solidBgColorLong != null) {
                                canvas.drawColor(currentState.solidBgColorLong.toInt())
                            } else {
                                canvas.drawColor(android.graphics.Color.BLACK)
                            }
                        } else if (currentState.backgroundDim > 0f) {
                            val dimAlpha = (currentState.backgroundDim * 255).toInt().coerceIn(0, 240)
                            canvas.drawColor(android.graphics.Color.argb(dimAlpha, 0, 0, 0))
                        }
                    }

                    val scaledTextSize = (currentState.fontSize / 380f) * width
                    val realTypeface = AtharFontManager.getTypeface(context, currentState.selectedFont)
                    val alphaFloat = currentState.textOpacity.coerceIn(0.15f, 1.0f)
                    val alphaInt = (alphaFloat * 255).toInt()

                    val alphaMatrix = ColorMatrix().apply {
                        setScale(1f, 1f, 1f, alphaFloat)
                    }
                    val opacityFilter = ColorMatrixColorFilter(alphaMatrix)

                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        textSize = scaledTextSize
                        letterSpacing = currentState.letterSpacing
                        isUnderlineText = currentState.isUnderline
                        isFakeBoldText = currentState.isBold
                        textSkewX = if (currentState.isItalic) -0.25f else 0f
                        alpha = alphaInt
                        colorFilter = opacityFilter
                        textAlign = when (currentState.textAlign) {
                            TextAlignOption.RIGHT -> Paint.Align.RIGHT
                            TextAlignOption.CENTER -> Paint.Align.CENTER
                            TextAlignOption.LEFT -> Paint.Align.LEFT
                        }
                        typeface = realTypeface
                    }

                    val strokePaint = if (currentState.strokeWidth > 0f) {
                        Paint().apply {
                            isAntiAlias = true
                            isDither = true
                            style = Paint.Style.STROKE
                            strokeJoin = Paint.Join.ROUND
                            strokeCap = Paint.Cap.ROUND
                            strokeWidth = (currentState.strokeWidth / 380f) * width
                            color = currentState.strokeColorLong.toInt()
                            letterSpacing = currentState.letterSpacing
                            isUnderlineText = currentState.isUnderline
                            isFakeBoldText = currentState.isBold
                            textSkewX = if (currentState.isItalic) -0.25f else 0f
                            alpha = alphaInt
                            colorFilter = opacityFilter
                            textSize = scaledTextSize
                            textAlign = paint.textAlign
                            typeface = realTypeface
                        }
                    } else null

                    // 2D Directional Shadow with Blur for ALL modes
                    val shadowPaint = if (currentState.shadowRadius > 0f) {
                        Paint().apply {
                            isAntiAlias = true
                            isDither = true
                            textSize = scaledTextSize
                            letterSpacing = currentState.letterSpacing
                            isFakeBoldText = currentState.isBold
                            textSkewX = if (currentState.isItalic) -0.25f else 0f
                            textAlign = paint.textAlign
                            typeface = realTypeface
                            val sColor = currentState.shadowColorLong.toInt()
                            val sAlpha = (((sColor ushr 24) and 0xFF) * alphaFloat).toInt()
                            color = (sColor and 0x00FFFFFF) or (sAlpha shl 24)
                            val scaledBlur = ((currentState.shadowRadius / 380f) * width).coerceAtLeast(1f)
                            maskFilter = BlurMaskFilter(scaledBlur, BlurMaskFilter.Blur.NORMAL)
                        }
                    } else null

                    val scaledShadowDx = (currentState.shadowDx / 380f) * width
                    val scaledShadowDy = (currentState.shadowDy / 380f) * width

                    val centerX = width * 0.5f + (width * currentState.normOffsetX)
                    val centerY = height * 0.5f + (height * currentState.normOffsetY)

                    val lines = currentState.displayText.split("\n")
                    val lineHeight = paint.textSize * currentState.lineSpacingMultiplier
                    val totalHeight = (lines.size - 1) * lineHeight
                    val startY = -totalHeight / 2f + (paint.textSize * 0.35f)

                    var maxLineWidth = 0f
                    lines.forEach { line ->
                        val w = paint.measureText(line)
                        if (w > maxLineWidth) maxLineWidth = w
                    }
                    val halfBlockW = maxLineWidth / 2f
                    val lineX = when (currentState.textAlign) {
                        TextAlignOption.RIGHT -> halfBlockW
                        TextAlignOption.CENTER -> 0f
                        TextAlignOption.LEFT -> -halfBlockW
                    }

                    canvas.save()
                    canvas.translate(centerX, centerY)
                    canvas.rotate(currentState.rotationAngle)
                    canvas.scale(currentState.scaleFactor, currentState.scaleFactor)

                    lines.forEachIndexed { index, line ->
                        val lineY = startY + (index * lineHeight)

                        // 1. Draw 2D Directional Shadow
                        shadowPaint?.let { shp ->
                            canvas.drawText(line, lineX + scaledShadowDx, lineY + scaledShadowDy, shp)
                        }

                        // 2. Draw Stroke
                        strokePaint?.let { sp ->
                            canvas.drawText(line, lineX, lineY, sp)
                        }

                        // 3. Exact Shader and Color calculation per line matching CanvasView
                        if (currentState.customTextColorLong != null && currentState.customTextColorLong != 0L) {
                            val baseColor = currentState.customTextColorLong.toInt()
                            paint.color = (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
                            paint.shader = null
                        } else {
                            when (currentState.selectedEffect) {
                                VisualEffect.ROYAL_GOLD -> {
                                    paint.shader = LinearGradient(
                                        0f, lineY - paint.textSize * 0.85f,
                                        0f, lineY + paint.textSize * 0.25f,
                                        intArrayOf(
                                            android.graphics.Color.parseColor("#FFF7DB"),
                                            android.graphics.Color.parseColor("#F5C369"),
                                            android.graphics.Color.parseColor("#D49438"),
                                            android.graphics.Color.parseColor("#FFE8A3"),
                                            android.graphics.Color.parseColor("#B0731E")
                                        ),
                                        floatArrayOf(0.0f, 0.25f, 0.55f, 0.75f, 1.0f),
                                        Shader.TileMode.CLAMP
                                    )
                                }
                                VisualEffect.LIQUID_CHROME -> {
                                    paint.shader = LinearGradient(
                                        -paint.textSize * 0.5f, lineY - paint.textSize * 0.85f,
                                        paint.textSize * 0.5f, lineY + paint.textSize * 0.25f,
                                        intArrayOf(
                                            android.graphics.Color.parseColor("#FFFFFF"),
                                            android.graphics.Color.parseColor("#9EA5B0"),
                                            android.graphics.Color.parseColor("#181C22"),
                                            android.graphics.Color.parseColor("#FFFFFF"),
                                            android.graphics.Color.parseColor("#505866"),
                                            android.graphics.Color.parseColor("#DDE2EA")
                                        ),
                                        floatArrayOf(0.0f, 0.2f, 0.45f, 0.55f, 0.8f, 1.0f),
                                        Shader.TileMode.CLAMP
                                    )
                                }
                                VisualEffect.CYBER_NEON -> {
                                    paint.color = (android.graphics.Color.parseColor("#00FF9D") and 0x00FFFFFF) or (alphaInt shl 24)
                                    paint.shader = null
                                }
                                VisualEffect.CINEMATIC_SHADOW -> {
                                    paint.color = (android.graphics.Color.parseColor("#F2F2F2") and 0x00FFFFFF) or (alphaInt shl 24)
                                    paint.shader = null
                                }
                                VisualEffect.FROSTED_GLASS -> {
                                    paint.shader = LinearGradient(
                                        0f, lineY - paint.textSize * 0.85f,
                                        0f, lineY + paint.textSize * 0.25f,
                                        intArrayOf(
                                            android.graphics.Color.parseColor("#E6FFFFFF"),
                                            android.graphics.Color.parseColor("#33E2E8F0")
                                        ),
                                        null,
                                        Shader.TileMode.CLAMP
                                    )
                                }
                                VisualEffect.MINIMAL_LUXURY -> {
                                    paint.color = (android.graphics.Color.WHITE and 0x00FFFFFF) or (alphaInt shl 24)
                                    paint.shader = null
                                }
                            }
                        }

                        canvas.drawText(line, lineX, lineY, paint)
                    }
                    canvas.restore()

                    // Trim transparent margins if sticker export requested
                    val finalBitmap = if (effectiveTransparent && isStickerTrimmed) {
                        val padding = (48f * (width / 1080f)).toInt()
                        val cropped = cropTransparentBitmap(bitmap, paddingPx = padding)
                        if (cropped != bitmap) {
                            bitmap.recycle()
                        }
                        cropped
                    } else {
                        bitmap
                    }

                    // Save to Gallery via MediaStore
                    val filename = "Athar_${System.currentTimeMillis()}.${if (effectiveTransparent) "png" else "jpg"}"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, if (effectiveTransparent) "image/png" else "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Athar")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val resolver = context.contentResolver
                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    if (imageUri != null) {
                        resolver.openOutputStream(imageUri)?.use { out ->
                            finalBitmap.compress(
                                if (effectiveTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                                100,
                                out
                            )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(imageUri, contentValues, null, null)
                        }
                    }

                    // Share Intent Cache file
                    val exportDir = File(context.getExternalFilesDir(null), "exports")
                    if (!exportDir.exists()) exportDir.mkdirs()
                    val cacheFile = File(exportDir, filename)
                    FileOutputStream(cacheFile).use { out ->
                        finalBitmap.compress(
                            if (effectiveTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                            100,
                            out
                        )
                    }

                    val shareUri: Uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", cacheFile)

                    if (onShareRequested) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (effectiveTransparent) "image/png" else "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة تصميم أَثَــر"))
                    }

                    finalBitmap.recycle()

                    Result.success(
                        if (effectiveTransparent && isStickerTrimmed) {
                            "تم تصدير الملصق الشفاف المقصوص بنجاح (PNG)"
                        } else {
                            "تم حفظ التصميم في المعرض بنجاح (${resolution.label})"
                        }
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            _isExporting.value = false
            if (exportResult.isSuccess) {
                _exportMessage.value = exportResult.getOrNull()
            } else {
                _exportMessage.value = "فشل التصدير: ${exportResult.exceptionOrNull()?.message}"
            }
        }
    }
}
