package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import app.xrtm8.athar.core.CollageCoverFitMath
import app.xrtm8.athar.model.CollageCell
import app.xrtm8.athar.model.CollageState

/**
 * Editorial Collage Viewport & Export Renderer.
 * Thread-safe via ThreadLocal Skia scratch objects (zero contention, subpixel Matrix rendering).
 */
object CollageCanvasRenderer {

    private class ThreadScratch {
        val matrix = Matrix()
        val clipPath = Path()
        val rectF = RectF()
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x25FFFFFF
            style = Paint.Style.FILL
        }
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFE02447.toInt()
        }
    }

    private val scratchLocal = ThreadLocal<ThreadScratch>()
    private fun scratch(): ThreadScratch =
        scratchLocal.get() ?: ThreadScratch().also { scratchLocal.set(it) }

    fun drawCollage(
        canvas: Canvas,
        state: CollageState,
        width: Float,
        height: Float,
        density: Float,
        cellBitmaps: Map<String, Bitmap>,
        paint: Paint
    ) {
        if (!state.isEnabled) return
        val s = scratch()
        s.framePaint.color = state.frameColorLong.toInt()
        canvas.drawRect(0f, 0f, width, height, s.framePaint)

        drawEmptyPlaceholders(canvas, state, width, height, density) { cell ->
            val bmp = cellBitmaps[cell.id]
            bmp == null || bmp.isRecycled
        }
        drawCollagePhotoLayer(canvas, state, width, height, density, cellBitmaps, paint)
    }

    fun drawEmptyPlaceholders(
        canvas: Canvas,
        state: CollageState,
        width: Float,
        height: Float,
        density: Float,
        isEmptyPredicate: (CollageCell) -> Boolean
    ) {
        if (!state.isEnabled) return
        val s = scratch()
        val spacing = state.spacingDp * density
        val radius = state.cornerRadiusDp * density
        state.cells.forEach { cell ->
            if (!isEmptyPredicate(cell)) return@forEach
            val b = CollageCoverFitMath.computeCellBounds(cell, width, height, spacing)
            if (b[2] <= b[0] || b[3] <= b[1]) return@forEach
            s.rectF.set(b[0], b[1], b[2], b[3])
            canvas.drawRoundRect(s.rectF, radius, radius, s.placeholderPaint)
        }
    }

    fun drawCollagePhotoLayer(
        canvas: Canvas,
        state: CollageState,
        width: Float,
        height: Float,
        density: Float,
        cellBitmaps: Map<String, Bitmap>,
        paint: Paint
    ) {
        if (!state.isEnabled) return
        val s = scratch()
        val spacing = state.spacingDp * density
        val radius = state.cornerRadiusDp * density
        state.cells.forEach { cell ->
            val bmp = cellBitmaps[cell.id]
            if (bmp == null || bmp.isRecycled) return@forEach
            drawCellBitmap(canvas, cell, bmp, width, height, spacing, radius, paint, s)
        }
    }

    fun drawSingleCellPhoto(
        canvas: Canvas,
        cell: CollageCell,
        bitmap: Bitmap,
        width: Float,
        height: Float,
        spacing: Float,
        radius: Float,
        paint: Paint
    ) {
        if (bitmap.isRecycled) return
        drawCellBitmap(canvas, cell, bitmap, width, height, spacing, radius, paint, scratch())
    }

    private fun drawCellBitmap(
        canvas: Canvas,
        cell: CollageCell,
        bmp: Bitmap,
        width: Float,
        height: Float,
        spacing: Float,
        radius: Float,
        paint: Paint,
        s: ThreadScratch
    ) {
        val b = CollageCoverFitMath.computeCellBounds(cell, width, height, spacing)
        if (b[2] <= b[0] || b[3] <= b[1]) return

        canvas.save()
        s.clipPath.reset()
        s.rectF.set(b[0], b[1], b[2], b[3])
        s.clipPath.addRoundRect(s.rectF, radius, radius, Path.Direction.CW)
        canvas.clipPath(s.clipPath)

        CollageCoverFitMath.populateMatrix(
            outMatrix = s.matrix,
            srcW = bmp.width.toFloat(),
            srcH = bmp.height.toFloat(),
            dstLeft = b[0],
            dstTop = b[1],
            dstRight = b[2],
            dstBottom = b[3],
            offsetX = cell.photoOffsetX,
            offsetY = cell.photoOffsetY,
            scale = cell.photoScale,
            rotationDeg = cell.photoRotationDeg,
            isFlippedH = cell.isFlippedH,
            isFlippedV = cell.isFlippedV
        )
        canvas.drawBitmap(bmp, s.matrix, paint)
        canvas.restore()
    }

    fun buildAllCellsClipPath(
        state: CollageState,
        width: Float,
        height: Float,
        density: Float
    ): Path {
        val path = Path()
        if (!state.isEnabled) return path
        val spacing = state.spacingDp * density
        val radius = state.cornerRadiusDp * density
        val rect = RectF()
        state.cells.forEach { cell ->
            val b = CollageCoverFitMath.computeCellBounds(cell, width, height, spacing)
            if (b[2] > b[0] && b[3] > b[1]) {
                rect.set(b[0], b[1], b[2], b[3])
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
        }
        return path
    }

    fun drawActiveCellHighlight(
        canvas: Canvas,
        state: CollageState,
        activeCellId: String?,
        width: Float,
        height: Float,
        density: Float
    ) {
        if (!state.isEnabled || activeCellId == null) return
        val cell = state.cells.find { it.id == activeCellId } ?: return
        val s = scratch()
        val spacing = state.spacingDp * density
        val radius = state.cornerRadiusDp * density
        val b = CollageCoverFitMath.computeCellBounds(cell, width, height, spacing)
        if (b[2] <= b[0] || b[3] <= b[1]) return

        val strokeW = (2.5f * density).coerceAtLeast(2f)
        s.highlightPaint.strokeWidth = strokeW
        s.rectF.set(
            b[0] + strokeW * 0.5f,
            b[1] + strokeW * 0.5f,
            b[2] - strokeW * 0.5f,
            b[3] - strokeW * 0.5f
        )
        val innerR = (radius - strokeW * 0.5f).coerceAtLeast(0f)
        canvas.drawRoundRect(s.rectF, innerR, innerR, s.highlightPaint)
    }
}
