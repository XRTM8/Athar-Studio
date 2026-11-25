package app.xrtm8.athar.project

import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON serialization and deserialization for multi-layer and legacy single-layer text documents.
 */
internal object AtharLayerJsonCodec {

    fun writeLayers(target: JSONObject, layers: List<TextLayer>, activeLayer: TextLayer, activeLayerId: String) {
        val layersArray = JSONArray()
        layers.forEach { layer -> layersArray.put(serializeLayer(layer)) }
        target.put("layers", layersArray)
        target.put("active_layer_id", activeLayerId)
        writeLayerFields(target, activeLayer, includeVisibility = false)
    }

    fun readLayers(c: JSONObject): Pair<List<TextLayer>, String> {
        val layers = ArrayList<TextLayer>()
        if (c.has("layers")) {
            val arr = c.getJSONArray("layers")
            for (i in 0 until arr.length()) {
                layers.add(parseLayer(arr.getJSONObject(i), index = i, isLegacyRoot = false))
            }
        } else {
            layers.add(parseLayer(c, index = 0, isLegacyRoot = true))
        }
        val safeLayers = if (layers.isNotEmpty()) layers else listOf(TextLayer())
        val activeLayerId = c.optString("active_layer_id", safeLayers.first().id)
        return safeLayers to activeLayerId
    }

    private fun serializeLayer(layer: TextLayer): JSONObject = JSONObject().apply {
        put("id", layer.id)
        put("name", layer.name)
        writeLayerFields(this, layer, includeVisibility = true)
        put("blend_mode", layer.blendMode.name)
        put("is_curved", layer.isCurved)
        put("curve_angle", layer.curveAngle.toDouble())
        put("gradient_preset", layer.gradientPreset.name)
        put("gradient_angle", layer.gradientAngle.toDouble())
        put("is_radial_gradient", layer.isRadialGradient)
        put("is_hollow_text", layer.isHollowText)
    }

    private fun writeLayerFields(target: JSONObject, layer: TextLayer, includeVisibility: Boolean) {
        target.apply {
            put("raw_text", layer.rawText)
            put("display_text", layer.displayText)
            put("stretch_level", layer.stretchLevel)
            put("kashida_mode", layer.kashidaMode.name)
            put("kashida_target_index", layer.kashidaTargetIndex)
            put("selected_font", layer.selectedFont.name)
            put("font_weight", layer.fontWeight)
            put("selected_effect", layer.selectedEffect.name)
            put("font_size", layer.fontSize.toDouble())
            put("custom_color", layer.customTextColorLong ?: 0L)
            put("stroke_width", layer.strokeWidth.toDouble())
            put("stroke_color", layer.strokeColorLong)
            put("shadow_radius", layer.shadowRadius.toDouble())
            put("shadow_dx", layer.shadowDx.toDouble())
            put("shadow_dy", layer.shadowDy.toDouble())
            put("shadow_color", layer.shadowColorLong)
            put("text_align", layer.textAlign.name)
            put("line_spacing", layer.lineSpacingMultiplier.toDouble())
            put("letter_spacing", layer.letterSpacing.toDouble())
            put("text_opacity", layer.textOpacity.toDouble())
            put("is_bold", layer.isBold)
            put("is_italic", layer.isItalic)
            put("is_underline", layer.isUnderline)
            put("is_locked", layer.isLocked)
            if (includeVisibility) put("is_visible", layer.isVisible)
            put("rotation_angle", layer.rotationAngle.toDouble())
            put("scale_factor", layer.scaleFactor.toDouble())
            put("norm_offset_x", layer.normOffsetX.toDouble())
            put("norm_offset_y", layer.normOffsetY.toDouble())
            put("enable_glass_backdrop", layer.enableGlassBackdrop)
            put("glass_backdrop_opacity", layer.glassBackdropOpacity.toDouble())
            put("glass_backdrop_color", layer.glassBackdropColorLong)
            put("glass_backdrop_border_color", layer.glassBackdropBorderColorLong)
            put("glass_backdrop_corner_radius", layer.glassBackdropCornerRadius.toDouble())
            put("glass_backdrop_padding", layer.glassBackdropPadding.toDouble())
            put("box_width_ratio", layer.boxWidthRatio.toDouble())
            put("custom_font_path", layer.customFontPath ?: "")
            put("custom_font_name", layer.customFontName ?: "")
            put("stylistic_set", layer.stylisticSet)
            put("enable_swash", layer.enableSwash)
            put("enable_dlig", layer.enableDiscretionaryLigatures)
            put("word_spacing", layer.wordSpacing.toDouble())
        }
    }

    private fun parseLayer(obj: JSONObject, index: Int, isLegacyRoot: Boolean): TextLayer {
        val rawText = obj.optString("raw_text", "أَثَــر")
        val stretchLevel = obj.optInt("stretch_level", 3).coerceIn(0, 8)
        val kMode = enumOrDefault(obj.optString("kashida_mode", KashidaMode.SMART.name), KashidaMode.SMART)
        val targetIdx = obj.optInt("kashida_target_index", -1)
        val font = enumOrDefault(obj.optString("selected_font", ArabicFontFamily.AREF_RUQAA.name), ArabicFontFamily.AREF_RUQAA)
        val effect = enumOrDefault(obj.optString("selected_effect", VisualEffect.ROYAL_GOLD.name), VisualEffect.ROYAL_GOLD)
        val align = enumOrDefault(obj.optString("text_align", TextAlignOption.CENTER.name), TextAlignOption.CENTER)
        val customColor = obj.optLong("custom_color", 0L)
        val dispText = obj.optString("display_text", "")
        val finalDisp = if (dispText.isNotEmpty()) dispText else PureArabicKashidaEngine.stretchText(rawText, stretchLevel, kMode, targetIdx)

        return TextLayer(
            id = if (isLegacyRoot) "layer_primary" else obj.optString("id", "layer_$index"),
            name = if (isLegacyRoot) "نص رئيسي" else obj.optString("name", "نص ${index + 1}"),
            rawText = rawText,
            displayText = finalDisp,
            stretchLevel = stretchLevel,
            kashidaMode = kMode,
            kashidaTargetIndex = targetIdx,
            selectedEffect = effect,
            selectedFont = font,
            fontWeight = obj.optInt("font_weight", 400).coerceIn(100, 900),
            fontSize = obj.optDouble("font_size", 52.0).toFloat(),
            customTextColorLong = if (customColor != 0L) customColor else null,
            strokeWidth = obj.optDouble("stroke_width", 0.0).toFloat(),
            strokeColorLong = obj.optLong("stroke_color", 0xFF000000),
            shadowRadius = obj.optDouble("shadow_radius", 16.0).toFloat(),
            shadowDx = obj.optDouble("shadow_dx", 0.0).toFloat(),
            shadowDy = obj.optDouble("shadow_dy", 6.0).toFloat(),
            shadowColorLong = obj.optLong("shadow_color", 0xAA000000),
            textAlign = align,
            lineSpacingMultiplier = obj.optDouble("line_spacing", 1.35).toFloat(),
            letterSpacing = obj.optDouble("letter_spacing", 0.0).toFloat(),
            textOpacity = obj.optDouble("text_opacity", 1.0).toFloat(),
            isBold = obj.optBoolean("is_bold", false),
            isItalic = obj.optBoolean("is_italic", false),
            isUnderline = obj.optBoolean("is_underline", false),
            isLocked = obj.optBoolean("is_locked", false),
            isVisible = if (isLegacyRoot) true else obj.optBoolean("is_visible", true),
            rotationAngle = obj.optDouble("rotation_angle", 0.0).toFloat(),
            scaleFactor = obj.optDouble("scale_factor", 1.0).toFloat(),
            normOffsetX = obj.optDouble("norm_offset_x", 0.0).toFloat(),
            normOffsetY = obj.optDouble("norm_offset_y", 0.0).toFloat(),
            enableGlassBackdrop = obj.optBoolean("enable_glass_backdrop", false),
            glassBackdropOpacity = obj.optDouble("glass_backdrop_opacity", 0.65).toFloat(),
            glassBackdropColorLong = obj.optLong("glass_backdrop_color", 0xFF0D0F14),
            glassBackdropBorderColorLong = obj.optLong("glass_backdrop_border_color", 0x55E5C07B),
            glassBackdropCornerRadius = obj.optDouble("glass_backdrop_corner_radius", 22.0).toFloat(),
            glassBackdropPadding = obj.optDouble("glass_backdrop_padding", 22.0).toFloat(),
            blendMode = if (isLegacyRoot) LayerBlendMode.NORMAL else enumOrDefault(obj.optString("blend_mode", LayerBlendMode.NORMAL.name), LayerBlendMode.NORMAL),
            isCurved = if (isLegacyRoot) false else obj.optBoolean("is_curved", false),
            curveAngle = if (isLegacyRoot) 0f else obj.optDouble("curve_angle", 0.0).toFloat(),
            gradientPreset = if (isLegacyRoot) LuxuryGradientPreset.NONE else enumOrDefault(obj.optString("gradient_preset", LuxuryGradientPreset.NONE.name), LuxuryGradientPreset.NONE),
            gradientAngle = if (isLegacyRoot) 90f else obj.optDouble("gradient_angle", 90.0).toFloat(),
            isRadialGradient = if (isLegacyRoot) false else obj.optBoolean("is_radial_gradient", false),
            isHollowText = if (isLegacyRoot) false else obj.optBoolean("is_hollow_text", false),
            boxWidthRatio = obj.optDouble("box_width_ratio", 0.85).toFloat(),
            customFontPath = obj.optString("custom_font_path", "").ifBlank { null },
            customFontName = obj.optString("custom_font_name", "").ifBlank { null },
            stylisticSet = obj.optInt("stylistic_set", 0),
            enableSwash = obj.optBoolean("enable_swash", false),
            enableDiscretionaryLigatures = obj.optBoolean("enable_dlig", false),
            wordSpacing = obj.optDouble("word_spacing", 0.0).toFloat()
        )
    }

    internal inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
        try { enumValueOf<T>(value) } catch (_: Exception) { default }
}
