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
