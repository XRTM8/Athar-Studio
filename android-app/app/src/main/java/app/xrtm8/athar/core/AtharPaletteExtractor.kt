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

        // 16-bucket hue histogram for chromatic clustering
        val numHueBins = 16
        val hueBuckets = IntArray(numHueBins)
        val bucketRedSum = LongArray(numHueBins)
        val bucketGreenSum = LongArray(numHueBins)
        val bucketBlueSum = LongArray(numHueBins)

        var totalR = 0L; var totalG = 0L; var totalB = 0L
        var minLum = 255f; var maxLum = 0f
        var darkPixel = Color.BLACK
        var brightPixel = Color.WHITE

        val hsv = FloatArray(3)

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

            Color.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val value = hsv[2]

            // Consider pixels with meaningful saturation and luminance for dominant/accent clustering
            if (sat >= 0.15f && value >= 0.15f && value <= 0.95f) {
                val bin = ((hsv[0] / 360f) * numHueBins).toInt().coerceIn(0, numHueBins - 1)
                hueBuckets[bin]++
                bucketRedSum[bin] = bucketRedSum[bin] + r.toLong()
                bucketGreenSum[bin] = bucketGreenSum[bin] + g.toLong()
                bucketBlueSum[bin] = bucketBlueSum[bin] + b.toLong()
            }
        }

        val count = pixels.size
        val avgR = (totalR / count).toInt().coerceIn(0, 255)
        val avgG = (totalG / count).toInt().coerceIn(0, 255)
        val avgB = (totalB / count).toInt().coerceIn(0, 255)

        // Find primary and secondary color cluster bins
        var bestBin = -1
        var bestCount = 0
        var secondBin = -1
        var secondCount = 0

        for (i in 0 until numHueBins) {
            val c = hueBuckets[i]
            if (c > bestCount) {
                secondBin = bestBin
                secondCount = bestCount
                bestBin = i
                bestCount = c
            } else if (c > secondCount) {
                secondBin = i
                secondCount = c
            }
        }

        // Dominant Color: Average of the largest color cluster, or overall average if image is monochromatic
        val dominantColor = if (bestCount >= 10 && bestBin >= 0) {
            val bR = (bucketRedSum[bestBin] / bestCount).toInt().coerceIn(0, 255)
            val bG = (bucketGreenSum[bestBin] / bestCount).toInt().coerceIn(0, 255)
            val bB = (bucketBlueSum[bestBin] / bestCount).toInt().coerceIn(0, 255)
            (0xFFL shl 24) or ((bR.toLong() and 0xFFL) shl 16) or ((bG.toLong() and 0xFFL) shl 8) or (bB.toLong() and 0xFFL)
        } else {
            (0xFFL shl 24) or ((avgR.toLong() and 0xFFL) shl 16) or ((avgG.toLong() and 0xFFL) shl 8) or (avgB.toLong() and 0xFFL)
        }

        // Accent Color: Second most prominent cluster, or harmonic hue shift
        val accentColor = if (secondCount >= 8 && secondBin >= 0 && kotlin.math.abs(secondBin - bestBin) >= 2) {
            val sR = (bucketRedSum[secondBin] / secondCount).toInt().coerceIn(0, 255)
            val sG = (bucketGreenSum[secondBin] / secondCount).toInt().coerceIn(0, 255)
            val sB = (bucketBlueSum[secondBin] / secondCount).toInt().coerceIn(0, 255)
            (0xFFL shl 24) or ((sR.toLong() and 0xFFL) shl 16) or ((sG.toLong() and 0xFFL) shl 8) or (sB.toLong() and 0xFFL)
        } else {
            Color.RGBToHSV(avgR, avgG, avgB, hsv)
            val accentHue = (hsv[0] + 180f) % 360f
            val accentSat = (hsv[1] * 1.35f).coerceIn(0.5f, 1.0f)
            val accentVal = if (hsv[2] < 0.5f) 0.95f else 0.35f
            val accentRgb = Color.HSVToColor(floatArrayOf(accentHue, accentSat, accentVal))
            (0xFFL shl 24) or (accentRgb.toLong() and 0x00FFFFFFL)
        }

        // Highlight Color: Bright tone with warm golden tint fallback
        val highlightColor = if (maxLum > 180f) {
            (0xFFL shl 24) or (brightPixel.toLong() and 0x00FFFFFFL)
        } else {
            0xFFFFD54FL
        }

        // Deep Shadow Tone
        val shadowColor = (0xDDL shl 24) or (darkPixel.toLong() and 0x00FFFFFFL)

        // Frosted Glass Tone
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
