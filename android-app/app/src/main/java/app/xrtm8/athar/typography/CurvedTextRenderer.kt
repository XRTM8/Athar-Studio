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
 * proper RTL ligature connectivity, bidirectional character flow, and alignment.
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

        // Detect if text is Arabic / RTL to preserve natural reading direction along path
        val isRtl = text.any { 
            it in '\u0600'..'\u06FF' || 
            it in '\u0750'..'\u077F' || 
            it in '\u08A0'..'\u08FF' || 
            it in '\uFB50'..'\uFDFF' || 
            it in '\uFE70'..'\uFEFF' 
        }

        cachedPath.reset()

        if (curveAngle > 0f) {
            // Convex Arc (Arched Upward like a dome)
            val top = y - radius * 2f
            val bottom = y
            val left = x - radius
            val right = x + radius
            cachedRect.set(left, top, right, bottom + radius)

            if (isRtl) {
                // For RTL Arabic: start from top-right and sweep counter-clockwise to top-left
                val startAngle = 270f + (sweepAngle / 2f)
                cachedPath.addArc(cachedRect, startAngle, -sweepAngle)
            } else {
                // For LTR English: start from top-left and sweep clockwise to top-right
                val startAngle = 270f - (sweepAngle / 2f)
                cachedPath.addArc(cachedRect, startAngle, sweepAngle)
            }
        } else {
            // Concave Arc (Arched Downward like a bowl)
            val top = y - radius
            val bottom = y + radius
            val left = x - radius
            val right = x + radius
            cachedRect.set(left, top, right, bottom)

            if (isRtl) {
                // For RTL Arabic: start from bottom-right (less than 90°) and sweep clockwise to bottom-left
                val startAngle = 90f - (sweepAngle / 2f)
                cachedPath.addArc(cachedRect, startAngle, sweepAngle)
            } else {
                // For LTR English: start from bottom-left and sweep counter-clockwise to bottom-right
                val startAngle = 90f + (sweepAngle / 2f)
                cachedPath.addArc(cachedRect, startAngle, -sweepAngle)
            }
        }

        val pathLength = (sweepRad * radius).toFloat()
        val hOffset = max(0f, (pathLength - textWidth) / 2f)

        canvas.drawTextOnPath(text, cachedPath, hOffset, 0f, paint)
    }
}
