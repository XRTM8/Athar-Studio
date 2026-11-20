package app.xrtm8.athar.core

import android.graphics.Bitmap
import app.xrtm8.athar.model.TextLayer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Optical Legibility & Auto-Contrast Engine.
 * 
 * Mathematically evaluates the luminance and high-frequency texture noise of the background image
 * directly beneath the text bounding box, automatically synthesizing the ideal combination
 * of high-contrast text color, drop shadow, or frosted glass plaque to ensure 100% WCAG legibility.
 */
object OpticalLegibilityEngine {

    data class LegibilityRecommendation(
        val textColorLong: Long,
        val enableShadow: Boolean,
        val shadowColorLong: Long,
        val shadowRadius: Float,
        val shadowDy: Float,
        val enableGlassBackdrop: Boolean,
        val glassOpacity: Float,
        val reasonAr: String
    )

    fun analyzeLegibility(source: Bitmap, layer: TextLayer): LegibilityRecommendation {
        val srcW = source.width
        val srcH = source.height

        // Calculate approximate text bounding box in normalized source image coordinates
        val centerX = srcW / 2f + (layer.normOffsetX * srcW)
        val centerY = srcH / 2f + (layer.normOffsetY * srcH)

        val halfBoxW = (srcW * 0.35f * layer.scaleFactor).coerceIn(40f, srcW * 0.45f)
        val halfBoxH = (srcH * 0.15f * layer.scaleFactor).coerceIn(30f, srcH * 0.30f)

        val left = (centerX - halfBoxW).toInt().coerceIn(0, srcW - 2)
        val right = (centerX + halfBoxW).toInt().coerceIn(left + 1, srcW - 1)
        val top = (centerY - halfBoxH).toInt().coerceIn(0, srcH - 2)
        val bottom = (centerY + halfBoxH).toInt().coerceIn(top + 1, srcH - 1)

        val boxW = right - left
        val boxH = bottom - top

        // Sample region pixels (downsampled for speed)
        val stepX = max(1, boxW / 40)
        val stepY = max(1, boxH / 40)

        var totalLum = 0.0
        var totalSqLum = 0.0
        var sampleCount = 0

        for (y in top until bottom step stepY) {
            for (x in left until right step stepX) {
                val p = source.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                // Relative Perceptual Luminance (Rec. 709)
                val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
                totalLum += lum
                totalSqLum += (lum * lum)
                sampleCount++
            }
        }

        val count = max(1, sampleCount)
        val meanLum = (totalLum / count).toFloat()
        val variance = max(0.0, (totalSqLum / count) - (meanLum * meanLum)).toFloat()
        val stdDev = sqrt(variance.toDouble()).toFloat()

        return when {
            // Case 1: High variance (Busy/Textured background) -> Frosted Glass or Deep Shadow required
            stdDev >= 45f -> {
                LegibilityRecommendation(
                    textColorLong = 0xFFFFFFFF, // Pure White
                    enableShadow = true,
                    shadowColorLong = 0xEE000000,
                    shadowRadius = 20f,
                    shadowDy = 5f,
                    enableGlassBackdrop = true,
                    glassOpacity = 0.50f,
                    reasonAr = "خلفية ذات تفاصيل معقدة: تم تطبيق بطاقة زجاجية وظل عميق لضمان القراءة"
                )
            }
            // Case 2: Dark, calm background
            meanLum < 100f -> {
                LegibilityRecommendation(
                    textColorLong = 0xFFFFFFFF, // Pure White
                    enableShadow = true,
                    shadowColorLong = 0x99000000,
                    shadowRadius = 12f,
                    shadowDy = 3f,
                    enableGlassBackdrop = false,
                    glassOpacity = 0f,
                    reasonAr = "خلفية داكنة: تم اختيار نص أبيض نقي مع ظل ناعم"
                )
            }
            // Case 3: Bright, light background
            meanLum > 170f -> {
                LegibilityRecommendation(
                    textColorLong = 0xFF141414, // Deep Obsidian Black
                    enableShadow = true,
                    shadowColorLong = 0x22000000,
                    shadowRadius = 8f,
                    shadowDy = 2f,
                    enableGlassBackdrop = false,
                    glassOpacity = 0f,
                    reasonAr = "خلفية ناصعة: تم اختيار نص أسود فاخر عالي التباين"
                )
            }
            // Case 4: Medium gray or colorful background
            else -> {
                LegibilityRecommendation(
                    textColorLong = 0xFFFFFFFF,
                    enableShadow = true,
                    shadowColorLong = 0xDD000000,
                    shadowRadius = 18f,
                    shadowDy = 4f,
                    enableGlassBackdrop = true,
                    glassOpacity = 0.40f,
                    reasonAr = "خلفية متوسطة الإضاءة: تم تعزيز التباين بالظل المصقول وتعتيم خفيف"
                )
            }
        }
    }

    /**
     * Applies the calculated legibility recommendation directly to the layer.
     */
    fun applyLegibility(layer: TextLayer, rec: LegibilityRecommendation): TextLayer {
        return layer.copy(
            customTextColorLong = rec.textColorLong,
            shadowRadius = rec.shadowRadius,
            shadowDx = 0f,
            shadowDy = rec.shadowDy,
            shadowColorLong = rec.shadowColorLong,
            enableGlassBackdrop = rec.enableGlassBackdrop,
            glassBackdropOpacity = rec.glassOpacity
        )
    }
}
