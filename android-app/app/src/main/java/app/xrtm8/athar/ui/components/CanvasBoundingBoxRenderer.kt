package app.xrtm8.athar.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.typography.TypographyLayoutResult

/**
 * Draws active selection bounding boxes, precision L-shaped corner brackets,
 * and midpoint anchor handles when the user is manipulating text.
 */
object CanvasBoundingBoxRenderer {

    fun drawBoundingBox(
        nativeCanvas: Canvas,
        textAlign: TextAlignOption,
        layout: TypographyLayoutResult,
        textSizePx: Float,
        density: Float,
        selectBoxPaint: Paint,
        handlePaint: Paint,
        handleFillPaint: Paint
    ) {
        val pad = 12f * density
        val bLeft = when (textAlign) {
            TextAlignOption.RIGHT -> -layout.maxLineWidth - pad
            TextAlignOption.CENTER -> -layout.halfBlockW - pad
            TextAlignOption.LEFT -> -pad
        }
        val bRight = when (textAlign) {
            TextAlignOption.RIGHT -> pad
            TextAlignOption.CENTER -> layout.halfBlockW + pad
            TextAlignOption.LEFT -> layout.maxLineWidth + pad
        }
        val bTop = layout.startY - textSizePx * 0.75f - pad
        val bBottom = layout.startY + layout.totalHeight + textSizePx * 0.25f + pad

        // Bounding Box
        nativeCanvas.drawRect(bLeft, bTop, bRight, bBottom, selectBoxPaint)

        // Corner L-Handles
        val cornerLen = 14f * density
        nativeCanvas.drawLine(bLeft, bTop, bLeft + cornerLen, bTop, handlePaint)
        nativeCanvas.drawLine(bLeft, bTop, bLeft, bTop + cornerLen, handlePaint)
        nativeCanvas.drawLine(bRight, bTop, bRight - cornerLen, bTop, handlePaint)
        nativeCanvas.drawLine(bRight, bTop, bRight, bTop + cornerLen, handlePaint)
        nativeCanvas.drawLine(bLeft, bBottom, bLeft + cornerLen, bBottom, handlePaint)
        nativeCanvas.drawLine(bLeft, bBottom, bLeft, bBottom - cornerLen, handlePaint)
        nativeCanvas.drawLine(bRight, bBottom, bRight - cornerLen, bBottom, handlePaint)
        nativeCanvas.drawLine(bRight, bBottom, bRight, bBottom - cornerLen, handlePaint)

        // Midpoint anchor dots
        val dotR = 2.5f * density
        nativeCanvas.drawCircle((bLeft + bRight) / 2f, bTop, dotR, handleFillPaint)
        nativeCanvas.drawCircle((bLeft + bRight) / 2f, bBottom, dotR, handleFillPaint)
        nativeCanvas.drawCircle(bLeft, (bTop + bBottom) / 2f, dotR, handleFillPaint)
        nativeCanvas.drawCircle(bRight, (bTop + bBottom) / 2f, dotR, handleFillPaint)
    }
}
