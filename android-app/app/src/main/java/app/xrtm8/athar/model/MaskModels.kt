package app.xrtm8.athar.model

import android.graphics.PorterDuff

enum class MaskCategory(val label: String) {
    ALL("الكل"),
    LIGHT_LEAKS("إضاءة"),
    PRISM_BOKEH("موشور وبوكيه"),
    SHADOWS("ظلال"),
    TEXTURES("خامات"),
    PAPER_GLASS("ورق وزجاج")
}

enum class MaskBlendMode(val label: String, val porterDuffMode: PorterDuff.Mode) {
    SCREEN("إضاءة Screen", PorterDuff.Mode.SCREEN),
    MULTIPLY("مضاعفة Multiply", PorterDuff.Mode.MULTIPLY),
    OVERLAY("تراكب Overlay", PorterDuff.Mode.OVERLAY),
    LIGHTEN("تفتيح Lighten", PorterDuff.Mode.LIGHTEN),
    DARKEN("تعتيم Darken", PorterDuff.Mode.DARKEN),
    NORMAL("عادي Normal", PorterDuff.Mode.SRC_OVER)
}

enum class BlackGradientPlacement(val label: String) {
    BOTTOM("أسفل (Bottom)"),
    TOP("أعلى (Top)"),
    LEFT("يسار (Left)"),
    RIGHT("يمين (Right)"),
    RADIAL("دائري (Radial)"),
    VERTICAL_SPLIT("شريطين (Cinematic Bars)")
}

data class BlackGradientState(
    val isEnabled: Boolean = false,
    val opacity: Float = 0.60f,
    val placement: BlackGradientPlacement = BlackGradientPlacement.BOTTOM,
    val coverage: Float = 0.55f // 0.20f to 1.0f
) {
    val isEffectActive: Boolean get() = isEnabled && opacity > 0.01f
}

data class AtharMaskDefinition(
    val id: String,
    val title: String,
    val category: MaskCategory,
    val assetFileName: String,
    val defaultBlendMode: MaskBlendMode = MaskBlendMode.SCREEN,
    val defaultOpacity: Float = 0.75f
)

data class AtharMaskState(
    val isEnabled: Boolean = false,
    val activeMaskId: String? = null,
    val opacity: Float = 0.75f,
    val blendMode: MaskBlendMode = MaskBlendMode.SCREEN,
    val hueShift: Float = 0f, // -180f to +180f
    val offsetX: Float = 0f, // -1f to +1f
    val offsetY: Float = 0f, // -1f to +1f
    val scale: Float = 1.0f, // 0.8f to 2.5f
    val isFlippedH: Boolean = false,
    val isFlippedV: Boolean = false,
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val blackGradient: BlackGradientState = BlackGradientState()
) {
    val isMaskActive: Boolean get() = isEnabled && !activeMaskId.isNullOrEmpty() && opacity > 0.01f
    val isEffectActive: Boolean get() = isMaskActive || blackGradient.isEffectActive
}

object AtharMaskCatalog {
    val ALL_MASKS = listOf(
        AtharMaskDefinition(
            id = "solar_dust",
            title = "غبار كهرماني",
            category = MaskCategory.LIGHT_LEAKS,
            assetFileName = "masks/mask_solar_dust.jpg",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.75f
        ),
        AtharMaskDefinition(
            id = "prism_flares",
            title = "انكسار موشوري",
            category = MaskCategory.PRISM_BOKEH,
            assetFileName = "masks/mask_prism_flares.jpg",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.80f
        ),
        AtharMaskDefinition(
            id = "sunset_leak",
            title = "حرق شمس وفيلم",
            category = MaskCategory.LIGHT_LEAKS,
            assetFileName = "masks/mask_sunset_leak.jpg",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.80f
        ),
        AtharMaskDefinition(
            id = "antique_linen",
            title = "قماش كتاب معتق",
            category = MaskCategory.TEXTURES,
            assetFileName = "masks/mask_antique_linen.jpg",
            defaultBlendMode = MaskBlendMode.OVERLAY,
            defaultOpacity = 0.60f
        ),
        AtharMaskDefinition(
            id = "dark_mesh",
            title = "نسيج كربوني",
            category = MaskCategory.TEXTURES,
            assetFileName = "masks/mask_dark_mesh.jpg",
            defaultBlendMode = MaskBlendMode.OVERLAY,
            defaultOpacity = 0.50f
        ),
        AtharMaskDefinition(
            id = "chroma_dream",
            title = "طيف لوني حالم",
            category = MaskCategory.PRISM_BOKEH,
            assetFileName = "masks/mask_chroma_dream.jpg",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.75f
        ),
        AtharMaskDefinition(
            id = "cyan_burn",
            title = "وهج أزرق سينمائي",
            category = MaskCategory.LIGHT_LEAKS,
            assetFileName = "masks/mask_cyan_burn.jpg",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.80f
        ),
        AtharMaskDefinition(
            id = "crt_scanlines",
            title = "ضوضاء وتلفاز",
            category = MaskCategory.TEXTURES,
            assetFileName = "masks/mask_crt_scanlines.jpg",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.50f
        ),
        AtharMaskDefinition(
            id = "window_shadow",
            title = "ظل نافذة وستارة",
            category = MaskCategory.SHADOWS,
            assetFileName = "masks/mask_window_shadow.jpg",
            defaultBlendMode = MaskBlendMode.MULTIPLY,
            defaultOpacity = 0.75f
        ),
        AtharMaskDefinition(
            id = "halftone_wave",
            title = "تنقيط مطبعي ريترو",
            category = MaskCategory.TEXTURES,
            assetFileName = "masks/mask_halftone_wave.jpg",
            defaultBlendMode = MaskBlendMode.OVERLAY,
            defaultOpacity = 0.55f
        ),
        AtharMaskDefinition(
            id = "fluted_glass",
            title = "زجاج مضلع مشبر",
            category = MaskCategory.PAPER_GLASS,
            assetFileName = "masks/mask_fluted_glass.jpg",
            defaultBlendMode = MaskBlendMode.OVERLAY,
            defaultOpacity = 0.65f
        ),
        AtharMaskDefinition(
            id = "folded_poster",
            title = "ثنيات ملصق مطوي",
            category = MaskCategory.PAPER_GLASS,
            assetFileName = "masks/mask_folded_poster.jpg",
            defaultBlendMode = MaskBlendMode.MULTIPLY,
            defaultOpacity = 0.70f
        )
    )

    fun getById(id: String?): AtharMaskDefinition? = ALL_MASKS.find { it.id == id }
}
