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

/**
 * Renders an ultra-luxurious frosted glass plaque / card behind Arabic typography.
 * Provides instant readability and high-end visual elegance over photos or solid backdrops.
 */
object TextGlassBackdropRenderer {

    fun drawGlassBackdrop(
        canvas: Canvas,
        state: CanvasState,
        textAlign: TextAlignOption,
        maxLineWidth: Float,
        halfBlockW: Float,
        startY: Float,
        totalHeight: Float,
        textSizePx: Float,
        density: Float
    ) {
        if (!state.enableGlassBackdrop) return

        val pad = state.glassBackdropPadding * density
        val bLeft = when (textAlign) {
            TextAlignOption.RIGHT -> -maxLineWidth - pad
            TextAlignOption.CENTER -> -halfBlockW - pad
            TextAlignOption.LEFT -> -pad
        }
        val bRight = when (textAlign) {
            TextAlignOption.RIGHT -> pad
            TextAlignOption.CENTER -> halfBlockW + pad
            TextAlignOption.LEFT -> maxLineWidth + pad
        }
        val bTop = startY - textSizePx * 0.75f - pad
        val bBottom = startY + totalHeight + textSizePx * 0.25f + pad

        val rectF = RectF(bLeft, bTop, bRight, bBottom)
        val cornerR = (state.glassBackdropCornerRadius * density).coerceAtLeast(4f)

        // 1. Soft Card Elevation Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 0, 0, 0)
            maskFilter = BlurMaskFilter(16f * density, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(rectF, cornerR, cornerR, shadowPaint)

        // 2. Frosted Glass Tint Base Fill
        val baseColor = state.glassBackdropColorLong.toInt()
        val alphaInt = (state.glassBackdropOpacity.coerceIn(0.1f, 0.98f) * 255).toInt()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
        }
        canvas.drawRoundRect(rectF, cornerR, cornerR, fillPaint)

        // 3. Diagonal Specular Glass Sheen Highlight
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                bLeft, bTop,
                bRight, bBottom,
                intArrayOf(
                    Color.argb((alphaInt * 0.40f).toInt(), 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Color.argb((alphaInt * 0.15f).toInt(), 255, 255, 255)
                ),
                floatArrayOf(0.0f, 0.45f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rectF, cornerR, cornerR, sheenPaint)

        // 4. Fine Precision Glass Border Stroke
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
            color = state.glassBackdropBorderColorLong.toInt()
        }
        canvas.drawRoundRect(rectF, cornerR, cornerR, strokePaint)
    }
}
