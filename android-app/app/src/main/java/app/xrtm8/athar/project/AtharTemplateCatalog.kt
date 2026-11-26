package app.xrtm8.athar.project

import app.xrtm8.athar.model.*
import app.xrtm8.athar.ui.viewmodel.StudioLayerTypographyOps.withKashida

data class AtharTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val buildState: () -> CanvasState
)

/**
 * Curated offline editable .athar templates — zero external assets, 100% live layers.
 */
object AtharTemplateCatalog {

    val templates: List<AtharTemplate> = listOf(
        AtharTemplate(
            id = "tpl_fluted_quote",
            title = "اقتباس زجاجي",
            subtitle = "ستوري 9:16 • زجاج مضلّع",
            buildState = {
                val main = TextLayer(
                    id = "tpl_main",
                    name = "الاقتباس",
                    rawText = "وَيَبْقَى الأَثَـرُ الجَمِيلُ\nفِي القُلُوبِ",
                    selectedFont = ArabicFontFamily.AREF_RUQAA,
                    fontSize = 46f,
                    normOffsetY = -0.04f,
                    textSpans = listOf(
                        TextSpanStyle(start = 15, end = 22, highlightColorLong = 0xFFE5C07BL, textColorLong = 0xFF0A0A0AL, highlightCornerRadius = 10f)
                    )
                ).withKashida()
                val divider = TextLayer(
                    id = "tpl_div",
                    name = "فاصل علوي",
                    normOffsetY = -0.24f,
                    shapeConfig = ShapeLayerConfig(type = ShapeType.DIVIDER_H, widthRatio = 0.55f, strokeWidth = 1.8f, fillColorLong = 0xFFE5C07BL)
                )
                CanvasState(
                    aspectRatio = CanvasAspectRatio.STORY_9_16,
                    solidBgColorLong = 0xFF14110FL,
                    flutedGlassState = FlutedGlassState(intensity = 0.65f, ribCount = 26, specular = 0.60f, coverage = 0.55f),
                    layers = listOf(divider, main),
                    activeLayerId = main.id
                )
            }
        ),
        AtharTemplate(
            id = "tpl_kufi_modern",
            title = "كوفي معماري",
            subtitle = "بورتريه 4:5 • إطار ذهبي",
            buildState = {
                val frame = TextLayer(
                    id = "tpl_frame",
                    name = "إطار",
                    shapeConfig = ShapeLayerConfig(type = ShapeType.FRAME, widthRatio = 0.86f, heightRatio = 0.86f, cornerRadius = 14f, strokeWidth = 1.5f, strokeColorLong = 0xFFE5C07BL, isFilled = false, isStroked = true)
                )
                val title = TextLayer(
                    id = "tpl_title",
                    name = "العنوان",
                    rawText = "كُنْ ذَا أَثَـر",
                    selectedFont = ArabicFontFamily.REEM_KUFI,
                    fontSize = 54f,
                    gradientPreset = LuxuryGradientPreset.ANDALUSIAN_GOLD
                ).withKashida()
                CanvasState(
                    aspectRatio = CanvasAspectRatio.PORTRAIT_4_5,
                    solidBgColorLong = 0xFF0B0D12L,
                    layers = listOf(frame, title),
                    activeLayerId = title.id
                )
            }
        ),
        AtharTemplate(
            id = "tpl_emerald_highlight",
            title = "بوستر مميّز",
            subtitle = "مربع 1:1 • تظليل كلمات",
            buildState = {
                val body = TextLayer(
                    id = "tpl_body",
                    name = "النص",
                    rawText = "البَسَاطَةُ هِيَ\nقِمَّةُ الأَنَاقَةِ",
                    selectedFont = ArabicFontFamily.CAIRO,
                    fontSize = 44f,
                    fontWeight = 700,
                    textSpans = listOf(
                        TextSpanStyle(start = 15, end = 21, highlightColorLong = 0xFF9E1B32L, textColorLong = 0xFFFFFFFFL, highlightCornerRadius = 12f, highlightPaddingH = 10f)
                    )
                ).withKashida()
                CanvasState(
                    aspectRatio = CanvasAspectRatio.SQUARE_1_1,
                    solidBgColorLong = 0xFF16161AL,
                    layers = listOf(body),
                    activeLayerId = body.id
                )
            }
        ),
        AtharTemplate(
            id = "tpl_glass_card",
            title = "بطاقة زجاجية",
            subtitle = "ستوري 9:16 • لوحة ضبابية",
            buildState = {
                val card = TextLayer(
                    id = "tpl_card",
                    name = "البطاقة",
                    rawText = "سَلامٌ عَلَى مَنْ\nصَنَعُوا الأَمَل",
                    selectedFont = ArabicFontFamily.AMIRI,
                    fontSize = 42f,
                    enableGlassBackdrop = true,
                    glassBackdropOpacity = 0.70f,
                    glassBackdropCornerRadius = 20f,
                    glassBackdropPadding = 24f
                ).withKashida()
                CanvasState(
                    aspectRatio = CanvasAspectRatio.STORY_9_16,
                    solidBgColorLong = 0xFF1A1014L,
                    layers = listOf(card),
                    activeLayerId = card.id
                )
            }
        )
    )
}
