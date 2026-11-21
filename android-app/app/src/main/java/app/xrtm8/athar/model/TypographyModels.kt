package app.xrtm8.athar.model

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
