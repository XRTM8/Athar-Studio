package app.xrtm8.athar.model

data class CustomFont(
    val id: String,
    val name: String,
    val filePath: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val familyName: String = name,
    val format: String = if (filePath.endsWith(".otf", true)) "OTF" else "TTF",
    val weightClass: Int = 400,
    val isVariable: Boolean = false,
    val supportedWeights: List<Int> = listOf(weightClass),
    val weightVariants: Map<Int, String> = mapOf(weightClass to filePath),
    val supportsArabic: Boolean = true,
    val supportsTatweel: Boolean = true,
    val supportedOpenTypeFeatures: Set<String> = emptySet(),
    val sha256Short: String = ""
) {
    fun resolveWeight(requestedWeight: Int): Int {
        val weights = supportedWeights.ifEmpty { listOf(weightClass) }
        if (requestedWeight in weights) return requestedWeight
        return weights.minWithOrNull(
            compareBy<Int> { kotlin.math.abs(it - requestedWeight) }
                .thenBy { if (it >= requestedWeight) 0 else 1 }
        ) ?: weightClass
    }

    fun resolvePathForWeight(requestedWeight: Int): String {
        if (isVariable || weightVariants.isEmpty()) return filePath
        val target = resolveWeight(requestedWeight)
        return weightVariants[target]
            ?: weightVariants.entries.minByOrNull { kotlin.math.abs(it.key - requestedWeight) }?.value
            ?: filePath
    }
}
