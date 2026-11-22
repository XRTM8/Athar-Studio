package app.xrtm8.athar.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.typography.TypographyLayoutResult

/**
 * Renders an ultra-luxurious frosted glass plaque / card behind Arabic typography.
 * Provides instant readability and high-end visual elegance over photos or solid backdrops.
 */
object TextGlassBackdropRenderer {

    private val reusableRectF = RectF()
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(100, 0, 0, 0) }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val sheenStops = floatArrayOf(0.0f, 0.45f, 1.0f)
    private val sheenColors = IntArray(3)

    private var cachedFilterDensity = 0f
    private var cachedBlurFilter: BlurMaskFilter? = null

    private fun getShadowBlurFilter(density: Float): BlurMaskFilter {
        val existing = cachedBlurFilter
        if (existing != null && cachedFilterDensity == density) {
            return existing
        }
        val filter = BlurMaskFilter((16f * density).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        cachedFilterDensity = density
        cachedBlurFilter = filter
        return filter
    }

    fun drawGlassBackdrop(
        canvas: Canvas,
        state: CanvasState,
        layout: TypographyLayoutResult,
        textSizePx: Float,
        density: Float
    ) {
        drawGlassBackdrop(
            canvas = canvas,
            enableGlassBackdrop = state.enableGlassBackdrop,
            glassBackdropPadding = state.glassBackdropPadding,
            glassBackdropCornerRadius = state.glassBackdropCornerRadius,
            glassBackdropColorLong = state.glassBackdropColorLong,
            glassBackdropBorderColorLong = state.glassBackdropBorderColorLong,
            glassBackdropOpacity = state.glassBackdropOpacity,
            textAlign = state.textAlign,
            layout = layout,
            textSizePx = textSizePx,
            density = density
        )
    }

    fun drawGlassBackdrop(
        canvas: Canvas,
        layer: TextLayer,
        layout: TypographyLayoutResult,
        textSizePx: Float,
        density: Float
    ) {
        drawGlassBackdrop(
            canvas = canvas,
            enableGlassBackdrop = layer.enableGlassBackdrop,
            glassBackdropPadding = layer.glassBackdropPadding,
            glassBackdropCornerRadius = layer.glassBackdropCornerRadius,
            glassBackdropColorLong = layer.glassBackdropColorLong,
            glassBackdropBorderColorLong = layer.glassBackdropBorderColorLong,
            glassBackdropOpacity = layer.glassBackdropOpacity,
            textAlign = layer.textAlign,
            layout = layout,
            textSizePx = textSizePx,
            density = density
        )
    }

    @Synchronized
    fun drawGlassBackdrop(
        canvas: Canvas,
        enableGlassBackdrop: Boolean,
        glassBackdropPadding: Float,
        glassBackdropCornerRadius: Float,
        glassBackdropColorLong: Long,
        glassBackdropBorderColorLong: Long,
        glassBackdropOpacity: Float,
        textAlign: TextAlignOption,
        layout: TypographyLayoutResult,
        textSizePx: Float,
        density: Float
    ) {
        if (!enableGlassBackdrop || layout.maxLineWidth <= 0f) return

        val pad = glassBackdropPadding * density
        val bLeft = when (textAlign) {
            TextAlignOption.RIGHT -> -layout.maxLineWidth - pad
            TextAlignOption.CENTER -> -layout.halfBlockW - pad
            TextAlignOption.LEFT -> -pad
        }
        val bRight = when (textAlign) {
            TextAlignOption.RIGHT -> pad
            TextAlignOption.CENTER -> layout.halfBlockW + pad
            TextAlignOption.LEFT -> layout.maxLineWidth + pad
        }
        val bTop = layout.startY - textSizePx * 0.75f - pad
        val bBottom = layout.startY + layout.totalHeight + textSizePx * 0.25f + pad

        reusableRectF.set(bLeft, bTop, bRight, bBottom)
        val cornerR = (glassBackdropCornerRadius * density).coerceAtLeast(4f)

        // 1. Soft Card Elevation Shadow (zero allocation)
        shadowPaint.maskFilter = getShadowBlurFilter(density)
        canvas.drawRoundRect(reusableRectF, cornerR, cornerR, shadowPaint)

        // 2. Frosted Glass Tint Base Fill
        val baseColor = glassBackdropColorLong.toInt()
        val alphaInt = (glassBackdropOpacity.coerceIn(0.1f, 0.98f) * 255).toInt()
        fillPaint.color = (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
        canvas.drawRoundRect(reusableRectF, cornerR, cornerR, fillPaint)

        // 3. Diagonal Specular Glass Sheen Highlight
        sheenColors[0] = Color.argb((alphaInt * 0.40f).toInt(), 255, 255, 255)
        sheenColors[1] = Color.argb(0, 255, 255, 255)
        sheenColors[2] = Color.argb((alphaInt * 0.15f).toInt(), 255, 255, 255)

        sheenPaint.shader = LinearGradient(
            bLeft, bTop,
            bRight, bBottom,
            sheenColors,
            sheenStops,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(reusableRectF, cornerR, cornerR, sheenPaint)

        // 4. Fine Precision Glass Border Stroke
        strokePaint.strokeWidth = 1.2f * density
        strokePaint.color = glassBackdropBorderColorLong.toInt()
        canvas.drawRoundRect(reusableRectF, cornerR, cornerR, strokePaint)
    }
}
