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
 * High-performance pointer gesture detector for interactive studio canvas.
 * Handles double-tap and transform gestures without recomposition lag or event conflicts.
 */
suspend fun PointerInputScope.detectCanvasGestures(
    isLocked: Boolean,
    onTransform: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onGestureEnd: () -> Unit,
    onDoubleTap: () -> Unit
) {
    var lastTapTime = 0L

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val now = System.currentTimeMillis()
        if (now - lastTapTime < 320L) {
            onDoubleTap()
            lastTapTime = 0L
        } else {
            lastTapTime = now
        }

        if (isLocked) return@awaitEachGesture

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
        }
    }
}
