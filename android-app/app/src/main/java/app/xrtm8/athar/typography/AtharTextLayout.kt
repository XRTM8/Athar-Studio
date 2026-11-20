package app.xrtm8.athar.typography

import android.graphics.Paint
import app.xrtm8.athar.model.TextAlignOption

/**
 * Standardized Typography Layout & Metrics calculation.
 * Ensures 100% pixel-perfect mathematical equivalence across:
 * - Interactive Canvas Viewport (CanvasView)
 * - Vector Export Engine (AtharExportManager)
 * - Frosted Glass Plaque (TextGlassBackdropRenderer)
 * - Studio Bounding Box (CanvasBoundingBoxRenderer)
 * - Project Thumbnail Generator (AtharProjectThumbnailRenderer)
 */
data class TypographyLayoutResult(
    val lines: List<String>,
    val lineHeight: Float,
    val totalHeight: Float,
    val startY: Float,
    val maxLineWidth: Float,
    val halfBlockW: Float,
    val lineX: Float
)

object AtharTextLayout {

    fun computeLayout(
        text: String,
        paint: Paint,
        lineSpacingMultiplier: Float,
        textAlign: TextAlignOption
    ): TypographyLayoutResult {
        val lines = text.split("\n")
        val textSize = paint.textSize
        val lineHeight = textSize * lineSpacingMultiplier
        val totalHeight = (lines.size - 1) * lineHeight
        val startY = -totalHeight / 2f + (textSize * 0.35f)

        var maxLineWidth = 0f
        lines.forEach { line ->
            val w = paint.measureText(line)
            if (w > maxLineWidth) maxLineWidth = w
        }
        val halfBlockW = maxLineWidth / 2f
        val lineX = when (textAlign) {
            TextAlignOption.RIGHT -> halfBlockW
            TextAlignOption.CENTER -> 0f
            TextAlignOption.LEFT -> -halfBlockW
        }

        return TypographyLayoutResult(
            lines = lines,
            lineHeight = lineHeight,
            totalHeight = totalHeight,
            startY = startY,
            maxLineWidth = maxLineWidth,
            halfBlockW = halfBlockW,
            lineX = lineX
        )
    }
}
