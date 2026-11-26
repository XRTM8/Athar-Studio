package app.xrtm8.athar.core

import android.graphics.Bitmap
import app.xrtm8.athar.model.ToneCurveState
import kotlin.math.abs
import kotlin.math.min

/**
 * 256-entry Luma Tone Curve + Split Toning Grading Engine.
 * Supports 4-zone tonal shaping (Blacks, Shadows, Highlights, Whites)
 * and smooth shadow/highlight hue tinting.
 */
object AtharToneCurveEngine {

    private const val STRIP_ROWS = 64

    @JvmOverloads
    fun processBitmap(
        source: Bitmap,
        state: ToneCurveState,
        inPlace: Boolean = false
    ): Bitmap {
        if (!state.isEnabled) return source
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source

        val lut = buildToneLut(state)
        val hasSplit = state.splitShadowSat > 0.005f || state.splitHighlightSat > 0.005f
        val shRgb = if (hasSplit) hueToRgb(state.splitShadowHue) else null
        val hiRgb = if (hasSplit) hueToRgb(state.splitHighlightHue) else null

        val target = if (inPlace && source.isMutable) source else Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val strip = IntArray(w * min(STRIP_ROWS, h))
        var yStart = 0
        while (yStart < h) {
            val rows = min(STRIP_ROWS, h - yStart)
            source.getPixels(strip, 0, w, 0, yStart, w, rows)
            applyToBuffer(strip, strip, lut, state, shRgb, hiRgb, w * rows)
            target.setPixels(strip, 0, w, 0, yStart, w, rows)
            yStart += rows
        }
        return target
    }

    fun applyToBuffer(
        srcPixels: IntArray,
        outPixels: IntArray,
        state: ToneCurveState
    ) {
        if (!state.isEnabled) {
            if (srcPixels !== outPixels) System.arraycopy(srcPixels, 0, outPixels, 0, min(srcPixels.size, outPixels.size))
            return
        }
        val lut = buildToneLut(state)
        val hasSplit = state.splitShadowSat > 0.005f || state.splitHighlightSat > 0.005f
        val shRgb = if (hasSplit) hueToRgb(state.splitShadowHue) else null
        val hiRgb = if (hasSplit) hueToRgb(state.splitHighlightHue) else null
        applyToBuffer(srcPixels, outPixels, lut, state, shRgb, hiRgb, min(srcPixels.size, outPixels.size))
    }

    private fun applyToBuffer(
        src: IntArray,
        out: IntArray,
        lut: IntArray,
        state: ToneCurveState,
        shRgb: FloatArray?,
        hiRgb: FloatArray?,
        count: Int
    ) {
        val shSat = state.splitShadowSat.coerceIn(0f, 0.6f)
        val hiSat = state.splitHighlightSat.coerceIn(0f, 0.6f)
        for (i in 0 until count) {
            val px = src[i]
            val a = px and 0xFF000000.toInt()
            var r = lut[(px ushr 16) and 0xFF]
            var g = lut[(px ushr 8) and 0xFF]
            var b = lut[px and 0xFF]

            if (shRgb != null && hiRgb != null) {
                val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val shW = (1f - luma) * (1f - luma) * shSat
                val hiW = luma * luma * hiSat
                r = (r + (shRgb[0] * 255f - r) * shW + (hiRgb[0] * 255f - r) * hiW).toInt().coerceIn(0, 255)
                g = (g + (shRgb[1] * 255f - g) * shW + (hiRgb[1] * 255f - g) * hiW).toInt().coerceIn(0, 255)
                b = (b + (shRgb[2] * 255f - b) * shW + (hiRgb[2] * 255f - b) * hiW).toInt().coerceIn(0, 255)
            }
            out[i] = a or (r shl 16) or (g shl 8) or b
        }
    }

    fun buildToneLut(state: ToneCurveState): IntArray {
        val y0 = (0f + state.blacks * 0.5f).coerceIn(0f, 1f)
        val y1 = (0.333f + state.shadows * 0.45f).coerceIn(0f, 1f)
        val y2 = (0.667f + state.highlights * 0.45f).coerceIn(0f, 1f)
        val y3 = (1f + state.whites * 0.5f).coerceIn(0f, 1f)

        val lut = IntArray(256)
        for (i in 0..255) {
            val x = i / 255f
            val y = when {
                x < 0.333f -> smoothLerp(y0, y1, x / 0.333f)
                x < 0.667f -> smoothLerp(y1, y2, (x - 0.333f) / 0.334f)
                else -> smoothLerp(y2, y3, (x - 0.667f) / 0.333f)
            }
            lut[i] = (y * 255f).toInt().coerceIn(0, 255)
        }
        return lut
    }

    private fun smoothLerp(a: Float, b: Float, t: Float): Float {
        val ct = t.coerceIn(0f, 1f)
        val s = ct * ct * (3f - 2f * ct)
        return a + (b - a) * s
    }

    private fun hueToRgb(hueDeg: Float): FloatArray {
        val h = ((hueDeg % 360f) + 360f) % 360f / 60f
        val x = 1f - abs(h % 2f - 1f)
        return when {
            h < 1f -> floatArrayOf(1f, x, 0f)
            h < 2f -> floatArrayOf(x, 1f, 0f)
            h < 3f -> floatArrayOf(0f, 1f, x)
            h < 4f -> floatArrayOf(0f, x, 1f)
            h < 5f -> floatArrayOf(x, 0f, 1f)
            else -> floatArrayOf(1f, 0f, x)
        }
    }
}
