package app.xrtm8.athar.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.xrtm8.athar.core.AtharBackgroundPipeline
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.ui.canvas.CanvasLayerTextRenderer
import app.xrtm8.athar.ui.components.CanvasPaintPool
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance export engine for Athar Studio.
 * Renders vector Skia typography at 1080p, 2K, and 4K resolutions with exact
 * shader gradients, multi-layer compositions, 2D blur shadows, photo backgrounds with framing,
 * film simulation ColorMatrix, directional gradient scrim, frosted glass backdrop cards,
 * and transparent sticker cropping.
 */
object AtharExportManager {

    suspend fun exportDesign(
        context: Context,
        state: CanvasState,
        resolution: ExportResolution = ExportResolution.HD_1080P,
        isTransparent: Boolean = false,
        isStickerTrimmed: Boolean = true,
        onShareRequested: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val width = resolution.width
            val effectiveTransparent = isTransparent || state.isTransparentBg
            val bitmap = renderDesign(context, state, width, effectiveTransparent)

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

            val filename = "Athar_${System.currentTimeMillis()}.${if (effectiveTransparent) "png" else "jpg"}"

            try {
                saveToMediaStore(context, finalBitmap, filename, effectiveTransparent)
                if (onShareRequested) {
                    shareExportedBitmap(context, finalBitmap, filename, effectiveTransparent)
                }
            } finally {
                finalBitmap.recycle()
            }

            val successMsg = if (effectiveTransparent && isStickerTrimmed) {
                "تم تصدير الملصق الشفاف المقصوص بنجاح (PNG)"
            } else {
                "تم حفظ التصميم في المعرض بنجاح (${resolution.label})"
            }

            Result.success(successMsg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportPdf(
        context: Context,
        state: CanvasState,
        onShareRequested: Boolean = false
    ): Result<String> = AtharPdfExporter.exportPdf(context, state, onShareRequested)

    suspend fun exportSvg(
        context: Context,
        state: CanvasState,
        onShareRequested: Boolean = false
    ): Result<String> = AtharSvgExporter.exportSvg(context, state, onShareRequested)

    internal fun renderDesign(context: Context, state: CanvasState, width: Int, transparent: Boolean): Bitmap {
        val height = (width / (state.aspectRatio.ratio ?: (9f / 16f))).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            if (!transparent) drawBackground(context, canvas, state, width, height)
            drawLayers(context, canvas, state.layers, width, height)
            return bitmap
        } catch (e: Exception) {
            bitmap.recycle()
            throw e
        }
    }

    internal fun drawBackground(context: Context, canvas: Canvas, state: CanvasState, width: Int, height: Int) {
        AtharBackgroundPipeline.renderSoftwareBackground(context, canvas, state, width, height)
    }

    internal fun drawLayers(context: Context, canvas: Canvas, layers: List<TextLayer>, width: Int, height: Int) {
        val density = width / 380f
        val paints = CanvasPaintPool(density)
        for (layer in layers) {
            CanvasLayerTextRenderer.renderTextLayer(
                nativeCanvas = canvas, context = context, layer = layer,
                isCurrentActive = false, isFramingMode = false,
                layerNormX = layer.normOffsetX, layerNormY = layer.normOffsetY,
                layerScale = layer.scaleFactor, layerRotation = layer.rotationAngle,
                canvasWidth = width.toFloat(), canvasHeight = height.toFloat(), density = density,
                paints = paints
            )
        }
    }

    private fun cropTransparentBitmap(src: Bitmap, paddingPx: Int): Bitmap {
        val width = src.width
        val height = src.height

        val rowPixels = IntArray(width)
        var left = width
        var right = -1
        var top = height
        var bottom = -1
        for (y in 0 until height) {
            src.getPixels(rowPixels, 0, width, 0, y, width, 1)
            val first = rowPixels.indexOfFirst { (it ushr 24) != 0 }
            if (first < 0) continue
            left = minOf(left, first)
            right = maxOf(right, rowPixels.indexOfLast { (it ushr 24) != 0 })
            top = minOf(top, y)
            bottom = y
        }
        if (right < 0) return src

        val minX = (left - paddingPx).coerceAtLeast(0)
        val minY = (top - paddingPx).coerceAtLeast(0)
        val maxX = (right + paddingPx).coerceAtMost(width - 1)
        val maxY = (bottom + paddingPx).coerceAtMost(height - 1)

        val cropW = maxX - minX + 1
        val cropH = maxY - minY + 1

        return Bitmap.createBitmap(src, minX, minY, cropW, cropH)
    }

    private fun saveToMediaStore(context: Context, bitmap: Bitmap, filename: String, isTransparent: Boolean) {
        val format = if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val mime = if (isTransparent) "image/png" else "image/jpeg"
        val quality = if (isTransparent) 100 else 98

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Athar")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        try {
            val output = checkNotNull(context.contentResolver.openOutputStream(uri)) { "تعذر فتح ملف الصورة" }
            output.use { check(bitmap.compress(format, quality, it)) { "تعذرت كتابة الصورة" } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                check(context.contentResolver.update(uri, values, null, null) == 1) { "تعذر إتمام الحفظ في المعرض" }
            }
        } catch (e: Exception) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            throw e
        }
    }

    private fun shareExportedBitmap(context: Context, bitmap: Bitmap, filename: String, isTransparent: Boolean) {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val shareFile = File(cacheDir, filename)
        val format = if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val quality = if (isTransparent) 100 else 98

        FileOutputStream(shareFile).use { out ->
            check(bitmap.compress(format, quality, out)) { "تعذرت كتابة الصورة" }
        }

        val uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", shareFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (isTransparent) "image/png" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, "مشاركة التصميم من أثر")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }
}
