package app.xrtm8.athar.core

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import app.xrtm8.athar.model.FilmPreset
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp
import kotlin.math.min

/**
 * Film Camera Simulation & Color Science Engine v2:
 * - Parametric FilmProfile per preset:
 *   1. 3x3 spectral dye crosstalk / panchromatic response
 *   2. Non-linear tone curve (exposure, shoulder highlight rolloff, Hermite S-contrast, black crush & toe lift)
 *   3. Luma-dependent shadow/highlight saturation taper
 *   4. Smoothstep luminance-weighted split-toning
 * - Lazily baked into a compact 17x17x17 3D RGB LUT (4,913 packed entries ~19.6 KB)
 * - Fast fixed-point trilinear interpolation in 64-row strips preserving source alpha
 * - Backward-compatible ColorMatrixColorFilter API for 3-Way Color Grading & photo adjustments.
 */
object FilmPresetEngine {

    const val LUT_SIZE = 17
    private const val LUT_TOTAL = LUT_SIZE * LUT_SIZE * LUT_SIZE
    private const val STRIP_ROWS = 64

    data class FilmProfile(
        val blackLift: Float = 0f,
        val blackCrush: Float = 0f,
        val contrastS: Float = 0f,
        val highlightRolloff: Float = 0f,
        val exposureGain: Float = 1f,
        val saturation: Float = 1f,
        val highlightDesat: Float = 0.22f,
        val shadowDesat: Float = 0.14f,
        val rFromR: Float = 1f, val rFromG: Float = 0f, val rFromB: Float = 0f,
        val gFromR: Float = 0f, val gFromG: Float = 1f, val gFromB: Float = 0f,
        val bFromR: Float = 0f, val bFromG: Float = 0f, val bFromB: Float = 1f,
        val shadowTintR: Float = 0f, val shadowTintG: Float = 0f, val shadowTintB: Float = 0f,
        val highlightTintR: Float = 0f, val highlightTintG: Float = 0f, val highlightTintB: Float = 0f,
        val isMonochrome: Boolean = false
    )

    private val lutCache = ConcurrentHashMap<FilmPreset, IntArray>()

    fun getFilmProfile(preset: FilmPreset): FilmProfile = when (preset) {
        FilmPreset.NONE -> FilmProfile(
            highlightDesat = 0f,
            shadowDesat = 0f
        )

        FilmPreset.FUJI_CLASSIC_CHROME -> FilmProfile(
            blackLift = 0.020f,
            contrastS = 0.22f,
            highlightRolloff = 0.22f,
            saturation = 0.86f,
            rFromR = 1.03f, rFromG = -0.02f,
            gFromG = 0.99f,
            bFromB = 0.95f,
            shadowTintR = -0.015f, shadowTintG = 0.014f, shadowTintB = 0.022f,
            highlightTintR = 0.020f, highlightTintG = 0.010f, highlightTintB = -0.022f
        )

        FilmPreset.FUJI_VELVIA -> FilmProfile(
            blackCrush = 0.022f,
            contrastS = 0.40f,
            highlightRolloff = 0.20f,
            saturation = 1.36f,
            rFromR = 1.04f, gFromG = 1.02f, bFromB = 1.03f,
            shadowTintR = 0.012f, shadowTintG = -0.010f, shadowTintB = 0.028f,
            highlightTintR = 0.018f, highlightTintG = 0.005f, highlightTintB = -0.010f
        )

        FilmPreset.FUJI_PROVIA -> FilmProfile(
            contrastS = 0.18f,
            highlightRolloff = 0.16f,
            saturation = 1.09f,
            shadowTintR = -0.006f, shadowTintB = 0.010f,
            highlightTintR = 0.008f, highlightTintG = 0.004f
        )

        FilmPreset.KODAK_PORTRA_400 -> FilmProfile(
            blackLift = 0.036f,
            contrastS = 0.16f,
            highlightRolloff = 0.32f,
            exposureGain = 1.05f,
            saturation = 1.02f,
            rFromR = 1.02f, rFromG = 0.03f, rFromB = -0.02f,
            gFromG = 1.01f,
            bFromG = -0.02f, bFromB = 0.97f,
            shadowTintR = -0.008f, shadowTintG = 0.010f, shadowTintB = -0.008f,
            highlightTintR = 0.042f, highlightTintG = 0.022f, highlightTintB = -0.034f
        )

        FilmPreset.KODAK_TRI_X -> FilmProfile(
            blackCrush = 0.032f,
            contrastS = 0.48f,
            highlightRolloff = 0.20f,
            rFromR = 0.36f, rFromG = 0.54f, rFromB = 0.10f,
            isMonochrome = true
        )

        FilmPreset.LEICA_MONOCHROME -> FilmProfile(
            blackLift = 0.018f,
            contrastS = 0.26f,
            highlightRolloff = 0.30f,
            rFromR = 0.26f, rFromG = 0.62f, rFromB = 0.12f,
            isMonochrome = true
        )

        FilmPreset.CINEMATIC_TEAL_ORANGE -> FilmProfile(
            blackLift = 0.018f,
            contrastS = 0.36f,
            highlightRolloff = 0.26f,
            saturation = 1.08f,
            rFromR = 1.03f, rFromB = -0.03f,
            bFromR = -0.03f, bFromB = 1.02f,
            shadowTintR = -0.068f, shadowTintG = 0.036f, shadowTintB = 0.095f,
            highlightTintR = 0.105f, highlightTintG = 0.032f, highlightTintB = -0.088f
        )

        FilmPreset.VINTAGE_WARMTH -> FilmProfile(
            blackLift = 0.056f,
            contrastS = 0.12f,
            highlightRolloff = 0.28f,
            saturation = 0.88f,
            rFromR = 1.02f, rFromG = 0.04f,
            bFromB = 0.92f,
            shadowTintR = 0.042f, shadowTintG = 0.018f, shadowTintB = -0.032f,
            highlightTintR = 0.068f, highlightTintG = 0.038f, highlightTintB = -0.068f
        )

        FilmPreset.DREAM_GLOW -> FilmProfile(
            blackLift = 0.042f,
            contrastS = -0.10f,
            highlightRolloff = 0.36f,
            exposureGain = 1.06f,
            saturation = 1.04f,
            shadowTintR = 0.024f, shadowTintG = 0.012f, shadowTintB = 0.030f,
            highlightTintR = 0.044f, highlightTintG = 0.024f, highlightTintB = -0.022f
        )

        FilmPreset.HYBRID_CCD_2005 -> FilmProfile(
            blackCrush = 0.015f,
            contrastS = 0.30f,
            highlightRolloff = 0.10f,
            exposureGain = 1.03f,
            saturation = 1.12f,
            rFromR = 1.05f, rFromG = -0.02f,
            bFromB = 1.04f,
            shadowTintR = -0.022f, shadowTintG = 0.004f, shadowTintB = 0.042f,
            highlightTintR = 0.030f, highlightTintG = 0.012f, highlightTintB = 0.002f
        )

        FilmPreset.MINIDV_CAM_2002 -> FilmProfile(
            blackLift = 0.046f,
            contrastS = 0.16f,
            highlightRolloff = 0.12f,
            saturation = 0.90f,
            gFromG = 1.02f,
            bFromB = 1.04f,
            shadowTintR = -0.042f, shadowTintG = 0.022f, shadowTintB = 0.062f,
            highlightTintR = -0.010f, highlightTintG = 0.022f, highlightTintB = 0.020f
        )

        FilmPreset.SLIDE_CHROME_100 -> FilmProfile(
            blackCrush = 0.045f,
            contrastS = 0.50f,
            highlightRolloff = 0.16f,
            saturation = 1.24f,
            rFromR = 1.03f, bFromB = 1.03f,
            shadowTintR = -0.022f, shadowTintG = -0.008f, shadowTintB = 0.042f,
            highlightTintR = 0.032f, highlightTintG = 0.012f, highlightTintB = -0.018f
        )

        FilmPreset.DISPOSABLE_COOL_400 -> FilmProfile(
            blackLift = 0.050f,
            contrastS = 0.22f,
            highlightRolloff = 0.24f,
            exposureGain = 1.04f,
            saturation = 0.96f,
            gFromG = 1.02f,
            shadowTintR = -0.032f, shadowTintG = 0.056f, shadowTintB = 0.032f,
            highlightTintR = 0.032f, highlightTintG = 0.028f, highlightTintB = -0.022f
        )

        FilmPreset.DISPOSABLE_WARM_800 -> FilmProfile(
            blackLift = 0.054f,
            contrastS = 0.24f,
            highlightRolloff = 0.26f,
            exposureGain = 1.03f,
            saturation = 1.04f,
            rFromR = 1.03f, rFromG = 0.03f,
            bFromB = 0.94f,
            shadowTintR = 0.030f, shadowTintG = 0.012f, shadowTintB = -0.040f,
            highlightTintR = 0.080f, highlightTintG = 0.040f, highlightTintB = -0.068f
        )

        FilmPreset.STREET_MONO_GR -> FilmProfile(
            blackCrush = 0.058f,
            contrastS = 0.62f,
            highlightRolloff = 0.12f,
            rFromR = 0.40f, rFromG = 0.50f, rFromB = 0.10f,
            isMonochrome = true
        )

        FilmPreset.TOY_MEDIUM_120 -> FilmProfile(
            blackLift = 0.046f,
            contrastS = 0.28f,
            highlightRolloff = 0.24f,
            saturation = 0.94f,
            rFromR = 1.02f, gFromB = 0.02f, bFromB = 0.96f,
            shadowTintR = -0.032f, shadowTintG = 0.032f, shadowTintB = 0.040f,
            highlightTintR = 0.060f, highlightTintG = 0.038f, highlightTintB = -0.060f
        )

        FilmPreset.RANGEFINDER_400 -> FilmProfile(
            blackLift = 0.026f,
            contrastS = 0.26f,
            highlightRolloff = 0.26f,
            saturation = 1.06f,
            gFromG = 1.01f, gFromB = 0.02f,
            bFromG = -0.02f, bFromB = 0.99f,
            shadowTintR = -0.020f, shadowTintG = 0.022f, shadowTintB = 0.020f,
            highlightTintR = 0.040f, highlightTintG = 0.020f, highlightTintB = -0.030f
        )
    }

    /**
     * Evaluates a single normalized RGB sample through [profile] for 3D LUT baking.
     */
    internal fun evaluateProfileSample(rIn: Float, gIn: Float, bIn: Float, p: FilmProfile): Int {
        var r: Float
        var g: Float
        var b: Float

        if (p.isMonochrome) {
            val mono = (rIn * p.rFromR + gIn * p.rFromG + bIn * p.rFromB).coerceIn(0f, 1f)
            val curved = applyToneCurve(mono, p)
            r = curved; g = curved; b = curved
        } else {
            r = applyToneCurve((rIn * p.rFromR + gIn * p.rFromG + bIn * p.rFromB).coerceIn(0f, 1f), p)
            g = applyToneCurve((rIn * p.gFromR + gIn * p.gFromG + bIn * p.gFromB).coerceIn(0f, 1f), p)
            b = applyToneCurve((rIn * p.bFromR + gIn * p.bFromG + bIn * p.bFromB).coerceIn(0f, 1f), p)

            val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
            val shadowFade = 1f - p.shadowDesat * ((1f - luma) * (1f - luma))
            val highFade = 1f - p.highlightDesat * (luma * luma)
            val effSat = (p.saturation * shadowFade * highFade).coerceIn(0f, 2.2f)
            if (effSat != 1f) {
                r = (luma + (r - luma) * effSat).coerceIn(0f, 1f)
                g = (luma + (g - luma) * effSat).coerceIn(0f, 1f)
                b = (luma + (b - luma) * effSat).coerceIn(0f, 1f)
            }

            val shadowW = (1f - luma) * (1f - luma)
            val highW = luma * luma
            r = (r + p.shadowTintR * shadowW + p.highlightTintR * highW).coerceIn(0f, 1f)
            g = (g + p.shadowTintG * shadowW + p.highlightTintG * highW).coerceIn(0f, 1f)
            b = (b + p.shadowTintB * shadowW + p.highlightTintB * highW).coerceIn(0f, 1f)
        }

        val ri = (r * 255f + 0.5f).toInt().coerceIn(0, 255)
        val gi = (g * 255f + 0.5f).toInt().coerceIn(0, 255)
        val bi = (b * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (ri shl 16) or (gi shl 8) or bi
    }

    private fun applyToneCurve(v: Float, p: FilmProfile): Float {
        var x = (v * p.exposureGain).coerceIn(0f, 1.5f)
        if (p.highlightRolloff > 0f && x > 0.60f) {
            val excess = x - 0.60f
            val span = 0.40f
            val compressed = span * (1f - exp((-excess / span) * (1f + p.highlightRolloff * 2.5f)))
            x = 0.60f + compressed * (1f - p.highlightRolloff * 0.28f)
        }
        x = x.coerceIn(0f, 1f)
        if (p.contrastS != 0f) {
            val s = x * x * (3f - 2f * x)
            x = (x + (s - x) * p.contrastS).coerceIn(0f, 1f)
        }
        if (p.blackCrush > 0f) {
            x = ((x - p.blackCrush) / (1f - p.blackCrush)).coerceIn(0f, 1f)
        }
        if (p.blackLift > 0f) {
            x = p.blackLift + x * (1f - p.blackLift)
        }
        return x.coerceIn(0f, 1f)
    }

    /**
     * Lazily computes and caches a packed 17x17x17 RGB 3D LUT for [preset].
     */
    fun getOrBakeLut(preset: FilmPreset): IntArray {
        return lutCache.getOrPut(preset) {
            val profile = getFilmProfile(preset)
            val lut = IntArray(LUT_TOTAL)
            val denom = (LUT_SIZE - 1).toFloat()
            var idx = 0
            for (b in 0 until LUT_SIZE) {
                val bf = b / denom
                for (g in 0 until LUT_SIZE) {
                    val gf = g / denom
                    for (r in 0 until LUT_SIZE) {
                        lut[idx++] = evaluateProfileSample(r / denom, gf, bf, profile)
                    }
                }
            }
            lut
        }
    }

    /**
     * Fixed-point trilinear 3D LUT evaluation on a single RGB pixel, blended by [intensity].
     * Preserves alpha channel cleanly.
     */
    fun evaluateFilmLutRgb(argb: Int, preset: FilmPreset, intensity: Float = 1.0f): Int {
        if (preset == FilmPreset.NONE || intensity <= 0.005f) return argb
        val alphaMask = argb and 0xFF000000.toInt()
        if (alphaMask == 0) return argb

        val lut = getOrBakeLut(preset)
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF

        val outRgb = evalLutFixed(r, g, b, lut)
        val blend = (intensity.coerceIn(0f, 1f) * 256f + 0.5f).toInt()
        if (blend >= 256) return alphaMask or outRgb

        val outR = (outRgb shr 16) and 0xFF
        val outG = (outRgb shr 8) and 0xFF
        val outB = outRgb and 0xFF
        val finalR = r + (((outR - r) * blend) shr 8)
        val finalG = g + (((outG - g) * blend) shr 8)
        val finalB = b + (((outB - b) * blend) shr 8)
        return alphaMask or (finalR shl 16) or (finalG shl 8) or finalB
    }

    private fun evalLutFixed(r: Int, g: Int, b: Int, lut: IntArray): Int {
        val posR = (r * 16 shl 8) / 255
        val posG = (g * 16 shl 8) / 255
        val posB = (b * 16 shl 8) / 255

        val r0 = min(posR shr 8, 15)
        val g0 = min(posG shr 8, 15)
        val b0 = min(posB shr 8, 15)

        val fr = posR - (r0 shl 8)
        val fg = posG - (g0 shl 8)
        val fb = posB - (b0 shl 8)

        val baseIdx = (b0 * 289) + (g0 * 17) + r0
        val c000 = lut[baseIdx]
        val c100 = lut[baseIdx + 1]
        val c010 = lut[baseIdx + 17]
        val c110 = lut[baseIdx + 18]
        val c001 = lut[baseIdx + 289]
        val c101 = lut[baseIdx + 290]
        val c011 = lut[baseIdx + 306]
        val c111 = lut[baseIdx + 307]

        val outR = trilinearChannel(c000 shr 16, c100 shr 16, c010 shr 16, c110 shr 16, c001 shr 16, c101 shr 16, c011 shr 16, c111 shr 16, fr, fg, fb)
        val outG = trilinearChannel(c000 shr 8, c100 shr 8, c010 shr 8, c110 shr 8, c001 shr 8, c101 shr 8, c011 shr 8, c111 shr 8, fr, fg, fb)
        val outB = trilinearChannel(c000, c100, c010, c110, c001, c101, c011, c111, fr, fg, fb)
        return (outR shl 16) or (outG shl 8) or outB
    }

    private fun trilinearChannel(
        v000: Int, v100: Int, v010: Int, v110: Int,
        v001: Int, v101: Int, v011: Int, v111: Int,
        fr: Int, fg: Int, fb: Int
    ): Int {
        val a000 = v000 and 0xFF; val a100 = v100 and 0xFF
        val a010 = v010 and 0xFF; val a110 = v110 and 0xFF
        val a001 = v001 and 0xFF; val a101 = v101 and 0xFF
        val a011 = v011 and 0xFF; val a111 = v111 and 0xFF

        val c00 = a000 + (((a100 - a000) * fr) shr 8)
        val c10 = a010 + (((a110 - a010) * fr) shr 8)
        val c01 = a001 + (((a101 - a001) * fr) shr 8)
        val c11 = a011 + (((a111 - a011) * fr) shr 8)

        val c0 = c00 + (((c10 - c00) * fg) shr 8)
        val c1 = c01 + (((c11 - c01) * fg) shr 8)
        return (c0 + (((c1 - c0) * fb) shr 8)).coerceIn(0, 255)
    }

    /**
     * Pure buffer 3D LUT pass (100% JVM unit-testable, preserves alpha).
     */
    fun applyFilmLutToBuffer(
        pixels: IntArray,
        preset: FilmPreset,
        intensity: Float = 1.0f
    ) {
        if (preset == FilmPreset.NONE || intensity <= 0.005f) return
        val lut = getOrBakeLut(preset)
        val blend = (intensity.coerceIn(0f, 1f) * 256f + 0.5f).toInt()
        val fullBlend = blend >= 256

        for (i in pixels.indices) {
            val c = pixels[i]
            val alphaMask = c and 0xFF000000.toInt()
            if (alphaMask == 0) continue

            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val outRgb = evalLutFixed(r, g, b, lut)

            pixels[i] = if (fullBlend) {
                alphaMask or outRgb
            } else {
                val outR = (outRgb shr 16) and 0xFF
                val outG = (outRgb shr 8) and 0xFF
                val outB = outRgb and 0xFF
                val finalR = r + (((outR - r) * blend) shr 8)
                val finalG = g + (((outG - g) * blend) shr 8)
                val finalB = b + (((outB - b) * blend) shr 8)
                alphaMask or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }
    }

    /**
     * Applies [preset] 3D LUT onto [source] in 64-row strips to prevent 4K heap OOM.
     * Preserves source alpha and returns [source] immediately if [preset] is NONE or [intensity] <= 0.
     */
    fun applyFilmLut(
        source: Bitmap,
        preset: FilmPreset,
        intensity: Float = 1.0f,
        inPlace: Boolean = false
    ): Bitmap {
        if (preset == FilmPreset.NONE || intensity <= 0.005f) return source
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source

        val target = if (inPlace && source.isMutable) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val lut = getOrBakeLut(preset)
        val blend = (intensity.coerceIn(0f, 1f) * 256f + 0.5f).toInt()
        val fullBlend = blend >= 256
        val stripRows = min(STRIP_ROWS, h)
        val stripBuf = IntArray(w * stripRows)

        var y = 0
        while (y < h) {
            val rows = min(stripRows, h - y)
            target.getPixels(stripBuf, 0, w, 0, y, w, rows)
            val count = w * rows
            for (i in 0 until count) {
                val c = stripBuf[i]
                val alphaMask = c and 0xFF000000.toInt()
                if (alphaMask == 0) continue

                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val outRgb = evalLutFixed(r, g, b, lut)

                stripBuf[i] = if (fullBlend) {
                    alphaMask or outRgb
                } else {
                    val outR = (outRgb shr 16) and 0xFF
                    val outG = (outRgb shr 8) and 0xFF
                    val outB = outRgb and 0xFF
                    val finalR = r + (((outR - r) * blend) shr 8)
                    val finalG = g + (((outG - g) * blend) shr 8)
                    val finalB = b + (((outB - b) * blend) shr 8)
                    alphaMask or (finalR shl 16) or (finalG shl 8) or finalB
                }
            }
            target.setPixels(stripBuf, 0, w, 0, y, w, rows)
            y += rows
        }
        return target
    }

    /**
     * Generates a compact [sizePx]x[sizePx] live swatch preview for UI preset cards.
     */
    fun generatePresetSwatch(
        sourceThumbnail: Bitmap?,
        preset: FilmPreset,
        sizePx: Int = 64
    ): Bitmap {
        val safeSize = sizePx.coerceIn(24, 128)
        val base = if (sourceThumbnail != null && !sourceThumbnail.isRecycled && sourceThumbnail.width > 0 && sourceThumbnail.height > 0) {
            val sw = sourceThumbnail.width
            val sh = sourceThumbnail.height
            val cropSide = min(sw, sh)
            val sx = (sw - cropSide) / 2
            val sy = (sh - cropSide) / 2
            val cropped = Bitmap.createBitmap(sourceThumbnail, sx, sy, cropSide, cropSide)
            val scaled = Bitmap.createScaledBitmap(cropped, safeSize, safeSize, true)
            if (cropped != scaled) cropped.recycle()
            if (scaled.isMutable) scaled else scaled.copy(Bitmap.Config.ARGB_8888, true).also { scaled.recycle() }
        } else {
            createFallbackSwatchBitmap(safeSize)
        }
        return applyFilmLut(base, preset, 1.0f, inPlace = true)
    }

    private fun createFallbackSwatchBitmap(size: Int): Bitmap {
        val pixels = IntArray(size * size)
        val denom = (size - 1).coerceAtLeast(1).toFloat()
        for (y in 0 until size) {
            val yf = y / denom
            for (x in 0 until size) {
                val xf = x / denom
                val r = (42f + xf * 185f * (1f - yf * 0.45f)).toInt().coerceIn(0, 255)
                val g = (52f + (1f - yf) * 145f + xf * 45f).toInt().coerceIn(0, 255)
                val b = (88f + (1f - xf) * 135f * (1f - yf * 0.35f)).toInt().coerceIn(0, 255)
                pixels[y * size + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }

    /**
     * Linear 5x4 ColorMatrix approximation preserved for backward-compatible ColorMatrix callers.
     */
    fun getFilmColorMatrix(preset: FilmPreset): ColorMatrix {
        return when (preset) {
            FilmPreset.NONE -> ColorMatrix()

            FilmPreset.FUJI_CLASSIC_CHROME -> ColorMatrix(
                floatArrayOf(
                    1.06f, 0f, 0f, 0f, 4f,
                    0f, 0.98f, 0f, 0f, 2f,
                    0f, 0f, 0.90f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(0.88f) }) }

            FilmPreset.FUJI_VELVIA -> ColorMatrix(
                floatArrayOf(
                    1.18f, 0f, 0f, 0f, -8f,
                    0f, 1.14f, 0f, 0f, -6f,
                    0f, 0f, 1.16f, 0f, -4f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(1.35f) }) }

            FilmPreset.FUJI_PROVIA -> ColorMatrix(
                floatArrayOf(
                    1.04f, 0f, 0f, 0f, 0f,
                    0f, 1.04f, 0f, 0f, 0f,
                    0f, 0f, 1.04f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(1.08f) }) }

            FilmPreset.KODAK_PORTRA_400 -> ColorMatrix(
                floatArrayOf(
                    1.12f, 0.02f, 0f, 0f, 12f,
                    0f, 1.04f, 0f, 0f, 6f,
                    0f, 0f, 0.92f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(1.02f) }) }

            FilmPreset.KODAK_TRI_X, FilmPreset.STREET_MONO_GR -> {
                val m = ColorMatrix().apply { setSaturation(0f) }
                val c = if (preset == FilmPreset.STREET_MONO_GR) 1.48f else 1.35f
                val t = (1.0f - c) * 128f
                m.postConcat(ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t, 0f, 0f, 0f, 1f, 0f)))
                m
            }

            FilmPreset.LEICA_MONOCHROME -> {
                val m = ColorMatrix().apply { setSaturation(0f) }
                val c = 1.12f
                val t = (1.0f - c) * 128f
                m.postConcat(ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t + 4f, 0f, c, 0f, 0f, t + 4f, 0f, 0f, c, 0f, t + 4f, 0f, 0f, 0f, 1f, 0f)))
                m
            }

            FilmPreset.CINEMATIC_TEAL_ORANGE -> ColorMatrix(
                floatArrayOf(
                    1.14f, 0f, -0.04f, 0f, -4f,
                    0f, 1.04f, 0f, 0f, 4f,
                    -0.04f, 0f, 1.10f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(1.12f) }) }

            FilmPreset.VINTAGE_WARMTH, FilmPreset.DISPOSABLE_WARM_800 -> ColorMatrix(
                floatArrayOf(
                    1.10f, 0.05f, 0f, 0f, 18f,
                    0f, 1.02f, 0f, 0f, 10f,
                    0f, 0f, 0.86f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(0.92f) }) }

            FilmPreset.DREAM_GLOW -> ColorMatrix(
                floatArrayOf(
                    0.96f, 0f, 0f, 0f, 14f,
                    0f, 0.95f, 0f, 0f, 12f,
                    0f, 0f, 0.94f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            FilmPreset.HYBRID_CCD_2005 -> ColorMatrix(
                floatArrayOf(
                    1.14f, 0f, 0f, 0f, -6f,
                    0f, 1.08f, 0f, 0f, -4f,
                    0f, 0f, 1.12f, 0f, 2f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(1.12f) }) }

            FilmPreset.MINIDV_CAM_2002 -> ColorMatrix(
                floatArrayOf(
                    0.96f, 0f, 0f, 0f, 4f,
                    0f, 1.02f, 0f, 0f, 8f,
                    0f, 0f, 1.08f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(0.90f) }) }

            FilmPreset.SLIDE_CHROME_100 -> ColorMatrix(
                floatArrayOf(
                    1.24f, 0f, 0f, 0f, -18f,
                    0f, 1.22f, 0f, 0f, -16f,
                    0f, 0f, 1.26f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(1.24f) }) }

            FilmPreset.DISPOSABLE_COOL_400 -> ColorMatrix(
                floatArrayOf(
                    1.02f, 0f, 0f, 0f, 6f,
                    0f, 1.06f, 0f, 0f, 14f,
                    0f, 0f, 1.00f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(0.96f) }) }

            FilmPreset.TOY_MEDIUM_120 -> ColorMatrix(
                floatArrayOf(
                    1.08f, 0f, 0f, 0f, 6f,
                    0f, 1.04f, 0f, 0f, 8f,
                    0f, 0f, 0.96f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(0.94f) }) }

            FilmPreset.RANGEFINDER_400 -> ColorMatrix(
                floatArrayOf(
                    1.08f, 0.02f, 0f, 0f, 6f,
                    0f, 1.05f, 0.02f, 0f, 6f,
                    0f, -0.02f, 0.96f, 0f, 2f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply { postConcat(ColorMatrix().apply { setSaturation(1.06f) }) }
        }
    }

    /**
     * Consolidated ColorMatrixColorFilter for 3-way grading, saturation, contrast, brightness & dim.
     * Pass [preset] = FilmPreset.NONE when [applyFilmLut] has already been applied in the pixel pipeline.
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

        if (preset != FilmPreset.NONE) {
            composite.postConcat(getFilmColorMatrix(preset))
        }

        if (colorGradingEnabled) {
            val gradingMatrix = AtharColorGradingEngine.createColorGradingMatrix(
                liftR, liftG, liftB,
                gammaR, gammaG, gammaB,
                gainR, gainG, gainB
            )
            composite.postConcat(gradingMatrix)
        }

        val effectiveSat = if (isGrayscale) 0f else saturation.coerceIn(0f, 2.5f)
        if (effectiveSat != 1.0f) {
            composite.postConcat(ColorMatrix().apply { setSaturation(effectiveSat) })
        }

        val c = contrast.coerceIn(0.4f, 2.0f)
        if (c != 1.0f) {
            val t = (1.0f - c) * 128f
            composite.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        val bOffset = (brightness.coerceIn(-0.6f, 0.6f) * 255f)
        if (bOffset != 0f) {
            composite.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, bOffset,
                        0f, 1f, 0f, 0f, bOffset,
                        0f, 0f, 1f, 0f, bOffset,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        val dimMult = (1.0f - dim.coerceIn(0f, 0.95f))
        if (dimMult != 1.0f) {
            composite.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        dimMult, 0f, 0f, 0f, 0f,
                        0f, dimMult, 0f, 0f, 0f,
                        0f, 0f, dimMult, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        return ColorMatrixColorFilter(composite)
    }
}
