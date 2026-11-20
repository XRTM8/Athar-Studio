package app.xrtm8.athar

import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ColorSwatch
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.LUXURY_BG_PRESETS
import app.xrtm8.athar.model.LUXURY_SWATCHES
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtharProjectModelTest {

    @Test
    fun testColorSwatchArgbToInt() {
        val goldSwatch = ColorSwatch("ذهب ملكي", 0xFFFFD700)
        assertEquals(-10496, goldSwatch.argb.toInt())
        val composeColor = goldSwatch.composeColor
        assertNotNull(composeColor)
    }

    @Test
    fun testLuxuryBgPresetsCompleteness() {
        assertEquals(6, LUXURY_BG_PRESETS.size)
        val amoled = LUXURY_BG_PRESETS.find { it.name.contains("AMOLED") }
        assertNotNull(amoled)
        assertEquals(0xFF000000L, amoled!!.argb)
    }

    @Test
    fun testCanvasStateAll25FieldsIntegrity() {
        val original = CanvasState(
            rawText = "أثر قرآني",
            displayText = "أثــــر قرآني",
            stretchLevel = 4,
            kashidaMode = KashidaMode.ALL,
            kashidaTargetIndex = 2,
            selectedEffect = VisualEffect.ROYAL_GOLD,
            selectedFont = ArabicFontFamily.ELGHARIB_HAFS_TAWEEL,
            fontSize = 64f,
            customTextColorLong = 0xFFFFD700L,
            strokeWidth = 3.5f,
            strokeColorLong = 0xFFFFFFFFL,
            shadowRadius = 24f,
            shadowDx = -8f,
            shadowDy = 12f,
            shadowColorLong = 0x88000000L,
            textAlign = TextAlignOption.RIGHT,
            lineSpacingMultiplier = 1.45f,
            letterSpacing = 0.08f,
            textOpacity = 0.85f,
            isBold = true,
            isItalic = true,
            isUnderline = true,
            isLocked = true,
            showGridGuides = true,
            aspectRatio = CanvasAspectRatio.SQUARE_1_1,
            solidBgColorLong = 0xFF1C1917L,
            isTransparentBg = false,
            rotationAngle = 15f,
            scaleFactor = 1.25f,
            normOffsetX = 0.05f,
            normOffsetY = -0.10f
        )

        // Simulate Project Manifest serialization
        val json = JSONObject().apply {
            put("raw_text", original.rawText)
            put("display_text", original.displayText)
            put("stretch_level", original.stretchLevel)
            put("kashida_mode", original.kashidaMode.name)
            put("kashida_target_index", original.kashidaTargetIndex)
            put("selected_font", original.selectedFont.name)
            put("selected_effect", original.selectedEffect.name)
            put("font_size", original.fontSize.toDouble())
            put("custom_color", original.customTextColorLong ?: 0L)
            put("stroke_width", original.strokeWidth.toDouble())
            put("stroke_color", original.strokeColorLong)
            put("shadow_radius", original.shadowRadius.toDouble())
            put("shadow_dx", original.shadowDx.toDouble())
            put("shadow_dy", original.shadowDy.toDouble())
            put("shadow_color", original.shadowColorLong)
            put("text_align", original.textAlign.name)
            put("line_spacing", original.lineSpacingMultiplier.toDouble())
            put("letter_spacing", original.letterSpacing.toDouble())
            put("text_opacity", original.textOpacity.toDouble())
            put("is_bold", original.isBold)
            put("is_italic", original.isItalic)
            put("is_underline", original.isUnderline)
            put("is_locked", original.isLocked)
            put("show_grid_guides", original.showGridGuides)
            put("aspect_ratio", original.aspectRatio.name)
            put("solid_bg_color", original.solidBgColorLong ?: 0L)
            put("rotation_angle", original.rotationAngle.toDouble())
            put("scale_factor", original.scaleFactor.toDouble())
            put("norm_offset_x", original.normOffsetX.toDouble())
            put("norm_offset_y", original.normOffsetY.toDouble())
            put("is_transparent_bg", original.isTransparentBg)
        }

        // Deserialize back
        val customColor = json.optLong("custom_color", 0L)
        val solidBg = json.optLong("solid_bg_color", 0L)

        val restored = CanvasState(
            rawText = json.optString("raw_text"),
            displayText = json.optString("display_text"),
            stretchLevel = json.optInt("stretch_level"),
            kashidaMode = KashidaMode.valueOf(json.optString("kashida_mode", KashidaMode.SMART.name)),
            kashidaTargetIndex = json.optInt("kashida_target_index", -1),
            selectedFont = ArabicFontFamily.valueOf(json.optString("selected_font")),
            selectedEffect = VisualEffect.valueOf(json.optString("selected_effect")),
            fontSize = json.optDouble("font_size").toFloat(),
            customTextColorLong = if (customColor != 0L) customColor else null,
            strokeWidth = json.optDouble("stroke_width").toFloat(),
            strokeColorLong = json.optLong("stroke_color"),
            shadowRadius = json.optDouble("shadow_radius").toFloat(),
            shadowDx = json.optDouble("shadow_dx").toFloat(),
            shadowDy = json.optDouble("shadow_dy").toFloat(),
            shadowColorLong = json.optLong("shadow_color"),
            textAlign = TextAlignOption.valueOf(json.optString("text_align")),
            lineSpacingMultiplier = json.optDouble("line_spacing").toFloat(),
            letterSpacing = json.optDouble("letter_spacing").toFloat(),
            textOpacity = json.optDouble("text_opacity").toFloat(),
            isBold = json.optBoolean("is_bold"),
            isItalic = json.optBoolean("is_italic"),
            isUnderline = json.optBoolean("is_underline"),
            isLocked = json.optBoolean("is_locked"),
            showGridGuides = json.optBoolean("show_grid_guides"),
            aspectRatio = CanvasAspectRatio.valueOf(json.optString("aspect_ratio")),
            solidBgColorLong = if (solidBg != 0L) solidBg else null,
            rotationAngle = json.optDouble("rotation_angle").toFloat(),
            scaleFactor = json.optDouble("scale_factor").toFloat(),
            normOffsetX = json.optDouble("norm_offset_x").toFloat(),
            normOffsetY = json.optDouble("norm_offset_y").toFloat(),
            isTransparentBg = json.optBoolean("is_transparent_bg")
        )

        assertEquals(original.rawText, restored.rawText)
        assertEquals(original.stretchLevel, restored.stretchLevel)
        assertEquals(original.kashidaMode, restored.kashidaMode)
        assertEquals(original.kashidaTargetIndex, restored.kashidaTargetIndex)
        assertEquals(original.selectedFont, restored.selectedFont)
        assertEquals(original.selectedEffect, restored.selectedEffect)
        assertEquals(original.strokeWidth, restored.strokeWidth, 0.01f)
        assertEquals(original.shadowDx, restored.shadowDx, 0.01f)
        assertEquals(original.shadowDy, restored.shadowDy, 0.01f)
        assertEquals(original.shadowRadius, restored.shadowRadius, 0.01f)
        assertEquals(original.letterSpacing, restored.letterSpacing, 0.01f)
        assertEquals(original.textOpacity, restored.textOpacity, 0.01f)
        assertEquals(original.isBold, restored.isBold)
        assertEquals(original.isItalic, restored.isItalic)
        assertEquals(original.isUnderline, restored.isUnderline)
        assertEquals(original.isLocked, restored.isLocked)
        assertEquals(original.showGridGuides, restored.showGridGuides)
        assertEquals(original.aspectRatio, restored.aspectRatio)
        assertEquals(original.solidBgColorLong, restored.solidBgColorLong)
        assertEquals(original.rotationAngle, restored.rotationAngle, 0.01f)
        assertEquals(original.scaleFactor, restored.scaleFactor, 0.01f)
        assertEquals(original.normOffsetX, restored.normOffsetX, 0.01f)
        assertEquals(original.normOffsetY, restored.normOffsetY, 0.01f)
        assertEquals(original.isTransparentBg, restored.isTransparentBg)
    }
}
