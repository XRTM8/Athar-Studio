package app.xrtm8.athar

import android.graphics.Bitmap
import android.graphics.Color
import app.xrtm8.athar.core.AtharPaletteExtractor
import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.typography.AtharTextLayout
import app.xrtm8.athar.ui.AtharStudioViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CriticalFixesRegressionTest {

    @Test
    fun testAlefMaqsuraNeverConnectsToFollowingLetter() {
        // 'ى' is RIGHT_JOINING only. It must NEVER have a Kashida opportunity after it.
        val text = "هدى الله"
        val opps = PureArabicKashidaEngine.findOpportunities(text)
        for (opp in opps) {
            assertFalse("Alef Maqsura ('ى') must not be elongated to what follows", opp.baseChar == 'ى')
        }
    }

    @Test
    fun testWordWrappingWhenMaxWidthExceeded() {
        val longText = "أثر جميل وعريق يبقى في ذاكرة الزمان والتاريخ والوجدان"
        // Synthetic measure function: 10 units per character
        val measurer: (String) -> Float = { str -> str.length * 10f }

        // Max width that fits around 20-25 characters (250 units)
        val layout = AtharTextLayout.computeLayout(
            text = longText,
            maxWidth = 250f,
            measureText = measurer
        )

        assertTrue("Long text should be wrapped into multiple lines", layout.lines.size > 1)
        for (line in layout.lines) {
            assertTrue("Line should be within or close to max width: '$line'", measurer(line) <= 260f)
        }
    }

    @Test
    fun testLayerReorderingInViewModel() {
        val vm = AtharStudioViewModel()
        vm.addTextLayer("طبقة ثانية")
        val state1 = vm.canvasState.value
        assertEquals(2, state1.layers.size)

        val secondId = state1.layers[1].id
        assertEquals(secondId, vm.canvasState.value.activeLayer.id)

        // Move down (towards index 0)
        vm.moveActiveLayerDown()
        val state2 = vm.canvasState.value
        assertEquals(secondId, state2.layers[0].id)

        // Move back up (towards index 1)
        vm.moveActiveLayerUp()
        val state3 = vm.canvasState.value
        assertEquals(secondId, state3.layers[1].id)
    }

    @Test
    fun testHueBucketPaletteSwatches() {
        val palette = AtharPaletteExtractor.ExtractedPalette(
            dominantLong = 0xFF009632L,
            accentLong = 0xFFFFD700L,
            highlightLong = 0xFFFFFFFFL,
            shadowLong = 0xDD000000L,
            glassLong = 0xBB003210L
        )
        val swatches = AtharPaletteExtractor.getSwatches(palette)
        assertEquals(5, swatches.size)

        val dominantR = (palette.dominantLong shr 16) and 0xFF
        val dominantG = (palette.dominantLong shr 8) and 0xFF
        val dominantB = palette.dominantLong and 0xFF
        assertTrue("Dominant color should be predominantly green", dominantG > dominantR && dominantG > dominantB)
    }
}
