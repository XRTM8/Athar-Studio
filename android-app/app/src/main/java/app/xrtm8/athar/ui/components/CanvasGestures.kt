package app.xrtm8.athar.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

/**
 * Company-Grade pointer gesture detector for interactive studio canvas.
 * Seamlessly arbitrates between:
 * - Precision layer hit-testing and direct selection on single tap
 * - Fast text editor modal invocation on double tap
 * - Multi-touch 120Hz translation, scaling, and rotation transforms
 */
suspend fun PointerInputScope.detectCanvasGestures(
    isLocked: Boolean,
    onTransform: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onGestureEnd: () -> Unit,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit
) {
    var lastTapTime = 0L
    var lastTapPos = Offset.Zero

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downPos = down.position

        if (isLocked) {
            val now = System.currentTimeMillis()
            if (now - lastTapTime < 350L && (downPos - lastTapPos).getDistance() < 45f) {
                onDoubleTap(downPos)
                lastTapTime = 0L
            } else {
                onTap(downPos)
                lastTapTime = now
                lastTapPos = downPos
            }
            return@awaitEachGesture
        }

        var zoom = 1f
        var rotation = 0f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1f - zoom) * centroidSize
                    val rotationMotion = abs(rotation * Math.PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                        onTransform(panChange, zoomChange, rotationChange)
                    }
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        if (pastTouchSlop) {
            onGestureEnd()
        } else {
            val now = System.currentTimeMillis()
            if (now - lastTapTime < 350L && (downPos - lastTapPos).getDistance() < 45f) {
                onDoubleTap(downPos)
                lastTapTime = 0L
            } else {
                onTap(downPos)
                lastTapTime = now
                lastTapPos = downPos
            }
        }
    }
}
