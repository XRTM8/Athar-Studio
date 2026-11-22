package app.xrtm8.athar

import app.xrtm8.athar.model.*
import app.xrtm8.athar.project.AtharProjectSerializer
import app.xrtm8.athar.ui.components.STUDIO_DOCK_TABS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtharStudioSupremacyTest {

    @Test
    fun testStudioDockFourCoreTabs() {
        assertEquals("Studio Dock must be streamlined to exactly 4 tools", 4, STUDIO_DOCK_TABS.size)
        assertEquals("الخط والنص", STUDIO_DOCK_TABS[0].title)
        assertEquals("المظهر", STUDIO_DOCK_TABS[1].title)
        assertEquals("الخلفية", STUDIO_DOCK_TABS[2].title)
        assertEquals("الموضع", STUDIO_DOCK_TABS[3].title)
    }

    @Test
    fun testBlendModesAndCurvedTextSerializationRoundTrip() {
        val layer = TextLayer(
            id = "layer_curved",
            name = "نص مقوس",
            rawText = "قُلْ هُوَ اللَّهُ أَحَدٌ",
            displayText = "قُلْ هُوَ اللَّهُ أَحَدٌ",
            blendMode = LayerBlendMode.SCREEN,
            isCurved = true,
            curveAngle = 65f
        )

        val state = CanvasState(
            layers = listOf(layer),
            activeLayerId = "layer_curved",
            aspectRatio = CanvasAspectRatio.LANDSCAPE_16_9
        )

        val json = AtharProjectSerializer.serializeCanvasState(state, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        assertEquals(CanvasAspectRatio.LANDSCAPE_16_9, restored.aspectRatio)

        assertEquals(1, restored.layers.size)
        val restoredLayer = restored.layers.first()
        assertEquals(LayerBlendMode.SCREEN, restoredLayer.blendMode)
        assertTrue(restoredLayer.isCurved)
        assertEquals(65f, restoredLayer.curveAngle, 0.001f)
    }

    @Test
    fun testAllAspectRatiosHaveValidRatioCalculations() {
        assertEquals(9f / 16f, CanvasAspectRatio.STORY_9_16.ratio!!, 0.001f)
        assertEquals(1f, CanvasAspectRatio.SQUARE_1_1.ratio!!, 0.001f)
        assertEquals(4f / 5f, CanvasAspectRatio.PORTRAIT_4_5.ratio!!, 0.001f)
        assertEquals(16f / 9f, CanvasAspectRatio.LANDSCAPE_16_9.ratio!!, 0.001f)
        assertEquals(9f / 19.5f, CanvasAspectRatio.WALLPAPER_9_19_5.ratio!!, 0.001f)
        assertEquals(null, CanvasAspectRatio.FREE.ratio)
    }

    @Test
    fun testLayerBlendModes() {
        assertEquals(6, LayerBlendMode.values().size)
        assertEquals(android.graphics.PorterDuff.Mode.SRC_OVER, LayerBlendMode.NORMAL.porterDuffMode)
        assertEquals(android.graphics.PorterDuff.Mode.MULTIPLY, LayerBlendMode.MULTIPLY.porterDuffMode)
        assertEquals(android.graphics.PorterDuff.Mode.SCREEN, LayerBlendMode.SCREEN.porterDuffMode)
        assertEquals(android.graphics.PorterDuff.Mode.OVERLAY, LayerBlendMode.OVERLAY.porterDuffMode)
        assertEquals(android.graphics.PorterDuff.Mode.LIGHTEN, LayerBlendMode.LIGHTEN.porterDuffMode)
        assertEquals(android.graphics.PorterDuff.Mode.DARKEN, LayerBlendMode.DARKEN.porterDuffMode)
    }

    @Test
    fun testAtharPaletteExtractorSwatches() {
        val palette = app.xrtm8.athar.core.AtharPaletteExtractor.ExtractedPalette(
            dominantLong = 0xFF121418,
            accentLong = 0xFFFFD700,
            highlightLong = 0xFFFFFFFF,
            shadowLong = 0xAA000000,
            glassLong = 0x88202020
        )
        val swatches = app.xrtm8.athar.core.AtharPaletteExtractor.getSwatches(palette)
        assertEquals(5, swatches.size)
    }

    @Test
    fun testArabicParagraphBalancing() {
        val input = "أَثَرٌ جَمِيلٌ يَبْقَى\nفِي قُلُوبِ النَّاسِ"
        val balanced = app.xrtm8.athar.typography.ArabicParagraphJustifier.balanceArabicBlock(input)
        val lines = balanced.split("\n")
        assertEquals(2, lines.size)
        assertTrue("Balanced text should be non-empty", balanced.isNotEmpty())
    }

    @Test
    fun testAtharPureEngineArchitecture() {
        val opportunities = app.xrtm8.athar.core.PureArabicKashidaEngine.findOpportunities("بسم الله الرحمن الرحيم")
        assertTrue("Should detect calligraphic kashida opportunities", opportunities.isNotEmpty())
    }

    @Test
    fun testAtharTextLayoutCache() {
        val r1 = app.xrtm8.athar.typography.AtharTextLayout.computeLayout(
            text = "خط النسخ العربي",
            lineSpacingMultiplier = 1.2f,
            textAlign = TextAlignOption.CENTER,
            fallbackTextSize = 32f,
            measureText = { it.length * 15f }
        )
        val r2 = app.xrtm8.athar.typography.AtharTextLayout.computeLayout(
            text = "خط النسخ العربي",
            lineSpacingMultiplier = 1.2f,
            textAlign = TextAlignOption.CENTER,
            fallbackTextSize = 32f,
            measureText = { it.length * 15f }
        )
        // Check cache identity
        org.junit.Assert.assertSame(r1, r2)
        assertEquals(1, r1.lines.size)
    }

    @Test
    fun testSelectBestApkAssetArm64Prioritization() {
        val assets = listOf(
            app.xrtm8.athar.updater.AtharUpdateManager.Companion.ReleaseAsset("athar-v2.0.1-universal.apk", "https://download/universal"),
            app.xrtm8.athar.updater.AtharUpdateManager.Companion.ReleaseAsset("athar-v2.0.1-arm64.apk", "https://download/arm64")
        )
        // 1. On ARM64-v8a device, should select arm64 APK
        val chosenArm64 = app.xrtm8.athar.updater.AtharUpdateManager.selectBestApkAsset(
            assets,
            deviceAbis = arrayOf("arm64-v8a")
        )
        assertEquals("https://download/arm64", chosenArm64)

        // 2. On x86_64 device without x86_64 APK, should fallback to universal
        val chosenFallback = app.xrtm8.athar.updater.AtharUpdateManager.selectBestApkAsset(
            assets,
            deviceAbis = arrayOf("x86_64")
        )
        assertEquals("https://download/universal", chosenFallback)

        // 3. Single asset list returns that asset
        val single = listOf(app.xrtm8.athar.updater.AtharUpdateManager.Companion.ReleaseAsset("athar-latest.apk", "https://download/single"))
        assertEquals("https://download/single", app.xrtm8.athar.updater.AtharUpdateManager.selectBestApkAsset(single))
    }

    @Test
    fun testColorGradingEngineMath() {
        val array = app.xrtm8.athar.core.AtharColorGradingEngine.computeColorGradingArray(
            liftR = 0.2f, liftG = -0.1f, liftB = 0f,
            gammaR = 0.1f, gammaG = 0f, gammaB = -0.1f,
            gainR = 0.3f, gainG = 0f, gainB = -0.2f
        )
        assertEquals(20, array.size)
        // Check scale and offset for Red channel
        assertTrue("Red scale should be greater than 1.0 due to positive gain and gamma", array[0] > 1.0f)
        assertTrue("Red offset should be positive due to positive lift", array[4] > 0f)
        // Check Alpha channel identity preserved
        assertEquals(1f, array[18], 0.001f)
    }

    @Test
    fun testLuxuryGradientPresetsValidity() {
        LuxuryGradientPreset.entries.forEach { preset ->
            if (preset != LuxuryGradientPreset.NONE) {
                assertTrue("Preset ${preset.name} should have at least 2 colors", preset.colors.size >= 2)
                assertEquals("Preset ${preset.name} colors and stops must match in size", preset.colors.size, preset.stops.size)
                assertEquals(0.0f, preset.stops.first(), 0.001f)
                assertEquals(1.0f, preset.stops.last(), 0.001f)
            }
        }
    }

    @Test
    fun testV210PillarsSerializationRoundTrip() {
        val layer = TextLayer(
            id = "layer_v210",
            name = "نص ملكي",
            rawText = "أَثَــر الخُلود",
            displayText = "أَثَـــر الخُلود",
            gradientPreset = LuxuryGradientPreset.ANDALUSIAN_GOLD,
            gradientAngle = 45f,
            isRadialGradient = true,
            isHollowText = true
        )

        val state = CanvasState(
            layers = listOf(layer),
            activeLayerId = "layer_v210",
            colorGradingEnabled = true,
            liftRed = 0.15f,
            liftGreen = -0.05f,
            liftBlue = 0.02f,
            gammaRed = 0.10f,
            gammaGreen = 0.04f,
            gammaBlue = -0.08f,
            gainRed = 0.25f,
            gainGreen = 0.12f,
            gainBlue = -0.15f
        )

        val json = AtharProjectSerializer.serializeCanvasState(state, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        val rLayer = restored.activeLayer
        assertEquals(LuxuryGradientPreset.ANDALUSIAN_GOLD, rLayer.gradientPreset)
        assertEquals(45f, rLayer.gradientAngle, 0.001f)
        assertTrue(rLayer.isRadialGradient)
        assertTrue(rLayer.isHollowText)

        assertTrue(restored.colorGradingEnabled)
        assertEquals(0.15f, restored.liftRed, 0.001f)
        assertEquals(0.25f, restored.gainRed, 0.001f)
    }

    @Test
    fun testV210BackwardCompatibilityOldJson() {
        // Minimal JSON from early v1.0 / v2.0
        val legacyJson = org.json.JSONObject().apply {
            put("raw_text", "تصميم قديم")
            put("display_text", "تصميم قديم")
            put("font_size", 48.0)
        }
        val restored = AtharProjectSerializer.deserializeCanvasState(legacyJson)
        val rLayer = restored.activeLayer

        assertEquals("تصميم قديم", rLayer.rawText)
        assertEquals(LuxuryGradientPreset.NONE, rLayer.gradientPreset)
        assertFalse(rLayer.isHollowText)
        assertFalse(restored.colorGradingEnabled)
    }
}
