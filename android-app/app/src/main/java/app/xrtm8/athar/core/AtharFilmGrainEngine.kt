package app.xrtm8.athar.core

import android.graphics.Bitmap
import app.xrtm8.athar.model.FilmGrainState

/**
 * Organic Silver Halide Film Grain Engine:
 * - Luminance-aware bell-curve modulation (only midtones show grain, blacks and highlights stay clean).
 * - Grain clumping (Size) to emulate 35mm physical chemical emulsion clusters.
 * - Roughness parameter to control organic irregularity and contrast.
 * - Zero-allocation deterministic integer hash for 120Hz smooth operation.
 */
object AtharFilmGrainEngine {

    fun processBitmap(source: Bitmap, state: FilmGrainState): Bitmap {
        if (!state.isEnabled) return source

        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val grainIntensity = (state.grain / 100f) * 65f // Max delta ±65 levels
        val clumpSize = ((state.size / 100f) * 3f).toInt() + 1 // 1 to 4 px clumps
        val roughnessRatio = (state.roughness / 100f).coerceIn(0.1f, 1.0f)

        for (y in 0 until h) {
            val rowOffset = y * w
            val clumpY = y / clumpSize

            for (x in 0 until w) {
                val clumpX = x / clumpSize
                val idx = rowOffset + x
                val c = pixels[idx]

                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                // Fast luminance calculation
                val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

                // Parabolic Bell-curve: w(L) = 4 * L * (1 - L)
                // Exactly 1.0 at L = 0.5 (midtones), and 0.0 at L = 0.0 (shadows) and L = 1.0 (highlights)
                val midtoneWeight = (4.0f * luma * (1.0f - luma)).coerceIn(0f, 1f)

                if (midtoneWeight > 0.02f) {
                    // Clump-level noise + high-frequency micro-roughness jitter
                    val clumpNoise = hash2D(clumpX, clumpY)
                    val microNoise = hash2D(x, y)
                    val blendedNoise = (clumpNoise * (1f - roughnessRatio * 0.4f)) + (microNoise * roughnessRatio * 0.4f)

                    val noiseDelta = (blendedNoise * grainIntensity * midtoneWeight).toInt()

                    val nr = (r + noiseDelta).coerceIn(0, 255)
                    val ng = (g + noiseDelta).coerceIn(0, 255)
                    val nb = (b + noiseDelta).coerceIn(0, 255)

                    pixels[idx] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
                }
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * Fast deterministic pseudo-random hash producing float in range [-1.0, 1.0].
     */
    private fun hash2D(x: Int, y: Int): Float {
        var h = (x * 0x1f1f1f1f) xor (y * 0x5a5a5a5a)
        h = (h xor (h ushr 16)) * 0x45d9f3b
        h = (h xor (h ushr 16)) * 0x45d9f3b
        h = h xor (h ushr 16)
        val normalized = (h and 0x7FFFFFFF) / 2147483647.0f
        return (normalized * 2.0f) - 1.0f
    }
}
