package app.xrtm8.athar.project

import android.net.Uri
import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-performance JSON serializer & deserializer for .athar design documents.
 * 100% Backward and Forward Compatible:
 * - Serializes multi-text layers with full independent styling
 * - Writes legacy fields to preserve compatibility with older versions
 * - Deserializes legacy single-layer documents seamlessly into multi-layer structure
 * - Serializes and restores Film Presets, Directional Gradient Scrim, and Photo Framing
 */
object AtharProjectSerializer {

    fun serializeCanvasState(state: CanvasState, hasBackgroundPhoto: Boolean): JSONObject {
        return JSONObject().apply {
            // Multi-Layer Array
            val layersArray = JSONArray()
            state.layers.forEach { layer ->
                val lObj = JSONObject().apply {
                    put("id", layer.id)
                    put("name", layer.name)
                    put("raw_text", layer.rawText)
                    put("display_text", layer.displayText)
                    put("stretch_level", layer.stretchLevel)
                    put("kashida_mode", layer.kashidaMode.name)
                    put("kashida_target_index", layer.kashidaTargetIndex)
                    put("selected_font", layer.selectedFont.name)
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
                    put("is_visible", layer.isVisible)
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
                    put("blend_mode", layer.blendMode.name)
                    put("is_curved", layer.isCurved)
                    put("curve_angle", layer.curveAngle.toDouble())
                    put("gradient_preset", layer.gradientPreset.name)
                    put("gradient_angle", layer.gradientAngle.toDouble())
                    put("is_radial_gradient", layer.isRadialGradient)
                    put("is_hollow_text", layer.isHollowText)
                    put("box_width_ratio", layer.boxWidthRatio.toDouble())
                    put("custom_font_path", layer.customFontPath ?: "")
                    put("custom_font_name", layer.customFontName ?: "")
                    put("stylistic_set", layer.stylisticSet)
                    put("enable_swash", layer.enableSwash)
                    put("enable_dlig", layer.enableDiscretionaryLigatures)
                    put("word_spacing", layer.wordSpacing.toDouble())
                }
                layersArray.put(lObj)
            }
            put("layers", layersArray)
            put("active_layer_id", state.activeLayerId)

            // Legacy backward-compatibility keys (from active layer)
            val active = state.activeLayer
            put("raw_text", active.rawText)
            put("display_text", active.displayText)
            put("stretch_level", active.stretchLevel)
            put("kashida_mode", active.kashidaMode.name)
            put("kashida_target_index", active.kashidaTargetIndex)
            put("selected_font", active.selectedFont.name)
            put("selected_effect", active.selectedEffect.name)
            put("font_size", active.fontSize.toDouble())
            put("custom_color", active.customTextColorLong ?: 0L)
            put("stroke_width", active.strokeWidth.toDouble())
            put("stroke_color", active.strokeColorLong)
            put("shadow_radius", active.shadowRadius.toDouble())
            put("shadow_dx", active.shadowDx.toDouble())
            put("shadow_dy", active.shadowDy.toDouble())
            put("shadow_color", active.shadowColorLong)
            put("text_align", active.textAlign.name)
            put("line_spacing", active.lineSpacingMultiplier.toDouble())
            put("letter_spacing", active.letterSpacing.toDouble())
            put("text_opacity", active.textOpacity.toDouble())
            put("is_bold", active.isBold)
            put("is_italic", active.isItalic)
            put("is_underline", active.isUnderline)
            put("is_locked", active.isLocked)
            put("rotation_angle", active.rotationAngle.toDouble())
            put("scale_factor", active.scaleFactor.toDouble())
            put("norm_offset_x", active.normOffsetX.toDouble())
            put("norm_offset_y", active.normOffsetY.toDouble())
            put("enable_glass_backdrop", active.enableGlassBackdrop)
            put("glass_backdrop_opacity", active.glassBackdropOpacity.toDouble())
            put("glass_backdrop_color", active.glassBackdropColorLong)
            put("glass_backdrop_border_color", active.glassBackdropBorderColorLong)
            put("glass_backdrop_corner_radius", active.glassBackdropCornerRadius.toDouble())
            put("glass_backdrop_padding", active.glassBackdropPadding.toDouble())
            put("box_width_ratio", active.boxWidthRatio.toDouble())
            put("custom_font_path", active.customFontPath ?: "")
            put("custom_font_name", active.customFontName ?: "")
            put("stylistic_set", active.stylisticSet)
            put("enable_swash", active.enableSwash)
            put("enable_dlig", active.enableDiscretionaryLigatures)
            put("word_spacing", active.wordSpacing.toDouble())

            // Global Canvas Keys
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
            put("is_transparent_bg", state.isTransparentBg)

            // Film Simulation
            put("film_preset", state.filmPreset.name)

            // Directional Gradient Scrim
            put("scrim_enabled", state.gradientScrim.isEnabled)
            put("scrim_direction", state.gradientScrim.direction.name)
            put("scrim_color", state.gradientScrim.colorLong)
            put("scrim_opacity", state.gradientScrim.opacity.toDouble())
            put("scrim_midpoint", state.gradientScrim.midpoint.toDouble())

            // Background Framing Pan & Zoom
            put("bg_photo_offset_x", state.bgPhotoOffsetX.toDouble())
            put("bg_photo_offset_y", state.bgPhotoOffsetY.toDouble())
            put("bg_photo_scale", state.bgPhotoScale.toDouble())

            // 3-Way Color Wheels Grading
            put("color_grading_enabled", state.colorGradingEnabled)
            put("lift_red", state.liftRed.toDouble())
            put("lift_green", state.liftGreen.toDouble())
            put("lift_blue", state.liftBlue.toDouble())
            put("gamma_red", state.gammaRed.toDouble())
            put("gamma_green", state.gammaGreen.toDouble())
            put("gamma_blue", state.gammaBlue.toDouble())
            put("gain_red", state.gainRed.toDouble())
            put("gain_green", state.gainGreen.toDouble())
            put("gain_blue", state.gainBlue.toDouble())

            // 8-Channel Lightroom Color Mix
            val cm = state.colorMixState
            put("color_mix_enabled", cm.isEnabled)
            if (cm.isolatedChannel != null) put("color_mix_isolated", cm.isolatedChannel.name)
            if (cm.monochromeChannel != null) put("color_mix_monochrome", cm.monochromeChannel.name)
            val cmObj = JSONObject()
            cm.channelAdjustments.forEach { (ch, adj) ->
                if (!adj.isDefault) {
                    val aObj = JSONObject().apply {
                        put("h", adj.hueShift.toDouble())
                        put("s", adj.satShift.toDouble())
                        put("l", adj.lumShift.toDouble())
                    }
                    cmObj.put(ch.name, aObj)
                }
            }
            put("color_mix_channels", cmObj)

            // Procedural Micro-Texture & Pattern
            val pat = state.patternState
            put("pattern_type", pat.type.name)
            put("pattern_opacity", pat.opacity.toDouble())
            put("pattern_scale", pat.scale.toDouble())
            put("pattern_blend", pat.blendMode.name)
            put("pattern_light_tint", pat.isLightTint)

            // Cinematic Glow & Motion
            val glow = state.cinematicGlowState
            put("glow_style", glow.style.name)
            put("glow_intensity", glow.glow.toDouble())
            put("glow_range", glow.range.toDouble())
            put("glow_spread", glow.spread.toDouble())
            put("glow_warmth", glow.warmth.toDouble())

            // Organic Film Grain
            val grain = state.filmGrainState
            put("film_grain", grain.grain.toDouble())
            put("grain_size", grain.size.toDouble())
            put("grain_roughness", grain.roughness.toDouble())

            // Analog Glitch, RGB Split & CRT
            val glitch = state.glitchEffectState
            put("glitch_rgb_split", glitch.rgbSplit.toDouble())
            put("glitch_slice_jitter", glitch.sliceJitter.toDouble())
            put("glitch_scanlines", glitch.scanlines.toDouble())
            put("glitch_noise", glitch.noise.toDouble())
            put("glitch_pixelate", glitch.pixelate.toDouble())
            put("glitch_seed", glitch.seed)

            // Editorial Photo Collage
            val col = state.collageState
            put("collage_layout", col.layout.name)
            put("collage_spacing", col.spacingDp.toDouble())
            put("collage_radius", col.cornerRadiusDp.toDouble())
            put("collage_frame_color", col.frameColorLong)
            val cellsArr = JSONArray()
            col.cells.forEach { cell ->
                val cellObj = JSONObject().apply {
                    put("id", cell.id)
                    put("uri", cell.imageUri?.toString() ?: "")
                    put("l", cell.leftNorm.toDouble())
                    put("t", cell.topNorm.toDouble())
                    put("r", cell.rightNorm.toDouble())
                    put("b", cell.bottomNorm.toDouble())
                    put("ox", cell.photoOffsetX.toDouble())
                    put("oy", cell.photoOffsetY.toDouble())
                    put("scale", cell.photoScale.toDouble())
                }
                cellsArr.put(cellObj)
            }
            put("collage_cells", cellsArr)

            // Masks & Atmospheric Black Gradient
            val m = state.maskState
            put("mask_enabled", m.isEnabled)
            if (m.activeMaskId != null) put("mask_id", m.activeMaskId)
            if (m.customMaskUri != null) put("mask_custom_uri", m.customMaskUri)
            put("mask_opacity", m.opacity.toDouble())
            put("mask_blend", m.blendMode.name)
            put("mask_hue_shift", m.hueShift.toDouble())
            put("mask_offset_x", m.offsetX.toDouble())
            put("mask_offset_y", m.offsetY.toDouble())
            put("mask_scale", m.scale.toDouble())
            put("mask_flipped_h", m.isFlippedH)
            put("mask_flipped_v", m.isFlippedV)
            put("mask_rotation", m.rotationDegrees)
            put("mask_contrast", m.contrast.toDouble())
            put("mask_inverted", m.isInverted)
            if (m.tintColorLong != null) put("mask_tint_color", m.tintColorLong)
            put("mask_luminance_mode", m.isLuminanceMode)

            val bgGrad = m.blackGradient
            put("mask_bg_grad_enabled", bgGrad.isEnabled)
            put("mask_bg_grad_opacity", bgGrad.opacity.toDouble())
            put("mask_bg_grad_placement", bgGrad.placement.name)
            put("mask_bg_grad_coverage", bgGrad.coverage.toDouble())
        }
    }

    fun deserializeCanvasState(c: JSONObject, backgroundUri: Uri? = null): CanvasState {
        val aspectRatioStr = c.optString("aspect_ratio", CanvasAspectRatio.STORY_9_16.name)
        val aspect = try { CanvasAspectRatio.valueOf(aspectRatioStr) } catch (e: Exception) { CanvasAspectRatio.STORY_9_16 }
        val solidBg = c.optLong("solid_bg_color", 0L)

        // Film Preset
        val filmStr = c.optString("film_preset", FilmPreset.NONE.name)
        val film = try { FilmPreset.valueOf(filmStr) } catch (e: Exception) { FilmPreset.NONE }

        // Gradient Scrim
        val scrimDirStr = c.optString("scrim_direction", CanvasGradientDirection.BOTTOM_TO_TOP.name)
        val scrimDir = try { CanvasGradientDirection.valueOf(scrimDirStr) } catch (e: Exception) { CanvasGradientDirection.BOTTOM_TO_TOP }
        val scrim = CanvasGradientScrim(
            isEnabled = c.optBoolean("scrim_enabled", false),
            direction = scrimDir,
            colorLong = c.optLong("scrim_color", 0xFF000000),
            opacity = c.optDouble("scrim_opacity", 0.65).toFloat(),
            midpoint = c.optDouble("scrim_midpoint", 0.5).toFloat()
        )

        val layers = ArrayList<TextLayer>()
        if (c.has("layers")) {
            val arr = c.getJSONArray("layers")
            for (i in 0 until arr.length()) {
                val lObj = arr.getJSONObject(i)
                val rawText = lObj.optString("raw_text", "أَثَــر")
                val stretchLevel = lObj.optInt("stretch_level", 3).coerceIn(0, 8)
                val kModeStr = lObj.optString("kashida_mode", KashidaMode.SMART.name)
                val kMode = try { KashidaMode.valueOf(kModeStr) } catch (e: Exception) { KashidaMode.SMART }
                val targetIdx = lObj.optInt("kashida_target_index", -1)
                val fStr = lObj.optString("selected_font", ArabicFontFamily.AREF_RUQAA.name)
                val eStr = lObj.optString("selected_effect", VisualEffect.ROYAL_GOLD.name)
                val aStr = lObj.optString("text_align", TextAlignOption.CENTER.name)

                val font = try { ArabicFontFamily.valueOf(fStr) } catch (e: Exception) { ArabicFontFamily.AREF_RUQAA }
                val effect = try { VisualEffect.valueOf(eStr) } catch (e: Exception) { VisualEffect.ROYAL_GOLD }
                val align = try { TextAlignOption.valueOf(aStr) } catch (e: Exception) { TextAlignOption.CENTER }
                val customColor = lObj.optLong("custom_color", 0L)

                val dispText = lObj.optString("display_text", "")
                val finalDisp = if (dispText.isNotEmpty()) dispText else PureArabicKashidaEngine.stretchText(rawText, stretchLevel, kMode, targetIdx)

                val blendModeStr = lObj.optString("blend_mode", LayerBlendMode.NORMAL.name)
                val blendMode = try { LayerBlendMode.valueOf(blendModeStr) } catch (e: Exception) { LayerBlendMode.NORMAL }
                val isCurved = lObj.optBoolean("is_curved", false)
                val curveAngle = lObj.optDouble("curve_angle", 0.0).toFloat()

                // Luxury Gradients & Hollow Vector Style
                val gradPresetStr = lObj.optString("gradient_preset", LuxuryGradientPreset.NONE.name)
                val gradPreset = try { LuxuryGradientPreset.valueOf(gradPresetStr) } catch (e: Exception) { LuxuryGradientPreset.NONE }
                val gradAngle = lObj.optDouble("gradient_angle", 90.0).toFloat()
                val isRadialGrad = lObj.optBoolean("is_radial_gradient", false)
                val isHollow = lObj.optBoolean("is_hollow_text", false)
                val customFontPath = lObj.optString("custom_font_path", "").ifBlank { null }
                val customFontName = lObj.optString("custom_font_name", "").ifBlank { null }

                layers.add(
                    TextLayer(
                        id = lObj.optString("id", "layer_$i"),
                        name = lObj.optString("name", "نص ${i + 1}"),
                        rawText = rawText,
                        displayText = finalDisp,
                        stretchLevel = stretchLevel,
                        kashidaMode = kMode,
                        kashidaTargetIndex = targetIdx,
                        selectedEffect = effect,
                        selectedFont = font,
                        fontSize = lObj.optDouble("font_size", 52.0).toFloat(),
                        customTextColorLong = if (customColor != 0L) customColor else null,
                        strokeWidth = lObj.optDouble("stroke_width", 0.0).toFloat(),
                        strokeColorLong = lObj.optLong("stroke_color", 0xFF000000),
                        shadowRadius = lObj.optDouble("shadow_radius", 16.0).toFloat(),
                        shadowDx = lObj.optDouble("shadow_dx", 0.0).toFloat(),
                        shadowDy = lObj.optDouble("shadow_dy", 6.0).toFloat(),
                        shadowColorLong = lObj.optLong("shadow_color", 0xAA000000),
                        textAlign = align,
                        lineSpacingMultiplier = lObj.optDouble("line_spacing", 1.35).toFloat(),
                        letterSpacing = lObj.optDouble("letter_spacing", 0.0).toFloat(),
                        textOpacity = lObj.optDouble("text_opacity", 1.0).toFloat(),
                        isBold = lObj.optBoolean("is_bold", false),
                        isItalic = lObj.optBoolean("is_italic", false),
                        isUnderline = lObj.optBoolean("is_underline", false),
                        isLocked = lObj.optBoolean("is_locked", false),
                        isVisible = lObj.optBoolean("is_visible", true),
                        rotationAngle = lObj.optDouble("rotation_angle", 0.0).toFloat(),
                        scaleFactor = lObj.optDouble("scale_factor", 1.0).toFloat(),
                        normOffsetX = lObj.optDouble("norm_offset_x", 0.0).toFloat(),
                        normOffsetY = lObj.optDouble("norm_offset_y", 0.0).toFloat(),
                        enableGlassBackdrop = lObj.optBoolean("enable_glass_backdrop", false),
                        glassBackdropOpacity = lObj.optDouble("glass_backdrop_opacity", 0.65).toFloat(),
                        glassBackdropColorLong = lObj.optLong("glass_backdrop_color", 0xFF0D0F14),
                        glassBackdropBorderColorLong = lObj.optLong("glass_backdrop_border_color", 0x55E5C07B),
                        glassBackdropCornerRadius = lObj.optDouble("glass_backdrop_corner_radius", 22.0).toFloat(),
                        glassBackdropPadding = lObj.optDouble("glass_backdrop_padding", 22.0).toFloat(),
                        blendMode = blendMode,
                        isCurved = isCurved,
                        curveAngle = curveAngle,
                        gradientPreset = gradPreset,
                        gradientAngle = gradAngle,
                        isRadialGradient = isRadialGrad,
                        isHollowText = isHollow,
                        boxWidthRatio = lObj.optDouble("box_width_ratio", 0.85).toFloat(),
                        customFontPath = customFontPath,
                        customFontName = customFontName,
                        stylisticSet = lObj.optInt("stylistic_set", 0),
                        enableSwash = lObj.optBoolean("enable_swash", false),
                        enableDiscretionaryLigatures = lObj.optBoolean("enable_dlig", false),
                        wordSpacing = lObj.optDouble("word_spacing", 0.0).toFloat()
                    )
                )
            }
        } else {
            // Legacy document fallback
            val rawText = c.optString("raw_text", "أَثَــر")
            val stretchLevel = c.optInt("stretch_level", 3).coerceIn(0, 8)
            val kModeStr = c.optString("kashida_mode", KashidaMode.SMART.name)
            val kMode = try { KashidaMode.valueOf(kModeStr) } catch (e: Exception) { KashidaMode.SMART }
            val targetIdx = c.optInt("kashida_target_index", -1)
            val fStr = c.optString("selected_font", ArabicFontFamily.AREF_RUQAA.name)
            val eStr = c.optString("selected_effect", VisualEffect.ROYAL_GOLD.name)
            val aStr = c.optString("text_align", TextAlignOption.CENTER.name)

            val font = try { ArabicFontFamily.valueOf(fStr) } catch (e: Exception) { ArabicFontFamily.AREF_RUQAA }
            val effect = try { VisualEffect.valueOf(eStr) } catch (e: Exception) { VisualEffect.ROYAL_GOLD }
            val align = try { TextAlignOption.valueOf(aStr) } catch (e: Exception) { TextAlignOption.CENTER }
            val customColor = c.optLong("custom_color", 0L)
            val customFontPath = c.optString("custom_font_path", "").ifBlank { null }
            val customFontName = c.optString("custom_font_name", "").ifBlank { null }
            val dispText = c.optString("display_text", "")
            val finalDisp = if (dispText.isNotEmpty()) dispText else PureArabicKashidaEngine.stretchText(rawText, stretchLevel, kMode, targetIdx)

            layers.add(
                TextLayer(
                    id = "layer_primary",
                    name = "نص رئيسي",
                    rawText = rawText,
                    displayText = finalDisp,
                    stretchLevel = stretchLevel,
                    kashidaMode = kMode,
                    kashidaTargetIndex = targetIdx,
                    selectedEffect = effect,
                    selectedFont = font,
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
                    isVisible = true,
                    rotationAngle = c.optDouble("rotation_angle", 0.0).toFloat(),
                    scaleFactor = c.optDouble("scale_factor", 1.0).toFloat(),
                    normOffsetX = c.optDouble("norm_offset_x", 0.0).toFloat(),
                    normOffsetY = c.optDouble("norm_offset_y", 0.0).toFloat(),
                    enableGlassBackdrop = c.optBoolean("enable_glass_backdrop", false),
                    glassBackdropOpacity = c.optDouble("glass_backdrop_opacity", 0.65).toFloat(),
                    glassBackdropColorLong = c.optLong("glass_backdrop_color", 0xFF0D0F14),
                    glassBackdropBorderColorLong = c.optLong("glass_backdrop_border_color", 0x55E5C07B),
                    glassBackdropCornerRadius = c.optDouble("glass_backdrop_corner_radius", 22.0).toFloat(),
                    glassBackdropPadding = c.optDouble("glass_backdrop_padding", 22.0).toFloat(),
                    boxWidthRatio = c.optDouble("box_width_ratio", 0.85).toFloat(),
                    customFontPath = customFontPath,
                    customFontName = customFontName,
                    stylisticSet = c.optInt("stylistic_set", 0),
                    enableSwash = c.optBoolean("enable_swash", false),
                    enableDiscretionaryLigatures = c.optBoolean("enable_dlig", false),
                    wordSpacing = c.optDouble("word_spacing", 0.0).toFloat()
                )
            )
        }

        val activeLayerId = c.optString("active_layer_id", layers.firstOrNull()?.id ?: "layer_primary")

        val cmEnabled = c.optBoolean("color_mix_enabled", false)
        val cmIsoStr = c.optString("color_mix_isolated", "")
        val cmIso = if (cmIsoStr.isNotBlank()) try { ColorMixChannel.valueOf(cmIsoStr) } catch (e: Exception) { null } else null
        val cmMonoStr = c.optString("color_mix_monochrome", "")
        val cmMono = if (cmMonoStr.isNotBlank()) try { ColorMixChannel.valueOf(cmMonoStr) } catch (e: Exception) { null } else null
        val cmMap = mutableMapOf<ColorMixChannel, ChannelAdjustment>()
        if (c.has("color_mix_channels")) {
            val cmObj = c.getJSONObject("color_mix_channels")
            ColorMixChannel.values().forEach { ch ->
                if (cmObj.has(ch.name)) {
                    val aObj = cmObj.getJSONObject(ch.name)
                    cmMap[ch] = ChannelAdjustment(
                        hueShift = aObj.optDouble("h", 0.0).toFloat(),
                        satShift = aObj.optDouble("s", 0.0).toFloat(),
                        lumShift = aObj.optDouble("l", 0.0).toFloat()
                    )
                }
            }
        }
        val deserializedColorMix = ColorMixState(
            isEnabled = cmEnabled,
            channelAdjustments = cmMap,
            isolatedChannel = cmIso,
            monochromeChannel = cmMono
        )

        val patTypeStr = c.optString("pattern_type", app.xrtm8.athar.model.PatternType.NONE.name)
        val patType = try { app.xrtm8.athar.model.PatternType.valueOf(patTypeStr) } catch (e: Exception) { app.xrtm8.athar.model.PatternType.NONE }
        val patBlendStr = c.optString("pattern_blend", app.xrtm8.athar.model.PatternBlend.OVERLAY.name)
        val patBlend = try { app.xrtm8.athar.model.PatternBlend.valueOf(patBlendStr) } catch (e: Exception) { app.xrtm8.athar.model.PatternBlend.OVERLAY }
        val deserializedPattern = app.xrtm8.athar.model.PatternState(
            type = patType,
            opacity = c.optDouble("pattern_opacity", 0.30).toFloat(),
            scale = c.optDouble("pattern_scale", 1.0).toFloat(),
            blendMode = patBlend,
            isLightTint = c.optBoolean("pattern_light_tint", true)
        )

        val glowStyleStr = c.optString("glow_style", app.xrtm8.athar.model.GlowStyle.MOTION.name)
        val glowStyle = try { app.xrtm8.athar.model.GlowStyle.valueOf(glowStyleStr) } catch (e: Exception) { app.xrtm8.athar.model.GlowStyle.MOTION }
        val deserializedGlow = app.xrtm8.athar.model.CinematicGlowState(
            style = glowStyle,
            glow = c.optDouble("glow_intensity", 0.0).toFloat(),
            range = c.optDouble("glow_range", 70.0).toFloat(),
            spread = c.optDouble("glow_spread", 65.0).toFloat(),
            warmth = c.optDouble("glow_warmth", 70.0).toFloat()
        )

        val deserializedGrain = app.xrtm8.athar.model.FilmGrainState(
            grain = c.optDouble("film_grain", 0.0).toFloat(),
            size = c.optDouble("grain_size", 47.0).toFloat(),
            roughness = c.optDouble("grain_roughness", 50.0).toFloat()
        )

        val deserializedGlitch = app.xrtm8.athar.model.GlitchEffectState(
            rgbSplit = c.optDouble("glitch_rgb_split", 0.0).toFloat(),
            sliceJitter = c.optDouble("glitch_slice_jitter", 0.0).toFloat(),
            scanlines = c.optDouble("glitch_scanlines", 0.0).toFloat(),
            noise = c.optDouble("glitch_noise", 0.0).toFloat(),
            pixelate = c.optDouble("glitch_pixelate", 0.0).toFloat(),
            seed = c.optLong("glitch_seed", 1337L)
        )

        val colLayoutStr = c.optString("collage_layout", app.xrtm8.athar.model.CollageLayoutType.NONE.name)
        val colLayout = try { app.xrtm8.athar.model.CollageLayoutType.valueOf(colLayoutStr) } catch (e: Exception) { app.xrtm8.athar.model.CollageLayoutType.NONE }
        val cellsArr = c.optJSONArray("collage_cells")
        val deserializedCells = mutableListOf<app.xrtm8.athar.model.CollageCell>()
        if (cellsArr != null) {
            for (i in 0 until cellsArr.length()) {
                val o = cellsArr.optJSONObject(i) ?: continue
                val uStr = o.optString("uri", "")
                val uri = if (uStr.isNotBlank()) try { Uri.parse(uStr) } catch (e: Exception) { null } else null
                deserializedCells.add(
                    app.xrtm8.athar.model.CollageCell(
                        id = o.optString("id", "cell_$i"),
                        imageUri = uri,
                        leftNorm = o.optDouble("l", 0.0).toFloat(),
                        topNorm = o.optDouble("t", 0.0).toFloat(),
                        rightNorm = o.optDouble("r", 1.0).toFloat(),
                        bottomNorm = o.optDouble("b", 1.0).toFloat(),
                        photoOffsetX = o.optDouble("ox", 0.0).toFloat(),
                        photoOffsetY = o.optDouble("oy", 0.0).toFloat(),
                        photoScale = o.optDouble("scale", 1.0).toFloat()
                    )
                )
            }
        }
        val deserializedCollage = app.xrtm8.athar.model.CollageState(
            layout = colLayout,
            cells = if (deserializedCells.isNotEmpty()) deserializedCells else app.xrtm8.athar.core.CollageLayoutCalculator.generateCellsForLayout(colLayout),
            spacingDp = c.optDouble("collage_spacing", 6.0).toFloat(),
            cornerRadiusDp = c.optDouble("collage_radius", 8.0).toFloat(),
            frameColorLong = c.optLong("collage_frame_color", 0xFF0D0F14)
        )

        val maskBlendStr = c.optString("mask_blend", MaskBlendMode.SCREEN.name)
        val maskBlend = try { MaskBlendMode.valueOf(maskBlendStr) } catch (e: Exception) { MaskBlendMode.SCREEN }

        val bgGradPlacementStr = c.optString("mask_bg_grad_placement", BlackGradientPlacement.BOTTOM.name)
        val bgGradPlacement = try { BlackGradientPlacement.valueOf(bgGradPlacementStr) } catch (e: Exception) { BlackGradientPlacement.BOTTOM }

        val deserializedMask = AtharMaskState(
            isEnabled = c.optBoolean("mask_enabled", false),
            activeMaskId = c.optString("mask_id", "").takeIf { it.isNotEmpty() },
            customMaskUri = c.optString("mask_custom_uri", "").takeIf { it.isNotEmpty() },
            opacity = c.optDouble("mask_opacity", 0.75).toFloat(),
            blendMode = maskBlend,
            hueShift = c.optDouble("mask_hue_shift", 0.0).toFloat(),
            offsetX = c.optDouble("mask_offset_x", 0.0).toFloat(),
            offsetY = c.optDouble("mask_offset_y", 0.0).toFloat(),
            scale = c.optDouble("mask_scale", 1.0).toFloat(),
            isFlippedH = c.optBoolean("mask_flipped_h", false),
            isFlippedV = c.optBoolean("mask_flipped_v", false),
            rotationDegrees = c.optInt("mask_rotation", 0),
            contrast = c.optDouble("mask_contrast", 1.0).toFloat(),
            isInverted = c.optBoolean("mask_inverted", false),
            tintColorLong = if (c.has("mask_tint_color")) c.optLong("mask_tint_color") else null,
            isLuminanceMode = c.optBoolean("mask_luminance_mode", false),
            blackGradient = BlackGradientState(
                isEnabled = c.optBoolean("mask_bg_grad_enabled", false),
                opacity = c.optDouble("mask_bg_grad_opacity", 0.60).toFloat(),
                placement = bgGradPlacement,
                coverage = c.optDouble("mask_bg_grad_coverage", 0.55).toFloat()
            )
        )

        return CanvasState(
            layers = if (layers.isNotEmpty()) layers else listOf(TextLayer()),
            activeLayerId = activeLayerId,
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
            isTransparentBg = c.optBoolean("is_transparent_bg", false),
            filmPreset = film,
            gradientScrim = scrim,
            bgPhotoOffsetX = c.optDouble("bg_photo_offset_x", 0.0).toFloat(),
            bgPhotoOffsetY = c.optDouble("bg_photo_offset_y", 0.0).toFloat(),
            bgPhotoScale = c.optDouble("bg_photo_scale", 1.0).toFloat(),
            colorGradingEnabled = c.optBoolean("color_grading_enabled", false),
            liftRed = c.optDouble("lift_red", 0.0).toFloat(),
            liftGreen = c.optDouble("lift_green", 0.0).toFloat(),
            liftBlue = c.optDouble("lift_blue", 0.0).toFloat(),
            gammaRed = c.optDouble("gamma_red", 0.0).toFloat(),
            gammaGreen = c.optDouble("gamma_green", 0.0).toFloat(),
            gammaBlue = c.optDouble("gamma_blue", 0.0).toFloat(),
            gainRed = c.optDouble("gain_red", 0.0).toFloat(),
            gainGreen = c.optDouble("gain_green", 0.0).toFloat(),
            gainBlue = c.optDouble("gain_blue", 0.0).toFloat(),
            colorMixState = deserializedColorMix,
            patternState = deserializedPattern,
            cinematicGlowState = deserializedGlow,
            filmGrainState = deserializedGrain,
            collageState = deserializedCollage,
            maskState = deserializedMask,
            glitchEffectState = deserializedGlitch
        )
    }
}
