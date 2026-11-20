package app.xrtm8.athar.typography

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max

/**
 * Curved and Circular Path Arabic Typography Renderer.
 * Accurately curves cursive Arabic text along an arc path while preserving
 * proper RTL ligature connectivity and alignment.
 */
object CurvedTextRenderer {

    private val cachedPath = Path()
    private val cachedRect = RectF()

    fun drawCurvedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        curveAngle: Float
    ) {
        if (abs(curveAngle) < 1f || text.isEmpty()) {
            canvas.drawText(text, x, y, paint)
            return
        }

        val textWidth = max(paint.measureText(text), 10f)
        val sweepAngle = abs(curveAngle).coerceIn(10f, 350f)
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val radius = (textWidth / sweepRad).toFloat().coerceAtLeast(textWidth * 0.35f)

        cachedPath.reset()

        if (curveAngle > 0f) {
            // Convex Arc (Arched Upward like an umbrella)
            val top = y - radius * 2f
            val bottom = y
            val left = x - radius
            val right = x + radius
            cachedRect.set(left, top, right, bottom + radius)

            // Start angle from top
            val startAngle = 270f - (sweepAngle / 2f)
            cachedPath.addArc(cachedRect, startAngle, sweepAngle)
        } else {
            // Concave Arc (Arched Downward like a bowl)
            val top = y - radius
            val bottom = y + radius
            val left = x - radius
            val right = x + radius
            cachedRect.set(left, top, right, bottom)

            val startAngle = 90f - (sweepAngle / 2f)
            cachedPath.addArc(cachedRect, startAngle, sweepAngle)
        }

        val pathLength = (sweepRad * radius).toFloat()
        val hOffset = max(0f, (pathLength - textWidth) / 2f)

        canvas.drawTextOnPath(text, cachedPath, hOffset, 0f, paint)
    }
}
