package app.xrtm8.athar.model

data class CustomFont(
    val id: String,
    val name: String,
    val filePath: String,
    val dateAdded: Long = System.currentTimeMillis()
)
