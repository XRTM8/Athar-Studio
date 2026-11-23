package app.xrtm8.athar.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.core.FilmPresetEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.ui.canvas.CanvasLayerTextRenderer
import app.xrtm8.athar.ui.components.CanvasGradientScrimRenderer
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

    private fun drawBackground(context: Context, canvas: Canvas, state: CanvasState, width: Int, height: Int) {
        var photoDrawn = false

        if (state.collageState.isEnabled) {
            val cellBitmaps = mutableMapOf<String, Bitmap>()
            state.collageState.cells.forEach { cell ->
                val uri = cell.imageUri
                if (uri != null) {
                    val cw = ((cell.rightNorm - cell.leftNorm) * width).toInt().coerceAtLeast(32)
                    val ch = ((cell.bottomNorm - cell.topNorm) * height).toInt().coerceAtLeast(32)
                    val scale = cell.photoScale.coerceIn(1.0f, 4.0f)
                    val sampleW = (cw * scale).toInt().coerceAtLeast(32)
                    val sampleH = (ch * scale).toInt().coerceAtLeast(32)
                    val bmp = AtharImageProcessor.decodeSampledBitmap(context, uri, sampleW, sampleH)
                    if (bmp != null) {
                        cellBitmaps[cell.id] = bmp
                    }
                }
            }
            val density = width / 380f
            val collagePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = FilmPresetEngine.createCompositeFilter(
                    preset = state.filmPreset,
                    dim = state.backgroundDim,
                    brightness = state.backgroundBrightness,
                    contrast = state.backgroundContrast,
                    saturation = state.backgroundSaturation,
                    isGrayscale = state.backgroundGrayscale,
                    colorGradingEnabled = state.colorGradingEnabled,
                    liftR = state.liftRed, liftG = state.liftGreen, liftB = state.liftBlue,
                    gammaR = state.gammaRed, gammaG = state.gammaGreen, gammaB = state.gammaBlue,
                    gainR = state.gainRed, gainG = state.gainGreen, gainB = state.gainBlue
                )
            }
            app.xrtm8.athar.ui.components.CollageCanvasRenderer.drawCollage(
                canvas = canvas,
                state = state.collageState,
                width = width.toFloat(),
                height = height.toFloat(),
                density = density,
                cellBitmaps = cellBitmaps,
                paint = collagePaint
            )
            cellBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
            photoDrawn = true
        } else {
            val bgUri = state.backgroundImageUri
            if (bgUri != null) {
            val bgScale = state.bgPhotoScale.coerceIn(1.0f, 4.0f)
            val targetW = (width * bgScale).toInt().coerceAtLeast(32)
            val targetH = (height * bgScale).toInt().coerceAtLeast(32)
            var srcBitmap = AtharImageProcessor.decodeSampledBitmap(context, bgUri, targetW, targetH)
            if (srcBitmap != null) {
                if (state.backgroundBlur > 0.5f) {
                    val blurR = (state.backgroundBlur * (width / 1080f)).toInt().coerceIn(1, 40)
                    // Downsample to max 1080p for blur processing to prevent 4K heap OOM
                    val maxBlurDim = 1080f
                    val scale = kotlin.math.min(1f, maxBlurDim / kotlin.math.max(srcBitmap.width, srcBitmap.height))
                    if (scale < 0.95f) {
                        val tw = (srcBitmap.width * scale).toInt().coerceAtLeast(10)
                        val th = (srcBitmap.height * scale).toInt().coerceAtLeast(10)
                        val downsampled = Bitmap.createScaledBitmap(srcBitmap, tw, th, true)
                        val scaledRadius = (blurR * scale).toInt().coerceIn(1, 35)
                        val blurred = AtharImageProcessor.fastStackBlur(downsampled, scaledRadius)
                        if (blurred != downsampled) {
                            downsampled.recycle()
                        }
                        srcBitmap.recycle()
                        srcBitmap = blurred
                    } else {
                        val blurred = AtharImageProcessor.fastStackBlur(srcBitmap, blurR)
                        if (blurred != srcBitmap) {
                            srcBitmap.recycle()
                            srcBitmap = blurred
                        }
                    }
                }

                if (state.colorMixState.isEnabled && !state.colorMixState.isDefault) {
                    val mixed = app.xrtm8.athar.core.AtharColorMixEngine.processBitmap(srcBitmap, state.colorMixState)
                    if (mixed != srcBitmap) {
                        srcBitmap.recycle()
                        srcBitmap = mixed
                    }
                }

                if (state.cinematicGlowState.isEnabled) {
                    val glowed = app.xrtm8.athar.core.AtharOpticalGlowEngine.processBitmap(srcBitmap, state.cinematicGlowState)
                    if (glowed != srcBitmap) {
                        srcBitmap.recycle()
                        srcBitmap = glowed
                    }
                }

                if (state.filmGrainState.isEnabled) {
                    val grained = app.xrtm8.athar.core.AtharFilmGrainEngine.processBitmap(srcBitmap, state.filmGrainState)
                    if (grained != srcBitmap) {
                        srcBitmap.recycle()
                        srcBitmap = grained
                    }
                }

                val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = FilmPresetEngine.createCompositeFilter(
                        preset = state.filmPreset,
                        dim = state.backgroundDim,
                        brightness = state.backgroundBrightness,
                        contrast = state.backgroundContrast,
                        saturation = state.backgroundSaturation,
                        isGrayscale = state.backgroundGrayscale,
                        colorGradingEnabled = state.colorGradingEnabled,
                        liftR = state.liftRed, liftG = state.liftGreen, liftB = state.liftBlue,
                        gammaR = state.gammaRed, gammaG = state.gammaGreen, gammaB = state.gammaBlue,
                        gainR = state.gainRed, gainG = state.gainGreen, gainB = state.gainBlue
                    )
                }

                val srcW = srcBitmap.width.toFloat()
                val srcH = srcBitmap.height.toFloat()
                val srcRatio = srcW / srcH
                val dstRatio = width.toFloat() / height.toFloat()

                val scale = state.bgPhotoScale.coerceIn(0.5f, 4.0f)
                val srcRect: Rect

                if (srcRatio > dstRatio) {
                    val newW = ((srcH * dstRatio) / scale).toInt().coerceIn(1, srcW.toInt())
                    val newH = (srcH / scale).toInt().coerceIn(1, srcH.toInt())
                    val maxShiftX = (srcW - newW) / 2f
                    val maxShiftY = (srcH - newH) / 2f
                    val xOffset = (((srcW - newW) / 2f) + (state.bgPhotoOffsetX * maxShiftX)).toInt().coerceIn(0, (srcW - newW).toInt())
                    val yOffset = (((srcH - newH) / 2f) + (state.bgPhotoOffsetY * maxShiftY)).toInt().coerceIn(0, (srcH - newH).toInt())
                    srcRect = Rect(xOffset, yOffset, xOffset + newW, yOffset + newH)
                } else {
                    val newW = (srcW / scale).toInt().coerceIn(1, srcW.toInt())
                    val newH = ((srcW / dstRatio) / scale).toInt().coerceIn(1, srcH.toInt())
                    val maxShiftX = (srcW - newW) / 2f
                    val maxShiftY = (srcH - newH) / 2f
                    val xOffset = (((srcW - newW) / 2f) + (state.bgPhotoOffsetX * maxShiftX)).toInt().coerceIn(0, (srcW - newW).toInt())
                    val yOffset = (((srcH - newH) / 2f) + (state.bgPhotoOffsetY * maxShiftY)).toInt().coerceIn(0, (srcH - newH).toInt())
                    srcRect = Rect(xOffset, yOffset, xOffset + newW, yOffset + newH)
                }
                val dstRect = Rect(0, 0, width, height)
                canvas.drawBitmap(srcBitmap, srcRect, dstRect, bgPaint)
                photoDrawn = true
                srcBitmap.recycle()
            }
        }
    }

        if (!photoDrawn) {
            if (state.solidBgColorLong != null) {
                canvas.drawColor(state.solidBgColorLong.toInt())
            } else {
                canvas.drawColor(Color.BLACK)
            }
        }

        if (state.backgroundVignette > 0f) {
            AtharImageProcessor.drawVignette(canvas, width.toFloat(), height.toFloat(), state.backgroundVignette)
        }

        if (!state.isTransparentBg && state.gradientScrim.isEnabled) {
            CanvasGradientScrimRenderer.drawGradientScrim(
                canvas = canvas,
                width = width.toFloat(),
                height = height.toFloat(),
                scrim = state.gradientScrim
            )
        }

        if (!state.isTransparentBg && state.patternState.isEnabled) {
            val exportDensity = width / 380f
            app.xrtm8.athar.core.AtharPatternFactory.drawPattern(
                canvas = canvas,
                width = width.toFloat(),
                height = height.toFloat(),
                state = state.patternState,
                density = exportDensity
            )
        }

        if (!state.isTransparentBg && state.maskState.isEffectActive) {
            val customUriStr = state.maskState.customMaskUri
            val maskBitmap = if (!customUriStr.isNullOrEmpty()) {
                try {
                    AtharImageProcessor.decodeSampledBitmap(context, android.net.Uri.parse(customUriStr), width, height)
                } catch (e: Exception) {
                    null
                }
            } else {
                val maskDef = app.xrtm8.athar.model.AtharMaskCatalog.getById(state.maskState.activeMaskId)
                maskDef?.let { def ->
                    AtharImageProcessor.decodeSampledAssetBitmap(context, def.assetFileName, width, height)
                }
            }
            try {
                app.xrtm8.athar.core.AtharMaskRenderer.drawMaskAndGradient(
                    canvas = canvas,
                    state = state.maskState,
                    width = width.toFloat(),
                    height = height.toFloat(),
                    maskBitmap = maskBitmap
                )
            } finally {
                if (maskBitmap != null && !maskBitmap.isRecycled) {
                    maskBitmap.recycle()
                }
            }
        }
    }

    private fun drawLayers(context: Context, canvas: Canvas, layers: List<TextLayer>, width: Int, height: Int) {
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
