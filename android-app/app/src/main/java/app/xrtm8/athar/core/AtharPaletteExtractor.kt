package app.xrtm8.athar.core

import android.graphics.Bitmap
import android.graphics.Color

/**
 * High-performance deterministic color palette extractor.
 * Downsamples background imagery and quantizes luminance/hue to extract 5 complementary
 * graphic design swatches (Dominant, Accent, Highlight, Shadow, Glass) for user selection.
 */
object AtharPaletteExtractor {

    data class ExtractedPalette(
        val dominantLong: Long,
        val accentLong: Long,
        val highlightLong: Long,
        val shadowLong: Long,
        val glassLong: Long
    )

    fun extractPalette(source: Bitmap): ExtractedPalette {
        val w = 64
        val h = 64
        val small = Bitmap.createScaledBitmap(source, w, h, false)
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        if (small != source) small.recycle()

        var totalR = 0L; var totalG = 0L; var totalB = 0L
        var minLum = 255f; var maxLum = 0f
        var darkPixel = Color.BLACK
        var brightPixel = Color.WHITE

        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            totalR += r; totalG += g; totalB += b
            val lum = 0.299f * r + 0.587f * g + 0.114f * b

            if (lum < minLum) {
                minLum = lum
                darkPixel = p
            }
            if (lum > maxLum) {
                maxLum = lum
                brightPixel = p
            }
        }

        val count = pixels.size
        val avgR = (totalR / count).toInt().coerceIn(0, 255)
        val avgG = (totalG / count).toInt().coerceIn(0, 255)
        val avgB = (totalB / count).toInt().coerceIn(0, 255)

        val dominantColor = (0xFFL shl 24) or ((avgR.toLong() and 0xFFL) shl 16) or ((avgG.toLong() and 0xFFL) shl 8) or (avgB.toLong() and 0xFFL)

        // Accent Color: Invert/shift hue by 180 degrees
        val hsv = FloatArray(3)
        Color.RGBToHSV(avgR, avgG, avgB, hsv)
        val accentHue = (hsv[0] + 180f) % 360f
        val accentSat = (hsv[1] * 1.35f).coerceIn(0.45f, 1.0f)
        val accentVal = if (hsv[2] < 0.5f) 0.95f else 0.25f
        val accentRgb = Color.HSVToColor(floatArrayOf(accentHue, accentSat, accentVal))
        val accentColor = (0xFFL shl 24) or (accentRgb.toLong() and 0x00FFFFFFL)

        // Highlight Color: Warm golden or bright tint
        val highlightColor = if (maxLum > 180f) {
            (0xFFL shl 24) or (brightPixel.toLong() and 0x00FFFFFFL)
        } else {
            0xFFFFD54FL
        }

        // Deep Shadow Tone
        val shadowColor = (0xDDL shl 24) or (darkPixel.toLong() and 0x00FFFFFFL)

        // Glass Tone
        val glassColor = (0xBBL shl 24) or ((avgR / 3).toLong() shl 16) or ((avgG / 3).toLong() shl 8) or ((avgB / 3).toLong())

        return ExtractedPalette(
            dominantLong = dominantColor,
            accentLong = accentColor,
            highlightLong = highlightColor,
            shadowLong = shadowColor,
            glassLong = glassColor
        )
    }

    fun getSwatches(palette: ExtractedPalette): List<Long> {
        return listOf(
            palette.highlightLong,
            palette.accentLong,
            palette.dominantLong,
            palette.glassLong,
            palette.shadowLong
        )
    }
}
