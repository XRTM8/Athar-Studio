package app.xrtm8.athar.model

import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class VisualEffect(val arabicName: String, val description: String) {
    ROYAL_GOLD("ذهب ملكي", "تذهيب عربي فاخر مع لمعان دافئ وتدرجات شمسية"),
    LIQUID_CHROME("كروم سائل", "انعكاسات معدنية لامعة وإضاءة ألمنيوم مصقول"),
    FROSTED_GLASS("زجاج مصنفر", "شفافية سينمائية مع تشتت ضوئي وحواف مصقولة"),
    CYBER_NEON("نيون متوهج", "إشعاع ضوئي متعدد الطبقات بألوان نابضة"),
    CINEMATIC_SHADOW("ظل سينمائي", "بروز ثلاثي الأبعاد مع ظل واقعي ممتد في الفضاء"),
    MINIMAL_LUXURY("أبيض ناصع", "بساطة مطلقة بأسلوب التصميم السويسري المعاصر")
}

enum class CanvasAspectRatio(val label: String, val ratio: Float?) {
    STORY_9_16("ستوري 9:16", 9f / 16f),
    SQUARE_1_1("مربع 1:1", 1f),
    PORTRAIT_4_5("بورتريه 4:5", 4f / 5f),
    FREE("حر / شفاف", null)
}

enum class ArabicFontFamily(val fontName: String, val category: String) {
    TRADITIONAL("أصيل كلاسيكي", "نسخ"),
    MODERN_SANS("عصري حديث", "سناب / ستوري"),
    RUQAA("عارف رقعة", "رقعة"),
    KUFI("هندسي كوفي", "كوفي"),
    ELEGANT("تجوال متزن", "أنيق"),
    MONOSPACE("طباعي تقني", "هندسي")
}

data class CanvasState(
    val rawText: String = "أَثَــر",
    val displayText: String = "أَثَـــر",
    val stretchLevel: Int = 3,
    val selectedEffect: VisualEffect = VisualEffect.ROYAL_GOLD,
    val selectedFont: ArabicFontFamily = ArabicFontFamily.TRADITIONAL,
    val fontSize: Float = 48f,
    val textColor: Color = Color.White,
    val isTextBehindSubject: Boolean = false,
    val aspectRatio: CanvasAspectRatio = CanvasAspectRatio.STORY_9_16,
    val backgroundImageUri: Uri? = null,
    val backgroundDim: Float = 0.25f,
    val letterSpacing: Float = 0f,
    val rotationAngle: Float = 0f,
    val scaleFactor: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
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
