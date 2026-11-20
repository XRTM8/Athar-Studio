package app.xrtm8.athar.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.xrtm8.athar.core.AtharNativeEngine
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.VisualEffect
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

    fun alignCenter() {
        _canvasState.update { it.copy(offsetX = 0f, offsetY = 0f, rotationAngle = 0f) }
    }

    fun alignTop() {
        _canvasState.update { it.copy(offsetX = 0f, offsetY = -350f, rotationAngle = 0f) }
    }

    fun alignBottom() {
        _canvasState.update { it.copy(offsetX = 0f, offsetY = 350f, rotationAngle = 0f) }
    }

    fun resetCanvasTransform() {
        _canvasState.update {
            it.copy(
                offsetX = 0f,
                offsetY = 0f,
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

    fun exportDesign(context: Context, isTransparent: Boolean) {
        if (_isExporting.value) return

        viewModelScope.launch {
            _isExporting.value = true
            val currentState = _canvasState.value

            val exportResult = withContext(Dispatchers.IO) {
                try {
                    val width = 1080
                    val height = when (currentState.aspectRatio) {
                        CanvasAspectRatio.STORY_9_16 -> 1920
                        CanvasAspectRatio.SQUARE_1_1 -> 1080
                        CanvasAspectRatio.PORTRAIT_4_5 -> 1350
                        CanvasAspectRatio.FREE -> 1920
                    }

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)

                    if (!isTransparent) {
                        // 1. Draw photo background if selected
                        var photoDrawn = false
                        val bgUri = currentState.backgroundImageUri
                        if (bgUri != null) {
                            try {
                                context.contentResolver.openInputStream(bgUri)?.use { stream ->
                                    val srcBitmap = BitmapFactory.decodeStream(stream)
                                    if (srcBitmap != null) {
                                        val srcRect = Rect(0, 0, srcBitmap.width, srcBitmap.height)
                                        val dstRect = Rect(0, 0, width, height)
                                        canvas.drawBitmap(srcBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                                        photoDrawn = true
                                    }
                                }
                            } catch (e: Exception) {
                                // fallback to black
                            }
                        }

                        if (!photoDrawn) {
                            canvas.drawColor(android.graphics.Color.BLACK)
                        } else if (currentState.backgroundDim > 0f) {
                            // Darken overlay for photo readability
                            val dimAlpha = (currentState.backgroundDim * 255).toInt().coerceIn(0, 240)
                            canvas.drawColor(android.graphics.Color.argb(dimAlpha, 0, 0, 0))
                        }
                    }

                    val paint = Paint().apply {
                        isAntiAlias = true
                        isDither = true
                        textSize = currentState.fontSize * 3.5f
                        textAlign = Paint.Align.CENTER
                        typeface = when (currentState.selectedFont) {
                            ArabicFontFamily.TRADITIONAL -> Typeface.SERIF
                            ArabicFontFamily.MODERN_SANS -> Typeface.SANS_SERIF
                            ArabicFontFamily.RUQAA -> Typeface.SERIF
                            ArabicFontFamily.KUFI -> Typeface.DEFAULT_BOLD
                            ArabicFontFamily.ELEGANT -> Typeface.DEFAULT
                            ArabicFontFamily.MONOSPACE -> Typeface.MONOSPACE
                        }
                    }

                    val centerX = width / 2f + (currentState.offsetX * 2.5f)
                    val centerY = height / 2f + (currentState.offsetY * 2.5f)

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

                    val lines = currentState.displayText.split("\n")
                    val lineHeight = paint.textSize * 1.32f
                    val totalHeight = (lines.size - 1) * lineHeight
                    val startY = centerY - (totalHeight / 2f)

                    canvas.save()
                    canvas.rotate(currentState.rotationAngle, centerX, centerY)
                    canvas.scale(currentState.scaleFactor, currentState.scaleFactor, centerX, centerY)

                    lines.forEachIndexed { index, line ->
                        canvas.drawText(line, centerX, startY + (index * lineHeight), paint)
                    }
                    canvas.restore()

                    val exportDir = File(context.getExternalFilesDir(null), "exports")
                    if (!exportDir.exists()) exportDir.mkdirs()

                    val ext = if (isTransparent) "png" else "jpg"
                    val file = File(exportDir, "Athar_${System.currentTimeMillis()}.$ext")
                    val out = FileOutputStream(file)
                    bitmap.compress(
                        if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                        100,
                        out
                    )
                    out.flush()
                    out.close()

                    val uri: Uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = if (isTransparent) "image/png" else "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة التصميم من أَثَــر"))
                    Result.success("تم التصدير بنجاح")
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            _isExporting.value = false
            if (exportResult.isFailure) {
                _exportMessage.value = "فشل التصدير: ${exportResult.exceptionOrNull()?.message}"
            }
        }
    }
}
