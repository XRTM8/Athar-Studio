package app.xrtm8.athar.model

enum class PatternType(val label: String) {
    NONE("بدون"),
    SCANLINES("خطوط CRT"),
    MICRO_GRID("شبكة هندسية"),
    HALFTONE_DOTS("تنقيط مطبعي"),
    DIAGONAL_HATCH("تهشير 45°"),
    CROSSHATCH_LINEN("نسيج كتان")
}

enum class PatternBlend(val label: String) {
    OVERLAY("تراكب"),
    SOFT_LIGHT("ضوء خافت"),
    SCREEN("إضاءة Screen"),
    MULTIPLY("مضاعفة Multiply")
}

data class PatternState(
    val type: PatternType = PatternType.NONE,
    val opacity: Float = 0.30f,
    val scale: Float = 1.0f,
    val blendMode: PatternBlend = PatternBlend.OVERLAY,
    val isLightTint: Boolean = true,
    val customColorLong: Long? = null,
    val rotationDegrees: Float = 0f
) {
    val isEnabled: Boolean get() = type != PatternType.NONE && opacity > 0.01f
}
