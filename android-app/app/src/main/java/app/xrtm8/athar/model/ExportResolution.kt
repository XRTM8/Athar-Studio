package app.xrtm8.athar.model

enum class ExportResolution(val label: String, val width: Int, val description: String) {
    HD_1080P("عالية HD (1080p)", 1080, "مناسبة للنشر على وسائل التواصل"),
    ULTRA_4K("دقة 4K (2160p)", 2160, "دقة عالية للطباعة والحفظ بجودة كاملة")
}

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val isMandatory: Boolean = false
)
