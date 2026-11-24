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
    ADD("توهج إشعاعي Add", PorterDuff.Mode.ADD),
    LIGHTEN("تفتيح Lighten", PorterDuff.Mode.LIGHTEN),
    DARKEN("تعتيم Darken", PorterDuff.Mode.DARKEN),
    NORMAL("عادي Normal", PorterDuff.Mode.SRC_OVER)
}

enum class ProceduralMaskType {
    WINDOW_BLINDS,       // ستائر شرائح (Venetian Blinds)
    FRENCH_WINDOW,       // إطار نافذة مقسم (French Window Frame)
    BOTANICAL_PALM,      // ظلال أوراق استوائية (Botanical Palm Leaf)
    GEOMETRIC_ARCH,      // أقواس معمارية (Architectural Arch Colonnade)
    OPTICAL_SUNBURST,    // تسريب شمسي نقي (Optical Sunburst Flare)
    ANAMORPHIC_STREAK,   // شريط توهج سينمائي (CinemaScope Flare Streak)
    PRISM_DISPERSION,    // انكسار موشوري طيفي (Prism Optical Dispersion)
    GOLDEN_FLARE,        // وهج الساعة الذهبية (Golden Hour Flare)
    FILM_BORDER_35MM,    // إطار فيلم 35mm أصيل (35mm Film Frame & Sprockets)
    STUDIO_PASSEPARTOUT, // باسبارتو استوديو فاخر (Studio Passepartout Mat)
    VECTOR_HALFTONE,     // تنقيط مطبعي نقي (Vector Halftone Screen)
    PRECISION_SCANLINES  // خطوط مسح أنالوج (Analog CRT Scanlines)
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
    val thumbFileName: String = assetFileName.replace("masks/", "masks/thumbs/"),
    val defaultBlendMode: MaskBlendMode = MaskBlendMode.SCREEN,
    val defaultOpacity: Float = 0.75f,
    val proceduralType: ProceduralMaskType? = null
)

data class AtharMaskState(
    val isEnabled: Boolean = false,
    val activeMaskId: String? = null,
    val customMaskUri: String? = null,
    val opacity: Float = 0.75f,
    val blendMode: MaskBlendMode = MaskBlendMode.SCREEN,
    val hueShift: Float = 0f, // -180f to +180f
    val offsetX: Float = 0f, // -1f to +1f
    val offsetY: Float = 0f, // -1f to +1f
    val scale: Float = 1.0f, // 0.5f to 3.0f
    val isFlippedH: Boolean = false,
    val isFlippedV: Boolean = false,
    val rotationDegrees: Int = 0, // 0 to 360
    val contrast: Float = 1.0f, // 0.5f to 2.5f
    val isInverted: Boolean = false,
    val tintColorLong: Long? = null,
    val isLuminanceMode: Boolean = false,
    val blackGradient: BlackGradientState = BlackGradientState()
) {
    val proceduralType: ProceduralMaskType? get() = AtharMaskCatalog.getById(activeMaskId)?.proceduralType
    val isMaskActive: Boolean get() = isEnabled && (!activeMaskId.isNullOrEmpty() || !customMaskUri.isNullOrEmpty()) && opacity > 0.01f
    val isEffectActive: Boolean get() = isMaskActive || blackGradient.isEffectActive
}

object AtharMaskCatalog {
    val ALL_MASKS = listOf(
        AtharMaskDefinition(
            id = "solar_dust",
            title = "تسريب شمسي نقي",
            category = MaskCategory.LIGHT_LEAKS,
            assetFileName = "masks/mask_solar_dust.webp",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.85f,
            proceduralType = ProceduralMaskType.OPTICAL_SUNBURST
        ),
        AtharMaskDefinition(
            id = "prism_flares",
            title = "انكسار موشوري",
            category = MaskCategory.PRISM_BOKEH,
            assetFileName = "masks/mask_prism_flares.webp",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.85f,
            proceduralType = ProceduralMaskType.PRISM_DISPERSION
        ),
        AtharMaskDefinition(
            id = "sunset_leak",
            title = "وهج الساعة الذهبية",
            category = MaskCategory.LIGHT_LEAKS,
            assetFileName = "masks/mask_sunset_leak.webp",
            defaultBlendMode = MaskBlendMode.SCREEN,
            defaultOpacity = 0.80f,
            proceduralType = ProceduralMaskType.GOLDEN_FLARE
        ),
        AtharMaskDefinition(
            id = "antique_linen",
            title = "ظلال أوراق استوائية",
            category = MaskCategory.SHADOWS,
            assetFileName = "masks/mask_antique_linen.webp",
            defaultBlendMode = MaskBlendMode.MULTIPLY,
            defaultOpacity = 0.70f,
            proceduralType = ProceduralMaskType.BOTANICAL_PALM
        ),
        AtharMaskDefinition(
            id = "dark_mesh",
            title = "أقواس معمارية",
            category = MaskCategory.SHADOWS,
            assetFileName = "masks/mask_dark_mesh.webp",
            defaultBlendMode = MaskBlendMode.MULTIPLY,
            defaultOpacity = 0.65f,
            proceduralType = ProceduralMaskType.GEOMETRIC_ARCH
        ),
        AtharMaskDefinition(
            id = "chroma_dream",
            title = "إطار نافذة مشمس",
            category = MaskCategory.SHADOWS,
            assetFileName = "masks/mask_chroma_dream.webp",
            defaultBlendMode = MaskBlendMode.MULTIPLY,
            defaultOpacity = 0.75f,
            proceduralType = ProceduralMaskType.FRENCH_WINDOW
        ),
        AtharMaskDefinition(
            id = "cyan_burn",
            title = "شريط توهج سينمائي",
            category = MaskCategory.LIGHT_LEAKS,
            assetFileName = "masks/mask_cyan_burn.webp",
            defaultBlendMode = MaskBlendMode.ADD,
            defaultOpacity = 0.85f,
            proceduralType = ProceduralMaskType.ANAMORPHIC_STREAK
        ),
        AtharMaskDefinition(
            id = "crt_scanlines",
            title = "خطوط مسح تلفاز",
            category = MaskCategory.TEXTURES,
            assetFileName = "masks/mask_crt_scanlines.webp",
            defaultBlendMode = MaskBlendMode.OVERLAY,
            defaultOpacity = 0.50f,
            proceduralType = ProceduralMaskType.PRECISION_SCANLINES
        ),
        AtharMaskDefinition(
            id = "window_shadow",
            title = "ظل ستائر شرائح",
            category = MaskCategory.SHADOWS,
            assetFileName = "masks/mask_window_shadow.webp",
            defaultBlendMode = MaskBlendMode.MULTIPLY,
            defaultOpacity = 0.80f,
            proceduralType = ProceduralMaskType.WINDOW_BLINDS
        ),
        AtharMaskDefinition(
            id = "halftone_wave",
            title = "تنقيط مطبعي نقي",
            category = MaskCategory.TEXTURES,
            assetFileName = "masks/mask_halftone_wave.webp",
            defaultBlendMode = MaskBlendMode.OVERLAY,
            defaultOpacity = 0.60f,
            proceduralType = ProceduralMaskType.VECTOR_HALFTONE
        ),
        AtharMaskDefinition(
            id = "fluted_glass",
            title = "باسبارتو استوديو",
            category = MaskCategory.PAPER_GLASS,
            assetFileName = "masks/mask_fluted_glass.webp",
            defaultBlendMode = MaskBlendMode.NORMAL,
            defaultOpacity = 0.75f,
            proceduralType = ProceduralMaskType.STUDIO_PASSEPARTOUT
        ),
        AtharMaskDefinition(
            id = "folded_poster",
            title = "إطار فيلم 35mm",
            category = MaskCategory.PAPER_GLASS,
            assetFileName = "masks/mask_folded_poster.webp",
            defaultBlendMode = MaskBlendMode.NORMAL,
            defaultOpacity = 0.80f,
            proceduralType = ProceduralMaskType.FILM_BORDER_35MM
        )
    )

    fun getById(id: String?): AtharMaskDefinition? = ALL_MASKS.find { it.id == id }
}
