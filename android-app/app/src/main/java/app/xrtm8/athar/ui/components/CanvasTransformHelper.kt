package app.xrtm8.athar.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

data class CanvasTransformResult(
    val normX: Float,
    val normY: Float,
    val scale: Float,
    val rotation: Float
)

object CanvasTransformHelper {
    fun applyTransform(
        pan: Offset,
        zoom: Float,
        rotate: Float,
        canvasW: Float,
        canvasH: Float,
        normX: Float,
        normY: Float,
        scale: Float,
        rotation: Float,
        onSnapHaptic: () -> Unit
    ): CanvasTransformResult {
        val cw = canvasW.coerceAtLeast(10f)
        val ch = canvasH.coerceAtLeast(10f)
        var nx = normX + pan.x / cw
        var ny = normY + pan.y / ch
        val s = (scale * zoom).coerceIn(0.25f, 4.0f)
        var rot = (rotation + rotate) % 360f
        if (rot > 180f) rot -= 360f
        if (rot < -180f) rot += 360f

        var snapped = false
        if (abs(nx) < 0.015f) { if (nx != 0f) snapped = true; nx = 0f }
        if (abs(ny) < 0.015f) { if (ny != 0f) snapped = true; ny = 0f }

        for (cardinal in listOf(-180f, -90f, 0f, 90f, 180f)) {
            if (abs(rot - cardinal) < 2.5f) {
                if (rot != cardinal) snapped = true
                rot = cardinal
                break
            }
        }
        if (rot == -0f) rot = 0f
        if (snapped) onSnapHaptic()
        return CanvasTransformResult(nx, ny, s, rot)
    }
}
