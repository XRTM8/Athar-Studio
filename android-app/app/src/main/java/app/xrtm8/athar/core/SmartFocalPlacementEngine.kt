package app.xrtm8.athar.core

import android.graphics.Bitmap
import android.graphics.Color
import app.xrtm8.athar.model.TextAlignOption
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Intelligent Negative Space & Focal Point Auto-Placement Engine.
 * 
 * Analyzes the visual saliency, edge energy, and feature density of the background image
 * to mathematically discover the calmest "Negative Space" sector (where there are no faces,
 * subjects, or high-contrast distractions) and automatically positions the typography there.
 */
object SmartFocalPlacementEngine {

    data class FocalPlacementResult(
        val normX: Float,
        val normY: Float,
        val recommendedAlign: TextAlignOption,
        val sectorDescriptionAr: String
    )

    private data class CandidateSector(
        val normX: Float,
        val normY: Float,
        val startXFraction: Float,
        val endXFraction: Float,
        val startYFraction: Float,
        val endYFraction: Float,
        val align: TextAlignOption,
        val labelAr: String
    )

    private val CANDIDATE_SECTORS = listOf(
        // 1. Top Center
        CandidateSector(
            normX = 0.0f, normY = -0.32f,
            startXFraction = 0.20f, endXFraction = 0.80f,
            startYFraction = 0.05f, endYFraction = 0.35f,
            align = TextAlignOption.CENTER,
            labelAr = "أعلى المنتصف (فراغ علوي)"
        ),
        // 2. Bottom Center
        CandidateSector(
            normX = 0.0f, normY = 0.32f,
            startXFraction = 0.20f, endXFraction = 0.80f,
            startYFraction = 0.65f, endYFraction = 0.95f,
            align = TextAlignOption.CENTER,
            labelAr = "أسفل المنتصف (فراغ سفلي)"
        ),
        // 3. Top Right (Arabic priority)
        CandidateSector(
            normX = 0.26f, normY = -0.28f,
            startXFraction = 0.50f, endXFraction = 0.95f,
            startYFraction = 0.08f, endYFraction = 0.40f,
            align = TextAlignOption.RIGHT,
            labelAr = "أعلى اليمين"
        ),
        // 4. Top Left
        CandidateSector(
            normX = -0.26f, normY = -0.28f,
            startXFraction = 0.05f, endXFraction = 0.50f,
            startYFraction = 0.08f, endYFraction = 0.40f,
            align = TextAlignOption.LEFT,
            labelAr = "أعلى اليسار"
        ),
        // 5. Bottom Right
        CandidateSector(
            normX = 0.26f, normY = 0.28f,
            startXFraction = 0.50f, endXFraction = 0.95f,
            startYFraction = 0.60f, endYFraction = 0.92f,
            align = TextAlignOption.RIGHT,
            labelAr = "أسفل اليمين"
        ),
        // 6. Bottom Left
        CandidateSector(
            normX = -0.26f, normY = 0.28f,
            startXFraction = 0.05f, endXFraction = 0.50f,
            startYFraction = 0.60f, endYFraction = 0.92f,
            align = TextAlignOption.LEFT,
            labelAr = "أسفل اليسار"
        ),
        // 7. Golden Center
        CandidateSector(
            normX = 0.0f, normY = 0.0f,
            startXFraction = 0.25f, endXFraction = 0.75f,
            startYFraction = 0.30f, endYFraction = 0.70f,
            align = TextAlignOption.CENTER,
            labelAr = "مركز الكانفاس"
        )
    )

    /**
     * Finds the optimal negative space sector with minimum visual clutter.
     */
    fun findOptimalPlacement(source: Bitmap): FocalPlacementResult {
        val sampleSize = 120
        val small = Bitmap.createScaledBitmap(source, sampleSize, sampleSize, false)
        val pixels = IntArray(sampleSize * sampleSize)
        small.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)
        if (small != source) small.recycle()

        // 1. Convert to Luminance Grayscale & compute Sobel Saliency Energy
        val energyMap = FloatArray(sampleSize * sampleSize)
        val lums = FloatArray(sampleSize * sampleSize)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lums[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Sobel Edge Gradient Magnitude
        for (y in 1 until sampleSize - 1) {
            for (x in 1 until sampleSize - 1) {
                val idx = y * sampleSize + x
                // Horizontal gradient
                val gx = (lums[idx - sampleSize + 1] + 2f * lums[idx + 1] + lums[idx + sampleSize + 1]) -
                         (lums[idx - sampleSize - 1] + 2f * lums[idx - 1] + lums[idx + sampleSize - 1])
                // Vertical gradient
                val gy = (lums[idx + sampleSize - 1] + 2f * lums[idx + sampleSize] + lums[idx + sampleSize + 1]) -
                         (lums[idx - sampleSize - 1] + 2f * lums[idx - sampleSize] + lums[idx - sampleSize + 1])

                val mag = sqrt((gx * gx + gy * gy).toDouble()).toFloat()
                energyMap[idx] = mag
            }
        }

        // 2. Evaluate Candidate Sectors for Lowest Clutter Score
        var bestSector = CANDIDATE_SECTORS.first()
        var minClutterScore = Float.MAX_VALUE

        for (sector in CANDIDATE_SECTORS) {
            val startX = (sector.startXFraction * sampleSize).toInt().coerceIn(1, sampleSize - 2)
            val endX = (sector.endXFraction * sampleSize).toInt().coerceIn(startX + 1, sampleSize - 2)
            val startY = (sector.startYFraction * sampleSize).toInt().coerceIn(1, sampleSize - 2)
            val endY = (sector.endYFraction * sampleSize).toInt().coerceIn(startY + 1, sampleSize - 2)

            var totalEnergy = 0.0
            var count = 0

            for (y in startY..endY) {
                for (x in startX..endX) {
                    totalEnergy += energyMap[y * sampleSize + x]
                    count++
                }
            }

            val avgEnergy = if (count > 0) (totalEnergy / count).toFloat() else Float.MAX_VALUE

            // Slight bias towards top/bottom center for high typography aesthetic
            val aestheticBias = when (sector.normX) {
                0.0f -> 0.90f // 10% preference to centered titles
                else -> 1.0f
            }

            val finalScore = avgEnergy * aestheticBias

            if (finalScore < minClutterScore) {
                minClutterScore = finalScore
                bestSector = sector
            }
        }

        return FocalPlacementResult(
            normX = bestSector.normX,
            normY = bestSector.normY,
            recommendedAlign = bestSector.align,
            sectorDescriptionAr = bestSector.labelAr
        )
    }
}
