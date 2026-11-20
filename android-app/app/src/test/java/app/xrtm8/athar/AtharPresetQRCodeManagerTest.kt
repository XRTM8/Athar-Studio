package app.xrtm8.athar

import app.xrtm8.athar.core.AtharFilterRecipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtharPresetQRCodeManagerTest {

    @Test
    fun testRecipeJsonRoundTrip() {
        val original = AtharFilterRecipe(
            name = "Cinematic Gold",
            filmPresetId = "kodak_portra_400",
            brightness = 0.08f,
            contrast = 1.25f,
            saturation = 1.1f,
            vignette = 0.35f,
            blur = 5f,
            scrimEnabled = true,
            scrimDirection = "BOTTOM_TO_TOP",
            scrimColorLong = 0xFF0D0F14,
            scrimOpacity = 0.75f
        )

        val json = original.toJson()
        assertTrue(json.contains("kodak_portra_400"))
        assertTrue(json.contains("Cinematic Gold"))

        val parsed = AtharFilterRecipe.fromJson(json)
        assertNotNull(parsed)
        assertEquals(original.name, parsed!!.name)
        assertEquals(original.filmPresetId, parsed.filmPresetId)
        assertEquals(original.brightness, parsed.brightness, 0.001f)
        assertEquals(original.contrast, parsed.contrast, 0.001f)
        assertEquals(original.saturation, parsed.saturation, 0.001f)
        assertEquals(original.vignette, parsed.vignette, 0.001f)
        assertEquals(original.blur, parsed.blur, 0.001f)
        assertEquals(original.scrimEnabled, parsed.scrimEnabled)
        assertEquals(original.scrimDirection, parsed.scrimDirection)
        assertEquals(original.scrimColorLong, parsed.scrimColorLong)
        assertEquals(original.scrimOpacity, parsed.scrimOpacity, 0.001f)
    }

    @Test
    fun testInvalidJsonReturnsNull() {
        val invalid = "{}"
        val parsed = AtharFilterRecipe.fromJson(invalid)
        assertEquals(null, parsed)
    }
}
