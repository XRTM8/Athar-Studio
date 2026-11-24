package app.xrtm8.athar.model

enum class GlowStyle(val label: String, val description: String) {
    BLOOM("Bloom", "توهج دائري ناعم للإضاءات العالية"),
    DIFFUSION("Diffusion", "انتشار ضبابي حالم لكامل المشهد"),
    HALATION("Halation", "تشتت فيلمي كهرماني كأفلام كوداك"),
    MOTION("Motion", "سحب ضوئي حركي أفقي للإضاءة"),
    STREAK("Streak", "أشعة سينمائية أفقية حادة وممتدة")
}

data class CinematicGlowState(
    val style: GlowStyle = GlowStyle.MOTION,
    val glow: Float = 0f,
    val range: Float = 70f,
    val spread: Float = 65f,
    val warmth: Float = 70f
) {
    val isEnabled: Boolean get() = glow > 0.5f
    val isDefault: Boolean get() = glow == 0f && style == GlowStyle.MOTION && range == 70f && spread == 65f && warmth == 70f
}

data class FilmGrainState(
    val grain: Float = 0f,
    val size: Float = 47f,
    val roughness: Float = 50f
) {
    val isEnabled: Boolean get() = grain > 0.5f
    val isDefault: Boolean get() = grain == 0f && size == 47f && roughness == 50f
}

data class GlitchEffectState(
    val rgbSplit: Float = 0f,
    val sliceJitter: Float = 0f,
    val scanlines: Float = 0f,
    val noise: Float = 0f,
    val pixelate: Float = 0f,
    val seed: Long = 1337L
) {
    val isEnabled: Boolean get() = rgbSplit > 0.5f || sliceJitter > 0.5f || scanlines > 0.5f || noise > 0.5f || pixelate > 0.5f
    val isDefault: Boolean get() = rgbSplit == 0f && sliceJitter == 0f && scanlines == 0f && noise == 0f && pixelate == 0f && seed == 1337L
}

