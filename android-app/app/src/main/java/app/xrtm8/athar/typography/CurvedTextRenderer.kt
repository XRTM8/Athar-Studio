package app.xrtm8.athar.typography

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
            .coerceAtLeast(paint.textSize * 0.6f)
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

    /**
     * Converts curved Arabic text into a warped vector outline [Path] matching [drawCurvedText].
     * Preserves complete HarfBuzz shaping, ligatures, diacritics, and kashida joins cleanly for SVG export.
     */
    fun buildCurvedOutlinePath(
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        curveAngle: Float
    ): Path? {
        if (text.isEmpty()) return null
        val textWidth = paint.measureText(text)
        if (textWidth <= 0f) return null

        val flatPath = Path()
        val savedAlign = paint.textAlign
        try {
            paint.textAlign = Paint.Align.LEFT
            paint.getTextPath(text, 0, text.length, 0f, 0f, flatPath)
        } finally {
            paint.textAlign = savedAlign
        }

        if (abs(curveAngle) < 1f) {
            val offsetX = when (savedAlign) {
                Paint.Align.LEFT -> x
                Paint.Align.RIGHT -> x - textWidth
                else -> x - textWidth / 2f
            }
            flatPath.offset(offsetX, y)
            return flatPath
        }

        val sweep = curveAngle.coerceIn(-350f, 350f)
        val radius = (textWidth / Math.toRadians(abs(sweep).toDouble())).toFloat()
            .coerceAtLeast(paint.textSize * 0.6f)
        val centerX = when (savedAlign) {
            Paint.Align.LEFT -> x + textWidth / 2f
            Paint.Align.RIGHT -> x - textWidth / 2f
            else -> x
        }
        val centerY = if (sweep > 0) y + radius else y - radius
        val middleRad = Math.toRadians(if (sweep > 0) 270.0 else 90.0)
        val sign = if (sweep > 0) 1.0 else -1.0

        val warped = Path().apply { fillType = flatPath.fillType }
        val pm = PathMeasure(flatPath, false)
        val pos = FloatArray(2)
        val stepPx = (paint.textSize * 0.025f).coerceIn(0.6f, 2.0f)

        do {
            val len = pm.length
            if (len <= 0f) continue
            val steps = (len / stepPx).toInt().coerceIn(8, 2000)
            for (i in 0..steps) {
                val d = if (i == steps) len else (len * i) / steps
                pm.getPosTan(d, pos, null)
                val px = pos[0]
                val py = pos[1]
                val theta = middleRad + sign * ((px - textWidth * 0.5f) / radius)
                val r = if (sweep > 0) (radius - py) else (radius + py)
                val wx = (centerX + r * cos(theta)).toFloat()
                val wy = (centerY + r * sin(theta)).toFloat()
                if (i == 0) warped.moveTo(wx, wy) else warped.lineTo(wx, wy)
            }
            warped.close()
        } while (pm.nextContour())

        return warped
    }
}
