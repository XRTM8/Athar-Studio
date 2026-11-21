package app.xrtm8.athar.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.xrtm8.athar.ui.theme.BurgundyAccent
import app.xrtm8.athar.ui.theme.WhiteSurfaceElevated
import kotlin.math.min

@Composable
fun ImageCropCanvas(
    bitmap: Bitmap?,
    isAmoledWhite: Boolean,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    onCropChange: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isAmoledWhite) WhiteSurfaceElevated else Color(0xFF0A0B0E))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / size.width
                    val dy = dragAmount.y / size.height

                    val newLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.15f)
                    val newRight = (cropRight + dx).coerceIn(cropLeft + 0.15f, 1f)
                    val newTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.15f)
                    val newBottom = (cropBottom + dy).coerceIn(cropTop + 0.15f, 1f)

                    onCropChange(newLeft, newTop, newRight, newBottom)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val imgW = bitmap.width.toFloat()
                val imgH = bitmap.height.toFloat()
                val canvasW = size.width
                val canvasH = size.height

                val scale = min(canvasW / imgW, canvasH / imgH)
                val drawW = imgW * scale
                val drawH = imgH * scale
                val ox = (canvasW - drawW) / 2f
                val oy = (canvasH - drawH) / 2f

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(ox.toInt(), oy.toInt()),
                    dstSize = IntSize(drawW.toInt(), drawH.toInt())
                )

                val cl = ox + cropLeft * drawW
                val ct = oy + cropTop * drawH
                val cr = ox + cropRight * drawW
                val cb = oy + cropBottom * drawH

                drawRect(Color.Black.copy(alpha = 0.55f))
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(cl, ct),
                    size = Size(cr - cl, cb - ct),
                    blendMode = BlendMode.Clear
                )
                drawRect(
                    color = BurgundyAccent,
                    topLeft = Offset(cl, ct),
                    size = Size(cr - cl, cb - ct),
                    style = Stroke(width = 2.dp.toPx())
                )

                val thirdW = (cr - cl) / 3f
                val thirdH = (cb - ct) / 3f
                drawLine(Color.White.copy(alpha = 0.4f), Offset(cl + thirdW, ct), Offset(cl + thirdW, cb), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.4f), Offset(cl + 2 * thirdW, ct), Offset(cl + 2 * thirdW, cb), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.4f), Offset(cl, ct + thirdH), Offset(cr, ct + thirdH), strokeWidth = 1f)
                drawLine(Color.White.copy(alpha = 0.4f), Offset(cl, ct + 2 * thirdH), Offset(cr, ct + 2 * thirdH), strokeWidth = 1f)
            }
        } else {
            CircularProgressIndicator(color = BurgundyAccent)
        }
    }
}
