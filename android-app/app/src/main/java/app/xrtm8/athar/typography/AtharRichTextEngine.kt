package app.xrtm8.athar.typography

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import app.xrtm8.athar.model.LuxuryGradientPreset
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.TextSpanStyle
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.ui.components.CanvasShaderFactory
import kotlin.math.max
import kotlin.math.min

/**
 * Rich-text layout and rendering engine for multi-font spans and per-word background highlights.
 * Uses Android's native HarfBuzz + BiDi StaticLayout for 100% accurate Arabic ligatures,
 * mixed RTL/LTR ordering, and pixel-identical output across Preview, PDF, and 4K Export.
 */
object AtharRichTextEngine {

    data class HighlightRect(
        val rect: RectF,
        val color: Int,
        val cornerRadiusPx: Float,
        val borderColor: Int?,
        val borderWidthPx: Float
    )

    private class RichSpanMetrics(
        private val customTypeface: Typeface?,
        private val fakeBold: Boolean?,
        private val skewX: Float?,
        private val scaleMultiplier: Float,
        private val baselineShiftPx: Float
    ) : MetricAffectingSpan() {

        override fun updateMeasureState(tp: TextPaint) {
            apply(tp)
        }

        override fun updateDrawState(tp: TextPaint) {
            apply(tp)
        }

        private fun apply(tp: TextPaint) {
            if (customTypeface != null) tp.typeface = customTypeface
            if (fakeBold != null) tp.isFakeBoldText = fakeBold
            if (skewX != null) tp.textSkewX = skewX
            if (kotlin.math.abs(scaleMultiplier - 1.0f) > 0.01f) {
                tp.textSize = tp.textSize * scaleMultiplier
            }
            if (kotlin.math.abs(baselineShiftPx) > 0.01f) {
                tp.baselineShift += baselineShiftPx.toInt()
            }
        }
    }

    fun buildTextPaintFromLayer(
        context: Context,
        layer: TextLayer,
        canvasWidth: Float,
        fontFeatures: String?
    ): TextPaint {
        val effectiveWeight = if (layer.isBold && layer.fontWeight < 700) 700 else layer.fontWeight
        val realTypeface = AtharFontManager.getTypeface(context, layer.selectedFont, layer.customFontPath, effectiveWeight)
        val useFakeBold = layer.isBold && !AtharFontManager.hasRealWeight(context, layer.selectedFont, effectiveWeight, layer.customFontPath)
        val textSizePx = (layer.fontSize / 380f) * canvasWidth

        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = realTypeface
            textSize = textSizePx
            letterSpacing = layer.letterSpacing
            isUnderlineText = layer.isUnderline
            isFakeBoldText = useFakeBold
            textSkewX = if (layer.isItalic) -0.25f else 0f
            fontFeatureSettings = fontFeatures
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && layer.wordSpacing != 0f) {
                wordSpacing = (layer.wordSpacing / 380f) * canvasWidth
            }
        }
    }

    fun buildRichStaticLayout(
        context: Context,
        layer: TextLayer,
        basePaint: TextPaint,
        canvasWidth: Float,
        includeColorSpans: Boolean
    ): Pair<StaticLayout, List<TextSpanStyle>> {
        val text = layer.displayText
        val mappedSpans = RichTextSpanMapper.mapRawSpansToDisplaySpans(layer.rawText, text, layer.textSpans)
        val ssb = SpannableStringBuilder(text)
        val textSizePx = basePaint.textSize

        for (span in mappedSpans) {
            val s = span.start.coerceIn(0, text.length)
            val e = span.end.coerceIn(s, text.length)
            if (e <= s) continue

            if (span.hasFontOverride) {
                val targetFont = span.fontFamily ?: layer.selectedFont
                val targetCustomPath = span.customFontPath ?: if (span.fontFamily != null) null else layer.customFontPath
                val targetBold = span.isBold ?: layer.isBold
                val baseWeight = span.fontWeight ?: layer.fontWeight
                val effectiveWeight = if (targetBold && baseWeight < 700) 700 else baseWeight
                val tf = AtharFontManager.getTypeface(context, targetFont, targetCustomPath, effectiveWeight)
                val fakeBold = targetBold && !AtharFontManager.hasRealWeight(context, targetFont, effectiveWeight, targetCustomPath)
                val skew = when (span.isItalic) {
                    true -> -0.25f
                    false -> 0f
                    null -> null
                }
                ssb.setSpan(
                    RichSpanMetrics(
                        customTypeface = tf,
                        fakeBold = fakeBold,
                        skewX = skew,
                        scaleMultiplier = span.scaleMultiplier.coerceIn(0.5f, 2.0f),
                        baselineShiftPx = span.baselineShift * textSizePx
                    ),
                    s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (includeColorSpans && span.textColorLong != null) {
                ssb.setSpan(
                    ForegroundColorSpan(span.textColorLong.toInt()),
                    s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        val maxWidthPx = (canvasWidth * layer.boxWidthRatio.coerceIn(0.2f, 1.0f)).toInt().coerceAtLeast(32)
        val alignment = when (layer.textAlign) {
            TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignOption.RIGHT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignOption.LEFT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val layout = StaticLayout.Builder.obtain(ssb, 0, ssb.length, basePaint, maxWidthPx)
            .setAlignment(alignment)
            .setLineSpacing(0f, layer.lineSpacingMultiplier.coerceIn(0.8f, 3.0f))
            .setIncludePad(false)
            .build()

        return layout to mappedSpans
    }

    fun computeHighlightRects(
        layout: StaticLayout,
        displayText: String,
        mappedSpans: List<TextSpanStyle>,
        canvasWidth: Float
    ): List<HighlightRect> {
        val densityScale = canvasWidth / 380f
        val results = mutableListOf<HighlightRect>()
        val selPath = Path()

        for (span in mappedSpans) {
            if (!span.hasHighlight) continue
            val baseColor = span.highlightColorLong?.toInt() ?: continue
            val alpha = (span.highlightAlpha.coerceIn(0f, 1f) * 255).toInt()
            val argb = (alpha shl 24) or (baseColor and 0x00FFFFFF)
            val padH = span.highlightPaddingH * densityScale
            val padV = span.highlightPaddingV * densityScale
            val radius = span.highlightCornerRadius * densityScale
            val borderW = span.highlightBorderWidth * densityScale

            val startLine = layout.getLineForOffset(span.start.coerceIn(0, displayText.length))
            val endLine = layout.getLineForOffset((span.end - 1).coerceIn(0, max(0, displayText.length - 1)))

            for (line in startLine..endLine) {
                val segStart = max(span.start, layout.getLineStart(line))
                var segEnd = min(span.end, layout.getLineEnd(line))
                while (segEnd > segStart && displayText[segEnd - 1] == '\n') {
                    segEnd--
                }
                if (segEnd <= segStart) continue

                selPath.reset()
                layout.getSelectionPath(segStart, segEnd, selPath)
                val bounds = RectF()
                selPath.computeBounds(bounds, true)

                if (bounds.width() <= 0.5f || bounds.height() <= 0.5f) {
                    val x1 = layout.getPrimaryHorizontal(segStart)
                    val x2 = layout.getPrimaryHorizontal(segEnd)
                    val leftX = min(x1, x2)
                    val rightX = max(x1, x2)
                    if (rightX - leftX > 0.5f) {
                        bounds.set(
                            leftX,
                            layout.getLineTop(line).toFloat(),
                            rightX,
                            layout.getLineBottom(line).toFloat()
                        )
                    }
                }

                if (bounds.width() > 0.5f && bounds.height() > 0.5f) {
                    bounds.inset(-padH, -padV)
                    results.add(
                        HighlightRect(
                            rect = bounds,
                            color = argb,
                            cornerRadiusPx = radius,
                            borderColor = span.highlightBorderColorLong?.toInt(),
                            borderWidthPx = borderW
                        )
                    )
                }
            }
        }
        return results
    }

    fun drawRichText(
        canvas: Canvas,
        context: Context,
        layer: TextLayer,
        canvasWidth: Float,
        fontFeatures: String?,
        shadowBlurFilter: BlurMaskFilter?
    ): TypographyLayoutResult {
        val basePaint = buildTextPaintFromLayer(context, layer, canvasWidth, fontFeatures)
        val (fillLayout, mappedSpans) = buildRichStaticLayout(context, layer, basePaint, canvasWidth, includeColorSpans = true)

        val totalW = fillLayout.width.toFloat()
        val totalH = fillLayout.height.toFloat()
        val minLineLeft = (0 until fillLayout.lineCount).minOfOrNull { fillLayout.getLineLeft(it) } ?: 0f
        val maxLineRight = (0 until fillLayout.lineCount).maxOfOrNull { fillLayout.getLineRight(it) } ?: totalW
        val originX = -(minLineLeft + maxLineRight) / 2f
        val originY = -totalH / 2f

        val maxLineW = (maxLineRight - minLineLeft).coerceAtLeast(0f)

        canvas.save()
        canvas.translate(originX, originY)

        // 1. Draw Word Highlight Pills
        val highlights = computeHighlightRects(fillLayout, layer.displayText, mappedSpans, canvasWidth)
        if (highlights.isNotEmpty()) {
            val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
            for (hl in highlights) {
                hlPaint.color = hl.color
                canvas.drawRoundRect(hl.rect, hl.cornerRadiusPx, hl.cornerRadiusPx, hlPaint)
                if (hl.borderColor != null && hl.borderWidthPx > 0.5f) {
                    borderPaint.color = hl.borderColor
                    borderPaint.strokeWidth = hl.borderWidthPx
                    canvas.drawRoundRect(hl.rect, hl.cornerRadiusPx, hl.cornerRadiusPx, borderPaint)
                }
            }
        }

        // 1.5 Draw Outer Glow Pass
        if (layer.outerGlowRadius > 0.5f) {
            val glowRadiusPx = ((layer.outerGlowRadius / 380f) * canvasWidth).coerceAtLeast(1f)
            val glowPaint = TextPaint(basePaint).apply {
                color = layer.outerGlowColorLong.toInt()
                maskFilter = BlurMaskFilter(glowRadiusPx, BlurMaskFilter.Blur.NORMAL)
            }
            val (glowLayout, _) = buildRichStaticLayout(context, layer, glowPaint, canvasWidth, includeColorSpans = false)
            glowLayout.draw(canvas)
        }

        // 2. Draw Shadow Pass
        if (layer.shadowRadius > 0f) {
            val shadowDxPx = (layer.shadowDx / 380f) * canvasWidth
            val shadowDyPx = (layer.shadowDy / 380f) * canvasWidth
            val shadowPaint = TextPaint(basePaint).apply {
                color = layer.shadowColorLong.toInt()
                maskFilter = shadowBlurFilter
            }
            val (shadowLayout, _) = buildRichStaticLayout(context, layer, shadowPaint, canvasWidth, includeColorSpans = false)
            canvas.save()
            canvas.translate(shadowDxPx, shadowDyPx)
            shadowLayout.draw(canvas)
            canvas.restore()
        }

        // 3. Draw Stroke Pass
        val effectiveStroke = if (layer.isHollowText && layer.strokeWidth <= 0f) 2.0f else layer.strokeWidth
        if (effectiveStroke > 0f) {
            val strokePaint = TextPaint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = (effectiveStroke / 380f) * canvasWidth
                color = layer.strokeColorLong.toInt()
            }
            val (strokeLayout, _) = buildRichStaticLayout(context, layer, strokePaint, canvasWidth, includeColorSpans = false)
            strokeLayout.draw(canvas)
        }

        // 4. Draw Fill Pass
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

            if (layer.customGradient.isValid) {
                basePaint.shader = CanvasShaderFactory.createCustomGradientShader(
                    state = layer.customGradient,
                    centerX = totalW / 2f,
                    centerY = totalH / 2f,
                    radiusPx = basePaint.textSize * 3f
                )
                basePaint.color = Color.WHITE
            } else if (activeGrad != LuxuryGradientPreset.NONE) {
                basePaint.shader = CanvasShaderFactory.createMultiStopGradientShader(
                    preset = activeGrad,
                    angle = layer.gradientAngle,
                    isRadial = layer.isRadialGradient,
                    centerX = totalW / 2f,
                    centerY = totalH / 2f,
                    radiusPx = basePaint.textSize * 3f
                )
                basePaint.color = Color.WHITE
            } else {
                basePaint.color = (layer.customTextColorLong ?: 0xFFFFFFFFL).toInt()
                basePaint.shader = null
            }
            fillLayout.draw(canvas)
        }

        canvas.restore()

        val lineCount = fillLayout.lineCount.coerceAtLeast(1)
        val lineH = totalH / lineCount
        val lines = (0 until lineCount).map { idx ->
            val s = fillLayout.getLineStart(idx)
            val e = fillLayout.getLineEnd(idx)
            layer.displayText.substring(s, e).trimEnd('\n')
        }
        return TypographyLayoutResult(
            lines = lines,
            lineHeight = lineH,
            totalHeight = totalH,
            startY = -totalH / 2f + lineH * 0.75f,
            maxLineWidth = maxLineW,
            halfBlockW = maxLineW / 2f,
            lineX = when (layer.textAlign) {
                TextAlignOption.CENTER -> 0f
                TextAlignOption.RIGHT -> maxLineW / 2f
                TextAlignOption.LEFT -> -maxLineW / 2f
            }
        )
    }
}
