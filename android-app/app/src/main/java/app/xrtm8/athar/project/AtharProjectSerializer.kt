package app.xrtm8.athar.project

import android.net.Uri
import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.model.*
import org.json.JSONObject

/**
 * High-performance JSON serializer & deserializer for .athar design documents.
 */
object AtharProjectSerializer {

    fun serializeCanvasState(state: CanvasState, hasBackgroundPhoto: Boolean): JSONObject {
        return JSONObject().apply {
            put("raw_text", state.rawText)
            put("display_text", state.displayText)
            put("stretch_level", state.stretchLevel)
            put("kashida_mode", state.kashidaMode.name)
            put("kashida_target_index", state.kashidaTargetIndex)
            put("selected_font", state.selectedFont.name)
            put("selected_effect", state.selectedEffect.name)
            put("font_size", state.fontSize.toDouble())
            put("custom_color", state.customTextColorLong ?: 0L)
            put("stroke_width", state.strokeWidth.toDouble())
            put("stroke_color", state.strokeColorLong)
            put("shadow_radius", state.shadowRadius.toDouble())
            put("shadow_dx", state.shadowDx.toDouble())
            put("shadow_dy", state.shadowDy.toDouble())
            put("shadow_color", state.shadowColorLong)
            put("text_align", state.textAlign.name)
            put("line_spacing", state.lineSpacingMultiplier.toDouble())
            put("letter_spacing", state.letterSpacing.toDouble())
            put("text_opacity", state.textOpacity.toDouble())
            put("is_bold", state.isBold)
            put("is_italic", state.isItalic)
            put("is_underline", state.isUnderline)
            put("is_locked", state.isLocked)
            put("show_grid_guides", state.showGridGuides)
            put("aspect_ratio", state.aspectRatio.name)
            put("solid_bg_color", state.solidBgColorLong ?: 0L)
            put("has_background_photo", hasBackgroundPhoto)
            put("background_dim", state.backgroundDim.toDouble())
            put("background_blur", state.backgroundBlur.toDouble())
            put("background_brightness", state.backgroundBrightness.toDouble())
            put("background_contrast", state.backgroundContrast.toDouble())
            put("background_saturation", state.backgroundSaturation.toDouble())
            put("background_vignette", state.backgroundVignette.toDouble())
            put("background_grayscale", state.backgroundGrayscale)
            put("enable_glass_backdrop", state.enableGlassBackdrop)
            put("glass_backdrop_opacity", state.glassBackdropOpacity.toDouble())
            put("glass_backdrop_color", state.glassBackdropColorLong)
            put("glass_backdrop_border_color", state.glassBackdropBorderColorLong)
            put("glass_backdrop_corner_radius", state.glassBackdropCornerRadius.toDouble())
            put("glass_backdrop_padding", state.glassBackdropPadding.toDouble())
            put("rotation_angle", state.rotationAngle.toDouble())
            put("scale_factor", state.scaleFactor.toDouble())
            put("norm_offset_x", state.normOffsetX.toDouble())
            put("norm_offset_y", state.normOffsetY.toDouble())
            put("is_transparent_bg", state.isTransparentBg)
        }
    }

    fun deserializeCanvasState(c: JSONObject, backgroundUri: Uri? = null): CanvasState {
        val rawText = c.optString("raw_text", "أَثَــر")
        val stretchLevel = c.optInt("stretch_level", 3)
        val kashidaModeStr = c.optString("kashida_mode", KashidaMode.SMART.name)
        val kashidaMode = try { KashidaMode.valueOf(kashidaModeStr) } catch (e: Exception) { KashidaMode.SMART }
        val kashidaTargetIndex = c.optInt("kashida_target_index", -1)
        val selectedFontStr = c.optString("selected_font", ArabicFontFamily.AREF_RUQAA.name)
        val selectedEffectStr = c.optString("selected_effect", VisualEffect.ROYAL_GOLD.name)
        val aspectRatioStr = c.optString("aspect_ratio", CanvasAspectRatio.STORY_9_16.name)
        val textAlignStr = c.optString("text_align", TextAlignOption.CENTER.name)

        val font = try { ArabicFontFamily.valueOf(selectedFontStr) } catch (e: Exception) { ArabicFontFamily.AREF_RUQAA }
        val effect = try { VisualEffect.valueOf(selectedEffectStr) } catch (e: Exception) { VisualEffect.ROYAL_GOLD }
        val aspect = try { CanvasAspectRatio.valueOf(aspectRatioStr) } catch (e: Exception) { CanvasAspectRatio.STORY_9_16 }
        val align = try { TextAlignOption.valueOf(textAlignStr) } catch (e: Exception) { TextAlignOption.CENTER }

        val customColor = c.optLong("custom_color", 0L)
        val solidBg = c.optLong("solid_bg_color", 0L)

        val stretched = PureArabicKashidaEngine.stretchText(rawText, stretchLevel, kashidaMode, kashidaTargetIndex)

        return CanvasState(
            rawText = rawText,
            displayText = stretched,
            stretchLevel = stretchLevel,
            kashidaMode = kashidaMode,
            kashidaTargetIndex = kashidaTargetIndex,
            selectedFont = font,
            selectedEffect = effect,
            fontSize = c.optDouble("font_size", 52.0).toFloat(),
            customTextColorLong = if (customColor != 0L) customColor else null,
            strokeWidth = c.optDouble("stroke_width", 0.0).toFloat(),
            strokeColorLong = c.optLong("stroke_color", 0xFF000000),
            shadowRadius = c.optDouble("shadow_radius", 16.0).toFloat(),
            shadowDx = c.optDouble("shadow_dx", 0.0).toFloat(),
            shadowDy = c.optDouble("shadow_dy", 6.0).toFloat(),
            shadowColorLong = c.optLong("shadow_color", 0xAA000000),
            textAlign = align,
            lineSpacingMultiplier = c.optDouble("line_spacing", 1.35).toFloat(),
            letterSpacing = c.optDouble("letter_spacing", 0.0).toFloat(),
            textOpacity = c.optDouble("text_opacity", 1.0).toFloat(),
            isBold = c.optBoolean("is_bold", false),
            isItalic = c.optBoolean("is_italic", false),
            isUnderline = c.optBoolean("is_underline", false),
            isLocked = c.optBoolean("is_locked", false),
            showGridGuides = c.optBoolean("show_grid_guides", false),
            aspectRatio = aspect,
            solidBgColorLong = if (solidBg != 0L) solidBg else null,
            backgroundImageUri = backgroundUri,
            backgroundDim = c.optDouble("background_dim", 0.25).toFloat(),
            backgroundBlur = c.optDouble("background_blur", 0.0).toFloat(),
            backgroundBrightness = c.optDouble("background_brightness", 0.0).toFloat(),
            backgroundContrast = c.optDouble("background_contrast", 1.0).toFloat(),
            backgroundSaturation = c.optDouble("background_saturation", 1.0).toFloat(),
            backgroundVignette = c.optDouble("background_vignette", 0.0).toFloat(),
            backgroundGrayscale = c.optBoolean("background_grayscale", false),
            enableGlassBackdrop = c.optBoolean("enable_glass_backdrop", false),
            glassBackdropOpacity = c.optDouble("glass_backdrop_opacity", 0.65).toFloat(),
            glassBackdropColorLong = c.optLong("glass_backdrop_color", 0xFF0D0F14),
            glassBackdropBorderColorLong = c.optLong("glass_backdrop_border_color", 0x55E5C07B),
            glassBackdropCornerRadius = c.optDouble("glass_backdrop_corner_radius", 22.0).toFloat(),
            glassBackdropPadding = c.optDouble("glass_backdrop_padding", 22.0).toFloat(),
            rotationAngle = c.optDouble("rotation_angle", 0.0).toFloat(),
            scaleFactor = c.optDouble("scale_factor", 1.0).toFloat(),
            normOffsetX = c.optDouble("norm_offset_x", 0.0).toFloat(),
            normOffsetY = c.optDouble("norm_offset_y", 0.0).toFloat(),
            isTransparentBg = c.optBoolean("is_transparent_bg", false)
        )
    }
}
