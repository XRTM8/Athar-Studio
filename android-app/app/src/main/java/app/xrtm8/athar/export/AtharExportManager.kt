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
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.core.FilmPresetEngine
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.LayerBlendMode
import app.xrtm8.athar.model.LuxuryGradientPreset
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.typography.AtharTextLayout
import app.xrtm8.athar.typography.CurvedTextRenderer
import app.xrtm8.athar.ui.components.CanvasGradientScrimRenderer
import app.xrtm8.athar.ui.components.CanvasShaderFactory
import app.xrtm8.athar.ui.components.TextGlassBackdropRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
            val height = when (state.aspectRatio) {
                CanvasAspectRatio.STORY_9_16 -> (width * 16f / 9f).toInt()
                CanvasAspectRatio.SQUARE_1_1 -> width
                CanvasAspectRatio.PORTRAIT_4_5 -> (width * 5f / 4f).toInt()
                CanvasAspectRatio.LANDSCAPE_16_9 -> (width * 9f / 16f).toInt()
                CanvasAspectRatio.WALLPAPER_9_19_5 -> (width * 19.5f / 9f).toInt()
                CanvasAspectRatio.FREE -> (width * 16f / 9f).toInt()
            }

            val effectiveTransparent = isTransparent || state.isTransparentBg
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            if (!effectiveTransparent) {
                drawBackground(context, canvas, state, width, height)
            }

            drawLayers(context, canvas, state.layers, width, height)

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
                    val newW = ((srcH * dstRatio) / scale).toInt().coerceIn(10, srcW.toInt())
                    val newH = (srcH / scale).toInt().coerceIn(10, srcH.toInt())
                    val maxShiftX = (srcW - newW) / 2f
                    val maxShiftY = (srcH - newH) / 2f
                    val xOffset = (((srcW - newW) / 2f) + (state.bgPhotoOffsetX * maxShiftX)).toInt().coerceIn(0, (srcW - newW).toInt())
                    val yOffset = (((srcH - newH) / 2f) + (state.bgPhotoOffsetY * maxShiftY)).toInt().coerceIn(0, (srcH - newH).toInt())
                    srcRect = Rect(xOffset, yOffset, xOffset + newW, yOffset + newH)
                } else {
                    val newW = (srcW / scale).toInt().coerceIn(10, srcW.toInt())
                    val newH = ((srcW / dstRatio) / scale).toInt().coerceIn(10, srcH.toInt())
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
    }

    private fun drawLayers(context: Context, canvas: Canvas, layers: List<TextLayer>, width: Int, height: Int) {
        val densityFactor = width / 380f

        layers.forEach { layer ->
            if (!layer.isVisible || layer.displayText.isEmpty()) return@forEach

            val scaledTextSize = (layer.fontSize / 380f) * width
            val realTypeface = AtharFontManager.getTypeface(context, layer.selectedFont, layer.customFontPath)
            val alphaFloat = layer.textOpacity.coerceIn(0.15f, 1.0f)
            val alphaInt = (alphaFloat * 255).toInt()
            val opacityFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(1f, 1f, 1f, alphaFloat) })

            val paint = Paint().apply {
                isAntiAlias = true
                isDither = true
                textSize = scaledTextSize
                letterSpacing = layer.letterSpacing
                isUnderlineText = layer.isUnderline
                isFakeBoldText = layer.isBold
                textSkewX = if (layer.isItalic) -0.25f else 0f
                alpha = alphaInt
                colorFilter = opacityFilter
                if (layer.blendMode != LayerBlendMode.NORMAL) {
                    xfermode = PorterDuffXfermode(layer.blendMode.porterDuffMode)
                }
                textAlign = when (layer.textAlign) {
                    TextAlignOption.RIGHT -> Paint.Align.RIGHT
                    TextAlignOption.CENTER -> Paint.Align.CENTER
                    TextAlignOption.LEFT -> Paint.Align.LEFT
                }
                typeface = realTypeface
            }

            val strokePaint = if (layer.strokeWidth > 0f || layer.isHollowText) {
                Paint().apply {
                    isAntiAlias = true
                    isDither = true
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    val effectiveWidth = if (layer.isHollowText && layer.strokeWidth <= 0f) 2.0f else layer.strokeWidth
                    strokeWidth = (effectiveWidth / 380f) * width
                    color = layer.strokeColorLong.toInt()
                    letterSpacing = layer.letterSpacing
                    isUnderlineText = layer.isUnderline
                    isFakeBoldText = layer.isBold
                    textSkewX = if (layer.isItalic) -0.25f else 0f
                    alpha = alphaInt
                    colorFilter = opacityFilter
                    textSize = scaledTextSize
                    textAlign = paint.textAlign
                    typeface = realTypeface
                }
            } else null

            val shadowPaint = if (layer.shadowRadius > 0f) {
                Paint().apply {
                    isAntiAlias = true
                    isDither = true
                    textSize = scaledTextSize
                    letterSpacing = layer.letterSpacing
                    isFakeBoldText = layer.isBold
                    textSkewX = if (layer.isItalic) -0.25f else 0f
                    textAlign = paint.textAlign
                    typeface = realTypeface
                    val sColor = layer.shadowColorLong.toInt()
                    val sAlpha = (((sColor ushr 24) and 0xFF) * alphaFloat).toInt()
                    color = (sColor and 0x00FFFFFF) or (sAlpha shl 24)
                    val scaledBlur = ((layer.shadowRadius / 380f) * width).coerceAtLeast(1f)
                    maskFilter = BlurMaskFilter(scaledBlur, BlurMaskFilter.Blur.NORMAL)
                }
            } else null

            val scaledShadowDx = (layer.shadowDx / 380f) * width
            val scaledShadowDy = (layer.shadowDy / 380f) * width

            val centerX = width * 0.5f + (width * layer.normOffsetX)
            val centerY = height * 0.5f + (height * layer.normOffsetY)

            val layout = AtharTextLayout.computeLayout(
                text = layer.displayText,
                paint = paint,
                lineSpacingMultiplier = layer.lineSpacingMultiplier,
                textAlign = layer.textAlign,
                maxWidth = width * layer.boxWidthRatio.coerceIn(0.2f, 1.0f)
            )

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(layer.rotationAngle)
            canvas.scale(layer.scaleFactor, layer.scaleFactor)

            // Draw Frosted Glass Plaque / Card behind text if enabled
            TextGlassBackdropRenderer.drawGlassBackdrop(
                canvas = canvas,
                layer = layer,
                layout = layout,
                textSizePx = paint.textSize,
                density = densityFactor
            )

            layout.lines.forEachIndexed { index, line ->
                val lineY = layout.startY + (index * layout.lineHeight)

                if (shadowPaint != null) {
                    if (layer.isCurved && kotlin.math.abs(layer.curveAngle) >= 1f) {
                        CurvedTextRenderer.drawCurvedText(canvas, line, layout.lineX + scaledShadowDx, lineY + scaledShadowDy, shadowPaint, layer.curveAngle)
                    } else {
                        canvas.drawText(line, layout.lineX + scaledShadowDx, lineY + scaledShadowDy, shadowPaint)
                    }
                }

                if (strokePaint != null) {
                    if (layer.isCurved && kotlin.math.abs(layer.curveAngle) >= 1f) {
                        CurvedTextRenderer.drawCurvedText(canvas, line, layout.lineX, lineY, strokePaint, layer.curveAngle)
                    } else {
                        canvas.drawText(line, layout.lineX, lineY, strokePaint)
                    }
                }

                if (!layer.isHollowText) {
                    val customColor = layer.customTextColorLong
                    val activeGrad = if (layer.gradientPreset != LuxuryGradientPreset.NONE) {
                        layer.gradientPreset
                    } else if (customColor == null) {
                        when (layer.selectedEffect) {
                            VisualEffect.LIQUID_CHROME -> LuxuryGradientPreset.TITANIUM_CHROME
                            VisualEffect.CYBER_NEON -> LuxuryGradientPreset.CYBER_EMERALD
                            VisualEffect.FROSTED_GLASS -> LuxuryGradientPreset.FROSTED_PEARL
                            VisualEffect.MINIMAL_LUXURY, VisualEffect.CINEMATIC_SHADOW -> LuxuryGradientPreset.NONE
                            else -> LuxuryGradientPreset.ANDALUSIAN_GOLD
                        }
                    } else {
                        LuxuryGradientPreset.NONE
                    }

                    if (activeGrad != LuxuryGradientPreset.NONE) {
                        val gradShader = CanvasShaderFactory.createMultiStopGradientShader(
                            preset = activeGrad,
                            angle = layer.gradientAngle,
                            isRadial = layer.isRadialGradient,
                            centerX = layout.lineX,
                            centerY = lineY,
                            radiusPx = paint.textSize * 3f
                        )
                        paint.shader = gradShader
                        paint.color = Color.WHITE
                    } else {
                        val baseColor = (customColor ?: 0xFFFFFFFFL).toInt()
                        paint.color = (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
                        paint.shader = null
                    }

                    if (layer.isCurved && kotlin.math.abs(layer.curveAngle) >= 1f) {
                        CurvedTextRenderer.drawCurvedText(canvas, line, layout.lineX, lineY, paint, layer.curveAngle)
                    } else {
                        canvas.drawText(line, layout.lineX, lineY, paint)
                    }
                }
            }

            canvas.restore()
        }
    }

    private fun cropTransparentBitmap(src: Bitmap, paddingPx: Int): Bitmap {
        val width = src.width
        val height = src.height

        val stride = if (width > 2000 || height > 2000) 8 else 4
        val rowPixels = IntArray(width)

        // 1. Find top boundary
        var top = -1
        for (y in 0 until height step stride) {
            src.getPixels(rowPixels, 0, width, 0, y, width, 1)
            var found = false
            for (x in 0 until width step stride) {
                if ((rowPixels[x] ushr 24) > 0) {
                    found = true
                    break
                }
            }
            if (found) {
                val exactStart = (y - stride + 1).coerceAtLeast(0)
                for (refY in exactStart..y) {
                    src.getPixels(rowPixels, 0, width, 0, refY, width, 1)
                    for (x in 0 until width) {
                        if ((rowPixels[x] ushr 24) > 0) {
                            top = refY
                            break
                        }
                    }
                    if (top != -1) break
                }
                break
            }
        }

        if (top == -1) return src

        // 2. Find bottom boundary
        var bottom = -1
        for (y in height - 1 downTo top step stride) {
            src.getPixels(rowPixels, 0, width, 0, y, width, 1)
            var found = false
            for (x in 0 until width step stride) {
                if ((rowPixels[x] ushr 24) > 0) {
                    found = true
                    break
                }
            }
            if (found) {
                val exactEnd = (y + stride - 1).coerceAtMost(height - 1)
                for (refY in exactEnd downTo y) {
                    src.getPixels(rowPixels, 0, width, 0, refY, width, 1)
                    for (x in 0 until width) {
                        if ((rowPixels[x] ushr 24) > 0) {
                            bottom = refY
                            break
                        }
                    }
                    if (bottom != -1) break
                }
                break
            }
        }
        if (bottom == -1) bottom = top

        // 3. Scan between top and bottom to find left and right bounds
        var left = width
        var right = 0
        for (y in top..bottom) {
            src.getPixels(rowPixels, 0, width, 0, y, width, 1)
            for (x in 0 until left) {
                if ((rowPixels[x] ushr 24) > 0) {
                    left = x
                    break
                }
            }
            for (x in width - 1 downTo kotlin.math.max(right, left)) {
                if ((rowPixels[x] ushr 24) > 0) {
                    right = x
                    break
                }
            }
        }

        if (left > right) return src

        val minX = (left - paddingPx).coerceAtLeast(0)
        val minY = (top - paddingPx).coerceAtLeast(0)
        val maxX = (right + paddingPx).coerceAtMost(width - 1)
        val maxY = (bottom + paddingPx).coerceAtMost(height - 1)

        val cropW = maxX - minX + 1
        val cropH = maxY - minY + 1

        return Bitmap.createBitmap(src, minX, minY, cropW, cropH)
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            var inSampleSize = 1
            while ((options.outHeight / inSampleSize) > reqHeight * 1.5 ||
                (options.outWidth / inSampleSize) > reqWidth * 1.5
            ) {
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

        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(format, quality, out)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
    }

    private fun shareExportedBitmap(context: Context, bitmap: Bitmap, filename: String, isTransparent: Boolean) {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val shareFile = File(cacheDir, filename)
        val format = if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val quality = if (isTransparent) 100 else 98

        FileOutputStream(shareFile).use { out ->
            bitmap.compress(format, quality, out)
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
