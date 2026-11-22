package app.xrtm8.athar.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import app.xrtm8.athar.model.CinematicGlowState
import app.xrtm8.athar.model.GlowStyle
import kotlin.math.max
import kotlin.math.min

/**
 * Optical Glow, Anamorphic Motion Streaks, and Halation Engine:
 * - High-pass luminance isolation with soft-knee threshold (Range).
 * - 1D horizontal convolution for Motion & Anamorphic Streak.
 * - Separable 2D Gaussian diffusion for Bloom & Soft Mist.
 * - Kodak 500T chromatic red/amber scatter for Halation.
 * - Warmth color temperature curve (5600K -> 3200K tungsten gold).
 * - Skia hardware-accelerated Screen composite with bilinear anti-aliasing.
 */
object AtharOpticalGlowEngine {

    fun processBitmap(source: Bitmap, state: CinematicGlowState): Bitmap {
        if (!state.isEnabled) return source

        val sw = source.width
        val sh = source.height

        // Downscale to max 640px for silky-smooth highlight blurring and zero OOM
        val maxDim = 640f
        val scale = min(1f, maxDim / max(sw, sh).toFloat())
        val pw = (sw * scale).toInt().coerceAtLeast(16)
        val ph = (sh * scale).toInt().coerceAtLeast(16)

        val smallSrc = Bitmap.createScaledBitmap(source, pw, ph, true)
        val pixels = IntArray(pw * ph)
        smallSrc.getPixels(pixels, 0, pw, 0, 0, pw, ph)
        smallSrc.recycle()

        // 1. Highlight Luminance Thresholding
        val threshold = (1.0f - (state.range / 100f) * 0.72f).coerceIn(0.15f, 0.95f)
        val knee = 0.12f
        val warmFactor = state.warmth / 100f

        // Warmth Tint multipliers (Tungsten Golden Glow)
        val rWarm = 1.0f + (0.28f * warmFactor)
        val gWarm = (1.0f - (0.10f * warmFactor)).coerceAtLeast(0.4f)
        val bWarm = (1.0f - (0.75f * warmFactor)).coerceAtLeast(0.08f)

        // Halation extra red scatter boost
        val isHalation = state.style == GlowStyle.HALATION
        val rMultiplier = if (isHalation) rWarm * 1.35f else rWarm
        val gMultiplier = if (isHalation) gWarm * 0.75f else gWarm
        val bMultiplier = if (isHalation) bWarm * 0.20f else bWarm

        val highlightPixels = IntArray(pw * ph)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            if (luma > threshold - knee) {
                val weight = if (luma >= threshold + knee) {
                    1.0f
                } else {
                    ((luma - (threshold - knee)) / (knee * 2f)).coerceIn(0f, 1f)
                }

                val nr = (r * rMultiplier * weight).toInt().coerceIn(0, 255)
                val ng = (g * gMultiplier * weight).toInt().coerceIn(0, 255)
                val nb = (b * bMultiplier * weight).toInt().coerceIn(0, 255)

                highlightPixels[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            } else {
                highlightPixels[i] = 0x00000000
            }
        }

        // 2. Convolution pass based on style
        val blurredHighlight: Bitmap = when (state.style) {
            GlowStyle.MOTION -> {
                // 1D Horizontal directional motion streak
                val radius = ((state.spread / 100f) * (pw * 0.18f)).toInt().coerceIn(4, pw / 3)
                convolveHorizontalStreak(highlightPixels, pw, ph, radius, directional = true)
            }
            GlowStyle.STREAK -> {
                // Extended anamorphic horizontal beam
                val radius = ((state.spread / 100f) * (pw * 0.35f)).toInt().coerceIn(6, pw / 2)
                convolveHorizontalStreak(highlightPixels, pw, ph, radius, directional = false)
            }
            GlowStyle.BLOOM, GlowStyle.DIFFUSION, GlowStyle.HALATION -> {
                // 2D Gaussian-like box blur
                val radius = ((state.spread / 100f) * (pw * 0.08f)).toInt().coerceIn(3, 40)
                convolve2D(highlightPixels, pw, ph, radius)
            }
        }

        // 3. Composite blurred highlight onto original image using Screen / Linear Dodge
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            alpha = ((state.glow / 100f) * 255f).toInt().coerceIn(0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blendMode = android.graphics.BlendMode.SCREEN
            } else {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
        }

        val dstRect = Rect(0, 0, sw, sh)
        canvas.drawBitmap(blurredHighlight, null, dstRect, paint)
        blurredHighlight.recycle()

        return result
    }

    private fun convolveHorizontalStreak(
        pixels: IntArray,
        w: Int,
        h: Int,
        radius: Int,
        directional: Boolean
    ): Bitmap {
        val outPixels = IntArray(w * h)
        val rL = if (directional) radius / 3 else radius
        val rR = if (directional) radius * 2 else radius
        val window = rL + rR + 1

        for (y in 0 until h) {
            val rowOffset = y * w
            var sumR = 0
            var sumG = 0
            var sumB = 0
            var sumA = 0

            // Initialize window
            for (i in -rL..rR) {
                val px = min(w - 1, max(0, i))
                val c = pixels[rowOffset + px]
                sumA += (c ushr 24)
                sumR += (c shr 16) and 0xFF
                sumG += (c shr 8) and 0xFF
                sumB += c and 0xFF
            }

            for (x in 0 until w) {
                val avgA = sumA / window
                val avgR = sumR / window
                val avgG = sumG / window
                val avgB = sumB / window

                outPixels[rowOffset + x] = (avgA shl 24) or (avgR shl 16) or (avgG shl 8) or avgB

                val xRemove = min(w - 1, max(0, x - rL))
                val xAdd = min(w - 1, max(0, x + rR + 1))
                val cRemove = pixels[rowOffset + xRemove]
                val cAdd = pixels[rowOffset + xAdd]

                sumA += (cAdd ushr 24) - (cRemove ushr 24)
                sumR += ((cAdd shr 16) and 0xFF) - ((cRemove shr 16) and 0xFF)
                sumG += ((cAdd shr 8) and 0xFF) - ((cRemove shr 8) and 0xFF)
                sumB += (cAdd and 0xFF) - (cRemove and 0xFF)
            }
        }

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(outPixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun convolve2D(pixels: IntArray, w: Int, h: Int, radius: Int): Bitmap {
        val intermediate = IntArray(w * h)
        val outPixels = IntArray(w * h)
        val window = radius * 2 + 1

        // Horizontal pass
        for (y in 0 until h) {
            val row = y * w
            var sumR = 0; var sumG = 0; var sumB = 0; var sumA = 0
            for (i in -radius..radius) {
                val c = pixels[row + min(w - 1, max(0, i))]
                sumA += (c ushr 24)
                sumR += (c shr 16) and 0xFF
                sumG += (c shr 8) and 0xFF
                sumB += c and 0xFF
            }
            for (x in 0 until w) {
                intermediate[row + x] = ((sumA / window) shl 24) or
                        ((sumR / window) shl 16) or
                        ((sumG / window) shl 8) or
                        (sumB / window)
                val cRem = pixels[row + min(w - 1, max(0, x - radius))]
                val cAdd = pixels[row + min(w - 1, max(0, x + radius + 1))]
                sumA += (cAdd ushr 24) - (cRem ushr 24)
                sumR += ((cAdd shr 16) and 0xFF) - ((cRem shr 16) and 0xFF)
                sumG += ((cAdd shr 8) and 0xFF) - ((cRem shr 8) and 0xFF)
                sumB += (cAdd and 0xFF) - (cRem and 0xFF)
            }
        }

        // Vertical pass
        for (x in 0 until w) {
            var sumR = 0; var sumG = 0; var sumB = 0; var sumA = 0
            for (i in -radius..radius) {
                val c = intermediate[min(h - 1, max(0, i)) * w + x]
                sumA += (c ushr 24)
                sumR += (c shr 16) and 0xFF
                sumG += (c shr 8) and 0xFF
                sumB += c and 0xFF
            }
            for (y in 0 until h) {
                outPixels[y * w + x] = ((sumA / window) shl 24) or
                        ((sumR / window) shl 16) or
                        ((sumG / window) shl 8) or
                        (sumB / window)
                val cRem = intermediate[min(h - 1, max(0, y - radius)) * w + x]
                val cAdd = intermediate[min(h - 1, max(0, y + radius + 1)) * w + x]
                sumA += (cAdd ushr 24) - (cRem ushr 24)
                sumR += ((cAdd shr 16) and 0xFF) - ((cRem shr 16) and 0xFF)
                sumG += ((cAdd shr 8) and 0xFF) - ((cRem shr 8) and 0xFF)
                sumB += (cAdd and 0xFF) - (cRem and 0xFF)
            }
        }

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(outPixels, 0, w, 0, 0, w, h)
        return bmp
    }
}
