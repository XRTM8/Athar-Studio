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

enum class OpticalLensType(
    val labelAr: String,
    val descriptionAr: String
) {
    NONE("بدون عدسة", "عرض طبيعي بدون إضافة بصرية"),
    SOFT_WIDE("عدسة واسعة ناعمة", "نعومة تدريجية للحواف مع انحراف لوني طفيف"),
    ORGANIC_VIGNETTE("تظليل عدسة طبيعي", "تدرج ضوئي بيضاوي يحاكي سقوط الضوء الطبيعي"),
    HALATION_LEAK("هالة وتسريب فيلمي", "هالة قرمزية حول الإضاءة الساطعة وتسريب دافئ"),
    PRISM_DIFFRACTION("انكسار موشوري", "تشتت طيفي موجه من نقاط الإضاءة الساطعة"),
    STARBURST_4PT("نجمة ضوئية رباعية", "أشعة متعامدة على اللمعان الساطع بأطراف بنفسجية")
}

data class OpticalLensState(
    val type: OpticalLensType = OpticalLensType.NONE,
    val intensity: Float = 0.65f,
    val spread: Float = 0.50f,
    val angleDegrees: Float = 45f
) {
    val isEnabled: Boolean get() = type != OpticalLensType.NONE && intensity > 0.01f
    val isDefault: Boolean get() = type == OpticalLensType.NONE && intensity == 0.65f && spread == 0.50f && angleDegrees == 45f
}

/**
 * Physical Fluted / Ribbed Glass refraction & specular ridge state.
 */
data class FlutedGlassState(
    val intensity: Float = 0f,
    val ribCount: Int = 28,
    val specular: Float = 0.55f,
    val coverage: Float = 1.0f,
    val coverageFromStart: Boolean = true,
    val angleDegrees: Float = 0f,
    val chromaticDispersion: Float = 0f,
    val edgeSoftness: Float = 0f
) {
    val isEnabled: Boolean get() = intensity > 0.005f
    val isDefault: Boolean
        get() = intensity == 0f &&
            ribCount == 28 &&
            specular == 0.55f &&
            coverage == 1.0f &&
            coverageFromStart &&
            angleDegrees == 0f &&
            chromaticDispersion == 0f &&
            edgeSoftness == 0f
}

/**
 * 4-Point Luma Tone Curve & Split Toning Grading State.
 */
data class ToneCurveState(
    val blacks: Float = 0f,
    val shadows: Float = 0f,
    val highlights: Float = 0f,
    val whites: Float = 0f,
    val splitShadowHue: Float = 220f,
    val splitShadowSat: Float = 0f,
    val splitHighlightHue: Float = 40f,
    val splitHighlightSat: Float = 0f
) {
    val isEnabled: Boolean
        get() = kotlin.math.abs(blacks) > 0.005f ||
            kotlin.math.abs(shadows) > 0.005f ||
            kotlin.math.abs(highlights) > 0.005f ||
            kotlin.math.abs(whites) > 0.005f ||
            splitShadowSat > 0.005f ||
            splitHighlightSat > 0.005f
}
