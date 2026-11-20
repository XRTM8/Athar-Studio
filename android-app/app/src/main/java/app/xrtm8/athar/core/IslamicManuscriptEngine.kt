package app.xrtm8.athar.core

import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.VisualEffect
import java.util.UUID

/**
 * Authentic Islamic Calligraphy & Manuscript Phrase Engine.
 * Curates magnificent classical Arabic calligraphy texts with optimized
 * typography font pairing, Kashida stretching, and royal shader styling.
 */
object IslamicManuscriptEngine {

    enum class ManuscriptCategory(val labelAr: String) {
        BISMILLAH("البسملة الشريفة"),
        SALAWAT("صلوات وأدعية"),
        QURANIC("آيات قرآنية"),
        OCCASIONS("تهاني ومناسبات"),
        WISDOM("حكم وأذكار")
    }

    data class ManuscriptEntry(
        val id: String,
        val category: ManuscriptCategory,
        val titleAr: String,
        val fullText: String,
        val recommendedFont: ArabicFontFamily,
        val recommendedEffect: VisualEffect = VisualEffect.ROYAL_GOLD,
        val recommendedStretch: Int = 3,
        val recommendedKashidaMode: KashidaMode = KashidaMode.SMART,
        val recommendedFontSize: Float = 48f
    )

    val MANUSCRIPTS = listOf(
        // البسملة
        ManuscriptEntry(
            id = "bismillah_ruqaa",
            category = ManuscriptCategory.BISMILLAH,
            titleAr = "بسم الله الرحمن الرحيم (رقعة)",
            fullText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            recommendedFont = ArabicFontFamily.AREF_RUQAA,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 3,
            recommendedFontSize = 46f
        ),
        ManuscriptEntry(
            id = "bismillah_amiri",
            category = ManuscriptCategory.BISMILLAH,
            titleAr = "بسم الله الرحمن الرحيم (نسخ)",
            fullText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            recommendedFont = ArabicFontFamily.AMIRI,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 4,
            recommendedFontSize = 48f
        ),
        ManuscriptEntry(
            id = "bismillah_kufi",
            category = ManuscriptCategory.BISMILLAH,
            titleAr = "بسم الله الرحمن الرحيم (كوفي)",
            fullText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            recommendedFont = ArabicFontFamily.REEM_KUFI,
            recommendedEffect = VisualEffect.MINIMAL_LUXURY,
            recommendedStretch = 2,
            recommendedFontSize = 44f
        ),

        // صلوات وأدعية
        ManuscriptEntry(
            id = "salawat_prophet",
            category = ManuscriptCategory.SALAWAT,
            titleAr = "الصلاة على النبي ﷺ",
            fullText = "صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ",
            recommendedFont = ArabicFontFamily.AREF_RUQAA,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 2,
            recommendedFontSize = 48f
        ),
        ManuscriptEntry(
            id = "subhan_allah",
            category = ManuscriptCategory.SALAWAT,
            titleAr = "سبحان الله وبحمده",
            fullText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ\nسُبْحَانَ اللَّهِ الْعَظِيمِ",
            recommendedFont = ArabicFontFamily.SCHEHERAZADE_NEW,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 3,
            recommendedFontSize = 42f
        ),
        ManuscriptEntry(
            id = "alhamdulillah",
            category = ManuscriptCategory.SALAWAT,
            titleAr = "الحمد لله رب العالمين",
            fullText = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
            recommendedFont = ArabicFontFamily.AMIRI,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 4,
            recommendedFontSize = 46f
        ),
        ManuscriptEntry(
            id = "la_ilaha_illallah",
            category = ManuscriptCategory.SALAWAT,
            titleAr = "لا إله إلا الله",
            fullText = "لَا إِلَٰهَ إِلَّا اللَّهُ\nمُحَمَّدٌ رَسُولُ اللَّهِ",
            recommendedFont = ArabicFontFamily.AREF_RUQAA,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 3,
            recommendedFontSize = 44f
        ),
        ManuscriptEntry(
            id = "hasbunallah",
            category = ManuscriptCategory.SALAWAT,
            titleAr = "حسبنا الله ونعم الوكيل",
            fullText = "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
            recommendedFont = ArabicFontFamily.EL_MESSIRI,
            recommendedEffect = VisualEffect.MINIMAL_LUXURY,
            recommendedStretch = 3,
            recommendedFontSize = 46f
        ),

        // آيات قرآنية
        ManuscriptEntry(
            id = "quran_rabi_zidni",
            category = ManuscriptCategory.QURANIC,
            titleAr = "وقل رب زدني علماً",
            fullText = "وَقُل رَّبِّ زِدْنِي عِلْمًا",
            recommendedFont = ArabicFontFamily.AMIRI,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 4,
            recommendedFontSize = 48f
        ),
        ManuscriptEntry(
            id = "quran_maa_alusr_yusra",
            category = ManuscriptCategory.QURANIC,
            titleAr = "فإن مع العسر يسراً",
            fullText = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا\nإِنَّ مَعَ الْعُسْرِ يُسْرًا",
            recommendedFont = ArabicFontFamily.AREF_RUQAA,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 3,
            recommendedFontSize = 44f
        ),
        ManuscriptEntry(
            id = "quran_nasrun_minallah",
            category = ManuscriptCategory.QURANIC,
            titleAr = "نصر من الله وفتح قريب",
            fullText = "نَصْرٌ مِّنَ اللَّهِ وَفَتْحٌ قَرِيبٌ",
            recommendedFont = ArabicFontFamily.ELGHARIB_HAFS_TAWEEL,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 5,
            recommendedFontSize = 42f
        ),
        ManuscriptEntry(
            id = "quran_in_maiya_rabbi",
            category = ManuscriptCategory.QURANIC,
            titleAr = "إن معي ربي سيهدين",
            fullText = "إِنَّ مَعِيَ رَبِّي سَيَهْدِينِ",
            recommendedFont = ArabicFontFamily.AREF_RUQAA,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 3,
            recommendedFontSize = 46f
        ),

        // تهاني ومناسبات
        ManuscriptEntry(
            id = "ramadan_kareem",
            category = ManuscriptCategory.OCCASIONS,
            titleAr = "رمضان كريم",
            fullText = "رَمَضَانُ كَرِيم\nمُبَارَكٌ عَلَيْكُمُ الشَّهْر",
            recommendedFont = ArabicFontFamily.AREF_RUQAA,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 4,
            recommendedFontSize = 48f
        ),
        ManuscriptEntry(
            id = "eid_mubarak",
            category = ManuscriptCategory.OCCASIONS,
            titleAr = "عيدكم مبارك",
            fullText = "عِيدُكُمْ مُبَارَك\nكُلُّ عَامٍ وَأَنْتُمْ بِخَيْر",
            recommendedFont = ArabicFontFamily.LALEZAR,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 2,
            recommendedFontSize = 46f
        ),
        ManuscriptEntry(
            id = "jumaa_mubaraka",
            category = ManuscriptCategory.OCCASIONS,
            titleAr = "جمعة مباركة",
            fullText = "جُمُعَةٌ مُبَارَكَة\nطَيَّبَ اللَّهُ أَوْقَاتَكُمْ بِالْخَيْر",
            recommendedFont = ArabicFontFamily.EL_MESSIRI,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 3,
            recommendedFontSize = 42f
        ),
        ManuscriptEntry(
            id = "sabah_alkhair",
            category = ManuscriptCategory.OCCASIONS,
            titleAr = "صباح الخير والسرور",
            fullText = "صَبَاحُ الْخَيْرِ وَالسُّرُور",
            recommendedFont = ArabicFontFamily.MARHEY,
            recommendedEffect = VisualEffect.MINIMAL_LUXURY,
            recommendedStretch = 2,
            recommendedFontSize = 46f
        ),

        // حكم وأذكار
        ManuscriptEntry(
            id = "mashaallah",
            category = ManuscriptCategory.WISDOM,
            titleAr = "ما شاء الله لا قوة إلا بالله",
            fullText = "مَا شَاءَ اللَّهُ لَا قُوَّةَ إِلَّا بِاللَّهِ",
            recommendedFont = ArabicFontFamily.AMIRI,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 4,
            recommendedFontSize = 44f
        ),
        ManuscriptEntry(
            id = "tafaaloo",
            category = ManuscriptCategory.WISDOM,
            titleAr = "تفاءلوا بالخير تجدوه",
            fullText = "تَفَاءَلُوا بِالْخَيْرِ تَجِدُوهُ",
            recommendedFont = ArabicFontFamily.AREF_RUQAA,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 3,
            recommendedFontSize = 48f
        ),
        ManuscriptEntry(
            id = "athar_jameel",
            category = ManuscriptCategory.WISDOM,
            titleAr = "أثر جميل يبقى",
            fullText = "أَثَــرٌ جَمِيلٌ يَبْقَى",
            recommendedFont = ArabicFontFamily.AREF_RUQAA,
            recommendedEffect = VisualEffect.ROYAL_GOLD,
            recommendedStretch = 4,
            recommendedFontSize = 52f
        )
    )

    fun createLayerFromManuscript(entry: ManuscriptEntry): TextLayer {
        val dispText = PureArabicKashidaEngine.stretchText(
            text = entry.fullText,
            level = entry.recommendedStretch,
            mode = entry.recommendedKashidaMode
        )
        return TextLayer(
            id = UUID.randomUUID().toString(),
            name = entry.titleAr,
            rawText = entry.fullText,
            displayText = dispText,
            stretchLevel = entry.recommendedStretch,
            kashidaMode = entry.recommendedKashidaMode,
            selectedFont = entry.recommendedFont,
            selectedEffect = entry.recommendedEffect,
            fontSize = entry.recommendedFontSize
        )
    }
}
