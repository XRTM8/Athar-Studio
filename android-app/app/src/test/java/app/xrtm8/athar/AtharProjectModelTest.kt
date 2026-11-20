package app.xrtm8.athar

import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ColorSwatch
import app.xrtm8.athar.model.GLASS_BACKDROP_PRESETS
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.LUXURY_BG_PRESETS
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertEquals
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
    fun testGlassBackdropPresetsCompleteness() {
        assertEquals(5, GLASS_BACKDROP_PRESETS.size)
        val darkGlass = GLASS_BACKDROP_PRESETS.find { it.name.contains("ليلي") }
        assertNotNull(darkGlass)
        assertTrue(darkGlass!!.opacity > 0.5f)
    }


    @Test
    fun testCanvasStateSerializationWithPhotoAndGlassBackdrop() {
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
            backgroundDim = 0.40f,
            backgroundBlur = 18f,
            backgroundBrightness = 0.15f,
            backgroundContrast = 1.25f,
            backgroundSaturation = 1.10f,
            backgroundVignette = 0.45f,
            enableGlassBackdrop = true,
            glassBackdropOpacity = 0.70f,
            glassBackdropColorLong = 0xFF0D0F14L,
            glassBackdropBorderColorLong = 0x55E5C07BL,
            glassBackdropCornerRadius = 26f,
            glassBackdropPadding = 28f,
            rotationAngle = 15f,
            scaleFactor = 1.25f,
            normOffsetX = 0.05f,
            normOffsetY = -0.10f
        )

        val json = AtharProjectSerializer.serializeCanvasState(original, hasBackgroundPhoto = true)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

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
        assertEquals(original.backgroundDim, restored.backgroundDim, 0.01f)
        assertEquals(original.backgroundBlur, restored.backgroundBlur, 0.01f)
        assertEquals(original.backgroundBrightness, restored.backgroundBrightness, 0.01f)
        assertEquals(original.backgroundContrast, restored.backgroundContrast, 0.01f)
        assertEquals(original.backgroundSaturation, restored.backgroundSaturation, 0.01f)
        assertEquals(original.backgroundVignette, restored.backgroundVignette, 0.01f)
        assertEquals(original.enableGlassBackdrop, restored.enableGlassBackdrop)
        assertEquals(original.glassBackdropOpacity, restored.glassBackdropOpacity, 0.01f)
        assertEquals(original.glassBackdropColorLong, restored.glassBackdropColorLong)
        assertEquals(original.glassBackdropBorderColorLong, restored.glassBackdropBorderColorLong)
        assertEquals(original.glassBackdropCornerRadius, restored.glassBackdropCornerRadius, 0.01f)
        assertEquals(original.glassBackdropPadding, restored.glassBackdropPadding, 0.01f)
        assertEquals(original.rotationAngle, restored.rotationAngle, 0.01f)
        assertEquals(original.scaleFactor, restored.scaleFactor, 0.01f)
        assertEquals(original.normOffsetX, restored.normOffsetX, 0.01f)
        assertEquals(original.normOffsetY, restored.normOffsetY, 0.01f)
        assertEquals(original.isTransparentBg, restored.isTransparentBg)
    }
}
