package app.xrtm8.athar.core

import android.graphics.Bitmap
import app.xrtm8.athar.model.ColorMixChannel
import app.xrtm8.athar.model.ColorMixState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 8-Channel Lightroom-Grade Color Mix Engine.
 * Supports:
 * - Independent HSL (Hue, Saturation, Luminance) shifts per color channel
 * - Color Splash / Isolation (desaturates all channels except the isolated one)
 * - Monochrome tint (transforms all tones into the selected color tone)
 */
object AtharColorMixEngine {

    fun processBitmap(src: Bitmap, state: ColorMixState): Bitmap {
        if (!state.isEnabled || state.isDefault) return src

        val width = src.width
        val height = src.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val isolated = state.isolatedChannel
        val monochrome = state.monochromeChannel

        // Precompute channel adjustment arrays to minimize overhead in hot loop
        val hueShifts = FloatArray(8)
        val satMultipliers = FloatArray(8)
        val lumShifts = FloatArray(8)
        val hasAdjustments = BooleanArray(8)

        ColorMixChannel.values().forEach { ch ->
            val adj = state.getAdjustment(ch)
            val idx = ch.ordinal
            hueShifts[idx] = adj.hueShift
            satMultipliers[idx] = (1f + (adj.satShift / 100f)).coerceAtLeast(0f)
            lumShifts[idx] = adj.lumShift / 100f
            hasAdjustments[idx] = !adj.isDefault
        }

        val hsl = FloatArray(3)

        for (i in 0 until totalPixels) {
            val color = pixels[i]
            val a = (color ushr 24) and 0xFF
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF

            rgbToHsl(r, g, b, hsl)
            var h = hsl[0]
            var s = hsl[1]
            var l = hsl[2]

            if (monochrome != null) {
                h = monochrome.centerHue
                s = 0.55f
                val adj = state.getAdjustment(monochrome)
                h = ((h + adj.hueShift) % 360f + 360f) % 360f
                s = (s * (1f + adj.satShift / 100f)).coerceIn(0f, 1f)
                l = (l * (1f + adj.lumShift / 100f)).coerceIn(0f, 1f)
            } else {
                val channel = ColorMixChannel.fromHue(h)
                val chIdx = channel.ordinal

                if (isolated != null) {
                    if (channel != isolated) {
                        s = 0f
                    } else {
                        h = ((h + hueShifts[chIdx]) % 360f + 360f) % 360f
                        s = (s * satMultipliers[chIdx]).coerceIn(0f, 1f)
                        l = (l + lumShifts[chIdx]).coerceIn(0f, 1f)
                    }
                } else if (hasAdjustments[chIdx]) {
                    h = ((h + hueShifts[chIdx]) % 360f + 360f) % 360f
                    s = (s * satMultipliers[chIdx]).coerceIn(0f, 1f)
                    l = (l + lumShifts[chIdx]).coerceIn(0f, 1f)
                }
            }

            pixels[i] = hslToRgb(h, s, l, a)
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    private fun rgbToHsl(r: Int, g: Int, b: Int, hsl: FloatArray) {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val cmax = max(rf, max(gf, bf))
        val cmin = min(rf, min(gf, bf))
        val delta = cmax - cmin

        val l = (cmax + cmin) / 2f
        val h: Float
        val s: Float

        if (delta < 0.00001f) {
            h = 0f
            s = 0f
        } else {
            s = if (l <= 0.5f) delta / (cmax + cmin) else delta / (2f - cmax - cmin)
            val hPre = when (cmax) {
                rf -> ((gf - bf) / delta) % 6f
                gf -> ((bf - rf) / delta) + 2f
                else -> ((rf - gf) / delta) + 4f
            }
            h = ((hPre * 60f) % 360f + 360f) % 360f
        }

        hsl[0] = h
        hsl[1] = s
        hsl[2] = l
    }

    private fun hslToRgb(h: Float, s: Float, l: Float, a: Int): Int {
        if (s <= 0.0001f) {
            val v = (l * 255f).toInt().coerceIn(0, 255)
            return (a shl 24) or (v shl 16) or (v shl 8) or v
        }

        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val rf: Float
        val gf: Float
        val bf: Float

        when ((h / 60f).toInt()) {
            0 -> { rf = c; gf = x; bf = 0f }
            1 -> { rf = x; gf = c; bf = 0f }
            2 -> { rf = 0f; gf = c; bf = x }
            3 -> { rf = 0f; gf = x; bf = c }
            4 -> { rf = x; gf = 0f; bf = c }
            else -> { rf = c; gf = 0f; bf = x }
        }

        val r = ((rf + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((gf + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((bf + m) * 255f).toInt().coerceIn(0, 255)

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
