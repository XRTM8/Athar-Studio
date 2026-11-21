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
 * High-performance pointer gesture detector for interactive typography canvas:
 * - Single tap layer hit-testing and direct selection
 * - Double tap instant text editor modal
 * - Multi-touch 120Hz translation, scaling, and rotation transforms
 * - Long-press hold to compare Before / After with instant restoration on release
 */
suspend fun PointerInputScope.detectCanvasGestures(
    isLocked: Boolean,
    onTransform: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onGestureEnd: () -> Unit,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onHoldBeforeAfter: ((Boolean) -> Unit)? = null
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
        val touchSlop = viewConfiguration.touchSlop

        // Detect if user holds without moving past touchSlop (> 360ms)
        val earlyExit = withTimeoutOrNull(360L) {
            do {
                val event = awaitPointerEvent()
                if (event.changes.any { it.isConsumed }) return@withTimeoutOrNull true

                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                zoom *= zoomChange
                rotation += rotationChange
                pan += panChange

                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1f - zoom) * centroidSize
                val rotationMotion = abs(rotation * Math.PI.toFloat() * centroidSize / 180f)
                val panMotion = pan.getDistance()

                if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                    if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                        onTransform(panChange, zoomChange, rotationChange)
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                    return@withTimeoutOrNull true
                }
            } while (event.changes.any { it.pressed })
            return@withTimeoutOrNull false
        }

        if (earlyExit == null) {
            // Held down for > 360ms without moving -> Before/After comparison!
            onHoldBeforeAfter?.invoke(true)
            do {
                awaitPointerEvent()
            } while (currentEvent.changes.any { it.pressed })
            onHoldBeforeAfter?.invoke(false)
            return@awaitEachGesture
        }

        if (earlyExit) {
            // Continued transform interaction
            do {
                val event = awaitPointerEvent()
                if (!event.changes.any { it.isConsumed }) {
                    val zc = event.calculateZoom()
                    val rc = event.calculateRotation()
                    val pc = event.calculatePan()
                    if (zc != 1f || rc != 0f || pc != Offset.Zero) {
                        onTransform(pc, zc, rc)
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
            onGestureEnd()
        } else {
            // Released within 360ms without moving -> Tap or Double Tap
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
