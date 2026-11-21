package app.xrtm8.athar.model

import android.graphics.PorterDuff
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class VisualEffect(val arabicName: String, val description: String) {
    ROYAL_GOLD("تذهيب", "لون ذهبي كلاسيكي مع تدرج متزن"),
    LIQUID_CHROME("كروم", "انعكاسات معدنية لامعة"),
    FROSTED_GLASS("زجاجي", "خلفية شبه شفافة مع حواف ناعمة"),
    CYBER_NEON("نيون", "إشعاع ضوئي بألوان نابضة"),
    CINEMATIC_SHADOW("ظل بارز", "بروز مع ظل ثلاثي الأبعاد"),
    MINIMAL_LUXURY("أبيض ناصع", "لون ناصع بسيط وعصري")
}

enum class LuxuryGradientPreset(val labelAr: String, val colors: List<Long>, val stops: List<Float>) {
    NONE("تلقائي", emptyList(), emptyList()),
    CHERRY_CRIMSON(
        "كرزي مخملي",
        listOf(0xFF4A0012, 0xFF9C1030, 0xFFE02447, 0xFFFF6B8B, 0xFF9C1030),
        listOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f)
    ),
    ANDALUSIAN_GOLD(
        "ذهبي معدني",
        listOf(0xFF4A3500, 0xFFD4AF37, 0xFFFFF2A3, 0xFFD4AF37, 0xFF785100),
        listOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f)
    ),
    PERSIAN_TURQUOISE(
        "فيروزي",
        listOf(0xFF0B2545, 0xFF134074, 0xFF00A896, 0xFF8ECAE6),
        listOf(0.0f, 0.35f, 0.70f, 1.0f)
    ),
    ROYAL_AMETHYST(
        "أرجواني",
        listOf(0xFF240046, 0xFF5A189A, 0xFF9D4EDD, 0xFFE0AAFF),
        listOf(0.0f, 0.30f, 0.65f, 1.0f)
    ),
    DESERT_ROSE(
        "وردي هادئ",
        listOf(0xFF4A0E17, 0xFF8B263E, 0xFFE0A996, 0xFFFBF2E9),
        listOf(0.0f, 0.30f, 0.70f, 1.0f)
    ),
    TITANIUM_CHROME(
        "تيتانيوم وكروم",
        listOf(0xFF1F2421, 0xFF495057, 0xFFCED4DA, 0xFFFFFFFF, 0xFF343A40),
        listOf(0.0f, 0.20f, 0.50f, 0.75f, 1.0f)
    ),
    CYBER_EMERALD(
        "زمرد نيون",
        listOf(0xFF003822, 0xFF00B0FF, 0xFF00FF9D, 0xFFB3FFD6),
        listOf(0.0f, 0.35f, 0.70f, 1.0f)
    ),
    FROSTED_PEARL(
        "لؤلؤي مثلج",
        listOf(0xCC3A3F47, 0xDD9EA5B0, 0xEEFFFFFF, 0xFFF8F9FA),
        listOf(0.0f, 0.30f, 0.70f, 1.0f)
    )
}

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

enum class ArabicFontFamily(
    val fontName: String,
    val category: String,
    val assetPath: String,
    val specimen: String
) {
    // كلاسيكي ورقعة
    AREF_RUQAA("رقعة", "رقعة", "fonts/aref_ruqaa.ttf", "أَثَــرٌ يَبْقَى"),
    AMIRI("نسخ أميري", "كلاسيكي", "fonts/amiri.ttf", "أَثَــرٌ جَمِيل"),
    SCHEHERAZADE_NEW("شهرزاد نسخ", "كلاسيكي", "fonts/scheherazade_new.ttf", "أَثَـرٌ شَهْرَازَاد"),
    EL_MESSIRI("المسيري", "كلاسيكي", "fonts/el_messiri.ttf", "أَثَــرٌ مُمَيَّز"),
    ELGHARIB_HAFS_TAWEEL("حفص ممدود", "قرآني", "fonts/elgharib_kfgqpc_hafs_taweel.ttf", "أَثَـــــرٌ قُـرْآنِـيَّ"),

    // نستعليق فارسي
    GULZAR("نستعليق", "نستعليق", "fonts/gulzar.ttf", "أَثَـرٌ نَسْتَعْلِيقِي"),

    // كوفي وهندسي
    REEM_KUFI("كوفي", "كوفي", "fonts/reem_kufi.ttf", "أَثَــرٌ كُوفِي"),
    QAHIRI("قاهري", "كوفي", "fonts/qahiri.ttf", "أَثَـرٌ قَاهِرِي"),
    KUFAM("كوفام", "كوفي", "fonts/kufam.ttf", "أَثَـرٌ كُوفَام"),

    // عناوين وملصقات عريضة
    LALEZAR("لاليزار", "عناوين", "fonts/lalezar.ttf", "أَثَـرٌ لَالِيزَار"),
    RAKKAS("رقاص", "عناوين", "fonts/rakkas.ttf", "أَثَـرٌ رَقَّاص"),

    // حديث وعصري
    CAIRO("كايرو", "حديث", "fonts/cairo.ttf", "أَثَــرٌ مُعَاصِر"),
    ALMARAI("المراعي", "حديث", "fonts/almarai.ttf", "أَثَـرٌ مَرَاعِي"),
    TAJAWAL("تجوال", "حديث", "fonts/tajawal.ttf", "أَثَــرٌ خَفِيف"),

    // تعبيري وحر
    MARHEY("مرحي", "تعبيري", "fonts/marhey.ttf", "أَثَــرٌ جَرِيء"),
    LEMONADA("ليمونادا", "تعبيري", "fonts/lemonada.ttf", "أَثَـرٌ لَيْمُونَادَا")
}

enum class TextAlignOption(val label: String) {
    RIGHT("يمين"),
    CENTER("وسط"),
    LEFT("يسار")
}

enum class KashidaMode(val label: String, val description: String = "") {
    SMART("تلقائي متزن", "توزيع المدود حسب قواعد الخط العربي"),
    PRE_FINAL("عجز الكلمة (قبل الأخير)", "مد الوصلة السابقة للحرف الأخير"),
    INITIAL("صدر الكلمة (البداية)", "مد ما بعد الحرف الأول"),
    MEDIAL("وسط الكلمة (القلب)", "مد الحروف المتصلة في منتصف الكلمة"),
    TEETH("الأسنان والأطباق (س، ص)", "مد مسار الحروف المسننة والأطباق الأفقية"),
    ALL("شامل (الكل)", "مد متساوٍ لكافة مواضع الوصل القابلة للمد"),
    CUSTOM("موضع مخصص", "اختيار يدوي دقيق للحرف المطلوب مده")
}

enum class ExportResolution(val label: String, val width: Int, val description: String) {
    HD_1080P("عالية HD (1080p)", 1080, "مناسبة للنشر على وسائل التواصل"),
    ULTRA_4K("فائقة 4K (2160p)", 2160, "دقة عالية للطباعة والحفظ بجودة كاملة")
}

data class ColorSwatch(val name: String, val argb: Long) {
    val composeColor: Color get() = Color(argb.toInt())
}

val LUXURY_SWATCHES = listOf(
    ColorSwatch("تلقائي", 0x00000000), // 0 means default effect shader
    ColorSwatch("كرزي", 0xFFE02447),
    ColorSwatch("كرزي داكن", 0xFF9C1030),
    ColorSwatch("أبيض ناصع", 0xFFFFFFFF),
    ColorSwatch("ذهبي", 0xFFFFD700),
    ColorSwatch("رمال", 0xFFE5C07B),
    ColorSwatch("زمرد", 0xFF00E676),
    ColorSwatch("ياقوت", 0xFFFF3366),
    ColorSwatch("كروم", 0xFFE0E0E0),
    ColorSwatch("أزرق", 0xFF448AFF),
    ColorSwatch("خزامي", 0xFFB388FF),
    ColorSwatch("برتقالي", 0xFFFF9100),
    ColorSwatch("فحم داكن", 0xFF212121),
    ColorSwatch("وردي", 0xFFE0A996),
    ColorSwatch("عنابي", 0xFF800020)
)

val LUXURY_BG_PRESETS = listOf(
    ColorSwatch("أسود AMOLED", 0xFF000000),
    ColorSwatch("أبيض ناصع", 0xFFFFFFFF),
    ColorSwatch("كرزي ليلي", 0xFF240810),
    ColorSwatch("ورق رمادي", 0xFF1C1917),
    ColorSwatch("كحلي ليلي", 0xFF0A1128),
    ColorSwatch("زمردي داكن", 0xFF081C15),
    ColorSwatch("عنابي", 0xFF1A080E),
    ColorSwatch("فحم رمادي", 0xFF18191C)
)

data class GlassBackdropPreset(
    val name: String,
    val bgArgb: Long,
    val strokeArgb: Long,
    val opacity: Float
)

val GLASS_BACKDROP_PRESETS = listOf(
    GlassBackdropPreset("زجاج ليلي داكن", 0xFF0D0F14, 0x55E5C07B, 0.65f),
    GlassBackdropPreset("زجاج مثلج نقي", 0xFFFFFFFF, 0x44FFFFFF, 0.25f),
    GlassBackdropPreset("زجاج كهرماني مذهب", 0xFF2B2110, 0x88FFD700, 0.70f),
    GlassBackdropPreset("زمردي فخم", 0xFF091F18, 0x6600FF9D, 0.65f),
    GlassBackdropPreset("عقيق قرمزي", 0xFF240A12, 0x66FF3366, 0.65f)
)

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

enum class FilmPreset(
    val id: String,
    val titleAr: String,
    val brand: String,
    val description: String
) {
    NONE("none", "بدون فلتر", "أصلي", "الصورة الطبيعية بدون تعديل لوني"),
    FUJI_CLASSIC_CHROME("fuji_classic_chrome", "فوجي كروم كلاسيك", "Fujifilm", "ألوان هادئة وسينمائية مع تباين ناعم للظلال"),
    FUJI_VELVIA("fuji_velvia", "فوجي فيلفيا 50", "Fujifilm", "تشبع فائق وألوان حية حارة للطبيعة والسماء"),
    FUJI_PROVIA("fuji_provia", "فوجي بروفيا 100F", "Fujifilm", "تدرج متزن وألوان حقيقية فائقة الواقعية"),
    KODAK_PORTRA_400("kodak_portra_400", "كوداك بورترا 400", "Kodak", "دفء ناعم وألوان بشرة ذهبية ساحرة"),
    KODAK_TRI_X("kodak_tri_x", "كوداك تري-إكس 400", "Kodak", "أبيض وأسود عالي التباين بطابع وثائقي عتيق"),
    LEICA_MONOCHROME("leica_mono", "لايكا مونوكروم", "Leica", "أبيض وأسود نقي بتدرجات رمادية ناعمة وعميقة"),
    CINEMATIC_TEAL_ORANGE("teal_orange", "سينمائي تيل & أورانج", "Cinema", "التدرج الهوليوودي العصري بين الأزرق والبرتقالي"),
    VINTAGE_WARMTH("vintage_warmth", "دفء السبعينات", "Vintage", "ألوان باهتة دافئة مع لمسة ذهبية نوستالجية")
}

data class TextLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "نص",
    val rawText: String = "",
    val displayText: String = "",
    val stretchLevel: Int = 3,
    val kashidaMode: KashidaMode = KashidaMode.SMART,
    val kashidaTargetIndex: Int = -1,
    val selectedEffect: VisualEffect = VisualEffect.ROYAL_GOLD,
    val selectedFont: ArabicFontFamily = ArabicFontFamily.AREF_RUQAA,
    val fontSize: Float = 52f,
    val textColor: Color = Color.White,
    val customTextColorLong: Long? = null,
    val strokeWidth: Float = 0f,
    val strokeColorLong: Long = 0xFF000000,
    val shadowRadius: Float = 16f,
    val shadowDx: Float = 0f,
    val shadowDy: Float = 6f,
    val shadowColorLong: Long = 0xAA000000,
    val textAlign: TextAlignOption = TextAlignOption.CENTER,
    val lineSpacingMultiplier: Float = 1.35f,
    val letterSpacing: Float = 0f,
    val textOpacity: Float = 1.0f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    val rotationAngle: Float = 0f,
    val scaleFactor: Float = 1.0f,
    val normOffsetX: Float = 0f,
    val normOffsetY: Float = 0f,
    val enableGlassBackdrop: Boolean = false,
    val glassBackdropOpacity: Float = 0.65f,
    val glassBackdropColorLong: Long = 0xFF0D0F14,
    val glassBackdropBorderColorLong: Long = 0x55E5C07B,
    val glassBackdropCornerRadius: Float = 22f,
    val glassBackdropPadding: Float = 22f,
    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    val isCurved: Boolean = false,
    val curveAngle: Float = 0f,
    val gradientPreset: LuxuryGradientPreset = LuxuryGradientPreset.NONE,
    val gradientAngle: Float = 90f,
    val isRadialGradient: Boolean = false,
    val isHollowText: Boolean = false,
    val boxWidthRatio: Float = 0.85f
)

data class CanvasState(
    val layers: List<TextLayer> = listOf(TextLayer(id = "layer_primary")),
    val activeLayerId: String = "layer_primary",
    val showGridGuides: Boolean = false,
    val aspectRatio: CanvasAspectRatio = CanvasAspectRatio.STORY_9_16,
    val solidBgColorLong: Long? = null,
    val backgroundImageUri: Uri? = null,
    val backgroundDim: Float = 0.25f,
    val backgroundBlur: Float = 0f,
    val backgroundBrightness: Float = 0f,
    val backgroundContrast: Float = 1.0f,
    val backgroundSaturation: Float = 1.0f,
    val backgroundVignette: Float = 0f,
    val backgroundGrayscale: Boolean = false,
    val isTransparentBg: Boolean = false,
    val gradientScrim: CanvasGradientScrim = CanvasGradientScrim(),
    val filmPreset: FilmPreset = FilmPreset.NONE,
    val bgPhotoOffsetX: Float = 0f,
    val bgPhotoOffsetY: Float = 0f,
    val bgPhotoScale: Float = 1.0f,
    val isFramingMode: Boolean = false,
    // 3-Way Color Wheels Grading (Lift / Gamma / Gain)
    val colorGradingEnabled: Boolean = false,
    val liftRed: Float = 0f,
    val liftGreen: Float = 0f,
    val liftBlue: Float = 0f,
    val gammaRed: Float = 0f,
    val gammaGreen: Float = 0f,
    val gammaBlue: Float = 0f,
    val gainRed: Float = 0f,
    val gainGreen: Float = 0f,
    val gainBlue: Float = 0f
) {
    val activeLayer: TextLayer
        get() = layers.find { it.id == activeLayerId } ?: layers.firstOrNull() ?: TextLayer()

    // 100% Backward compatibility delegates for existing components
    val rawText: String get() = activeLayer.rawText
    val displayText: String get() = activeLayer.displayText
    val stretchLevel: Int get() = activeLayer.stretchLevel
    val kashidaMode: KashidaMode get() = activeLayer.kashidaMode
    val kashidaTargetIndex: Int get() = activeLayer.kashidaTargetIndex
    val selectedEffect: VisualEffect get() = activeLayer.selectedEffect
    val selectedFont: ArabicFontFamily get() = activeLayer.selectedFont
    val fontSize: Float get() = activeLayer.fontSize
    val textColor: Color get() = activeLayer.textColor
    val customTextColorLong: Long? get() = activeLayer.customTextColorLong
    val strokeWidth: Float get() = activeLayer.strokeWidth
    val strokeColorLong: Long get() = activeLayer.strokeColorLong
    val shadowRadius: Float get() = activeLayer.shadowRadius
    val shadowDx: Float get() = activeLayer.shadowDx
    val shadowDy: Float get() = activeLayer.shadowDy
    val shadowColorLong: Long get() = activeLayer.shadowColorLong
    val textAlign: TextAlignOption get() = activeLayer.textAlign
    val lineSpacingMultiplier: Float get() = activeLayer.lineSpacingMultiplier
    val letterSpacing: Float get() = activeLayer.letterSpacing
    val textOpacity: Float get() = activeLayer.textOpacity
    val isBold: Boolean get() = activeLayer.isBold
    val boxWidthRatio: Float get() = activeLayer.boxWidthRatio
    val isItalic: Boolean get() = activeLayer.isItalic
    val isUnderline: Boolean get() = activeLayer.isUnderline
    val isLocked: Boolean get() = activeLayer.isLocked
    val rotationAngle: Float get() = activeLayer.rotationAngle
    val scaleFactor: Float get() = activeLayer.scaleFactor
    val normOffsetX: Float get() = activeLayer.normOffsetX
    val normOffsetY: Float get() = activeLayer.normOffsetY
    val enableGlassBackdrop: Boolean get() = activeLayer.enableGlassBackdrop
    val glassBackdropOpacity: Float get() = activeLayer.glassBackdropOpacity
    val glassBackdropColorLong: Long get() = activeLayer.glassBackdropColorLong
    val glassBackdropBorderColorLong: Long get() = activeLayer.glassBackdropBorderColorLong
    val glassBackdropCornerRadius: Float get() = activeLayer.glassBackdropCornerRadius
    val glassBackdropPadding: Float get() = activeLayer.glassBackdropPadding
    val blendMode: LayerBlendMode get() = activeLayer.blendMode
    val isCurved: Boolean get() = activeLayer.isCurved
    val curveAngle: Float get() = activeLayer.curveAngle
    val gradientPreset: LuxuryGradientPreset get() = activeLayer.gradientPreset
    val gradientAngle: Float get() = activeLayer.gradientAngle
    val isRadialGradient: Boolean get() = activeLayer.isRadialGradient
    val isHollowText: Boolean get() = activeLayer.isHollowText

    // Convenience constructor for legacy / single-layer callers
    constructor(
        rawText: String = "",
        displayText: String = "",
        stretchLevel: Int = 3,
        kashidaMode: KashidaMode = KashidaMode.SMART,
        kashidaTargetIndex: Int = -1,
        selectedEffect: VisualEffect = VisualEffect.ROYAL_GOLD,
        selectedFont: ArabicFontFamily = ArabicFontFamily.AREF_RUQAA,
        fontSize: Float = 52f,
        textColor: Color = Color.White,
        customTextColorLong: Long? = null,
        strokeWidth: Float = 0f,
        strokeColorLong: Long = 0xFF000000,
        shadowRadius: Float = 16f,
        shadowDx: Float = 0f,
        shadowDy: Float = 6f,
        shadowColorLong: Long = 0xAA000000,
        textAlign: TextAlignOption = TextAlignOption.CENTER,
        lineSpacingMultiplier: Float = 1.35f,
        letterSpacing: Float = 0f,
        textOpacity: Float = 1.0f,
        isBold: Boolean = false,
        isItalic: Boolean = false,
        isUnderline: Boolean = false,
        isLocked: Boolean = false,
        showGridGuides: Boolean = false,
        aspectRatio: CanvasAspectRatio = CanvasAspectRatio.STORY_9_16,
        solidBgColorLong: Long? = null,
        backgroundImageUri: Uri? = null,
        backgroundDim: Float = 0.25f,
        backgroundBlur: Float = 0f,
        backgroundBrightness: Float = 0f,
        backgroundContrast: Float = 1.0f,
        backgroundSaturation: Float = 1.0f,
        backgroundVignette: Float = 0f,
        backgroundGrayscale: Boolean = false,
        enableGlassBackdrop: Boolean = false,
        glassBackdropOpacity: Float = 0.65f,
        glassBackdropColorLong: Long = 0xFF0D0F14,
        glassBackdropBorderColorLong: Long = 0x55E5C07B,
        glassBackdropCornerRadius: Float = 22f,
        glassBackdropPadding: Float = 22f,
        rotationAngle: Float = 0f,
        scaleFactor: Float = 1.0f,
        normOffsetX: Float = 0f,
        normOffsetY: Float = 0f,
        isTransparentBg: Boolean = false,
        gradientScrim: CanvasGradientScrim = CanvasGradientScrim(),
        filmPreset: FilmPreset = FilmPreset.NONE,
        bgPhotoOffsetX: Float = 0f,
        bgPhotoOffsetY: Float = 0f,
        bgPhotoScale: Float = 1.0f,
        isFramingMode: Boolean = false,
        blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
        isCurved: Boolean = false,
        curveAngle: Float = 0f
    ) : this(
        layers = listOf(
            TextLayer(
                id = "layer_primary",
                name = "نص رئيسي",
                rawText = rawText,
                displayText = displayText,
                stretchLevel = stretchLevel,
                kashidaMode = kashidaMode,
                kashidaTargetIndex = kashidaTargetIndex,
                selectedEffect = selectedEffect,
                selectedFont = selectedFont,
                fontSize = fontSize,
                textColor = textColor,
                customTextColorLong = customTextColorLong,
                strokeWidth = strokeWidth,
                strokeColorLong = strokeColorLong,
                shadowRadius = shadowRadius,
                shadowDx = shadowDx,
                shadowDy = shadowDy,
                shadowColorLong = shadowColorLong,
                textAlign = textAlign,
                lineSpacingMultiplier = lineSpacingMultiplier,
                letterSpacing = letterSpacing,
                textOpacity = textOpacity,
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                isLocked = isLocked,
                rotationAngle = rotationAngle,
                scaleFactor = scaleFactor,
                normOffsetX = normOffsetX,
                normOffsetY = normOffsetY,
                enableGlassBackdrop = enableGlassBackdrop,
                glassBackdropOpacity = glassBackdropOpacity,
                glassBackdropColorLong = glassBackdropColorLong,
                glassBackdropBorderColorLong = glassBackdropBorderColorLong,
                glassBackdropCornerRadius = glassBackdropCornerRadius,
                glassBackdropPadding = glassBackdropPadding,
                blendMode = blendMode,
                isCurved = isCurved,
                curveAngle = curveAngle
            )
        ),
        activeLayerId = "layer_primary",
        showGridGuides = showGridGuides,
        aspectRatio = aspectRatio,
        solidBgColorLong = solidBgColorLong,
        backgroundImageUri = backgroundImageUri,
        backgroundDim = backgroundDim,
        backgroundBlur = backgroundBlur,
        backgroundBrightness = backgroundBrightness,
        backgroundContrast = backgroundContrast,
        backgroundSaturation = backgroundSaturation,
        backgroundVignette = backgroundVignette,
        backgroundGrayscale = backgroundGrayscale,
        isTransparentBg = isTransparentBg,
        gradientScrim = gradientScrim,
        filmPreset = filmPreset,
        bgPhotoOffsetX = bgPhotoOffsetX,
        bgPhotoOffsetY = bgPhotoOffsetY,
        bgPhotoScale = bgPhotoScale,
        isFramingMode = isFramingMode
    )
}

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val isMandatory: Boolean = false
)
