package app.xrtm8.athar.ui.canvas

import android.content.Context
import android.graphics.*
import app.xrtm8.athar.model.LayerBlendMode
import app.xrtm8.athar.model.LuxuryGradientPreset
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.typography.AtharTextLayout
import app.xrtm8.athar.typography.CurvedTextRenderer
import app.xrtm8.athar.ui.components.CanvasPaintPool
import app.xrtm8.athar.ui.components.CanvasBoundingBoxRenderer
import app.xrtm8.athar.ui.components.CanvasShaderFactory
import app.xrtm8.athar.ui.components.TextGlassBackdropRenderer
import kotlin.math.abs

object CanvasLayerTextRenderer {

    fun renderTextLayer(
        nativeCanvas: Canvas,
        context: Context,
        layer: TextLayer,
        isCurrentActive: Boolean,
        isFramingMode: Boolean,
        layerNormX: Float,
        layerNormY: Float,
        layerScale: Float,
        layerRotation: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        density: Float,
        paints: CanvasPaintPool
    ) {
        if (!layer.isVisible || layer.displayText.isEmpty()) return

        val realTypeface = AtharFontManager.getTypeface(context, layer.selectedFont, layer.customFontPath)
        val canvasDensityFactor = canvasWidth / 380f
        val textSizePx = (layer.fontSize / 380f) * canvasWidth
        val alphaInt = (layer.textOpacity.coerceIn(0f, 1f) * 255).toInt()
        val basePaint = paints.basePaint
        val shadowPaint = paints.shadowPaint
        val strokePaint = paints.strokePaint

        basePaint.typeface = realTypeface
        shadowPaint.typeface = realTypeface
        strokePaint.typeface = realTypeface

        val align = when (layer.textAlign) {
            TextAlignOption.RIGHT -> Paint.Align.RIGHT
            TextAlignOption.CENTER -> Paint.Align.CENTER
            TextAlignOption.LEFT -> Paint.Align.LEFT
        }

        basePaint.textAlign = align
        shadowPaint.textAlign = align
        strokePaint.textAlign = align

        basePaint.textSize = textSizePx
        basePaint.letterSpacing = layer.letterSpacing
        basePaint.isUnderlineText = layer.isUnderline
        basePaint.isFakeBoldText = layer.isBold
        basePaint.textSkewX = if (layer.isItalic) -0.25f else 0f
        shadowPaint.textSize = textSizePx
        shadowPaint.letterSpacing = layer.letterSpacing
        shadowPaint.textSkewX = basePaint.textSkewX
        shadowPaint.isFakeBoldText = layer.isBold

        strokePaint.textSize = textSizePx
        strokePaint.strokeJoin = Paint.Join.ROUND
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.letterSpacing = layer.letterSpacing
        strokePaint.isUnderlineText = layer.isUnderline
        strokePaint.isFakeBoldText = layer.isBold
        strokePaint.textSkewX = basePaint.textSkewX

        // OpenType Features: Stylistic Sets (ss01..ss20), Swash, and Discretionary Ligatures
        val fontFeatures = buildString {
            if (layer.stylisticSet in 1..20) {
                append(String.format(java.util.Locale.US, "\"ss%02d\" 1, ", layer.stylisticSet))
            }
            if (layer.enableSwash) {
                append("\"swsh\" 1, ")
            }
            if (layer.enableDiscretionaryLigatures) {
                append("\"dlig\" 1, ")
            }
        }.trimEnd(',', ' ').ifEmpty { null }

        basePaint.fontFeatureSettings = fontFeatures
        shadowPaint.fontFeatureSettings = fontFeatures
        strokePaint.fontFeatureSettings = fontFeatures

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && layer.wordSpacing != 0f) {
            val wordSpacingPx = (layer.wordSpacing / 380f) * canvasWidth
            basePaint.wordSpacing = wordSpacingPx
            shadowPaint.wordSpacing = wordSpacingPx
            strokePaint.wordSpacing = wordSpacingPx
        }

        val effectiveStroke = if (layer.isHollowText && layer.strokeWidth <= 0f) 2.0f else layer.strokeWidth
        if (effectiveStroke > 0f) {
            strokePaint.strokeWidth = (effectiveStroke / 380f) * canvasWidth
            strokePaint.color = layer.strokeColorLong.toInt()
        }

        val layerLayout = AtharTextLayout.computeLayout(
            text = layer.displayText,
            paint = basePaint,
            lineSpacingMultiplier = layer.lineSpacingMultiplier,
            textAlign = layer.textAlign,
            maxWidth = canvasWidth * layer.boxWidthRatio.coerceIn(0.2f, 1.0f)
        )

        nativeCanvas.save()

        val centerX = canvasWidth / 2f + (canvasWidth * layerNormX)
        val centerY = canvasHeight / 2f + (canvasHeight * layerNormY)

        nativeCanvas.translate(centerX, centerY)
        nativeCanvas.rotate(layerRotation)
        nativeCanvas.scale(layerScale, layerScale)

        // Draw Frosted Glass Plaque
        TextGlassBackdropRenderer.drawGlassBackdrop(
            canvas = nativeCanvas,
            layer = layer,
            layout = layerLayout,
            textSizePx = textSizePx,
            density = canvasDensityFactor
        )

        val shadowDxPx = (layer.shadowDx / 380f) * canvasWidth
        val shadowDyPx = (layer.shadowDy / 380f) * canvasWidth
        val shadowRadiusInt = ((layer.shadowRadius / 380f) * canvasWidth).toInt()
        val shadowBlurFilter = if (shadowRadiusInt > 0) {
            paints.shadowBlurFilterCache.getOrPut(shadowRadiusInt) {
                BlurMaskFilter(shadowRadiusInt.toFloat().coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
            }
        } else null

        val composite = alphaInt < 255 || layer.blendMode != LayerBlendMode.NORMAL
        if (composite) {
            paints.compositePaint.alpha = alphaInt
            paints.compositePaint.xfermode = if (layer.blendMode == LayerBlendMode.NORMAL) null else
                paints.blendModeXfermode.getOrPut(layer.blendMode) { PorterDuffXfermode(layer.blendMode.porterDuffMode) }
            nativeCanvas.saveLayer(null, paints.compositePaint)
        }
        layerLayout.lines.forEachIndexed { index, line ->
            val lineY = layerLayout.startY + (index * layerLayout.lineHeight)

            // 1. Draw 2D Directional Shadow
            if (layer.shadowRadius > 0f) {
                shadowPaint.color = layer.shadowColorLong.toInt()
                shadowPaint.maskFilter = shadowBlurFilter
                if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                    CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX + shadowDxPx, lineY + shadowDyPx, shadowPaint, layer.curveAngle)
                } else {
                    nativeCanvas.drawText(line, layerLayout.lineX + shadowDxPx, lineY + shadowDyPx, shadowPaint)
                }
            }

            // 2. Draw Stroke
            if (effectiveStroke > 0f) {
                strokePaint.strokeWidth = (effectiveStroke / 380f) * canvasWidth
                strokePaint.color = layer.strokeColorLong.toInt()
                if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                    CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, strokePaint, layer.curveAngle)
                } else {
                    nativeCanvas.drawText(line, layerLayout.lineX, lineY, strokePaint)
                }
            }

            // 3. Fill: Luxury Gradient or Solid Swatch
            if (!layer.isHollowText) {
                val activeGrad = if (layer.gradientPreset != LuxuryGradientPreset.NONE) {
                    layer.gradientPreset
                } else if (layer.customTextColorLong == null) {
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
                        centerX = layerLayout.lineX,
                        centerY = lineY,
                        radiusPx = textSizePx * 3f
                    )
                    basePaint.shader = gradShader
                    basePaint.color = Color.WHITE
                } else {
                    basePaint.color = (layer.customTextColorLong ?: 0xFFFFFFFFL).toInt()
                    basePaint.shader = null
                }

                if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                    CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, basePaint, layer.curveAngle)
                } else {
                    nativeCanvas.drawText(line, layerLayout.lineX, lineY, basePaint)
                }
            }
        }

        if (composite) nativeCanvas.restore()

        // 4. Draw Selection Handles on active layer
        if (isCurrentActive && !isFramingMode) {
            CanvasBoundingBoxRenderer.drawBoundingBox(
                nativeCanvas = nativeCanvas,
                textAlign = layer.textAlign,
                layout = layerLayout,
                textSizePx = textSizePx,
                density = density,
                selectBoxPaint = paints.selectBoxPaint,
                handlePaint = paints.handlePaint,
                handleFillPaint = paints.handleFillPaint
            )
        }

        nativeCanvas.restore()
    }

    fun drawEmptyPrompt(
        nativeCanvas: Canvas,
        context: Context,
        canvasWidth: Float,
        canvasHeight: Float,
        density: Float,
        isAmoledWhite: Boolean
    ) {
        val promptW = (230f * density).coerceAtMost(canvasWidth * 0.85f)
        val promptH = 68f * density
        val left = (canvasWidth - promptW) / 2f
        val top = (canvasHeight - promptH) / 2f

        val dashedBoxPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = 0x55E02447.toInt()
            strokeWidth = 1.4f * density
            pathEffect = DashPathEffect(floatArrayOf(8f * density, 6f * density), 0f)
        }
        val bgBoxPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = if (isAmoledWhite) 0x14000000.toInt() else 0x33000000.toInt()
        }
        val textPromptPaint = Paint().apply {
            isAntiAlias = true
            color = 0xCCE02447.toInt()
            textSize = 13.5f * density
            textAlign = Paint.Align.CENTER
            typeface = AtharFontManager.getTypeface(context, app.xrtm8.athar.model.ArabicFontFamily.AREF_RUQAA)
        }
        val subTextPaint = Paint().apply {
            isAntiAlias = true
            color = if (isAmoledWhite) 0x99000000.toInt() else 0x88FFFFFF.toInt()
            textSize = 10f * density
            textAlign = Paint.Align.CENTER
        }

        val rectF = RectF(left, top, left + promptW, top + promptH)
        nativeCanvas.drawRoundRect(rectF, 14f * density, 14f * density, bgBoxPaint)
        nativeCanvas.drawRoundRect(rectF, 14f * density, 14f * density, dashedBoxPaint)
        nativeCanvas.drawText("انقر مرتين هنا لكتابة النص العربي", canvasWidth / 2f, top + 30f * density, textPromptPaint)
        nativeCanvas.drawText("أو استخدم خيار الخط والنص بالأسفل", canvasWidth / 2f, top + 50f * density, subTextPaint)
    }
}
