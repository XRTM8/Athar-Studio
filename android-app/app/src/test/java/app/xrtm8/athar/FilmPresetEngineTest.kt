package app.xrtm8.athar

import app.xrtm8.athar.model.FilmPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilmPresetEngineTest {

    @Test
    fun testAllFilmPresetsCompleteness() {
        assertEquals(9, FilmPreset.entries.size)
        val fuji = FilmPreset.entries.filter { it.brand == "Fujifilm" }
        assertEquals(3, fuji.size)
        val kodak = FilmPreset.entries.filter { it.brand == "Kodak" }
        assertEquals(2, kodak.size)
        val leica = FilmPreset.entries.find { it.brand == "Leica" }
        assertNotNull(leica)
    }

    @Test
    fun testFilmPresetDescriptions() {
        FilmPreset.entries.forEach { preset ->
            assertTrue(preset.titleAr.isNotEmpty())
            assertTrue(preset.description.isNotEmpty())
            assertTrue(preset.id.isNotEmpty())
        }
    }
}
