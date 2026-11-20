package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import app.xrtm8.athar.model.CanvasState

/**
 * Renders the canvas viewport background:
 * - Transparency checkerboard grid
 * - Aspect-fit photo image with optional grayscale and dim
 * - Solid theme background color
 */
object CanvasBackgroundRenderer {

    fun drawBackground(
        nativeCanvas: Canvas,
        state: CanvasState,
        canvasWidth: Float,
        canvasHeight: Float,
        density: Float,
        bgBitmap: Bitmap?,
        bgPaint: Paint,
        grayMatrix: ColorMatrix
    ) {
        if (state.isTransparentBg) {
            val tileSize = 16f * density
            val darkTile = Color.parseColor("#121418")
            val lightTile = Color.parseColor("#1A1D24")
            val tilePaint = Paint()
            val cols = (canvasWidth / tileSize).toInt() + 1
            val rows = (canvasHeight / tileSize).toInt() + 1
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    tilePaint.color = if ((r + c) % 2 == 0) darkTile else lightTile
                    nativeCanvas.drawRect(
                        c * tileSize,
                        r * tileSize,
                        (c + 1) * tileSize,
                        (r + 1) * tileSize,
                        tilePaint
                    )
                }
            }
        } else if (bgBitmap != null) {
            if (state.backgroundGrayscale) {
                bgPaint.colorFilter = ColorMatrixColorFilter(grayMatrix)
            } else {
                bgPaint.colorFilter = null
            }
            val srcW = bgBitmap.width.toFloat()
            val srcH = bgBitmap.height.toFloat()
            val srcRatio = srcW / srcH
            val dstRatio = canvasWidth / canvasHeight

            val srcRect = if (srcRatio > dstRatio) {
                val newW = (srcH * dstRatio).toInt()
                val xOffset = ((srcW - newW) / 2).toInt()
                Rect(xOffset, 0, xOffset + newW, srcH.toInt())
            } else {
                val newH = (srcW / dstRatio).toInt()
                val yOffset = ((srcH - newH) / 2).toInt()
                Rect(0, yOffset, srcW.toInt(), yOffset + newH)
            }
            val dstRect = Rect(0, 0, canvasWidth.toInt(), canvasHeight.toInt())
            nativeCanvas.drawBitmap(bgBitmap, srcRect, dstRect, bgPaint)

            // Dim Overlay for Text Legibility
            if (state.backgroundDim > 0f) {
                val alpha = (state.backgroundDim * 255).toInt().coerceIn(0, 240)
                nativeCanvas.drawColor(Color.argb(alpha, 0, 0, 0))
            }
        } else if (state.solidBgColorLong != null) {
            nativeCanvas.drawColor(state.solidBgColorLong.toInt())
        } else {
            nativeCanvas.drawColor(Color.BLACK)
        }
    }
}
