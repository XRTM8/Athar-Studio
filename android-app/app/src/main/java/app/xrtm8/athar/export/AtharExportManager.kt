package app.xrtm8.athar.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
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
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.typography.AtharTextLayout
import app.xrtm8.athar.ui.components.TextGlassBackdropRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance export engine for Athar Studio.
 * Renders vector Skia typography at 1080p, 2K, and 4K resolutions with exact
 * shader gradients, multi-line alignments, 2D blur shadows, photo backgrounds,
 * frosted glass backdrop card, and transparent sticker cropping.
 */
object AtharExportManager {

    suspend fun exportDesign(
        context: Context,
        state: CanvasState,
        resolution: ExportResolution = ExportResolution.HD_1080P,
        isTransparent: Boolean = false,
        isStickerTrimmed: Boolean = true,
        onShareRequested: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val width = resolution.width
            val height = when (state.aspectRatio) {
                CanvasAspectRatio.STORY_9_16 -> (width * 16f / 9f).toInt()
                CanvasAspectRatio.SQUARE_1_1 -> width
                CanvasAspectRatio.PORTRAIT_4_5 -> (width * 5f / 4f).toInt()
                CanvasAspectRatio.FREE -> (width * 16f / 9f).toInt()
            }

            val effectiveTransparent = isTransparent || state.isTransparentBg
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            if (!effectiveTransparent) {
                drawBackground(context, canvas, state, width, height)
            }

            drawTypography(context, canvas, state, width, height)

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

            saveToMediaStore(context, finalBitmap, filename, effectiveTransparent)

            if (onShareRequested) {
                shareExportedBitmap(context, finalBitmap, filename, effectiveTransparent)
            }

            finalBitmap.recycle()

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

    private fun drawBackground(context: Context, canvas: Canvas, state: CanvasState, width: Int, height: Int) {
        var photoDrawn = false
        val bgUri = state.backgroundImageUri
        if (bgUri != null) {
            var srcBitmap = decodeSampledBitmap(context, bgUri, width, height)
            if (srcBitmap != null) {
                // Apply background blur if requested
                if (state.backgroundBlur > 0.5f) {
                    val blurR = (state.backgroundBlur * (width / 1080f)).toInt().coerceIn(1, 40)
                    val blurred = AtharImageProcessor.fastStackBlur(srcBitmap, blurR)
                    if (blurred != srcBitmap) {
                        srcBitmap.recycle()
                        srcBitmap = blurred
                    }
                }

                val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = AtharImageProcessor.createCompositeColorFilter(
                        dim = state.backgroundDim,
                        brightness = state.backgroundBrightness,
                        contrast = state.backgroundContrast,
                        saturation = state.backgroundSaturation,
                        isGrayscale = state.backgroundGrayscale
                    )
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
            if (state.solidBgColorLong != null) {
                canvas.drawColor(state.solidBgColorLong.toInt())
            } else {
                canvas.drawColor(Color.BLACK)
            }
        }

        // Apply cinematic vignette overlay if requested
        if (state.backgroundVignette > 0f) {
            AtharImageProcessor.drawVignette(canvas, width.toFloat(), height.toFloat(), state.backgroundVignette)
        }
    }

    private fun drawTypography(context: Context, canvas: Canvas, state: CanvasState, width: Int, height: Int) {
        val densityFactor = width / 380f
        val scaledTextSize = (state.fontSize / 380f) * width
        val realTypeface = AtharFontManager.getTypeface(context, state.selectedFont)
        val alphaFloat = state.textOpacity.coerceIn(0.15f, 1.0f)
        val alphaInt = (alphaFloat * 255).toInt()

        val alphaMatrix = ColorMatrix().apply {
            setScale(1f, 1f, 1f, alphaFloat)
        }
        val opacityFilter = ColorMatrixColorFilter(alphaMatrix)

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            textSize = scaledTextSize
            letterSpacing = state.letterSpacing
            isUnderlineText = state.isUnderline
            isFakeBoldText = state.isBold
            textSkewX = if (state.isItalic) -0.25f else 0f
            alpha = alphaInt
            colorFilter = opacityFilter
            textAlign = when (state.textAlign) {
                TextAlignOption.RIGHT -> Paint.Align.RIGHT
                TextAlignOption.CENTER -> Paint.Align.CENTER
                TextAlignOption.LEFT -> Paint.Align.LEFT
            }
            typeface = realTypeface
        }

        val strokePaint = if (state.strokeWidth > 0f) {
            Paint().apply {
                isAntiAlias = true
                isDither = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = (state.strokeWidth / 380f) * width
                color = state.strokeColorLong.toInt()
                letterSpacing = state.letterSpacing
                isUnderlineText = state.isUnderline
                isFakeBoldText = state.isBold
                textSkewX = if (state.isItalic) -0.25f else 0f
                alpha = alphaInt
                colorFilter = opacityFilter
                textSize = scaledTextSize
                textAlign = paint.textAlign
                typeface = realTypeface
            }
        } else null

        val shadowPaint = if (state.shadowRadius > 0f) {
            Paint().apply {
                isAntiAlias = true
                isDither = true
                textSize = scaledTextSize
                letterSpacing = state.letterSpacing
                isFakeBoldText = state.isBold
                textSkewX = if (state.isItalic) -0.25f else 0f
                textAlign = paint.textAlign
                typeface = realTypeface
                val sColor = state.shadowColorLong.toInt()
                val sAlpha = (((sColor ushr 24) and 0xFF) * alphaFloat).toInt()
                color = (sColor and 0x00FFFFFF) or (sAlpha shl 24)
                val scaledBlur = ((state.shadowRadius / 380f) * width).coerceAtLeast(1f)
                maskFilter = BlurMaskFilter(scaledBlur, BlurMaskFilter.Blur.NORMAL)
            }
        } else null

        val scaledShadowDx = (state.shadowDx / 380f) * width
        val scaledShadowDy = (state.shadowDy / 380f) * width

        val centerX = width * 0.5f + (width * state.normOffsetX)
        val centerY = height * 0.5f + (height * state.normOffsetY)

        val layout = AtharTextLayout.computeLayout(
            text = state.displayText,
            paint = paint,
            lineSpacingMultiplier = state.lineSpacingMultiplier,
            textAlign = state.textAlign
        )

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(state.rotationAngle)
        canvas.scale(state.scaleFactor, state.scaleFactor)

        // Draw Frosted Glass Plaque / Card behind text if enabled
        TextGlassBackdropRenderer.drawGlassBackdrop(
            canvas = canvas,
            state = state,
            layout = layout,
            textSizePx = paint.textSize,
            density = densityFactor
        )

        layout.lines.forEachIndexed { index, line ->
            val lineY = layout.startY + (index * layout.lineHeight)

            // 1. Draw 2D Directional Shadow
            shadowPaint?.let { shp ->
                canvas.drawText(line, layout.lineX + scaledShadowDx, lineY + scaledShadowDy, shp)
            }

            // 2. Draw Stroke
            strokePaint?.let { sp ->
                canvas.drawText(line, layout.lineX, lineY, sp)
            }

            // 3. Exact Shader and Color calculation per line matching CanvasView
            if (state.customTextColorLong != null && state.customTextColorLong != 0L) {
                val baseColor = state.customTextColorLong.toInt()
                paint.color = (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
                paint.shader = null
            } else {
                when (state.selectedEffect) {
                    VisualEffect.ROYAL_GOLD -> {
                        paint.shader = LinearGradient(
                            0f, lineY - paint.textSize * 0.85f,
                            0f, lineY + paint.textSize * 0.25f,
                            intArrayOf(
                                Color.parseColor("#FFF7DB"),
                                Color.parseColor("#F5C369"),
                                Color.parseColor("#D49438"),
                                Color.parseColor("#FFE8A3"),
                                Color.parseColor("#B0731E")
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
                                Color.parseColor("#FFFFFF"),
                                Color.parseColor("#9EA5B0"),
                                Color.parseColor("#181C22"),
                                Color.parseColor("#FFFFFF"),
                                Color.parseColor("#505866"),
                                Color.parseColor("#DDE2EA")
                            ),
                            floatArrayOf(0.0f, 0.2f, 0.45f, 0.55f, 0.8f, 1.0f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    VisualEffect.CYBER_NEON -> {
                        paint.color = (Color.parseColor("#00FF9D") and 0x00FFFFFF) or (alphaInt shl 24)
                        paint.shader = null
                    }
                    VisualEffect.CINEMATIC_SHADOW -> {
                        paint.color = (Color.parseColor("#F2F2F2") and 0x00FFFFFF) or (alphaInt shl 24)
                        paint.shader = null
                    }
                    VisualEffect.FROSTED_GLASS -> {
                        paint.shader = LinearGradient(
                            0f, lineY - paint.textSize * 0.85f,
                            0f, lineY + paint.textSize * 0.25f,
                            intArrayOf(
                                Color.parseColor("#E6FFFFFF"),
                                Color.parseColor("#33E2E8F0")
                            ),
                            null,
                            Shader.TileMode.CLAMP
                        )
                    }
                    VisualEffect.MINIMAL_LUXURY -> {
                        paint.color = (Color.WHITE and 0x00FFFFFF) or (alphaInt shl 24)
                        paint.shader = null
                    }
                }
            }

            canvas.drawText(line, layout.lineX, lineY, paint)
        }
        canvas.restore()
    }

    private fun saveToMediaStore(context: Context, bitmap: Bitmap, filename: String, isTransparent: Boolean) {
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
    }

    private fun shareExportedBitmap(context: Context, bitmap: Bitmap, filename: String, isTransparent: Boolean) {
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
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (isTransparent) "image/png" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة تصميم أَثَــر"))
    }

    fun decodeSampledBitmap(context: Context, uri: Uri, targetW: Int, targetH: Int): Bitmap? {
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

    fun cropTransparentBitmap(src: Bitmap, paddingPx: Int = 48): Bitmap {
        val width = src.width
        val height = src.height
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        val batchRows = 64.coerceAtMost(height)
        val batchPixels = IntArray(width * batchRows)

        var y = 0
        while (y < height) {
            val currentH = batchRows.coerceAtMost(height - y)
            src.getPixels(batchPixels, 0, width, 0, y, width, currentH)

            var offset = 0
            for (localY in 0 until currentH) {
                val actualY = y + localY
                var rowHasVisible = false
                for (x in 0 until width) {
                    val alpha = (batchPixels[offset + x] ushr 24) and 0xFF
                    if (alpha > 12) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        rowHasVisible = true
                    }
                }
                if (rowHasVisible) {
                    if (actualY < minY) minY = actualY
                    if (actualY > maxY) maxY = actualY
                }
                offset += width
            }
            y += currentH
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
}
