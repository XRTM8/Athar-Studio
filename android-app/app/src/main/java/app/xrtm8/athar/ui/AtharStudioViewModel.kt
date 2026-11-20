package app.xrtm8.athar.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
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

    private var kashidaDebounceJob: Job? = null

    init {
        updateKashidaInternal(_canvasState.value.rawText, _canvasState.value.stretchLevel)
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
        scheduleKashidaUpdate(newText, _canvasState.value.stretchLevel)
    }

    fun setStretchLevel(newLevel: Int) {
        if (newLevel == _canvasState.value.stretchLevel) return
        pushHistory(_canvasState.value.copy(stretchLevel = newLevel))
        scheduleKashidaUpdate(_canvasState.value.rawText, newLevel)
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

    fun setShadowDy(dy: Float) {
        pushHistory(_canvasState.value.copy(shadowDy = dy.coerceIn(-30f, 40f)))
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

    fun alignCenter() {
        pushHistory(_canvasState.value.copy(normOffsetX = 0f, normOffsetY = 0f, rotationAngle = 0f))
    }

    fun alignTop() {
        pushHistory(_canvasState.value.copy(normOffsetX = 0f, normOffsetY = -0.28f, rotationAngle = 0f))
    }

    fun alignBottom() {
        pushHistory(_canvasState.value.copy(normOffsetX = 0f, normOffsetY = 0.28f, rotationAngle = 0f))
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

    private fun scheduleKashidaUpdate(text: String, level: Int) {
        kashidaDebounceJob?.cancel()
        kashidaDebounceJob = viewModelScope.launch(Dispatchers.Default) {
            updateKashidaInternal(text, level)
        }
    }

    private fun updateKashidaInternal(text: String, level: Int) {
        val stretched = AtharNativeEngine.applyKashida(text, level)
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

    fun exportDesign(
        context: Context,
        resolution: ExportResolution = ExportResolution.HD_1080P,
        isTransparent: Boolean = false,
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

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)

                    if (!isTransparent) {
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
                            }
                        }

                        if (!photoDrawn) {
                            canvas.drawColor(android.graphics.Color.BLACK)
                        } else if (currentState.backgroundDim > 0f) {
                            val dimAlpha = (currentState.backgroundDim * 255).toInt().coerceIn(0, 240)
                            canvas.drawColor(android.graphics.Color.argb(dimAlpha, 0, 0, 0))
                        }
                    }

                    val scaledTextSize = (currentState.fontSize / 380f) * width
                    val realTypeface = AtharFontManager.getTypeface(context, currentState.selectedFont)

                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        textSize = scaledTextSize
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
                            strokeWidth = (currentState.strokeWidth / 380f) * width
                            color = currentState.strokeColorLong.toInt()
                            textSize = scaledTextSize
                            textAlign = paint.textAlign
                            typeface = realTypeface
                        }
                    } else null

                    // Color & Shader configuration
                    if (currentState.customTextColorLong != null && currentState.customTextColorLong != 0L) {
                        paint.color = currentState.customTextColorLong.toInt()
                        paint.shader = null
                    } else {
                        when (currentState.selectedEffect) {
                            VisualEffect.ROYAL_GOLD -> {
                                paint.shader = LinearGradient(
                                    0f, -paint.textSize * 0.8f,
                                    0f, paint.textSize * 0.2f,
                                    intArrayOf(
                                        android.graphics.Color.parseColor("#FFF7DB"),
                                        android.graphics.Color.parseColor("#F5C369"),
                                        android.graphics.Color.parseColor("#D49438"),
                                        android.graphics.Color.parseColor("#FFE8A3")
                                    ),
                                    null,
                                    Shader.TileMode.CLAMP
                                )
                            }
                            VisualEffect.LIQUID_CHROME -> {
                                paint.shader = LinearGradient(
                                    0f, -paint.textSize * 0.8f,
                                    0f, paint.textSize * 0.2f,
                                    intArrayOf(
                                        android.graphics.Color.parseColor("#FFFFFF"),
                                        android.graphics.Color.parseColor("#D8D8E0"),
                                        android.graphics.Color.parseColor("#8E919D"),
                                        android.graphics.Color.parseColor("#F2F4F8")
                                    ),
                                    null,
                                    Shader.TileMode.CLAMP
                                )
                            }
                            VisualEffect.CYBER_NEON -> {
                                paint.color = android.graphics.Color.parseColor("#00FF9D")
                            }
                            VisualEffect.CINEMATIC_SHADOW -> {
                                paint.color = android.graphics.Color.parseColor("#F2F2F2")
                            }
                            VisualEffect.FROSTED_GLASS -> {
                                paint.color = android.graphics.Color.parseColor("#DDFFFFFF")
                            }
                            VisualEffect.MINIMAL_LUXURY -> {
                                paint.color = android.graphics.Color.WHITE
                            }
                        }
                    }

                    // Shadow configuration
                    if (currentState.shadowRadius > 0f) {
                        val scaledShadowRadius = (currentState.shadowRadius / 380f) * width
                        val scaledShadowDy = (currentState.shadowDy / 380f) * width
                        paint.setShadowLayer(scaledShadowRadius, 0f, scaledShadowDy, android.graphics.Color.parseColor("#AA000000"))
                    }

                    val centerX = width * 0.5f + (width * currentState.normOffsetX)
                    val centerY = height * 0.5f + (height * currentState.normOffsetY)

                    val lines = currentState.displayText.split("\n")
                    val lineHeight = paint.textSize * currentState.lineSpacingMultiplier
                    val totalHeight = (lines.size - 1) * lineHeight
                    val startY = -totalHeight / 2f + (paint.textSize * 0.32f)

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
                        strokePaint?.let { sp ->
                            canvas.drawText(line, lineX, lineY, sp)
                        }
                        canvas.drawText(line, lineX, lineY, paint)
                    }
                    canvas.restore()

                    // Save to Gallery via MediaStore
                    val filename = "Athar_${System.currentTimeMillis()}.${if (isTransparent) "png" else "jpg"}"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, if (isTransparent) "image/png" else "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Athar")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val resolver = context.contentResolver
                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    if (imageUri != null) {
                        resolver.openOutputStream(imageUri)?.use { out ->
                            bitmap.compress(
                                if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
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
                        bitmap.compress(
                            if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                            100,
                            out
                        )
                    }

                    val shareUri: Uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", cacheFile)

                    if (onShareRequested) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (isTransparent) "image/png" else "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة تصميم أَثَــر"))
                    }

                    Result.success("تم حفظ التصميم في المعرض بنجاح (${resolution.label})")
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
