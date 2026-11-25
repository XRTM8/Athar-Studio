package app.xrtm8.athar.model

import android.net.Uri

enum class CollageLayoutType(val label: String, val cellCount: Int) {
    NONE("بدون كولاج", 0),
    DIPTYCH_V("ثنائي عمودي 50/50", 2),
    DIPTYCH_H("ثنائي أفقي 50/50", 2),
    DIPTYCH_ASYM("ثنائي تحريري 60/40", 2),
    TRIPTYCH_HERO("ثلاثي بطل ومكملات", 3),
    TRIPTYCH_COLS("ثلاثي أعمدة متساوية", 3),
    QUAD_GRID("شبكة مربعة 2x2", 4),
    QUAD_STRIP("شريط سينمائي 35mm", 4)
}

data class CollageCell(
    val id: String,
    val imageUri: Uri? = null,
    val leftNorm: Float,
    val topNorm: Float,
    val rightNorm: Float,
    val bottomNorm: Float,
    val photoOffsetX: Float = 0f,
    val photoOffsetY: Float = 0f,
    val photoScale: Float = 1.0f,
    val photoRotationDeg: Int = 0,
    val isFlippedH: Boolean = false,
    val isFlippedV: Boolean = false
)

data class CollageState(
    val layout: CollageLayoutType = CollageLayoutType.NONE,
    val cells: List<CollageCell> = emptyList(),
    val activeCellId: String? = null,
    val spacingDp: Float = 6f,
    val cornerRadiusDp: Float = 8f,
    val frameColorLong: Long = 0xFF0D0F14
) {
    val isEnabled: Boolean get() = layout != CollageLayoutType.NONE && cells.isNotEmpty()

    companion object {
        const val MIN_CELL_SCALE = 1.0f
        const val MAX_CELL_SCALE = 4.0f
    }
}
