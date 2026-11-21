package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import app.xrtm8.athar.model.CollageState

/**
 * Editorial Collage Viewport & Export Renderer (< 160 lines).
 */
object CollageCanvasRenderer {

    private val reusableSrcRect = Rect()
    private val reusableDstRect = Rect()
    private val clipPath = Path()
    private val rectF = RectF()

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

        // 1. Draw frame canvas background
        paint.color = state.frameColorLong.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width, height, paint)

        val spacing = state.spacingDp * density
        val radius = state.cornerRadiusDp * density

        val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x25FFFFFF
            style = Paint.Style.FILL
        }

        // 2. Render each cell
        state.cells.forEach { cell ->
            val left = (cell.leftNorm * width) + (spacing / 2f)
            val top = (cell.topNorm * height) + (spacing / 2f)
            val right = (cell.rightNorm * width) - (spacing / 2f)
            val bottom = (cell.bottomNorm * height) - (spacing / 2f)

            if (right <= left || bottom <= top) return@forEach

            canvas.save()
            clipPath.reset()
            rectF.set(left, top, right, bottom)
            clipPath.addRoundRect(rectF, radius, radius, Path.Direction.CW)
            canvas.clipPath(clipPath)

            val bmp = cellBitmaps[cell.id]
            if (bmp != null && !bmp.isRecycled) {
                val bw = bmp.width.toFloat()
                val bh = bmp.height.toFloat()
                val bRatio = bw / bh
                val cRatio = (right - left) / (bottom - top)

                val scale = cell.photoScale.coerceIn(0.5f, 4.0f)
                if (bRatio > cRatio) {
                    val newW = ((bh * cRatio) / scale).toInt().coerceIn(10, bw.toInt())
                    val newH = (bh / scale).toInt().coerceIn(10, bh.toInt())
                    val maxShiftX = (bw - newW) / 2f
                    val maxShiftY = (bh - newH) / 2f
                    val xOff = (((bw - newW) / 2f) + (cell.photoOffsetX * maxShiftX)).toInt().coerceIn(0, (bw - newW).toInt())
                    val yOff = (((bh - newH) / 2f) + (cell.photoOffsetY * maxShiftY)).toInt().coerceIn(0, (bh - newH).toInt())
                    reusableSrcRect.set(xOff, yOff, xOff + newW, yOff + newH)
                } else {
                    val newW = (bw / scale).toInt().coerceIn(10, bw.toInt())
                    val newH = ((bw / cRatio) / scale).toInt().coerceIn(10, bh.toInt())
                    val maxShiftX = (bw - newW) / 2f
                    val maxShiftY = (bh - newH) / 2f
                    val xOff = (((bw - newW) / 2f) + (cell.photoOffsetX * maxShiftX)).toInt().coerceIn(0, (bw - newW).toInt())
                    val yOff = (((bh - newH) / 2f) + (cell.photoOffsetY * maxShiftY)).toInt().coerceIn(0, (bh - newH).toInt())
                    reusableSrcRect.set(xOff, yOff, xOff + newW, yOff + newH)
                }

                reusableDstRect.set(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                canvas.drawBitmap(bmp, reusableSrcRect, reusableDstRect, paint)
            } else {
                canvas.drawRect(left, top, right, bottom, placeholderPaint)
            }

            canvas.restore()
        }
    }
}
