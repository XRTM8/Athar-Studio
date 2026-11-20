package app.xrtm8.athar.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.typography.AtharTextLayout
import app.xrtm8.athar.ui.components.TextGlassBackdropRenderer

/**
 * Dedicated thumbnail renderer for Athar Projects (.athar).
 * Generates crisp, lightweight preview bitmaps for the Projects bottom sheet.
 * Renders all visible layers in full fidelity.
 */
object AtharProjectThumbnailRenderer {

    fun renderThumbnail(context: Context, state: CanvasState): Bitmap {
        val width = 480
        val height = when (state.aspectRatio) {
            CanvasAspectRatio.STORY_9_16 -> (width * 16f / 9f).toInt()
            CanvasAspectRatio.SQUARE_1_1 -> width
            CanvasAspectRatio.PORTRAIT_4_5 -> (width * 5f / 4f).toInt()
            CanvasAspectRatio.LANDSCAPE_16_9 -> (width * 9f / 16f).toInt()
            CanvasAspectRatio.WALLPAPER_9_19_5 -> (width * 19.5f / 9f).toInt()
            CanvasAspectRatio.FREE -> (width * 16f / 9f).toInt()
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!state.isTransparentBg) {
            drawBackground(context, canvas, state, width, height)
        }

        drawTypography(context, canvas, state, width, height)

        return bitmap
    }

    private fun drawBackground(context: Context, canvas: Canvas, state: CanvasState, width: Int, height: Int) {
        var photoDrawn = false
        if (state.backgroundImageUri != null) {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(state.backgroundImageUri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                var sample = 1
                while ((opts.outWidth / sample) > width * 2 || (opts.outHeight / sample) > height * 2) {
                    sample *= 2
                }
                val finalOpts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val srcBm = context.contentResolver.openInputStream(state.backgroundImageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, finalOpts)
                }
                if (srcBm != null) {
                    val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                    if (state.backgroundGrayscale) {
                        val cm = ColorMatrix().apply { setSaturation(0f) }
                        bgPaint.colorFilter = ColorMatrixColorFilter(cm)
                    }
                    val srcW = srcBm.width.toFloat()
                    val srcH = srcBm.height.toFloat()
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
                    canvas.drawBitmap(srcBm, srcRect, Rect(0, 0, width, height), bgPaint)
                    photoDrawn = true
                    srcBm.recycle()
                }
            } catch (e: Exception) {
                // Ignore fallback to color
            }
        }

        if (!photoDrawn) {
            if (state.solidBgColorLong != null) {
                canvas.drawColor(state.solidBgColorLong.toInt())
            } else {
                val topColor = Color.parseColor("#12141A")
                val bottomColor = Color.parseColor("#1B2232")
                val gradientPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        topColor, bottomColor, Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
            }
        }
    }

    private fun drawTypography(context: Context, canvas: Canvas, state: CanvasState, width: Int, height: Int) {
        val layersToRender = if (state.layers.isNotEmpty()) state.layers else listOf(state.activeLayer)

        layersToRender.forEach { layer ->
            if (!layer.isVisible || layer.displayText.isEmpty()) return@forEach

            val scaledTextSize = (layer.fontSize / 380f) * width
            val realTypeface = AtharFontManager.getTypeface(context, layer.selectedFont)

            val paint = Paint().apply {
                isAntiAlias = true
                isDither = true
                textSize = scaledTextSize
                letterSpacing = layer.letterSpacing
                isUnderlineText = layer.isUnderline
                isFakeBoldText = layer.isBold
                textSkewX = if (layer.isItalic) -0.25f else 0f
                alpha = (layer.textOpacity * 255).toInt().coerceIn(20, 255)
                textAlign = when (layer.textAlign) {
                    TextAlignOption.RIGHT -> Paint.Align.RIGHT
                    TextAlignOption.CENTER -> Paint.Align.CENTER
                    TextAlignOption.LEFT -> Paint.Align.LEFT
                }
                typeface = realTypeface
            }

            val customColor = layer.customTextColorLong
            if (customColor != null && customColor != 0L) {
                paint.color = customColor.toInt()
            } else {
                when (layer.selectedEffect) {
                    VisualEffect.ROYAL_GOLD -> {
                        paint.shader = LinearGradient(
                            0f, -paint.textSize * 0.8f,
                            0f, paint.textSize * 0.2f,
                            intArrayOf(
                                Color.parseColor("#FFF7DB"),
                                Color.parseColor("#F5C369"),
                                Color.parseColor("#D49438"),
                                Color.parseColor("#FFE8A3")
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
                                Color.parseColor("#FFFFFF"),
                                Color.parseColor("#9EA5B0"),
                                Color.parseColor("#181C22"),
                                Color.parseColor("#FFFFFF")
                            ),
                            null,
                            Shader.TileMode.CLAMP
                        )
                    }
                    VisualEffect.CYBER_NEON -> paint.color = Color.parseColor("#00FF9D")
                    VisualEffect.CINEMATIC_SHADOW -> paint.color = Color.parseColor("#F2F2F2")
                    VisualEffect.FROSTED_GLASS -> paint.color = Color.parseColor("#E6FFFFFF")
                    VisualEffect.MINIMAL_LUXURY -> paint.color = Color.WHITE
                }
            }

            val centerX = width * 0.5f + (width * layer.normOffsetX)
            val centerY = height * 0.5f + (height * layer.normOffsetY)

            val layout = AtharTextLayout.computeLayout(
                text = layer.displayText,
                paint = paint,
                lineSpacingMultiplier = layer.lineSpacingMultiplier,
                textAlign = layer.textAlign
            )

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(layer.rotationAngle)
            canvas.scale(layer.scaleFactor, layer.scaleFactor)

            // Draw Frosted Glass Plaque in thumbnail if enabled
            TextGlassBackdropRenderer.drawGlassBackdrop(
                canvas = canvas,
                layer = layer,
                layout = layout,
                textSizePx = paint.textSize,
                density = width / 380f
            )

            layout.lines.forEachIndexed { index, line ->
                val lineY = layout.startY + (index * layout.lineHeight)
                canvas.drawText(line, layout.lineX, lineY, paint)
            }
            canvas.restore()
        }
    }
}
