package app.xrtm8.athar.core

import android.graphics.Bitmap
import app.xrtm8.athar.model.GlitchEffectState
import kotlin.math.min

/**
 * Pure Mathematical Digital Glitch, RGB Split & Analog CRT Engine v2:
 * - Chromatic Aberration (independent horizontal displacement of R and B color channels with edge alpha fallback).
 * - Deterministic Horizontal Slicing & Tearing (seeded hash row displacement).
 * - Resolution-Normalized Analog CRT Scanlines (consistent density across 480px, 1080p, and 4K export).
 * - High-Frequency Digital Noise & Macroblock Pixelation.
 * - Strip-based row processing to minimize 4K heap memory spikes.
 */
object AtharGlitchEngine {

    private const val STRIP_ROWS = 64

    @JvmOverloads
    fun processBitmap(
        source: Bitmap,
        state: GlitchEffectState,
        inPlace: Boolean = false
    ): Bitmap {
        if (!state.isEnabled) return source

        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source

        val sliceOffsets = IntArray(h)
        if (state.sliceJitter > 0.5f) {
            computeSliceDisplacement(sliceOffsets, w, h, state.sliceJitter, state.seed)
        }

        val target = if (inPlace && source.isMutable) {
            source
        } else {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }

        val hasPixelate = state.pixelate > 0.5f
        val blockSize = if (hasPixelate) ((state.pixelate / 100f) * (w * 0.035f)).toInt().coerceIn(2, 64) else STRIP_ROWS
        val chunkHeight = if (hasPixelate) blockSize else min(STRIP_ROWS, h)

        val srcStrip = IntArray(w * chunkHeight)
        val outStrip = IntArray(w * chunkHeight)

        var yStart = 0
        while (yStart < h) {
            val rows = min(chunkHeight, h - yStart)
            source.getPixels(srcStrip, 0, w, 0, yStart, w, rows)

            if (hasPixelate) {
                applyPixelationStrip(srcStrip, w, rows, blockSize)
            }

            processGlitchRows(srcStrip, outStrip, w, h, yStart, rows, sliceOffsets, state)
            target.setPixels(outStrip, 0, w, 0, yStart, w, rows)
            yStart += rows
        }

        return target
    }

    /**
     * Pure buffer glitch pass (100% JVM unit-testable).
     */
    fun applyGlitchToBuffer(
        srcPixels: IntArray,
        outPixels: IntArray,
        w: Int,
        h: Int,
        state: GlitchEffectState
    ) {
        if (!state.isEnabled || w <= 0 || h <= 0) return
        val workingSrc = if (state.pixelate > 0.5f) {
            srcPixels.copyOf().also {
                val blockSize = ((state.pixelate / 100f) * (w * 0.035f)).toInt().coerceIn(2, 64)
                var by = 0
                while (by < h) {
                    val bh = min(blockSize, h - by)
                    val rowSlice = IntArray(w * bh)
                    System.arraycopy(it, by * w, rowSlice, 0, w * bh)
                    applyPixelationStrip(rowSlice, w, bh, blockSize)
                    System.arraycopy(rowSlice, 0, it, by * w, w * bh)
                    by += bh
                }
            }
        } else {
            srcPixels
        }

        val sliceOffsets = IntArray(h)
        if (state.sliceJitter > 0.5f) {
            computeSliceDisplacement(sliceOffsets, w, h, state.sliceJitter, state.seed)
        }
        processGlitchRows(workingSrc, outPixels, w, h, yStart = 0, rows = h, sliceOffsets = sliceOffsets, state = state)
    }

    private fun processGlitchRows(
        srcStrip: IntArray,
        outStrip: IntArray,
        w: Int,
        totalH: Int,
        yStart: Int,
        rows: Int,
        sliceOffsets: IntArray,
        state: GlitchEffectState
    ) {
        val rgbSplitOffset = if (state.rgbSplit > 0.5f) {
            ((state.rgbSplit / 100f) * (w * 0.045f)).toInt().coerceIn(1, w / 4)
        } else {
            0
        }

        val hasScanlines = state.scanlines > 0.5f
        val scanlineDim = 1.0f - ((state.scanlines / 100f) * 0.55f).coerceIn(0f, 0.85f)
        val hasNoise = state.noise > 0.5f
        val noiseAmp = ((state.noise / 100f) * 55f).toInt()
        val seed = state.seed
        val safeTotalH = totalH.coerceAtLeast(1)

        for (localY in 0 until rows) {
            val y = yStart + localY
            val rowOffset = localY * w
            val sliceShift = sliceOffsets[y]
            // Viewport-normalized scanline phase (~360 virtual lines across canvas height)
            val virtualY = ((y.toLong() * 1080L) / safeTotalH).toInt()
            val isDarkScanline = hasScanlines && ((virtualY % 3) == 0)

            for (x in 0 until w) {
                val displacedX = (x + sliceShift).coerceIn(0, w - 1)
                val cG = srcStrip[rowOffset + displacedX]
                val a = (cG ushr 24) and 0xFF
                if (a == 0) {
                    outStrip[rowOffset + x] = 0
                    continue
                }

                val rx = (displacedX + rgbSplitOffset).coerceIn(0, w - 1)
                val bx = (displacedX - rgbSplitOffset).coerceIn(0, w - 1)
                val cR = srcStrip[rowOffset + rx]
                val cB = srcStrip[rowOffset + bx]

                var r = if ((cR ushr 24) != 0) (cR shr 16) and 0xFF else (cG shr 16) and 0xFF
                var g = (cG shr 8) and 0xFF
                var b = if ((cB ushr 24) != 0) cB and 0xFF else cG and 0xFF

                if (isDarkScanline) {
                    r = (r * scanlineDim).toInt()
                    g = (g * scanlineDim).toInt()
                    b = (b * scanlineDim).toInt()
                }

                if (hasNoise) {
                    val n = (fastHash(x, y, seed) * noiseAmp).toInt()
                    r = (r + n).coerceIn(0, 255)
                    g = (g + n).coerceIn(0, 255)
                    b = (b + n).coerceIn(0, 255)
                }

                outStrip[rowOffset + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    private fun applyPixelationStrip(pixels: IntArray, w: Int, bh: Int, blockSize: Int) {
        for (bx in 0 until w step blockSize) {
            val bw = (w - bx).coerceAtMost(blockSize)
            val sampleX = bx + (bw / 2)
            val sampleY = bh / 2
            val sampledColor = pixels[(sampleY * w) + sampleX]

            for (y in 0 until bh) {
                val row = y * w
                for (x in bx until (bx + bw)) {
                    pixels[row + x] = sampledColor
                }
            }
        }
    }

    private fun computeSliceDisplacement(
        offsets: IntArray,
        w: Int,
        h: Int,
        jitter: Float,
        seed: Long
    ) {
        val maxOffset = ((jitter / 100f) * (w * 0.08f)).toInt().coerceAtLeast(2)
        val sliceCount = (6 + (jitter / 100f * 20f)).toInt().coerceIn(6, 32)
        val sliceHeight = (h / sliceCount).coerceAtLeast(4)

        var y = 0
        var sliceIndex = 0
        while (y < h) {
            val currentSliceH = (sliceHeight + ((pseudoRandom(sliceIndex, seed) * 12).toInt())).coerceIn(2, sliceHeight * 2)
            val isDisplaced = pseudoRandom(sliceIndex + 101, seed) > 0.45f
            val shift = if (isDisplaced) {
                val sign = if (pseudoRandom(sliceIndex + 202, seed) > 0.5f) 1 else -1
                (pseudoRandom(sliceIndex + 303, seed) * maxOffset * sign).toInt()
            } else {
                0
            }

            val endY = (y + currentSliceH).coerceAtMost(h)
            for (line in y until endY) {
                offsets[line] = shift
            }

            y = endY
            sliceIndex++
        }
    }

    private fun pseudoRandom(step: Int, seed: Long): Float {
        var h = (step.toLong() xor seed) * 0x5deece66dL + 0xbL
        h = (h xor (h ushr 16)) * 0x45d9f3bL
        h = h xor (h ushr 16)
        val norm = (h and 0x7FFFFFFFL).toFloat() / 0x7FFFFFFFL.toFloat()
        return norm.coerceIn(0f, 1f)
    }

    private fun fastHash(x: Int, y: Int, seed: Long): Float {
        var h = (x * 0x27d4eb2d) xor (y * 0x165667b1) xor seed.toInt()
        h = (h xor (h ushr 15)) * 0x2c1b3c6d
        h = h xor (h ushr 15)
        val norm = (h and 0x7FFFFFFF) / 2147483647.0f
        return (norm * 2.0f) - 1.0f
    }
}
