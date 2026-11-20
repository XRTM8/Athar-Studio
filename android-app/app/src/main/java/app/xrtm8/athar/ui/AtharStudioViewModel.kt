package app.xrtm8.athar.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import java.io.OutputStream

class AtharStudioViewModel : ViewModel() {

    private val _canvasState = MutableStateFlow(CanvasState())
    val canvasState: StateFlow<CanvasState> = _canvasState.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private var kashidaDebounceJob: Job? = null

    init {
        updateKashidaInternal(_canvasState.value.rawText, _canvasState.value.stretchLevel)
    }

    fun setText(newText: String) {
        _canvasState.update { it.copy(rawText = newText) }
        scheduleKashidaUpdate(newText, _canvasState.value.stretchLevel)
    }

    fun setStretchLevel(newLevel: Int) {
        if (newLevel == _canvasState.value.stretchLevel) return
        _canvasState.update { it.copy(stretchLevel = newLevel) }
        scheduleKashidaUpdate(_canvasState.value.rawText, newLevel)
    }

    fun setEffect(effect: VisualEffect) {
        _canvasState.update { it.copy(selectedEffect = effect) }
    }

    fun setFont(font: ArabicFontFamily) {
        _canvasState.update { it.copy(selectedFont = font) }
    }

    fun setAspectRatio(ratio: CanvasAspectRatio) {
        _canvasState.update { it.copy(aspectRatio = ratio) }
    }

    fun setBackgroundImageUri(uri: Uri?) {
        _canvasState.update { it.copy(backgroundImageUri = uri) }
    }

    fun setBackgroundDim(dim: Float) {
        _canvasState.update { it.copy(backgroundDim = dim.coerceIn(0f, 0.9f)) }
    }

    fun setFontSize(size: Float) {
        _canvasState.update { it.copy(fontSize = size) }
    }

    fun setTextBehindSubject(enabled: Boolean) {
        _canvasState.update { it.copy(isTextBehindSubject = enabled) }
    }

    /**
     * Unified Normalized Coordinate Transformation:
     * Receives gestures from CanvasView and persists the final transformation state.
     */
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
        _canvasState.update { it.copy(normOffsetX = 0f, normOffsetY = 0f, rotationAngle = 0f) }
    }

    fun alignTop() {
        _canvasState.update { it.copy(normOffsetX = 0f, normOffsetY = -0.28f, rotationAngle = 0f) }
    }

    fun alignBottom() {
        _canvasState.update { it.copy(normOffsetX = 0f, normOffsetY = 0.28f, rotationAngle = 0f) }
    }

    fun resetCanvasTransform() {
        _canvasState.update {
            it.copy(
                normOffsetX = 0f,
                normOffsetY = 0f,
                scaleFactor = 1.0f,
                rotationAngle = 0f
            )
        }
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

    /**
     * Memory-safe image decoder preventing OOM crashes with ultra-large camera photos.
     */
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

    /**
     * Ultra-High Resolution Rendering Engine supporting up to 4K (2160x3840).
     */
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
                                val srcRect = Rect(0, 0, srcBitmap.width, srcBitmap.height)
                                val dstRect = Rect(0, 0, width, height)
                                canvas.drawBitmap(srcBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
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

                    // Scaled typography paint matching preview perfectly
                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        textSize = (currentState.fontSize / 380f) * width
                        textAlign = Paint.Align.CENTER
                        typeface = AtharFontManager.getTypeface(context, currentState.selectedFont)
                    }

                    // Apply visual metallic shaders
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
                            paint.setShadowLayer(paint.textSize * 0.25f, 0f, paint.textSize * 0.08f, android.graphics.Color.parseColor("#994A2800"))
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
                            paint.setShadowLayer(paint.textSize * 0.22f, 0f, paint.textSize * 0.08f, android.graphics.Color.parseColor("#99202028"))
                        }
                        VisualEffect.CYBER_NEON -> {
                            paint.color = android.graphics.Color.parseColor("#00FF9D")
                            paint.setShadowLayer(paint.textSize * 0.35f, 0f, 0f, android.graphics.Color.parseColor("#FF00FF9D"))
                        }
                        VisualEffect.CINEMATIC_SHADOW -> {
                            paint.color = android.graphics.Color.parseColor("#F2F2F2")
                            paint.setShadowLayer(paint.textSize * 0.4f, 0f, paint.textSize * 0.15f, android.graphics.Color.parseColor("#DD000000"))
                        }
                        VisualEffect.FROSTED_GLASS -> {
                            paint.color = android.graphics.Color.parseColor("#DDFFFFFF")
                            paint.setShadowLayer(paint.textSize * 0.2f, 0f, 0f, android.graphics.Color.parseColor("#66FFFFFF"))
                        }
                        VisualEffect.MINIMAL_LUXURY -> {
                            paint.color = android.graphics.Color.WHITE
                        }
                    }

                    // Normalized coordinate translation matching screen preview 100%
                    val centerX = width * 0.5f + (width * currentState.normOffsetX)
                    val centerY = height * 0.5f + (height * currentState.normOffsetY)

                    val lines = currentState.displayText.split("\n")
                    val lineHeight = paint.textSize * 1.35f
                    val totalHeight = (lines.size - 1) * lineHeight
                    val startY = centerY - (totalHeight / 2f) + (paint.textSize * 0.32f)

                    canvas.save()
                    canvas.rotate(currentState.rotationAngle, centerX, centerY)
                    canvas.scale(currentState.scaleFactor, currentState.scaleFactor, centerX, centerY)

                    lines.forEachIndexed { index, line ->
                        canvas.drawText(line, centerX, startY + (index * lineHeight), paint)
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
