package app.xrtm8.athar.model

enum class ColorMixChannel(
    val labelAr: String,
    val centerHue: Float,
    val swatchColorLong: Long
) {
    RED("أحمر", 0f, 0xFFE53935),
    ORANGE("برتقالي", 30f, 0xFFFB8C00),
    YELLOW("أصفر", 60f, 0xFFFDD835),
    GREEN("أخضر", 120f, 0xFF43A047),
    CYAN("سماوي", 180f, 0xFF00ACC1),
    BLUE("أزرق", 235f, 0xFF1E88E5),
    PURPLE("أرجواني", 285f, 0xFF8E24AA),
    MAGENTA("وردي", 325f, 0xFFD81B60);

    companion object {
        fun fromHue(hue: Float): ColorMixChannel {
            val normalized = ((hue % 360f) + 360f) % 360f
            return when {
                normalized >= 345f || normalized < 15f -> RED
                normalized in 15f..45f -> ORANGE
                normalized in 45f..75f -> YELLOW
                normalized in 75f..165f -> GREEN
                normalized in 165f..205f -> CYAN
                normalized in 205f..265f -> BLUE
                normalized in 265f..310f -> PURPLE
                else -> MAGENTA
            }
        }
    }
}

data class ChannelAdjustment(
    val hueShift: Float = 0f,
    val satShift: Float = 0f,
    val lumShift: Float = 0f
) {
    val isDefault: Boolean
        get() = hueShift == 0f && satShift == 0f && lumShift == 0f
}

data class ColorMixState(
    val isEnabled: Boolean = false,
    val channelAdjustments: Map<ColorMixChannel, ChannelAdjustment> = emptyMap(),
    val isolatedChannel: ColorMixChannel? = null,
    val monochromeChannel: ColorMixChannel? = null
) {
    fun getAdjustment(channel: ColorMixChannel): ChannelAdjustment {
        return channelAdjustments[channel] ?: ChannelAdjustment()
    }

    val isDefault: Boolean
        get() = !isEnabled && channelAdjustments.values.all { it.isDefault } && isolatedChannel == null && monochromeChannel == null
}
