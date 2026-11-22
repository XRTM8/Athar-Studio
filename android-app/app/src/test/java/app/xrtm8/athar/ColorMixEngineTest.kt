package app.xrtm8.athar

import app.xrtm8.athar.model.ChannelAdjustment
import app.xrtm8.athar.model.ColorMixChannel
import app.xrtm8.athar.model.ColorMixState
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.*
import org.junit.Test

class ColorMixEngineTest {

    @Test
    fun testColorMixChannelFromHueMapping() {
        assertEquals(ColorMixChannel.RED, ColorMixChannel.fromHue(0f))
        assertEquals(ColorMixChannel.RED, ColorMixChannel.fromHue(10f))
        assertEquals(ColorMixChannel.RED, ColorMixChannel.fromHue(355f))
        assertEquals(ColorMixChannel.ORANGE, ColorMixChannel.fromHue(30f))
        assertEquals(ColorMixChannel.YELLOW, ColorMixChannel.fromHue(60f))
        assertEquals(ColorMixChannel.GREEN, ColorMixChannel.fromHue(120f))
        assertEquals(ColorMixChannel.CYAN, ColorMixChannel.fromHue(180f))
        assertEquals(ColorMixChannel.BLUE, ColorMixChannel.fromHue(240f))
        assertEquals(ColorMixChannel.PURPLE, ColorMixChannel.fromHue(285f))
        assertEquals(ColorMixChannel.MAGENTA, ColorMixChannel.fromHue(325f))
    }

    @Test
    fun testColorMixStateSerializationAndRestoration() {
        val originalState = app.xrtm8.athar.model.CanvasState(
            colorMixState = ColorMixState(
                isEnabled = true,
                channelAdjustments = mapOf(
                    ColorMixChannel.GREEN to ChannelAdjustment(hueShift = 15f, satShift = 30f, lumShift = -10f),
                    ColorMixChannel.BLUE to ChannelAdjustment(hueShift = -5f, satShift = 50f, lumShift = 20f)
                ),
                isolatedChannel = ColorMixChannel.RED,
                monochromeChannel = null
            )
        )

        val json = AtharProjectSerializer.serializeCanvasState(originalState, hasBackgroundPhoto = true)
        val deserialized = AtharProjectSerializer.deserializeCanvasState(json, backgroundUri = null)

        assertTrue(deserialized.colorMixState.isEnabled)
        assertEquals(ColorMixChannel.RED, deserialized.colorMixState.isolatedChannel)
        assertNull(deserialized.colorMixState.monochromeChannel)

        val greenAdj = deserialized.colorMixState.getAdjustment(ColorMixChannel.GREEN)
        assertEquals(15f, greenAdj.hueShift, 0.01f)
        assertEquals(30f, greenAdj.satShift, 0.01f)
        assertEquals(-10f, greenAdj.lumShift, 0.01f)

        val blueAdj = deserialized.colorMixState.getAdjustment(ColorMixChannel.BLUE)
        assertEquals(-5f, blueAdj.hueShift, 0.01f)
        assertEquals(50f, blueAdj.satShift, 0.01f)
        assertEquals(20f, blueAdj.lumShift, 0.01f)
    }

    @Test
    fun testDefaultColorMixStateBehavior() {
        val defaultState = ColorMixState()
        assertTrue(defaultState.isDefault)
        assertFalse(defaultState.isEnabled)
        assertNull(defaultState.isolatedChannel)
        assertNull(defaultState.monochromeChannel)

        val adj = defaultState.getAdjustment(ColorMixChannel.RED)
        assertTrue(adj.isDefault)
        assertEquals(0f, adj.hueShift, 0.001f)
        assertEquals(0f, adj.satShift, 0.001f)
        assertEquals(0f, adj.lumShift, 0.001f)
    }
}
