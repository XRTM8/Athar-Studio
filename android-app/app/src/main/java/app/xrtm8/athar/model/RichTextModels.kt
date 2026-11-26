package app.xrtm8.athar.model

/**
 * Per-span rich typography & highlight styling anchored to [TextLayer.rawText] indices.
 * All visual properties are optional overrides on top of the parent [TextLayer] defaults.
 */
data class TextSpanStyle(
    val start: Int,
    val end: Int,
    val fontFamily: ArabicFontFamily? = null,
    val customFontPath: String? = null,
    val customFontName: String? = null,
    val fontWeight: Int? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val scaleMultiplier: Float = 1.0f,
    val baselineShift: Float = 0f,
    val textColorLong: Long? = null,
    val highlightColorLong: Long? = null,
    val highlightAlpha: Float = 0.85f,
    val highlightCornerRadius: Float = 8f,
    val highlightPaddingH: Float = 6f,
    val highlightPaddingV: Float = 2f,
    val highlightBorderColorLong: Long? = null,
    val highlightBorderWidth: Float = 0f
) {
    val isValid: Boolean get() = start >= 0 && end > start
    val hasFontOverride: Boolean
        get() = fontFamily != null || !customFontPath.isNullOrBlank() ||
            fontWeight != null || isBold != null || isItalic != null ||
            kotlin.math.abs(scaleMultiplier - 1.0f) > 0.01f ||
            kotlin.math.abs(baselineShift) > 0.01f
    val hasHighlight: Boolean get() = highlightColorLong != null && highlightAlpha > 0.01f
    val hasAnyStyle: Boolean get() = hasFontOverride || hasHighlight || textColorLong != null
}

data class CustomGradientStop(
    val colorLong: Long,
    val position: Float
)

data class CustomGradientState(
    val isEnabled: Boolean = false,
    val stops: List<CustomGradientStop> = listOf(
        CustomGradientStop(0xFFE5C07BL, 0f),
        CustomGradientStop(0xFF9E1B32L, 1f)
    ),
    val angle: Float = 90f,
    val isRadial: Boolean = false
) {
    val isValid: Boolean get() = isEnabled && stops.size >= 2
}

enum class ShapeType(val labelAr: String) {
    NONE("بدون"),
    DIVIDER_H("خط أفقي"),
    DIVIDER_V("خط عمودي"),
    RECTANGLE("مستطيل"),
    CIRCLE("دائرة"),
    FRAME("إطار")
}

data class ShapeLayerConfig(
    val type: ShapeType = ShapeType.NONE,
    val widthRatio: Float = 0.65f,
    val heightRatio: Float = 0.08f,
    val cornerRadius: Float = 8f,
    val strokeWidth: Float = 2f,
    val fillColorLong: Long = 0xFFE5C07BL,
    val strokeColorLong: Long = 0xFFFFFFFFL,
    val isFilled: Boolean = true,
    val isStroked: Boolean = false
) {
    val isShape: Boolean get() = type != ShapeType.NONE
}
