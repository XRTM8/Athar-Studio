package app.xrtm8.athar

import app.xrtm8.athar.core.FreeTashkeelEngine
import app.xrtm8.athar.core.IslamicManuscriptEngine
import app.xrtm8.athar.model.*
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtharStudioSupremacyTest {

    @Test
    fun testFreeTashkeelEngineOperations() {
        var layer = TextLayer(rawText = "أثر", displayText = "أثر")
        assertEquals(0, layer.tashkeelItems.size)

        // 1. Add Fatha
        layer = FreeTashkeelEngine.addMark(layer, "َ")
        assertEquals(1, layer.tashkeelItems.size)
        val fatha = layer.tashkeelItems.first()
        assertEquals("َ", fatha.symbol)

        // 2. Add Shadda
        layer = FreeTashkeelEngine.addMark(layer, "ّ")
        assertEquals(2, layer.tashkeelItems.size)

        // 3. Nudge Fatha
        layer = FreeTashkeelEngine.nudgeMark(layer, fatha.id, 0.1f, -0.05f)
        val nudgedFatha = layer.tashkeelItems.find { it.id == fatha.id }!!
        assertEquals(0.1f, nudgedFatha.normX, 0.001f)
        assertEquals(-0.40f, nudgedFatha.normY, 0.001f)

        // 4. Update Scale & Color
        layer = FreeTashkeelEngine.updateMark(layer, nudgedFatha.copy(scale = 1.5f, colorLong = 0xFFFFD700))
        val updatedFatha = layer.tashkeelItems.find { it.id == fatha.id }!!
        assertEquals(1.5f, updatedFatha.scale, 0.001f)
        assertEquals(0xFFFFD700, updatedFatha.colorLong)

        // 5. Remove Mark
        layer = FreeTashkeelEngine.removeMark(layer, fatha.id)
        assertEquals(1, layer.tashkeelItems.size)
        assertEquals("ّ", layer.tashkeelItems.first().symbol)
    }

    @Test
    fun testIslamicManuscriptEngine() {
        val manuscripts = IslamicManuscriptEngine.MANUSCRIPTS
        assertTrue("Manuscript library should contain curated entries", manuscripts.size >= 15)

        // Verify categories exist
        val categories = IslamicManuscriptEngine.ManuscriptCategory.values()
        categories.forEach { cat ->
            val inCat = manuscripts.filter { it.category == cat }
            assertTrue("Category ${cat.name} should have entries", inCat.isNotEmpty())
        }

        // Test layer generation from entry
        val bismillah = manuscripts.find { it.id == "bismillah_ruqaa" }!!
        val layer = IslamicManuscriptEngine.createLayerFromManuscript(bismillah)

        assertEquals(bismillah.fullText, layer.rawText)
        assertEquals(bismillah.recommendedFont, layer.selectedFont)
        assertEquals(bismillah.recommendedEffect, layer.selectedEffect)
        assertTrue(layer.displayText.isNotEmpty())
    }

    @Test
    fun testBlendModesAndCurvedTextSerializationRoundTrip() {
        val tItem = FreeTashkeelEngine.createMark("ُ", normX = 0.05f, normY = -0.38f, scale = 1.25f, colorLong = 0xFFFFD700)
        val layer = TextLayer(
            id = "layer_curved",
            name = "نص مقوس",
            rawText = "قُلْ هُوَ اللَّهُ أَحَدٌ",
            displayText = "قُلْ هُوَ اللَّهُ أَحَدٌ",
            blendMode = LayerBlendMode.SCREEN,
            isCurved = true,
            curveAngle = 65f,
            isBehindSubject = true,
            tashkeelItems = listOf(tItem)
        )

        val state = CanvasState(
            layers = listOf(layer),
            activeLayerId = "layer_curved",
            aspectRatio = CanvasAspectRatio.LANDSCAPE_16_9,
            isTextBehindSubject = true,
            subjectDepthSensitivity = 0.65f,
            filmGrainIntensity = 0.35f,
            lensFlareEnabled = true,
            lensFlareX = 0.8f,
            lensFlareY = 0.15f,
            lensFlareIntensity = 0.9f,
            lensFlareColorLong = 0xFF80D8FF
        )

        val json = AtharProjectSerializer.serializeCanvasState(state, false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)

        assertEquals(CanvasAspectRatio.LANDSCAPE_16_9, restored.aspectRatio)
        assertTrue(restored.isTextBehindSubject)
        assertEquals(0.65f, restored.subjectDepthSensitivity, 0.001f)
        assertEquals(0.35f, restored.filmGrainIntensity, 0.001f)
        assertTrue(restored.lensFlareEnabled)
        assertEquals(0.8f, restored.lensFlareX, 0.001f)
        assertEquals(0.15f, restored.lensFlareY, 0.001f)
        assertEquals(0.9f, restored.lensFlareIntensity, 0.001f)
        assertEquals(0xFF80D8FF, restored.lensFlareColorLong)

        assertEquals(1, restored.layers.size)
        val restoredLayer = restored.layers.first()
        assertEquals(LayerBlendMode.SCREEN, restoredLayer.blendMode)
        assertTrue(restoredLayer.isCurved)
        assertEquals(65f, restoredLayer.curveAngle, 0.001f)
        assertTrue(restoredLayer.isBehindSubject)

        assertEquals(1, restoredLayer.tashkeelItems.size)
        val restoredT = restoredLayer.tashkeelItems.first()
        assertEquals("ُ", restoredT.symbol)
        assertEquals(0.05f, restoredT.normX, 0.001f)
        assertEquals(-0.38f, restoredT.normY, 0.001f)
        assertEquals(1.25f, restoredT.scale, 0.001f)
        assertEquals(0xFFFFD700, restoredT.colorLong)
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
    fun testSubjectDepthEngineCache() {
        app.xrtm8.athar.core.SubjectDepthEngine.clearCache()
    }

    @Test
    fun testSmartTypographyBalancingEngine() {
        val input = "أَثَرٌ جَمِيلٌ يَبْقَى\nفِي قُلُوبِ النَّاسِ"
        val balanced = app.xrtm8.athar.core.SmartTypographyBalancingEngine.balanceArabicBlock(input)
        val lines = balanced.split("\n")
        assertEquals(2, lines.size)
        assertTrue("Balanced text should be non-empty", balanced.isNotEmpty())
    }

    @Test
    fun testAtharPureEngineArchitecture() {
        val ready = app.xrtm8.athar.core.AtharNativeEngine.isNativeReady()
        assertTrue("Pure Kotlin engine should always be ready", ready)
        assertEquals("2.0.0-pure", app.xrtm8.athar.core.AtharNativeEngine.getEngineVersion())
        val opportunities = app.xrtm8.athar.core.AtharNativeEngine.findOpportunities("بسم الله الرحمن الرحيم")
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
            texturePreset = TextureMaskPreset.GOLD_LEAF,
            textureScale = 1.75f,
            textureOffsetX = 12f,
            textureOffsetY = -8f,
            gradientPreset = LuxuryGradientPreset.ANDALUSIAN_GOLD,
            gradientAngle = 45f,
            isRadialGradient = true,
            isHollowText = true,
            retroExtrusionDepth = 18f,
            retroExtrusionAngle = 135f,
            retroExtrusionColorLong = 0xFF3E2723
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
        assertEquals(TextureMaskPreset.GOLD_LEAF, rLayer.texturePreset)
        assertEquals(1.75f, rLayer.textureScale, 0.001f)
        assertEquals(12f, rLayer.textureOffsetX, 0.001f)
        assertEquals(-8f, rLayer.textureOffsetY, 0.001f)
        assertEquals(LuxuryGradientPreset.ANDALUSIAN_GOLD, rLayer.gradientPreset)
        assertEquals(45f, rLayer.gradientAngle, 0.001f)
        assertTrue(rLayer.isRadialGradient)
        assertTrue(rLayer.isHollowText)
        assertEquals(18f, rLayer.retroExtrusionDepth, 0.001f)
        assertEquals(135f, rLayer.retroExtrusionAngle, 0.001f)
        assertEquals(0xFF3E2723, rLayer.retroExtrusionColorLong)

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
        assertEquals(TextureMaskPreset.NONE, rLayer.texturePreset)
        assertEquals(LuxuryGradientPreset.NONE, rLayer.gradientPreset)
        assertFalse(rLayer.isHollowText)
        assertEquals(0f, rLayer.retroExtrusionDepth, 0.001f)
        assertFalse(restored.colorGradingEnabled)
    }
}
