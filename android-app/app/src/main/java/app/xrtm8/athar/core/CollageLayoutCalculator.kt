package app.xrtm8.athar.core

import android.graphics.Matrix
import app.xrtm8.athar.model.CollageCell
import app.xrtm8.athar.model.CollageLayoutType
import app.xrtm8.athar.model.CollageState
import kotlin.math.max

/**
 * Editorial Collage Layout Geometry Calculator & Pure Cover-Fit Framing Math.
 */
object CollageLayoutCalculator {

    fun generateCellsForLayout(layout: CollageLayoutType, existingCells: List<CollageCell> = emptyList()): List<CollageCell> {
        if (layout == CollageLayoutType.NONE) return emptyList()

        val boundsList: List<FloatArray> = when (layout) {
            CollageLayoutType.DIPTYCH_V -> listOf(
                floatArrayOf(0.0f, 0.0f, 0.5f, 1.0f),
                floatArrayOf(0.5f, 0.0f, 1.0f, 1.0f)
            )
            CollageLayoutType.DIPTYCH_H -> listOf(
                floatArrayOf(0.0f, 0.0f, 1.0f, 0.5f),
                floatArrayOf(0.0f, 0.5f, 1.0f, 1.0f)
            )
            CollageLayoutType.DIPTYCH_ASYM -> listOf(
                floatArrayOf(0.0f, 0.0f, 0.62f, 1.0f),
                floatArrayOf(0.62f, 0.0f, 1.0f, 1.0f)
            )
            CollageLayoutType.TRIPTYCH_HERO -> listOf(
                floatArrayOf(0.0f, 0.0f, 0.65f, 1.0f),
                floatArrayOf(0.65f, 0.0f, 1.0f, 0.5f),
                floatArrayOf(0.65f, 0.5f, 1.0f, 1.0f)
            )
            CollageLayoutType.TRIPTYCH_COLS -> listOf(
                floatArrayOf(0.0f, 0.0f, 1f / 3f, 1.0f),
                floatArrayOf(1f / 3f, 0.0f, 2f / 3f, 1.0f),
                floatArrayOf(2f / 3f, 0.0f, 1.0f, 1.0f)
            )
            CollageLayoutType.QUAD_GRID -> listOf(
                floatArrayOf(0.0f, 0.0f, 0.5f, 0.5f),
                floatArrayOf(0.5f, 0.0f, 1.0f, 0.5f),
                floatArrayOf(0.0f, 0.5f, 0.5f, 1.0f),
                floatArrayOf(0.5f, 0.5f, 1.0f, 1.0f)
            )
            CollageLayoutType.QUAD_STRIP -> listOf(
                floatArrayOf(0.0f, 0.0f, 1.0f, 0.25f),
                floatArrayOf(0.0f, 0.25f, 1.0f, 0.5f),
                floatArrayOf(0.0f, 0.5f, 1.0f, 0.75f),
                floatArrayOf(0.0f, 0.75f, 1.0f, 1.0f)
            )
            CollageLayoutType.MAGAZINE_MOSAIC -> listOf(
                floatArrayOf(0.0f, 0.0f, 0.6f, 0.55f),
                floatArrayOf(0.6f, 0.0f, 1.0f, 0.35f),
                floatArrayOf(0.6f, 0.35f, 1.0f, 0.55f),
                floatArrayOf(0.0f, 0.55f, 0.5f, 1.0f),
                floatArrayOf(0.5f, 0.55f, 1.0f, 1.0f)
            )
            CollageLayoutType.POLAROID_DUO -> listOf(
                floatArrayOf(0.05f, 0.06f, 0.95f, 0.49f),
                floatArrayOf(0.05f, 0.51f, 0.95f, 0.94f)
            )
            CollageLayoutType.BENTO_SIX -> listOf(
                floatArrayOf(0.0f, 0.0f, 0.5f, 1f / 3f),
                floatArrayOf(0.5f, 0.0f, 1.0f, 1f / 3f),
                floatArrayOf(0.0f, 1f / 3f, 0.5f, 2f / 3f),
                floatArrayOf(0.5f, 1f / 3f, 1.0f, 2f / 3f),
                floatArrayOf(0.0f, 2f / 3f, 0.5f, 1.0f),
                floatArrayOf(0.5f, 2f / 3f, 1.0f, 1.0f)
            )
            CollageLayoutType.NONE -> emptyList()
        }

        return boundsList.mapIndexed { idx, bounds ->
            val prev = existingCells.getOrNull(idx)
            CollageCell(
                id = "cell_$idx",
                imageUri = prev?.imageUri,
                leftNorm = bounds[0],
                topNorm = bounds[1],
                rightNorm = bounds[2],
                bottomNorm = bounds[3],
                photoOffsetX = prev?.photoOffsetX ?: 0f,
                photoOffsetY = prev?.photoOffsetY ?: 0f,
                photoScale = (prev?.photoScale ?: 1.0f).coerceIn(
                    CollageState.MIN_CELL_SCALE,
                    CollageState.MAX_CELL_SCALE
                ),
                photoRotationDeg = prev?.photoRotationDeg ?: 0,
                isFlippedH = prev?.isFlippedH ?: false,
                isFlippedV = prev?.isFlippedV ?: false
            )
        }
    }
}

/**
 * Pure Kotlin cover-fit geometry helper shared across preview rendering, 1:1 touch gestures,
 * export rendering, and JVM unit tests. Guarantees gapless cover-fit across all aspect ratios.
 */
object CollageCoverFitMath {

    data class CoverTransform(
        val totalScale: Float,
        val centerX: Float,
        val centerY: Float,
        val maxSlackPxX: Float,
        val maxSlackPxY: Float,
        val dispW: Float,
        val dispH: Float,
        val normalizedRotDeg: Int
    )

    data class FramingStepResult(
        val offsetX: Float,
        val offsetY: Float,
        val scale: Float
    )

    fun computeCellBounds(
        cell: CollageCell,
        canvasW: Float,
        canvasH: Float,
        spacingPx: Float
    ): FloatArray {
        val halfGap = spacingPx * 0.5f
        return floatArrayOf(
            (cell.leftNorm * canvasW) + halfGap,
            (cell.topNorm * canvasH) + halfGap,
            (cell.rightNorm * canvasW) - halfGap,
            (cell.bottomNorm * canvasH) - halfGap
        )
    }

    fun computeCoverTransform(
        srcW: Float,
        srcH: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        offsetX: Float,
        offsetY: Float,
        scale: Float,
        rotationDeg: Int = 0
    ): CoverTransform {
        val cellW = (dstRight - dstLeft).coerceAtLeast(1f)
        val cellH = (dstBottom - dstTop).coerceAtLeast(1f)
        val safeSrcW = srcW.coerceAtLeast(1f)
        val safeSrcH = srcH.coerceAtLeast(1f)

        val normRot = ((rotationDeg % 360) + 360) % 360
        val swapAxes = (normRot == 90 || normRot == 270)
        val effW = if (swapAxes) safeSrcH else safeSrcW
        val effH = if (swapAxes) safeSrcW else safeSrcH

        val baseCoverScale = max(cellW / effW, cellH / effH)
        val clampedScale = scale.coerceIn(CollageState.MIN_CELL_SCALE, CollageState.MAX_CELL_SCALE)
        val totalScale = baseCoverScale * clampedScale

        val dispW = effW * totalScale
        val dispH = effH * totalScale
        val maxSlackPxX = ((dispW - cellW) * 0.5f).coerceAtLeast(0f)
        val maxSlackPxY = ((dispH - cellH) * 0.5f).coerceAtLeast(0f)

        val cx = (dstLeft + dstRight) * 0.5f - (offsetX.coerceIn(-1f, 1f) * maxSlackPxX)
        val cy = (dstTop + dstBottom) * 0.5f - (offsetY.coerceIn(-1f, 1f) * maxSlackPxY)

        return CoverTransform(
            totalScale = totalScale,
            centerX = cx,
            centerY = cy,
            maxSlackPxX = maxSlackPxX,
            maxSlackPxY = maxSlackPxY,
            dispW = dispW,
            dispH = dispH,
            normalizedRotDeg = normRot
        )
    }

    fun computeNextPanOffsets(
        currentOffsetX: Float,
        currentOffsetY: Float,
        panDeltaX: Float,
        panDeltaY: Float,
        maxSlackPxX: Float,
        maxSlackPxY: Float
    ): Pair<Float, Float> {
        val nextX = if (maxSlackPxX > 0.5f) {
            (currentOffsetX - panDeltaX / maxSlackPxX).coerceIn(-1f, 1f)
        } else {
            0f
        }
        val nextY = if (maxSlackPxY > 0.5f) {
            (currentOffsetY - panDeltaY / maxSlackPxY).coerceIn(-1f, 1f)
        } else {
            0f
        }
        return nextX to nextY
    }

    fun stepFramingTransform(
        srcW: Float,
        srcH: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        currentOffsetX: Float,
        currentOffsetY: Float,
        currentScale: Float,
        panDeltaX: Float,
        panDeltaY: Float,
        zoomDelta: Float,
        rotationDeg: Int = 0
    ): FramingStepResult {
        val nextScale = (currentScale * zoomDelta).coerceIn(CollageState.MIN_CELL_SCALE, CollageState.MAX_CELL_SCALE)
        val tf = computeCoverTransform(
            srcW, srcH, dstLeft, dstTop, dstRight, dstBottom,
            currentOffsetX, currentOffsetY, nextScale, rotationDeg
        )
        val (nx, ny) = computeNextPanOffsets(
            currentOffsetX, currentOffsetY, panDeltaX, panDeltaY, tf.maxSlackPxX, tf.maxSlackPxY
        )
        return FramingStepResult(nx, ny, nextScale)
    }

    fun populateMatrix(
        outMatrix: Matrix,
        srcW: Float,
        srcH: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        offsetX: Float,
        offsetY: Float,
        scale: Float,
        rotationDeg: Int = 0,
        isFlippedH: Boolean = false,
        isFlippedV: Boolean = false
    ) {
        val t = computeCoverTransform(
            srcW, srcH, dstLeft, dstTop, dstRight, dstBottom,
            offsetX, offsetY, scale, rotationDeg
        )
        val sx = if (isFlippedH) -t.totalScale else t.totalScale
        val sy = if (isFlippedV) -t.totalScale else t.totalScale
        outMatrix.reset()
        outMatrix.postTranslate(-srcW * 0.5f, -srcH * 0.5f)
        if (t.normalizedRotDeg != 0) {
            outMatrix.postRotate(t.normalizedRotDeg.toFloat())
        }
        outMatrix.postScale(sx, sy)
        outMatrix.postTranslate(t.centerX, t.centerY)
    }
}
