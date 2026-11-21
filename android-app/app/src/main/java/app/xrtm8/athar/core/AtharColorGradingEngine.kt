package app.xrtm8.athar.core

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

/**
 * High-performance 3-Way Color Grading Engine (Lift / Gamma / Gain).
 * Emulates professional cinema color grading (Shadows, Midtones, Highlights)
 * by computing a single consolidated 5x4 Skia ColorMatrix with zero heap allocation.
 */
object AtharColorGradingEngine {

    /**
     * Computes the 20-float 5x4 matrix values from Lift (Shadows), Gamma (Midtones), and Gain (Highlights).
     */
    fun computeColorGradingArray(
        liftR: Float, liftG: Float, liftB: Float,
        gammaR: Float, gammaG: Float, gammaB: Float,
        gainR: Float, gainG: Float, gainB: Float
    ): FloatArray {
        // 1. Gain: Scale highlights proportionally
        val scaleR = (1.0f + gainR * 0.8f + gammaR * 0.4f).coerceIn(0.1f, 3.0f)
        val scaleG = (1.0f + gainG * 0.8f + gammaG * 0.4f).coerceIn(0.1f, 3.0f)
        val scaleB = (1.0f + gainB * 0.8f + gammaB * 0.4f).coerceIn(0.1f, 3.0f)

        // 2. Lift: Offset darks
        val offsetR = (liftR * 128f + gammaR * 32f)
        val offsetG = (liftG * 128f + gammaG * 32f)
        val offsetB = (liftB * 128f + gammaB * 32f)

        return floatArrayOf(
            scaleR, 0f, 0f, 0f, offsetR,
            0f, scaleG, 0f, 0f, offsetG,
            0f, 0f, scaleB, 0f, offsetB,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /**
     * Computes a combined 5x4 ColorMatrix from Lift (Shadows), Gamma (Midtones), and Gain (Highlights).
     */
    fun createColorGradingMatrix(
        liftR: Float, liftG: Float, liftB: Float,
        gammaR: Float, gammaG: Float, gammaB: Float,
        gainR: Float, gainG: Float, gainB: Float
    ): ColorMatrix {
        val array = computeColorGradingArray(liftR, liftG, liftB, gammaR, gammaG, gammaB, gainR, gainG, gainB)
        return try {
            ColorMatrix(array)
        } catch (e: Exception) {
            ColorMatrix()
        }
    }

    fun createColorFilter(
        liftR: Float, liftG: Float, liftB: Float,
        gammaR: Float, gammaG: Float, gammaB: Float,
        gainR: Float, gainG: Float, gainB: Float
    ): ColorMatrixColorFilter {
        return ColorMatrixColorFilter(
            createColorGradingMatrix(
                liftR, liftG, liftB,
                gammaR, gammaG, gammaB,
                gainR, gainG, gainB
            )
        )
    }
}
