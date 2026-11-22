package app.xrtm8.athar

import app.xrtm8.athar.model.*
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.*
import org.junit.Test

class AtharMaskSystemTest {

    @Test
    fun testAtharMaskCatalogIntegrity() {
        val masks = AtharMaskCatalog.ALL_MASKS
        assertEquals(12, masks.size)

        val uniqueIds = masks.map { it.id }.toSet()
        assertEquals("Mask IDs must all be unique", 12, uniqueIds.size)

        masks.forEach { mask ->
            assertTrue("Mask title must not be empty", mask.title.isNotEmpty())
            assertTrue("Asset file must start with masks/", mask.assetFileName.startsWith("masks/"))
            assertTrue("Asset file must end with .webp", mask.assetFileName.endsWith(".webp"))
            assertTrue("Thumb file must start with masks/thumbs/", mask.thumbFileName.startsWith("masks/thumbs/"))
            assertTrue("Default opacity must be in valid range", mask.defaultOpacity in 0.1f..1.0f)
            assertNotNull("Must find by ID", AtharMaskCatalog.getById(mask.id))
        }

        assertNotNull(AtharMaskCatalog.getById("solar_dust"))
        assertNotNull(AtharMaskCatalog.getById("prism_flares"))
        assertNotNull(AtharMaskCatalog.getById("sunset_leak"))
        assertNotNull(AtharMaskCatalog.getById("antique_linen"))
        assertNotNull(AtharMaskCatalog.getById("dark_mesh"))
        assertNotNull(AtharMaskCatalog.getById("chroma_dream"))
        assertNotNull(AtharMaskCatalog.getById("cyan_burn"))
        assertNotNull(AtharMaskCatalog.getById("crt_scanlines"))
        assertNotNull(AtharMaskCatalog.getById("window_shadow"))
        assertNotNull(AtharMaskCatalog.getById("halftone_wave"))
        assertNotNull(AtharMaskCatalog.getById("fluted_glass"))
        assertNotNull(AtharMaskCatalog.getById("folded_poster"))
    }

    @Test
    fun testAtharMaskStateDefaults() {
        val defaultState = AtharMaskState()
        assertFalse(defaultState.isEnabled)
        assertNull(defaultState.activeMaskId)
        assertFalse(defaultState.isMaskActive)
        assertFalse(defaultState.isEffectActive)
        assertEquals(0.75f, defaultState.opacity, 0.001f)
        assertEquals(0f, defaultState.hueShift, 0.001f)
        assertEquals(0f, defaultState.offsetX, 0.001f)
        assertEquals(0f, defaultState.offsetY, 0.001f)
        assertEquals(1.0f, defaultState.scale, 0.001f)
        assertFalse(defaultState.isFlippedH)
        assertFalse(defaultState.isFlippedV)
        assertEquals(0, defaultState.rotationDegrees)
        assertFalse(defaultState.blackGradient.isEnabled)
        assertFalse(defaultState.blackGradient.isEffectActive)
    }

    @Test
    fun testAtharMaskStateSerializationRoundtrip() {
        val originalMask = AtharMaskState(
            isEnabled = true,
            activeMaskId = "window_shadow",
            opacity = 0.85f,
            blendMode = MaskBlendMode.MULTIPLY,
            hueShift = 45f,
            offsetX = 0.25f,
            offsetY = -0.15f,
            scale = 1.35f,
            isFlippedH = true,
            isFlippedV = false,
            rotationDegrees = 90,
            blackGradient = BlackGradientState(
                isEnabled = true,
                opacity = 0.70f,
                placement = BlackGradientPlacement.BOTTOM,
                coverage = 0.65f
            )
        )

        val canvasState = CanvasState(
            maskState = originalMask
        )

        val json = AtharProjectSerializer.serializeCanvasState(canvasState, hasBackgroundPhoto = false)
        val deserialized = AtharProjectSerializer.deserializeCanvasState(json)

        val restored = deserialized.maskState
        assertTrue(restored.isEnabled)
        assertEquals("window_shadow", restored.activeMaskId)
        assertEquals(0.85f, restored.opacity, 0.01f)
        assertEquals(MaskBlendMode.MULTIPLY, restored.blendMode)
        assertEquals(45f, restored.hueShift, 0.01f)
        assertEquals(0.25f, restored.offsetX, 0.01f)
        assertEquals(-0.15f, restored.offsetY, 0.01f)
        assertEquals(1.35f, restored.scale, 0.01f)
        assertTrue(restored.isFlippedH)
        assertFalse(restored.isFlippedV)
        assertEquals(90, restored.rotationDegrees)

        assertTrue(restored.blackGradient.isEnabled)
        assertEquals(0.70f, restored.blackGradient.opacity, 0.01f)
        assertEquals(BlackGradientPlacement.BOTTOM, restored.blackGradient.placement)
        assertEquals(0.65f, restored.blackGradient.coverage, 0.01f)
        assertTrue(restored.isEffectActive)
    }

    @Test
    fun testBlackGradientPlacements() {
        assertEquals("أسفل (Bottom)", BlackGradientPlacement.BOTTOM.label)
        assertEquals("أعلى (Top)", BlackGradientPlacement.TOP.label)
        assertEquals("يسار (Left)", BlackGradientPlacement.LEFT.label)
        assertEquals("يمين (Right)", BlackGradientPlacement.RIGHT.label)
        assertEquals("دائري (Radial)", BlackGradientPlacement.RADIAL.label)
        assertEquals("شريطين (Cinematic Bars)", BlackGradientPlacement.VERTICAL_SPLIT.label)
    }
}
