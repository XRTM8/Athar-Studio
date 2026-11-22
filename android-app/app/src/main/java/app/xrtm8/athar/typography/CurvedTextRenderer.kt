package app.xrtm8.athar.typography

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs

/**
 * Curved and Circular Path Arabic Typography Renderer.
 * Accurately curves cursive Arabic text along an arc path while preserving
 * proper RTL ligature connectivity, bidirectional character flow, and alignment.
 * 
 * Thread-safe: Uses ThreadLocal instances for Path and RectF to prevent
 * concurrency collisions between the UI render loop and background 4K export threads.
 */
object CurvedTextRenderer {

    private val threadPath = ThreadLocal.withInitial { Path() }
    private val threadRect = ThreadLocal.withInitial { RectF() }

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

        val textWidth = paint.measureText(text)
        if (textWidth <= 0f) return
        val sweep = curveAngle.coerceIn(-350f, 350f)
        val radius = (textWidth / Math.toRadians(abs(sweep).toDouble())).toFloat()
        val centerX = when (paint.textAlign) {
            Paint.Align.LEFT -> x + textWidth / 2f
            Paint.Align.RIGHT -> x - textWidth / 2f
            else -> x
        }
        val centerY = if (sweep > 0) y + radius else y - radius
        val middleAngle = if (sweep > 0) 270f else 90f
        val path = threadPath.get()!!
        val rect = threadRect.get()!!
        rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        path.reset()
        // Android shapes the RTL run. Both arcs need a left-to-right tangent for upright glyphs.
        path.addArc(rect, middleAngle - sweep / 2f, sweep)
        val align = paint.textAlign
        try {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawTextOnPath(text, path, 0f, 0f, paint)
        } finally {
            paint.textAlign = align
        }
    }
}
