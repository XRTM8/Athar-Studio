package app.xrtm8.mada.model

import androidx.compose.ui.graphics.Color

enum class VisualEffect(val arabicName: String, val description: String) {
    LIQUID_CHROME("كروم سائل", "انعكاسات معدنية لامعة وإضاءة ألمنيوم مصقول"),
    ROYAL_GOLD("ذهب ملكي", "تذهيب عربي فاخر مع لمعان دافئ وتدرجات شمسية"),
    FROSTED_GLASS("زجاج مصنفر", "شفافية سينمائية مع تشتت ضوئي وحواف مصقولة"),
    CYBER_NEON("نيون متوهج", "إشعاع ضوئي متعدد الطبقات بألوان نابضة"),
    CINEMATIC_SHADOW("ظل سينمائي", "بروز ثلاثي الأبعاد مع ظل واقعي ممتد في الفضاء"),
    MINIMAL_LUXURY("أبيض ناصع", "بساطة مطلقة بأسلوب التصميم السويسري المعاصر")
}

data class CanvasState(
    val rawText: String = "مَــدَى",
    val displayText: String = "مَــدَى",
    val stretchLevel: Int = 3,
    val selectedEffect: VisualEffect = VisualEffect.ROYAL_GOLD,
    val fontSize: Float = 48f,
    val textColor: Color = Color.White,
    val isTextBehindSubject: Boolean = false,
    val letterSpacing: Float = 0f,
    val rotationAngle: Float = 0f,
    val scaleFactor: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val isTransparentBg: Boolean = true
)

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val isMandatory: Boolean = false
)
