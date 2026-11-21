package app.xrtm8.athar.ui.canvas

import android.graphics.Paint
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.typography.AtharTextLayout
import kotlin.math.abs

object CanvasHitTester {

    /**
     * Precision canvas hit-testing to detect which text layer the user tapped.
     * Accounts for canvas offset, scaling, rotation angle, and finger touch targets.
     */
    fun findHitLayer(
        touchX: Float,
        touchY: Float,
        canvasW: Float,
        canvasH: Float,
        state: CanvasState,
        density: Float
    ): TextLayer? {
        val pad = 28f * density
        // Search top-to-bottom in visual z-order
        for (layer in state.layers.asReversed()) {
            if (!layer.isVisible || layer.displayText.isEmpty()) continue

            val cx = canvasW / 2f + (layer.normOffsetX * canvasW)
            val cy = canvasH / 2f + (layer.normOffsetY * canvasH)
            val scale = layer.scaleFactor.coerceAtLeast(0.1f)

            val scaledTextSize = (layer.fontSize / 380f) * canvasW
            val testPaint = Paint().apply {
                textSize = scaledTextSize
                letterSpacing = layer.letterSpacing
            }
            val layout = AtharTextLayout.computeLayout(
                text = layer.displayText,
                paint = testPaint,
                lineSpacingMultiplier = layer.lineSpacingMultiplier,
                textAlign = layer.textAlign,
                maxWidth = canvasW * layer.boxWidthRatio.coerceIn(0.2f, 1.0f)
            )

            val totalW = layout.maxLineWidth.coerceAtLeast(scaledTextSize * 1.5f)
            val totalH = (layout.totalHeight + scaledTextSize).coerceAtLeast(scaledTextSize)

            val halfW = (totalW / 2f) * scale + pad
            val halfH = (totalH / 2f) * scale + pad

            val dx = touchX - cx
            val dy = touchY - cy

            val rad = Math.toRadians(-layer.rotationAngle.toDouble())
            val cosA = Math.cos(rad).toFloat()
            val sinA = Math.sin(rad).toFloat()
            val localX = dx * cosA - dy * sinA
            val localY = dx * sinA + dy * cosA

            if (abs(localX) <= halfW && abs(localY) <= halfH) {
                return layer
            }
        }
        return null
    }
}
