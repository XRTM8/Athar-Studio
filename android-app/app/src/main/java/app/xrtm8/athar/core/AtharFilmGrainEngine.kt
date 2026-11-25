package app.xrtm8.athar.core

import android.graphics.Bitmap
import app.xrtm8.athar.model.FilmGrainState
import kotlin.math.floor
import kotlin.math.min

/**
 * Organic Silver Halide Film Grain Engine v2:
 * - Luminance-aware parabolic midtone weighting (midtones get peak grain; deep blacks & clipped whites stay clean).
 * - Smooth bilinear emulsion clump noise (no blocky integer stair-step grids).
 * - Viewport-normalized grain scale relative to [referenceWidthPx] for 1:1 preview-to-4K parity.
 * - Optional subtle dye-cloud chroma variance for color stocks; strict monochrome grain when [isMonochrome].
 * - Preserves source alpha (`a == 0` untouched) and processes in 64-row strips to prevent 4K heap OOM.
 */
object AtharFilmGrainEngine {

    private const val STRIP_ROWS = 64

    @JvmOverloads
    fun processBitmap(
        source: Bitmap,
        state: FilmGrainState,
        referenceWidthPx: Int = source.width,
        isMonochrome: Boolean = false,
        inPlace: Boolean = false
    ): Bitmap {
        if (!state.isEnabled) return source
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source

        val target = if (inPlace && source.isMutable) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val stripRows = min(STRIP_ROWS, h)
        val stripBuf = IntArray(w * stripRows)

        var y = 0
        while (y < h) {
            val rows = min(stripRows, h - y)
            target.getPixels(stripBuf, 0, w, 0, y, w, rows)
            applyGrainToStrip(stripBuf, w, rows, yOffset = y, state = state, referenceWidthPx = referenceWidthPx, isMonochrome = isMonochrome)
            target.setPixels(stripBuf, 0, w, 0, y, w, rows)
            y += rows
        }
        return target
    }

    /**
     * Pure buffer grain pass (100% JVM unit-testable, preserves alpha).
     */
    fun applyGrainToBuffer(
        pixels: IntArray,
        width: Int,
        height: Int,
        state: FilmGrainState,
        referenceWidthPx: Int = width,
        isMonochrome: Boolean = false
    ) {
        if (!state.isEnabled || width <= 0 || height <= 0) return
        applyGrainToStrip(pixels, width, height, yOffset = 0, state = state, referenceWidthPx = referenceWidthPx, isMonochrome = isMonochrome)
    }

    private fun applyGrainToStrip(
        pixels: IntArray,
        w: Int,
        rows: Int,
        yOffset: Int,
        state: FilmGrainState,
        referenceWidthPx: Int,
        isMonochrome: Boolean
    ) {
        val grainIntensity = (state.grain / 100f) * 62f
        val clumpPeriodPx = ((0.85f + (state.size / 100f) * 2.75f) * (referenceWidthPx.coerceAtLeast(320) / 1080f))
            .coerceIn(0.85f, 12.0f)
        val invPeriod = 1f / clumpPeriodPx
        val roughnessRatio = (state.roughness / 100f).coerceIn(0.1f, 1.0f)
        val chromaStrength = if (isMonochrome) 0f else 0.22f

        for (localY in 0 until rows) {
            val y = yOffset + localY
            val rowOffset = localY * w
            val gy = y * invPeriod

            for (x in 0 until w) {
                val idx = rowOffset + x
                val c = pixels[idx]
                val alphaMask = c and 0xFF000000.toInt()
                if (alphaMask == 0) continue

                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val midtoneWeight = (4.0f * luma * (1.0f - luma)).coerceIn(0f, 1f)

                if (midtoneWeight > 0.02f) {
                    val clumpNoise = bilinearHashNoise(x * invPeriod, gy)
                    val microNoise = hash2D(x, y, 0x9E3779B9.toInt())
                    val lumaNoise = (clumpNoise * (1f - roughnessRatio * 0.42f)) + (microNoise * roughnessRatio * 0.42f)
                    val deltaBase = lumaNoise * grainIntensity * midtoneWeight

                    val nr: Int
                    val ng: Int
                    val nb: Int
                    if (chromaStrength > 0f) {
                        val cR = hash2D(x, y, 0x517CC1B7) * grainIntensity * midtoneWeight * chromaStrength
                        val cB = hash2D(x, y, 0x2D51A9F3) * grainIntensity * midtoneWeight * chromaStrength
                        nr = (r + deltaBase + cR).toInt().coerceIn(0, 255)
                        ng = (g + deltaBase).toInt().coerceIn(0, 255)
                        nb = (b + deltaBase + cB).toInt().coerceIn(0, 255)
                    } else {
                        val d = deltaBase.toInt()
                        nr = (r + d).coerceIn(0, 255)
                        ng = (g + d).coerceIn(0, 255)
                        nb = (b + d).coerceIn(0, 255)
                    }

                    pixels[idx] = alphaMask or (nr shl 16) or (ng shl 8) or nb
                }
            }
        }
    }

    private fun bilinearHashNoise(fx: Float, fy: Float): Float {
        val ix = floor(fx).toInt()
        val iy = floor(fy).toInt()
        val wx = fx - ix
        val wy = fy - iy
        // Smooth Hermite interpolation prevents square grid artifacts
        val sx = wx * wx * (3f - 2f * wx)
        val sy = wy * wy * (3f - 2f * wy)

        val n00 = hash2D(ix, iy, 0)
        val n10 = hash2D(ix + 1, iy, 0)
        val n01 = hash2D(ix, iy + 1, 0)
        val n11 = hash2D(ix + 1, iy + 1, 0)

        val nx0 = n00 + (n10 - n00) * sx
        val nx1 = n01 + (n11 - n01) * sx
        return nx0 + (nx1 - nx0) * sy
    }

    private fun hash2D(x: Int, y: Int, salt: Int): Float {
        var h = (x * 0x1f1f1f1f) xor (y * 0x5a5a5a5a) xor salt
        h = (h xor (h ushr 16)) * 0x45d9f3b
        h = (h xor (h ushr 16)) * 0x45d9f3b
        h = h xor (h ushr 16)
        val normalized = (h and 0x7FFFFFFF) / 2147483647.0f
        return (normalized * 2.0f) - 1.0f
    }
}
