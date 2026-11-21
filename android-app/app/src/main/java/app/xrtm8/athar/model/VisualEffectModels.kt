package app.xrtm8.athar.model

import androidx.compose.ui.graphics.Color

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
