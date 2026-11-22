package app.xrtm8.athar.ui.canvas

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.abs

object CanvasGuidesRenderer {

    fun drawGridGuides(
        nativeCanvas: Canvas,
        canvasWidth: Float,
        canvasHeight: Float,
        gridPaint: Paint
    ) {
        nativeCanvas.drawLine(canvasWidth / 3f, 0f, canvasWidth / 3f, canvasHeight, gridPaint)
        nativeCanvas.drawLine(canvasWidth * 2f / 3f, 0f, canvasWidth * 2f / 3f, canvasHeight, gridPaint)
        nativeCanvas.drawLine(0f, canvasHeight / 3f, canvasWidth, canvasHeight / 3f, gridPaint)
        nativeCanvas.drawLine(0f, canvasHeight * 2f / 3f, canvasWidth, canvasHeight * 2f / 3f, gridPaint)
    }

    fun drawSnapGuides(
        nativeCanvas: Canvas,
        canvasWidth: Float,
        canvasHeight: Float,
        localNormX: Float,
        localNormY: Float,
        guidePaint: Paint
    ) {
        if (abs(localNormX) < 0.005f) {
            nativeCanvas.drawLine(canvasWidth / 2f, 0f, canvasWidth / 2f, canvasHeight, guidePaint)
        }
        if (abs(localNormY) < 0.005f) {
            nativeCanvas.drawLine(0f, canvasHeight / 2f, canvasWidth, canvasHeight / 2f, guidePaint)
        }
    }
}
