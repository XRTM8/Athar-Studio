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
    fun stretchAndUndoKeepEachLayersOwnText() {
        val vm = AtharStudioViewModel()
        vm.createNewProject(initialText = "أثر")
        val first = vm.canvasState.value.activeLayer
        assertEquals("أثـــر", first.displayText)
        vm.addTextLayer("مجد")
        val second = vm.canvasState.value.activeLayer
        assertEquals("مجـــد", second.displayText)
        vm.setStretchLevel(2)
        vm.setStretchLevel(6)
        vm.commitHistoryCheckpoint()
        vm.selectLayer(first.id)
        assertEquals(first, vm.canvasState.value.activeLayer)
        assertEquals("مجــــــد", vm.canvasState.value.layers.last().displayText)
        vm.undo()
        assertEquals(second, vm.canvasState.value.activeLayer)
        vm.redo()
        assertEquals("مجــــــد", vm.canvasState.value.layers.last().displayText)
    }

    @Test
    fun existingTatweelAndArabicMarksPreserveTheJoin() {
        val text = "أثَـر"
        assertEquals("أثَـــر", PureArabicKashidaEngine.stretchText(text, 2))
        assertEquals(text, PureArabicKashidaEngine.stretchText(text, 0))
        assertEquals("ب\u0656ــت", PureArabicKashidaEngine.stretchText("ب\u0656ت", 2))
    }


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

    @Test
    fun testLongArabicTextParagraphWrapping() {
        val paragraph = "بسم الله الرحمن الرحيم والصلاة والسلام على رسول الله الكريم محمد بن عبد الله"
        val measurer: (String) -> Float = { str -> str.length * 12f }
        val layout = AtharTextLayout.computeLayout(
            text = paragraph,
            maxWidth = 300f,
            measureText = measurer
        )
        assertTrue("Paragraph must wrap into 3 or more lines", layout.lines.size >= 3)
        for (line in layout.lines) {
            assertTrue("No line should exceed max allowed boundary plus word grace", measurer(line) <= 320f)
        }
    }

    @Test
    fun testViewModelBoxWidthRatioClamp() {
        val vm = AtharStudioViewModel()
        vm.setBoxWidthRatio(0.1f) // below min 0.3f
        assertEquals(0.30f, vm.canvasState.value.activeLayer.boxWidthRatio, 0.001f)

        vm.setBoxWidthRatio(1.5f) // above max 1.0f
        assertEquals(1.0f, vm.canvasState.value.activeLayer.boxWidthRatio, 0.001f)

        vm.setBoxWidthRatio(0.75f)
        assertEquals(0.75f, vm.canvasState.value.activeLayer.boxWidthRatio, 0.001f)
    }
}
