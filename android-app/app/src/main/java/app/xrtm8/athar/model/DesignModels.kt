package app.xrtm8.athar.model

import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class VisualEffect(val arabicName: String, val description: String) {
    ROYAL_GOLD("ذهب ملكي 24K", "تذهيب عربي فاخر مع لمعان دافئ وتدرجات شمسية"),
    LIQUID_CHROME("كروم بلاتيني", "انعكاسات معدنية لامعة وإضاءة ألمنيوم مصقول"),
    FROSTED_GLASS("زجاج مصنفر", "شفافية سينمائية مع تشتت ضوئي وحواف مصقولة"),
    CYBER_NEON("نيون زمردي", "إشعاع ضوئي متعدد الطبقات بألوان نابضة"),
    CINEMATIC_SHADOW("ظل سينمائي", "بروز ثلاثي الأبعاد مع ظل واقعي ممتد في الفضاء"),
    MINIMAL_LUXURY("أبيض ناصع", "بساطة مطلقة بأسلوب التصميم المعاصر")
}

enum class CanvasAspectRatio(val label: String, val ratio: Float?) {
    STORY_9_16("ستوري 9:16", 9f / 16f),
    SQUARE_1_1("مربع 1:1", 1f),
    PORTRAIT_4_5("بورتريه 4:5", 4f / 5f),
    FREE("حر / شفاف", null)
}

enum class ArabicFontFamily(
    val fontName: String,
    val category: String,
    val assetPath: String,
    val specimen: String
) {
    AREF_RUQAA("رقعة أصيل", "رقعة", "fonts/aref_ruqaa.ttf", "أَثَــرٌ يَبْقَى"),
    AMIRI("نسخ قرآني", "كلاسيكي", "fonts/amiri.ttf", "أَثَــرٌ جَمِيل"),
    REEM_KUFI("كوفي هندسي", "كوفي فاخر", "fonts/reem_kufi.ttf", "أَثَــر مِعْمَارِي"),
    CAIRO("كايرو عصري", "ستوري حديث", "fonts/cairo.ttf", "أَثَــر مُعَاصِر"),
    TAJAWAL("تجوال هادئ", "أنيق وبسيط", "fonts/tajawal.ttf", "أَثَــر خَفِيف"),
    MARHEY("مرحي فني", "تعبيري", "fonts/marhey.ttf", "أَثَــر جَرِيء"),
    EL_MESSIRI("المسيري الملكي", "فخامة متزنة", "fonts/el_messiri.ttf", "أَثَــر مَلَكِي")
}

enum class ExportResolution(val label: String, val width: Int, val description: String) {
    HD_1080P("عالية الدقة HD (1080p)", 1080, "مثالية للنشر السريع على انستغرام وتيك توك"),
    ULTRA_4K("فائقة الدقة Ultra 4K (2160p)", 2160, "دقة فائقة النقاء للطباعة وتفاصيل التايبوغرافي الحادة")
}

data class CanvasState(
    val rawText: String = "أَثَــر",
    val displayText: String = "أَثَـــر",
    val stretchLevel: Int = 3,
    val selectedEffect: VisualEffect = VisualEffect.ROYAL_GOLD,
    val selectedFont: ArabicFontFamily = ArabicFontFamily.AREF_RUQAA,
    val fontSize: Float = 52f,
    val textColor: Color = Color.White,
    val isTextBehindSubject: Boolean = false,
    val aspectRatio: CanvasAspectRatio = CanvasAspectRatio.STORY_9_16,
    val backgroundImageUri: Uri? = null,
    val backgroundDim: Float = 0.25f,
    val letterSpacing: Float = 0f,
    val rotationAngle: Float = 0f,
    val scaleFactor: Float = 1.0f,
    val normOffsetX: Float = 0f,
    val normOffsetY: Float = 0f,
    val isTransparentBg: Boolean = false
)

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val isMandatory: Boolean = false
)
