package app.xrtm8.athar.model

import android.graphics.PorterDuff

enum class CanvasAspectRatio(val label: String, val ratio: Float?) {
    STORY_9_16("ستوري 9:16", 9f / 16f),
    SQUARE_1_1("مربع 1:1", 1f),
    PORTRAIT_4_5("بورتريه 4:5", 4f / 5f),
    LANDSCAPE_16_9("عرضي 16:9", 16f / 9f),
    WALLPAPER_9_19_5("خلفية 9:19.5", 9f / 19.5f),
    FREE("حر / شفاف", null)
}

enum class LayerBlendMode(val labelAr: String, val porterDuffMode: PorterDuff.Mode) {
    NORMAL("عادي", PorterDuff.Mode.SRC_OVER),
    MULTIPLY("ضرب (تعتيم)", PorterDuff.Mode.MULTIPLY),
    SCREEN("شاشة (تفتيح)", PorterDuff.Mode.SCREEN),
    OVERLAY("تراكب", PorterDuff.Mode.OVERLAY),
    LIGHTEN("إنارة", PorterDuff.Mode.LIGHTEN),
    DARKEN("تغميق", PorterDuff.Mode.DARKEN)
}

enum class CanvasGradientDirection(val label: String) {
    BOTTOM_TO_TOP("من الأسفل للأعلى"),
    TOP_TO_BOTTOM("من الأعلى للأسفل"),
    LEFT_TO_RIGHT("من اليسار لليمين"),
    RIGHT_TO_LEFT("من اليمين لليسار"),
    RADIAL("شعاعي دائري")
}

data class CanvasGradientScrim(
    val isEnabled: Boolean = false,
    val direction: CanvasGradientDirection = CanvasGradientDirection.BOTTOM_TO_TOP,
    val colorLong: Long = 0xFF000000,
    val opacity: Float = 0.65f,
    val midpoint: Float = 0.5f
)
