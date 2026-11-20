package app.xrtm8.athar.core

import app.xrtm8.athar.model.TashkeelItem
import app.xrtm8.athar.model.TextLayer
import java.util.UUID

/**
 * High-precision free-floating Tashkeel (Arabic Diacritics) Engine.
 * Allows micro-positioning, scaling, and individual coloring of diacritical marks
 * independent of base font ligature constraints.
 */
object FreeTashkeelEngine {

    data class TashkeelDefinition(
        val symbol: String,
        val nameAr: String,
        val defaultOffsetY: Float = -0.35f
    )

    val PRESET_TASHKEEL = listOf(
        TashkeelDefinition("َ", "فَتْحَة", -0.35f),
        TashkeelDefinition("ً", "تَنْوِين فَتْح", -0.42f),
        TashkeelDefinition("ُ", "ضَمَّة", -0.38f),
        TashkeelDefinition("ٌ", "تَنْوِين ضَمّ", -0.42f),
        TashkeelDefinition("ِ", "كَسْرَة", 0.35f),
        TashkeelDefinition("ٍ", "تَنْوِين كَسْر", 0.42f),
        TashkeelDefinition("ّ", "شَدَّة", -0.35f),
        TashkeelDefinition("ْ", "سُكُون", -0.35f),
        TashkeelDefinition("ٰ", "أَلِف خَنْجَرِيَّة", -0.35f),
        TashkeelDefinition("ٓ", "مَدَّة", -0.40f)
    )

    fun createMark(
        symbol: String,
        normX: Float = 0f,
        normY: Float = -0.35f,
        scale: Float = 1.0f,
        colorLong: Long? = null
    ): TashkeelItem {
        return TashkeelItem(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            normX = normX,
            normY = normY,
            scale = scale,
            colorLong = colorLong
        )
    }

    fun addMark(layer: TextLayer, symbol: String): TextLayer {
        val def = PRESET_TASHKEEL.find { it.symbol == symbol }
        val newY = def?.defaultOffsetY ?: -0.35f
        val newMark = createMark(symbol = symbol, normX = 0f, normY = newY)
        return layer.copy(tashkeelItems = layer.tashkeelItems + newMark)
    }

    fun removeMark(layer: TextLayer, markId: String): TextLayer {
        return layer.copy(tashkeelItems = layer.tashkeelItems.filterNot { it.id == markId })
    }

    fun updateMark(layer: TextLayer, updated: TashkeelItem): TextLayer {
        return layer.copy(tashkeelItems = layer.tashkeelItems.map { if (it.id == updated.id) updated else it })
    }

    fun nudgeMark(layer: TextLayer, markId: String, dx: Float, dy: Float): TextLayer {
        return layer.copy(
            tashkeelItems = layer.tashkeelItems.map {
                if (it.id == markId) {
                    it.copy(
                        normX = (it.normX + dx).coerceIn(-1.5f, 1.5f),
                        normY = (it.normY + dy).coerceIn(-1.5f, 1.5f)
                    )
                } else it
            }
        )
    }
}
