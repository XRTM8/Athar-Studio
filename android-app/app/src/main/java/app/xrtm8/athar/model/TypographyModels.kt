package app.xrtm8.athar.model

enum class ArabicFontFamily(
    val fontName: String,
    val category: String,
    val assetPath: String,
    val specimen: String,
    val cssFamilyName: String = fontName,
    val isVariable: Boolean = false,
    val supportedWeights: List<Int> = listOf(400),
    val staticWeightAssets: Map<Int, String> = emptyMap(),
    val supportedOpenTypeFeatures: Set<String> = emptySet(),
    val supportsTatweel: Boolean = true
) {
    // كلاسيكي ورقعة
    AREF_RUQAA(
        fontName = "رقعة",
        category = "رقعة",
        assetPath = "fonts/aref_ruqaa.ttf",
        specimen = "أَثَــرٌ يَبْقَى",
        cssFamilyName = "Aref Ruqaa",
        supportedWeights = listOf(700),
        supportedOpenTypeFeatures = setOf("ss01", "ss02", "ss03", "ss04", "ss05", "ss06")
    ),
    AMIRI(
        fontName = "نسخ أميري",
        category = "كلاسيكي",
        assetPath = "fonts/amiri.ttf",
        specimen = "أَثَــرٌ جَمِيل",
        cssFamilyName = "Amiri",
        supportedWeights = listOf(700),
        supportedOpenTypeFeatures = setOf("ss01", "ss02", "ss03", "ss04", "ss05", "ss06")
    ),
    SCHEHERAZADE_NEW(
        fontName = "شهرزاد نسخ",
        category = "كلاسيكي",
        assetPath = "fonts/scheherazade_new.ttf",
        specimen = "أَثَـرٌ شَهْرَازَاد",
        cssFamilyName = "Scheherazade New",
        supportedWeights = listOf(700)
    ),
    EL_MESSIRI(
        fontName = "المسيري",
        category = "كلاسيكي",
        assetPath = "fonts/el_messiri.ttf",
        specimen = "أَثَــرٌ مُمَيَّز",
        cssFamilyName = "El Messiri",
        isVariable = true,
        supportedWeights = listOf(400, 500, 600, 700),
        supportedOpenTypeFeatures = setOf("dlig")
    ),
    ELGHARIB_HAFS_TAWEEL(
        fontName = "أميري قرآني",
        category = "قرآني",
        assetPath = "fonts/amiri_quran.ttf",
        specimen = "أَثَـــــرٌ قُـرْآنِـيَّ",
        cssFamilyName = "Amiri Quran",
        supportedWeights = listOf(400),
        supportedOpenTypeFeatures = setOf("ss01", "ss02", "ss05")
    ),

    // نستعليق فارسي
    GULZAR(
        fontName = "نستعليق",
        category = "نستعليق",
        assetPath = "fonts/gulzar.ttf",
        specimen = "أَثَـرٌ نَسْتَعْلِيقِي",
        cssFamilyName = "Gulzar",
        supportedWeights = listOf(400),
        supportedOpenTypeFeatures = setOf("ss01", "ss02", "ss03", "ss04", "swsh"),
        supportsTatweel = false
    ),

    // كوفي وهندسي
    REEM_KUFI(
        fontName = "كوفي",
        category = "كوفي",
        assetPath = "fonts/reem_kufi.ttf",
        specimen = "أَثَــرٌ كُوفِي",
        cssFamilyName = "Reem Kufi",
        isVariable = true,
        supportedWeights = listOf(400, 500, 600, 700)
    ),
    QAHIRI(
        fontName = "قاهري",
        category = "كوفي",
        assetPath = "fonts/qahiri.ttf",
        specimen = "أَثَـرٌ قَاهِرِي",
        cssFamilyName = "Qahiri",
        supportedWeights = listOf(400),
        supportedOpenTypeFeatures = setOf("ss01", "ss02", "ss03")
    ),
    KUFAM(
        fontName = "كوفام",
        category = "كوفي",
        assetPath = "fonts/kufam.ttf",
        specimen = "أَثَـرٌ كُوفَام",
        cssFamilyName = "Kufam",
        isVariable = true,
        supportedWeights = listOf(400, 500, 600, 700, 800, 900),
        supportedOpenTypeFeatures = setOf("ss01", "dlig")
    ),

    // عناوين وملصقات عريضة
    LALEZAR(
        fontName = "لاليزار",
        category = "عناوين",
        assetPath = "fonts/lalezar.ttf",
        specimen = "أَثَـرٌ لَالِيزَار",
        cssFamilyName = "Lalezar",
        supportedWeights = listOf(400),
        supportedOpenTypeFeatures = setOf("dlig")
    ),
    RAKKAS(
        fontName = "رقاص",
        category = "عناوين",
        assetPath = "fonts/rakkas.ttf",
        specimen = "أَثَـرٌ رَقَّاص",
        cssFamilyName = "Rakkas",
        supportedWeights = listOf(400),
        supportedOpenTypeFeatures = setOf("ss01", "ss02", "ss03", "ss04", "ss05", "ss06", "dlig")
    ),

    // حديث وعصري
    CAIRO(
        fontName = "كايرو",
        category = "حديث",
        assetPath = "fonts/cairo.ttf",
        specimen = "أَثَــرٌ مُعَاصِر",
        cssFamilyName = "Cairo",
        isVariable = true,
        supportedWeights = listOf(200, 300, 400, 500, 600, 700, 800, 900),
        supportedOpenTypeFeatures = setOf("dlig")
    ),
    ALEXANDRIA(
        fontName = "الإسكندرية",
        category = "حديث",
        assetPath = "fonts/alexandria.ttf",
        specimen = "أَثَــرٌ هَنْدَسِي",
        cssFamilyName = "Alexandria",
        isVariable = true,
        supportedWeights = listOf(200, 300, 400, 500, 600, 700, 800, 900),
        supportedOpenTypeFeatures = setOf("ss01", "dlig")
    ),
    READEX_PRO(
        fontName = "ريدكس برو",
        category = "حديث",
        assetPath = "fonts/readex_pro.ttf",
        specimen = "أَثَــرٌ مَتِين",
        cssFamilyName = "Readex Pro",
        isVariable = true,
        supportedWeights = listOf(200, 300, 400, 500, 600, 700),
        supportedOpenTypeFeatures = setOf("ss01", "ss02", "ss03", "dlig")
    ),
    ALMARAI(
        fontName = "المراعي",
        category = "حديث",
        assetPath = "fonts/almarai.ttf",
        specimen = "أَثَـرٌ مَرَاعِي",
        cssFamilyName = "Almarai",
        supportedWeights = listOf(700),
        supportedOpenTypeFeatures = setOf("ss01")
    ),
    TAJAWAL(
        fontName = "تجوال",
        category = "حديث",
        assetPath = "fonts/tajawal.ttf",
        specimen = "أَثَــرٌ عَصْرِي",
        cssFamilyName = "Tajawal",
        supportedWeights = listOf(200, 300, 400, 500, 700, 800, 900),
        staticWeightAssets = mapOf(
            200 to "fonts/tajawal_extralight.ttf",
            300 to "fonts/tajawal_light.ttf",
            400 to "fonts/tajawal.ttf",
            500 to "fonts/tajawal_medium.ttf",
            700 to "fonts/tajawal_bold.ttf",
            800 to "fonts/tajawal_extrabold.ttf",
            900 to "fonts/tajawal_black.ttf"
        ),
        supportedOpenTypeFeatures = setOf("dlig")
    ),

    // تعبيري وحر
    MARHEY(
        fontName = "مرحي",
        category = "تعبيري",
        assetPath = "fonts/marhey.ttf",
        specimen = "أَثَــرٌ جَرِيء",
        cssFamilyName = "Marhey",
        isVariable = true,
        supportedWeights = listOf(300, 400, 500, 600, 700),
        supportedOpenTypeFeatures = setOf("dlig")
    ),
    LEMONADA(
        fontName = "ليمونادا",
        category = "تعبيري",
        assetPath = "fonts/lemonada.ttf",
        specimen = "أَثَـرٌ لَيْمُونَادَا",
        cssFamilyName = "Lemonada",
        isVariable = true,
        supportedWeights = listOf(300, 400, 500, 600, 700),
        supportedOpenTypeFeatures = setOf("dlig")
    );

    val defaultWeight: Int
        get() = if (400 in supportedWeights) 400 else supportedWeights.firstOrNull() ?: 400

    fun resolveWeight(requestedWeight: Int): Int {
        if (supportedWeights.isEmpty()) return 400
        if (requestedWeight in supportedWeights) return requestedWeight
        return supportedWeights.minWithOrNull(
            compareBy<Int> { kotlin.math.abs(it - requestedWeight) }
                .thenBy { if (it >= requestedWeight) 0 else 1 }
        ) ?: 400
    }

    fun assetPathForWeight(requestedWeight: Int): String {
        if (staticWeightAssets.isEmpty()) return assetPath
        val resolved = resolveWeight(requestedWeight)
        return staticWeightAssets[resolved] ?: assetPath
    }

    fun hasRealWeight(requestedWeight: Int): Boolean {
        if (requestedWeight < 600) return supportedWeights.size > 1
        val resolved = resolveWeight(requestedWeight)
        return supportedWeights.size > 1 && resolved >= 600
    }

    companion object {
        fun weightLabel(weight: Int): String = when {
            weight <= 250 -> "رفيع"
            weight <= 350 -> "خفيف"
            weight <= 450 -> "عادي"
            weight <= 550 -> "متوسط"
            weight <= 650 -> "شبه عريض"
            weight <= 750 -> "عريض"
            weight <= 850 -> "سميك"
            else -> "أسود"
        }
    }
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
