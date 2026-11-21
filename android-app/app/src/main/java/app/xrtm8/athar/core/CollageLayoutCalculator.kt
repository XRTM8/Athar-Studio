package app.xrtm8.athar.core

import app.xrtm8.athar.model.CollageCell
import app.xrtm8.athar.model.CollageLayoutType
import app.xrtm8.athar.model.CollageState

/**
 * Editorial Collage Layout Geometry Calculator (< 120 lines).
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
                floatArrayOf(0.0f, 0.0f, 0.333f, 1.0f),
                floatArrayOf(0.333f, 0.0f, 0.666f, 1.0f),
                floatArrayOf(0.666f, 0.0f, 1.0f, 1.0f)
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
                photoScale = prev?.photoScale ?: 1.0f
            )
        }
    }
}
