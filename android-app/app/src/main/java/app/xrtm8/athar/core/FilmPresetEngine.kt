package app.xrtm8.athar.core

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import app.xrtm8.athar.model.FilmPreset

/**
 * High-performance Film Camera Simulation & Color Grading Engine.
 * Simulates legendary analogue film stocks and cinematic aesthetics:
 * - Fujifilm Classic Chrome, Velvia 50, Provia 100F
 * - Kodak Portra 400, Kodak Tri-X 400
 * - Leica Monochrome
 * - Modern Hollywood Teal & Orange
 * - Vintage 1970s Warmth
 */
object FilmPresetEngine {

    fun getFilmColorMatrix(preset: FilmPreset): ColorMatrix {
        return when (preset) {
            FilmPreset.NONE -> ColorMatrix()

            FilmPreset.FUJI_CLASSIC_CHROME -> {
                val m = ColorMatrix(
                    floatArrayOf(
                        1.06f, 0.00f, 0.00f, 0f, 4f,
                        0.00f, 0.98f, 0.00f, 0f, 2f,
                        0.00f, 0.00f, 0.90f, 0f, -6f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sat = ColorMatrix().apply { setSaturation(0.88f) }
                m.postConcat(sat)
                m
            }

            FilmPreset.FUJI_VELVIA -> {
                val m = ColorMatrix(
                    floatArrayOf(
                        1.18f, 0f, 0f, 0f, -8f,
                        0f, 1.14f, 0f, 0f, -6f,
                        0f, 0f, 1.16f, 0f, -4f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sat = ColorMatrix().apply { setSaturation(1.35f) }
                m.postConcat(sat)
                m
            }

            FilmPreset.FUJI_PROVIA -> {
                val m = ColorMatrix(
                    floatArrayOf(
                        1.04f, 0f, 0f, 0f, 0f,
                        0f, 1.04f, 0f, 0f, 0f,
                        0f, 0f, 1.04f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sat = ColorMatrix().apply { setSaturation(1.08f) }
                m.postConcat(sat)
                m
            }

            FilmPreset.KODAK_PORTRA_400 -> {
                val m = ColorMatrix(
                    floatArrayOf(
                        1.12f, 0.02f, 0.00f, 0f, 12f,
                        0.00f, 1.04f, 0.00f, 0f, 6f,
                        0.00f, 0.00f, 0.92f, 0f, -8f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sat = ColorMatrix().apply { setSaturation(1.02f) }
                m.postConcat(sat)
                m
            }

            FilmPreset.KODAK_TRI_X -> {
                val m = ColorMatrix().apply { setSaturation(0f) }
                val c = 1.35f
                val t = (1.0f - c) * 128f
                val cMat = ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                m.postConcat(cMat)
                m
            }

            FilmPreset.LEICA_MONOCHROME -> {
                val m = ColorMatrix().apply { setSaturation(0f) }
                val c = 1.12f
                val t = (1.0f - c) * 128f
                val cMat = ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t + 4f,
                        0f, c, 0f, 0f, t + 4f,
                        0f, 0f, c, 0f, t + 4f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                m.postConcat(cMat)
                m
            }

            FilmPreset.CINEMATIC_TEAL_ORANGE -> {
                val m = ColorMatrix(
                    floatArrayOf(
                        1.22f, 0.00f, 0.00f, 0f, 16f,
                        0.00f, 1.05f, 0.00f, 0f, 4f,
                        0.00f, 0.00f, 1.18f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sat = ColorMatrix().apply { setSaturation(1.15f) }
                m.postConcat(sat)
                m
            }

            FilmPreset.VINTAGE_WARMTH -> {
                val m = ColorMatrix(
                    floatArrayOf(
                        1.10f, 0.05f, 0.00f, 0f, 18f,
                        0.00f, 1.02f, 0.00f, 0f, 10f,
                        0.00f, 0.00f, 0.86f, 0f, -12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sat = ColorMatrix().apply { setSaturation(0.92f) }
                m.postConcat(sat)
                m
            }
        }
    }

    /**
     * Builds a consolidated ColorMatrixColorFilter combining:
     * 1. Film Stock Simulation Matrix
     * 2. Saturation & Grayscale
     * 3. Contrast S-curve
     * 4. Brightness exposure
     * 5. Background Dimming
     */
    fun createCompositeFilter(
        preset: FilmPreset,
        dim: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        isGrayscale: Boolean,
        colorGradingEnabled: Boolean = false,
        liftR: Float = 0f, liftG: Float = 0f, liftB: Float = 0f,
        gammaR: Float = 0f, gammaG: Float = 0f, gammaB: Float = 0f,
        gainR: Float = 0f, gainG: Float = 0f, gainB: Float = 0f
    ): ColorMatrixColorFilter {
        val composite = ColorMatrix()

        // 1. Film preset matrix
        val filmMatrix = getFilmColorMatrix(preset)
        composite.postConcat(filmMatrix)

        // 1.5 3-Way Color Wheels Grading (Lift / Gamma / Gain)
        if (colorGradingEnabled) {
            val gradingMatrix = AtharColorGradingEngine.createColorGradingMatrix(
                liftR, liftG, liftB,
                gammaR, gammaG, gammaB,
                gainR, gainG, gainB
            )
            composite.postConcat(gradingMatrix)
        }

        // 2. Saturation
        val effectiveSat = if (isGrayscale) 0f else saturation.coerceIn(0f, 2.5f)
        if (effectiveSat != 1.0f) {
            val satMatrix = ColorMatrix().apply { setSaturation(effectiveSat) }
            composite.postConcat(satMatrix)
        }

        // 3. Contrast
        val c = contrast.coerceIn(0.4f, 2.0f)
        if (c != 1.0f) {
            val t = (1.0f - c) * 128f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            composite.postConcat(contrastMatrix)
        }

        // 4. Brightness
        val bOffset = (brightness.coerceIn(-0.6f, 0.6f) * 255f)
        if (bOffset != 0f) {
            val brightMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, bOffset,
                    0f, 1f, 0f, 0f, bOffset,
                    0f, 0f, 1f, 0f, bOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            composite.postConcat(brightMatrix)
        }

        // 5. Dimming
        val dimMult = (1.0f - dim.coerceIn(0f, 0.95f))
        if (dimMult != 1.0f) {
            val dimMatrix = ColorMatrix(
                floatArrayOf(
                    dimMult, 0f, 0f, 0f, 0f,
                    0f, dimMult, 0f, 0f, 0f,
                    0f, 0f, dimMult, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            composite.postConcat(dimMatrix)
        }

        return ColorMatrixColorFilter(composite)
    }
}
