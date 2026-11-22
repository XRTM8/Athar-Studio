package app.xrtm8.athar.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.xrtm8.athar.ui.theme.BurgundyAccent
import app.xrtm8.athar.ui.theme.PureWhite
import app.xrtm8.athar.ui.theme.WhiteSurfaceElevated
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

private enum class CropHandle {
    NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT
}

/**
 * Ultra-responsive 120Hz Image Cropper with 4 interactive corner brackets and edge handles (< 230 lines).
 */
@Composable
fun ImageCropCanvas(
    bitmap: Bitmap?,
    isAmoledWhite: Boolean,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    targetRatio: Float?,
    onCropChange: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLeft by rememberUpdatedState(cropLeft)
    val currentTop by rememberUpdatedState(cropTop)
    val currentRight by rememberUpdatedState(cropRight)
    val currentBottom by rememberUpdatedState(cropBottom)
    val currentRatio by rememberUpdatedState(targetRatio)

    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isAmoledWhite) WhiteSurfaceElevated else Color(0xFF08090C))
            .pointerInput(bitmap) {
                if (bitmap == null) return@pointerInput
                val imgW = bitmap.width.toFloat()
                val imgH = bitmap.height.toFloat()

                detectDragGestures(
                    onDragStart = { touchOffset ->
                        val scale = min(size.width.toFloat() / imgW, size.height.toFloat() / imgH)
                        val drawW = imgW * scale
                        val drawH = imgH * scale
                        val ox = (size.width - drawW) / 2f
                        val oy = (size.height - drawH) / 2f

                        val cl = ox + currentLeft * drawW
                        val ct = oy + currentTop * drawH
                        val cr = ox + currentRight * drawW
                        val cb = oy + currentBottom * drawH

                        val px = touchOffset.x
                        val py = touchOffset.y
                        val cornerHitR = 48.dp.toPx()
                        val edgeHitR = 28.dp.toPx()

                        activeHandle = when {
                            hypot(px - cl, py - ct) <= cornerHitR -> CropHandle.TOP_LEFT
                            hypot(px - cr, py - ct) <= cornerHitR -> CropHandle.TOP_RIGHT
                            hypot(px - cl, py - cb) <= cornerHitR -> CropHandle.BOTTOM_LEFT
                            hypot(px - cr, py - cb) <= cornerHitR -> CropHandle.BOTTOM_RIGHT
                            abs(py - ct) <= edgeHitR && px in (cl - edgeHitR)..(cr + edgeHitR) -> CropHandle.TOP
                            abs(py - cb) <= edgeHitR && px in (cl - edgeHitR)..(cr + edgeHitR) -> CropHandle.BOTTOM
                            abs(px - cl) <= edgeHitR && py in (ct - edgeHitR)..(cb + edgeHitR) -> CropHandle.LEFT
                            abs(px - cr) <= edgeHitR && py in (ct - edgeHitR)..(cb + edgeHitR) -> CropHandle.RIGHT
                            px in cl..cr && py in ct..cb -> CropHandle.MOVE
                            else -> CropHandle.NONE
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val scale = min(size.width.toFloat() / imgW, size.height.toFloat() / imgH)
                        val drawW = imgW * scale
                        val drawH = imgH * scale
                        if (drawW <= 0f || drawH <= 0f || activeHandle == CropHandle.NONE) return@detectDragGestures

                        val dx = dragAmount.x / drawW
                        val dy = dragAmount.y / drawH
                        val minSpan = 0.08f

                        var nl = currentLeft
                        var nt = currentTop
                        var nr = currentRight
                        var nb = currentBottom

                        val ratio = currentRatio
                        if (ratio == null) {
                            when (activeHandle) {
                                CropHandle.MOVE -> {
                                    val spanX = nr - nl; val spanY = nb - nt
                                    nl = (nl + dx).coerceIn(0f, 1f - spanX)
                                    nr = nl + spanX
                                    nt = (nt + dy).coerceIn(0f, 1f - spanY)
                                    nb = nt + spanY
                                }
                                CropHandle.TOP_LEFT -> { nl = (nl + dx).coerceIn(0f, nr - minSpan); nt = (nt + dy).coerceIn(0f, nb - minSpan) }
                                CropHandle.TOP_RIGHT -> { nr = (nr + dx).coerceIn(nl + minSpan, 1f); nt = (nt + dy).coerceIn(0f, nb - minSpan) }
                                CropHandle.BOTTOM_LEFT -> { nl = (nl + dx).coerceIn(0f, nr - minSpan); nb = (nb + dy).coerceIn(nt + minSpan, 1f) }
                                CropHandle.BOTTOM_RIGHT -> { nr = (nr + dx).coerceIn(nl + minSpan, 1f); nb = (nb + dy).coerceIn(nt + minSpan, 1f) }
                                CropHandle.TOP -> nt = (nt + dy).coerceIn(0f, nb - minSpan)
                                CropHandle.BOTTOM -> nb = (nb + dy).coerceIn(nt + minSpan, 1f)
                                CropHandle.LEFT -> nl = (nl + dx).coerceIn(0f, nr - minSpan)
                                CropHandle.RIGHT -> nr = (nr + dx).coerceIn(nl + minSpan, 1f)
                                CropHandle.NONE -> {}
                            }
                        } else {
                            val normRatio = ratio * (imgH / imgW)
                            when (activeHandle) {
                                CropHandle.MOVE -> {
                                    val spanX = nr - nl; val spanY = nb - nt
                                    nl = (nl + dx).coerceIn(0f, 1f - spanX)
                                    nr = nl + spanX
                                    nt = (nt + dy).coerceIn(0f, 1f - spanY)
                                    nb = nt + spanY
                                }
                                CropHandle.BOTTOM_RIGHT, CropHandle.RIGHT, CropHandle.BOTTOM -> {
                                    val newW = (nr + dx - nl).coerceIn(minSpan, 1f - nl)
                                    val newH = (newW / normRatio).coerceIn(minSpan, 1f - nt)
                                    nr = nl + (newH * normRatio).coerceAtMost(1f - nl)
                                    nb = nt + newH
                                }
                                CropHandle.TOP_LEFT, CropHandle.LEFT, CropHandle.TOP -> {
                                    val newW = (nr - (nl + dx)).coerceIn(minSpan, nr)
                                    val newH = (newW / normRatio).coerceIn(minSpan, nb)
                                    nl = nr - (newH * normRatio).coerceAtMost(nr)
                                    nt = nb - newH
                                }
                                CropHandle.TOP_RIGHT -> {
                                    val newW = (nr + dx - nl).coerceIn(minSpan, 1f - nl)
                                    val newH = (newW / normRatio).coerceIn(minSpan, nb)
                                    nr = nl + (newH * normRatio).coerceAtMost(1f - nl)
                                    nt = nb - newH
                                }
                                CropHandle.BOTTOM_LEFT -> {
                                    val newW = (nr - (nl + dx)).coerceIn(minSpan, nr)
                                    val newH = (newW / normRatio).coerceIn(minSpan, 1f - nt)
                                    nl = nr - (newH * normRatio).coerceAtMost(nr)
                                    nb = nt + newH
                                }
                                CropHandle.NONE -> {}
                            }
                        }
                        onCropChange(nl, nt, nr, nb)
                    },
                    onDragEnd = { activeHandle = CropHandle.NONE },
                    onDragCancel = { activeHandle = CropHandle.NONE }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val imgW = bitmap.width.toFloat()
                val imgH = bitmap.height.toFloat()
                val scale = min(size.width / imgW, size.height / imgH)
                val drawW = imgW * scale
                val drawH = imgH * scale
                val ox = (size.width - drawW) / 2f
                val oy = (size.height - drawH) / 2f

                // 1. Scaled background image
                drawImage(bitmap.asImageBitmap(), dstOffset = IntOffset(ox.toInt(), oy.toInt()), dstSize = IntSize(drawW.toInt(), drawH.toInt()))

                val cl = ox + currentLeft * drawW
                val ct = oy + currentTop * drawH
                val cr = ox + currentRight * drawW
                val cb = oy + currentBottom * drawH

                // 2. Dark mask overlay outside crop bounds
                drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(0f, 0f), size = Size(size.width, ct))
                drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(0f, cb), size = Size(size.width, size.height - cb))
                drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(0f, ct), size = Size(cl, cb - ct))
                drawRect(Color.Black.copy(alpha = 0.65f), topLeft = Offset(cr, ct), size = Size(size.width - cr, cb - ct))

                // 3. Crop outline & Rule-of-Thirds Grid
                drawRect(BurgundyAccent, topLeft = Offset(cl, ct), size = Size(cr - cl, cb - ct), style = Stroke(width = 1.5.dp.toPx()))
                val thirdW = (cr - cl) / 3f
                val thirdH = (cb - ct) / 3f
                drawLine(Color.White.copy(alpha = 0.35f), Offset(cl + thirdW, ct), Offset(cl + thirdW, cb), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.35f), Offset(cl + 2 * thirdW, ct), Offset(cl + 2 * thirdW, cb), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.35f), Offset(cl, ct + thirdH), Offset(cr, ct + thirdH), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.35f), Offset(cl, ct + 2 * thirdH), Offset(cr, ct + 2 * thirdH), strokeWidth = 1f)

                // 4. Solid Corner Brackets (Visual Affordance)
                val cL = 20.dp.toPx()
                val cW = 3.5.dp.toPx()
                val col = PureWhite
                listOf(
                    (cl - 1f) to (ct - 1f) to (1f to 1f),
                    (cr + 1f) to (ct - 1f) to (-1f to 1f),
                    (cl - 1f) to (cb + 1f) to (1f to -1f),
                    (cr + 1f) to (cb + 1f) to (-1f to -1f)
                ).forEach { (pos, dir) ->
                    val (x, y) = pos
                    val (dx, dy) = dir
                    drawLine(col, Offset(x, y), Offset(x + dx * cL, y), strokeWidth = cW)
                    drawLine(col, Offset(x, y), Offset(x, y + dy * cL), strokeWidth = cW)
                }
            }
        } else {
            CircularProgressIndicator(color = BurgundyAccent)
        }
    }
}
