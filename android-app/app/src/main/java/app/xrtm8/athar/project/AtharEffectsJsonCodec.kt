package app.xrtm8.athar.project

import android.net.Uri
import app.xrtm8.athar.core.CollageLayoutCalculator
import app.xrtm8.athar.model.*
import app.xrtm8.athar.project.AtharLayerJsonCodec.enumOrDefault
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON serialization and deserialization for background grading, color mix,
 * film/lens attachments, patterns, glow/grain/glitch, collage, and masks.
 */
internal object AtharEffectsJsonCodec {

    fun writeEffects(target: JSONObject, state: CanvasState) {
        target.apply {
            put("film_preset", state.filmPreset.name)
            put("film_preset_intensity", state.filmPresetIntensity.toDouble())
            val lens = state.opticalLensState
            put("lens_type", lens.type.name)
            put("lens_intensity", lens.intensity.toDouble())
            put("lens_spread", lens.spread.toDouble())
            put("lens_angle", lens.angleDegrees.toDouble())

            put("scrim_enabled", state.gradientScrim.isEnabled)
            put("scrim_direction", state.gradientScrim.direction.name)
            put("scrim_color", state.gradientScrim.colorLong)
            put("scrim_opacity", state.gradientScrim.opacity.toDouble())
            put("scrim_midpoint", state.gradientScrim.midpoint.toDouble())

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

            val cm = state.colorMixState
            put("color_mix_enabled", cm.isEnabled)
            if (cm.isolatedChannel != null) put("color_mix_isolated", cm.isolatedChannel.name)
            if (cm.monochromeChannel != null) put("color_mix_monochrome", cm.monochromeChannel.name)
            val cmObj = JSONObject()
            cm.channelAdjustments.forEach { (ch, adj) ->
                if (!adj.isDefault) {
                    cmObj.put(ch.name, JSONObject().apply {
                        put("h", adj.hueShift.toDouble())
                        put("s", adj.satShift.toDouble())
                        put("l", adj.lumShift.toDouble())
                    })
                }
            }
            put("color_mix_channels", cmObj)

            val pat = state.patternState
            put("pattern_type", pat.type.name)
            put("pattern_opacity", pat.opacity.toDouble())
            put("pattern_scale", pat.scale.toDouble())
            put("pattern_blend", pat.blendMode.name)
            put("pattern_light_tint", pat.isLightTint)

            val glow = state.cinematicGlowState
            put("glow_style", glow.style.name)
            put("glow_intensity", glow.glow.toDouble())
            put("glow_range", glow.range.toDouble())
            put("glow_spread", glow.spread.toDouble())
            put("glow_warmth", glow.warmth.toDouble())

            val grain = state.filmGrainState
            put("film_grain", grain.grain.toDouble())
            put("grain_size", grain.size.toDouble())
            put("grain_roughness", grain.roughness.toDouble())

            val glitch = state.glitchEffectState
            put("glitch_rgb_split", glitch.rgbSplit.toDouble())
            put("glitch_slice_jitter", glitch.sliceJitter.toDouble())
            put("glitch_scanlines", glitch.scanlines.toDouble())
            put("glitch_noise", glitch.noise.toDouble())
            put("glitch_pixelate", glitch.pixelate.toDouble())
            put("glitch_seed", glitch.seed)

            val col = state.collageState
            put("collage_layout", col.layout.name)
            put("collage_spacing", col.spacingDp.toDouble())
            put("collage_radius", col.cornerRadiusDp.toDouble())
            put("collage_frame_color", col.frameColorLong)
            val cellsArr = JSONArray()
            col.cells.forEach { cell ->
                cellsArr.put(JSONObject().apply {
                    put("id", cell.id)
                    put("uri", cell.imageUri?.toString() ?: "")
                    put("l", cell.leftNorm.toDouble())
                    put("t", cell.topNorm.toDouble())
                    put("r", cell.rightNorm.toDouble())
                    put("b", cell.bottomNorm.toDouble())
                    put("ox", cell.photoOffsetX.toDouble())
                    put("oy", cell.photoOffsetY.toDouble())
                    put("scale", cell.photoScale.toDouble())
                    put("rot", cell.photoRotationDeg)
                    put("fh", cell.isFlippedH)
                    put("fv", cell.isFlippedV)
                })
            }
            put("collage_cells", cellsArr)

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
            put("mask_softness", m.softness.toDouble())
            put("mask_warmth", m.warmth.toDouble())

            val bgGrad = m.blackGradient
            put("mask_bg_grad_enabled", bgGrad.isEnabled)
            put("mask_bg_grad_opacity", bgGrad.opacity.toDouble())
            put("mask_bg_grad_placement", bgGrad.placement.name)
            put("mask_bg_grad_coverage", bgGrad.coverage.toDouble())
        }
    }

    fun readColorMix(c: JSONObject): ColorMixState {
        val cmIsoStr = c.optString("color_mix_isolated", "")
        val cmIso = if (cmIsoStr.isNotBlank()) try { ColorMixChannel.valueOf(cmIsoStr) } catch (_: Exception) { null } else null
        val cmMonoStr = c.optString("color_mix_monochrome", "")
        val cmMono = if (cmMonoStr.isNotBlank()) try { ColorMixChannel.valueOf(cmMonoStr) } catch (_: Exception) { null } else null
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
        return ColorMixState(
            isEnabled = c.optBoolean("color_mix_enabled", false),
            channelAdjustments = cmMap,
            isolatedChannel = cmIso,
            monochromeChannel = cmMono
        )
    }

    fun readCollage(c: JSONObject): CollageState {
        val colLayout = enumOrDefault(c.optString("collage_layout", CollageLayoutType.NONE.name), CollageLayoutType.NONE)
        val cellsArr = c.optJSONArray("collage_cells")
        val cells = mutableListOf<CollageCell>()
        if (cellsArr != null) {
            for (i in 0 until cellsArr.length()) {
                val o = cellsArr.optJSONObject(i) ?: continue
                val uStr = o.optString("uri", "")
                val uri = if (uStr.isNotBlank()) try { Uri.parse(uStr) } catch (_: Exception) { null } else null
                cells.add(
                    CollageCell(
                        id = o.optString("id", "cell_$i"),
                        imageUri = uri,
                        leftNorm = o.optDouble("l", 0.0).toFloat(),
                        topNorm = o.optDouble("t", 0.0).toFloat(),
                        rightNorm = o.optDouble("r", 1.0).toFloat(),
                        bottomNorm = o.optDouble("b", 1.0).toFloat(),
                        photoOffsetX = o.optDouble("ox", 0.0).toFloat(),
                        photoOffsetY = o.optDouble("oy", 0.0).toFloat(),
                        photoScale = o.optDouble("scale", 1.0).toFloat(),
                        photoRotationDeg = o.optInt("rot", 0),
                        isFlippedH = o.optBoolean("fh", false),
                        isFlippedV = o.optBoolean("fv", false)
                    )
                )
            }
        }
        return CollageState(
            layout = colLayout,
            cells = if (cells.isNotEmpty()) cells else CollageLayoutCalculator.generateCellsForLayout(colLayout),
            spacingDp = c.optDouble("collage_spacing", 6.0).toFloat(),
            cornerRadiusDp = c.optDouble("collage_radius", 8.0).toFloat(),
            frameColorLong = c.optLong("collage_frame_color", 0xFF0D0F14)
        )
    }

    fun readMask(c: JSONObject): AtharMaskState = AtharMaskState(
        isEnabled = c.optBoolean("mask_enabled", false),
        activeMaskId = c.optString("mask_id", "").takeIf { it.isNotEmpty() },
        customMaskUri = c.optString("mask_custom_uri", "").takeIf { it.isNotEmpty() },
        opacity = c.optDouble("mask_opacity", 0.75).toFloat(),
        blendMode = enumOrDefault(c.optString("mask_blend", MaskBlendMode.SCREEN.name), MaskBlendMode.SCREEN),
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
        softness = c.optDouble("mask_softness", 0.25).toFloat().coerceIn(0f, 1f),
        warmth = c.optDouble("mask_warmth", 0.35).toFloat().coerceIn(0f, 1f),
        blackGradient = BlackGradientState(
            isEnabled = c.optBoolean("mask_bg_grad_enabled", false),
            opacity = c.optDouble("mask_bg_grad_opacity", 0.60).toFloat(),
            placement = enumOrDefault(c.optString("mask_bg_grad_placement", BlackGradientPlacement.BOTTOM.name), BlackGradientPlacement.BOTTOM),
            coverage = c.optDouble("mask_bg_grad_coverage", 0.55).toFloat()
        )
    )
}
