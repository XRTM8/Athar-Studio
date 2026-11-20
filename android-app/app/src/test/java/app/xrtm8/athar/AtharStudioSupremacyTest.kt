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
}
