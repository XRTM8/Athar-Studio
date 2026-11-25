package app.xrtm8.athar.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import app.xrtm8.athar.model.FilmPreset
import app.xrtm8.athar.model.OpticalLensState
import app.xrtm8.athar.model.OpticalLensType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Image-Reactive Optical Lens Attachment Engine (Dazz-inspired, zero AI-slop overlays):
 * - SOFT_WIDE: Radial sharp-center to soft-corner blend + r^2 radial edge chromatic aberration.
 * - ORGANIC_VIGNETTE: Aspect-matched elliptical cos^4 optical light falloff.
 * - HALATION_LEAK: Dual-stage crimson core + wide amber specular scatter + organic edge gate leak.
 * - PRISM_DIFFRACTION: Highlight-driven directional spectral taps + subtle edge caustic.
 * - STARBURST_4PT: Tile-max specular peak isolation -> orthogonal exponential ray decay (white-gold to purple-cyan tips).
 * - Presets TOY_MEDIUM_120 & MINIDV_CAM_2002 also get inherent subtle optical physics (corner softness / 4:1:1 chroma bleed).
 *
 * All spatial kernels scale with [referenceWidthPx] so 480px thumbs, 1080p previews, and 4K exports match 1:1.
 */
object AtharOpticalLensEngine {

    /**
     * Pre-composite lens geometry & edge optics (runs on framed/cropped bitmap or photo buffer).
     */
    @JvmOverloads
    fun applyLensGeometryAndEdgeEffects(
        source: Bitmap,
        state: OpticalLensState,
        filmPreset: FilmPreset = FilmPreset.NONE,
        referenceWidthPx: Int = source.width
    ): Bitmap {
        val needsMiniDvBleed = filmPreset == FilmPreset.MINIDV_CAM_2002
        val needsToyOptics = filmPreset == FilmPreset.TOY_MEDIUM_120 &&
            state.type != OpticalLensType.SOFT_WIDE &&
            state.type != OpticalLensType.ORGANIC_VIGNETTE
        val needsLensGeo = state.isEnabled &&
            (state.type == OpticalLensType.SOFT_WIDE || state.type == OpticalLensType.ORGANIC_VIGNETTE)

        if (!needsMiniDvBleed && !needsToyOptics && !needsLensGeo) return source

        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source

        val needsFullFrameSoftWide = needsToyOptics ||
            (needsLensGeo && state.type == OpticalLensType.SOFT_WIDE)

        if (!needsFullFrameSoftWide) {
            // Strip-based row processing (~64 rows) avoids a full 33MB IntArray at 4K
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val stripRows = min(64, h)
            val strip = IntArray(w * stripRows)
            val rowScratch = if (needsMiniDvBleed) IntArray(w) else IntArray(0)
            val vignetteActive = needsLensGeo && state.type == OpticalLensType.ORGANIC_VIGNETTE
            val miniDvRadius = if (needsMiniDvBleed) ((referenceWidthPx / 1080f) * 3f).toInt().coerceIn(1, 8) else 0

            var yStart = 0
            while (yStart < h) {
                val rows = min(stripRows, h - yStart)
                source.getPixels(strip, 0, w, 0, yStart, w, rows)
                if (needsMiniDvBleed) {
                    applyMiniDvChromaBleedRows(strip, w, rows, miniDvRadius, rowScratch)
                }
                if (vignetteActive) {
                    applyOrganicVignetteRows(strip, w, h, yStart, rows, state.intensity, state.spread)
                }
                out.setPixels(strip, 0, w, 0, yStart, w, rows)
                yStart += rows
            }
            return out
        }

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        if (needsMiniDvBleed) {
            applyMiniDvChromaBleedToBuffer(pixels, w, h, referenceWidthPx)
        }

        if (needsToyOptics) {
            val softToy = OpticalLensState(
                type = OpticalLensType.SOFT_WIDE,
                intensity = 0.42f,
                spread = 0.45f,
                angleDegrees = 45f
            )
            applySoftWideToBuffer(pixels, w, h, softToy, referenceWidthPx)
            applyOrganicVignetteToBuffer(pixels, w, h, intensity = 0.40f, spread = 0.55f)
        }

        if (needsLensGeo && state.type == OpticalLensType.SOFT_WIDE) {
            applySoftWideToBuffer(pixels, w, h, state, referenceWidthPx)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Post-composite highlight diffraction & halation overlay (runs after framing & film LUT, composited via SCREEN).
     */
    @JvmOverloads
    fun applyLensHighlightOverlay(
        source: Bitmap,
        state: OpticalLensState,
        filmPreset: FilmPreset = FilmPreset.NONE,
        referenceWidthPx: Int = source.width
    ): Bitmap {
        val overlay = extractLensHighlightOverlay(source, state, filmPreset, referenceWidthPx) ?: return source

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            alpha = 255
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                blendMode = android.graphics.BlendMode.SCREEN
            } else {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
        }
        canvas.drawBitmap(overlay, null, Rect(0, 0, source.width, source.height), paint)
        overlay.recycle()
        return result
    }

    /**
     * Generates a highlight-only downscaled overlay bitmap (`maxDim <= 512px`) for SCREEN compositing,
     * or null when no highlight attachment is active.
     */
    @JvmOverloads
    fun extractLensHighlightOverlay(
        source: Bitmap,
        state: OpticalLensState,
        filmPreset: FilmPreset = FilmPreset.NONE,
        referenceWidthPx: Int = source.width
    ): Bitmap? {
        val explicitLensActive = state.isEnabled && (
            state.type == OpticalLensType.HALATION_LEAK ||
                state.type == OpticalLensType.PRISM_DIFFRACTION ||
                state.type == OpticalLensType.STARBURST_4PT
            )
        val implicitDreamHalation = !explicitLensActive && filmPreset == FilmPreset.DREAM_GLOW
        if (!explicitLensActive && !implicitDreamHalation) return null

        val sw = source.width
        val sh = source.height
        if (sw <= 0 || sh <= 0) return null

        val maxDim = if (state.type == OpticalLensType.STARBURST_4PT) 420f else 512f
        val scale = min(1f, maxDim / max(sw, sh).toFloat())
        val pw = (sw * scale).toInt().coerceAtLeast(16)
        val ph = (sh * scale).toInt().coerceAtLeast(16)

        val smallSrc = Bitmap.createScaledBitmap(source, pw, ph, true)
        val srcPixels = IntArray(pw * ph)
        smallSrc.getPixels(srcPixels, 0, pw, 0, 0, pw, ph)
        smallSrc.recycle()

        val effectiveState = if (implicitDreamHalation) {
            OpticalLensState(OpticalLensType.HALATION_LEAK, intensity = 0.42f, spread = 0.55f, angleDegrees = 30f)
        } else {
            state
        }

        val suppressColorOnMono = filmPreset.isMonochrome && !explicitLensActive
        val refScale = (pw.toFloat() / (referenceWidthPx.coerceAtLeast(1) * scale)).coerceIn(0.5f, 2.0f)
        val outPixels = when (effectiveState.type) {
            OpticalLensType.HALATION_LEAK ->
                buildHalationLeakBuffer(srcPixels, pw, ph, effectiveState, suppressColorOnMono, refScale)
            OpticalLensType.PRISM_DIFFRACTION ->
                buildPrismDiffractionBuffer(srcPixels, pw, ph, effectiveState, suppressColorOnMono, refScale)
            OpticalLensType.STARBURST_4PT ->
                buildStarburst4PtBuffer(srcPixels, pw, ph, effectiveState, suppressColorOnMono, refScale)
            else -> return null
        }

        val overlayBmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
        overlayBmp.setPixels(outPixels, 0, pw, 0, 0, pw, ph)
        return overlayBmp
    }

    // =========================================================================
    // Pure Kotlin Pixel Buffer Algorithms (100% JVM Unit-Testable)
    // =========================================================================

    fun applyMiniDvChromaBleedToBuffer(
        pixels: IntArray,
        w: Int,
        h: Int,
        referenceWidthPx: Int = w
    ) {
        val radius = ((referenceWidthPx / 1080f) * 3f).toInt().coerceIn(1, 8)
        val rowOut = IntArray(w)
        applyMiniDvChromaBleedRows(pixels, w, h, radius, rowOut)
    }

    private fun applyMiniDvChromaBleedRows(
        pixels: IntArray,
        w: Int,
        rows: Int,
        radius: Int,
        rowOut: IntArray
    ) {
        val window = radius * 2 + 1
        for (y in 0 until rows) {
            val rowOff = y * w
            var sumRg = 0
            var sumBg = 0
            for (i in -radius..radius) {
                val c = pixels[rowOff + i.coerceIn(0, w - 1)]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                sumRg += (r - g)
                sumBg += (b - g)
            }
            for (x in 0 until w) {
                val c = pixels[rowOff + x]
                val a = c and 0xFF000000.toInt()
                if (a == 0) {
                    rowOut[x] = c
                } else {
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    val blurredR = (g + sumRg / window).coerceIn(0, 255)
                    val blurredB = (g + sumBg / window).coerceIn(0, 255)
                    val nr = (r * 0.35f + blurredR * 0.65f).toInt().coerceIn(0, 255)
                    val nb = (b * 0.35f + blurredB * 0.65f).toInt().coerceIn(0, 255)
                    rowOut[x] = a or (nr shl 16) or (g shl 8) or nb
                }
                val cRem = pixels[rowOff + (x - radius).coerceIn(0, w - 1)]
                val cAdd = pixels[rowOff + (x + radius + 1).coerceIn(0, w - 1)]
                val gRem = (cRem shr 8) and 0xFF
                val gAdd = (cAdd shr 8) and 0xFF
                sumRg += (((cAdd shr 16) and 0xFF) - gAdd) - (((cRem shr 16) and 0xFF) - gRem)
                sumBg += ((cAdd and 0xFF) - gAdd) - ((cRem and 0xFF) - gRem)
            }
            System.arraycopy(rowOut, 0, pixels, rowOff, w)
        }
    }

    fun applySoftWideToBuffer(
        pixels: IntArray,
        w: Int,
        h: Int,
        state: OpticalLensState,
        referenceWidthPx: Int = w
    ) {
        val intensity = state.intensity.coerceIn(0f, 1f)
        if (intensity <= 0.01f || w <= 2 || h <= 2) return

        val blurRadius = ((referenceWidthPx / 1080f) * (3f + state.spread * 7f) * intensity)
            .toInt().coerceIn(1, 14)

        // Downscale the soft-edge Gaussian pass on large (>720px) buffers to prevent 4K heap spikes
        val maxSide = max(w, h)
        val bw: Int
        val bh: Int
        val blurred: IntArray
        if (maxSide > 720) {
            val scale = 720f / maxSide
            bw = (w * scale).toInt().coerceAtLeast(32)
            bh = (h * scale).toInt().coerceAtLeast(32)
            val down = IntArray(bw * bh)
            for (dy in 0 until bh) {
                val sy = (dy * h / bh).coerceIn(0, h - 1)
                val dstRow = dy * bw
                val srcRow = sy * w
                for (dx in 0 until bw) {
                    down[dstRow + dx] = pixels[srcRow + (dx * w / bw).coerceIn(0, w - 1)]
                }
            }
            val scaledRadius = (blurRadius * scale).toInt().coerceIn(1, 12)
            blurred = AtharOpticalGlowEngine.convolveGaussianBuffer(down, bw, bh, scaledRadius, passes = 2)
        } else {
            bw = w
            bh = h
            blurred = AtharOpticalGlowEngine.convolveGaussianBuffer(pixels, w, h, blurRadius, passes = 2)
        }
        val orig = pixels.copyOf()

        val cx = (w - 1) * 0.5f
        val cy = (h - 1) * 0.5f
        val invHalfW = 1f / max(1f, cx)
        val invHalfH = 1f / max(1f, cy)
        val caStrength = (0.004f + state.spread * 0.012f) * intensity

        for (y in 0 until h) {
            val ny = (y - cy) * invHalfH
            val rowOff = y * w
            val byBlur = if (bh == h) y else (y * bh / h).coerceIn(0, bh - 1)
            val blurRowOff = byBlur * bw
            for (x in 0 until w) {
                val c = orig[rowOff + x]
                val a = c and 0xFF000000.toInt()
                if (a == 0) continue

                val nx = (x - cx) * invHalfW
                val rDist = hypot(nx, ny)
                val edgeT = ((rDist - 0.38f) / 0.92f).coerceIn(0f, 1f)
                val smoothEdge = edgeT * edgeT * (3f - 2f * edgeT)
                val blurMix = (smoothEdge * intensity * 0.88f).coerceIn(0f, 1f)

                val r2 = rDist * rDist
                val rx = (cx + (x - cx) * (1f + caStrength * r2)).toInt().coerceIn(0, w - 1)
                val ry = (cy + (y - cy) * (1f + caStrength * r2)).toInt().coerceIn(0, h - 1)
                val bx = (cx + (x - cx) * (1f - caStrength * r2)).toInt().coerceIn(0, w - 1)
                val by = (cy + (y - cy) * (1f - caStrength * r2)).toInt().coerceIn(0, h - 1)

                val sharpR = (orig[ry * w + rx] shr 16) and 0xFF
                val sharpG = (c shr 8) and 0xFF
                val sharpB = orig[by * w + bx] and 0xFF

                val bxBlur = if (bw == w) x else (x * bw / w).coerceIn(0, bw - 1)
                val bc = blurred[blurRowOff + bxBlur]
                val blurR = (bc shr 16) and 0xFF
                val blurG = (bc shr 8) and 0xFF
                val blurB = bc and 0xFF

                val outR = (sharpR + (blurR - sharpR) * blurMix).toInt().coerceIn(0, 255)
                val outG = (sharpG + (blurG - sharpG) * blurMix).toInt().coerceIn(0, 255)
                val outB = (sharpB + (blurB - sharpB) * blurMix).toInt().coerceIn(0, 255)

                pixels[rowOff + x] = a or (outR shl 16) or (outG shl 8) or outB
            }
        }
    }

    fun applyOrganicVignetteToBuffer(
        pixels: IntArray,
        w: Int,
        h: Int,
        intensity: Float,
        spread: Float = 0.50f
    ) {
        applyOrganicVignetteRows(pixels, w, h, yStart = 0, rows = h, intensity = intensity, spread = spread)
    }

    private fun applyOrganicVignetteRows(
        pixels: IntArray,
        w: Int,
        totalH: Int,
        yStart: Int,
        rows: Int,
        intensity: Float,
        spread: Float
    ) {
        val safeIntensity = intensity.coerceIn(0f, 1f)
        if (safeIntensity <= 0.01f || w <= 1 || totalH <= 1) return

        val cx = (w - 1) * 0.5f
        val cy = (totalH - 1) * 0.5f
        val invHalfW = 1f / max(1f, cx)
        val invHalfH = 1f / max(1f, cy)
        val angleScale = (0.58f + (1f - spread.coerceIn(0f, 1f)) * 0.40f) * (PI.toFloat() * 0.48f)

        for (localY in 0 until rows) {
            val y = yStart + localY
            val ny = (y - cy) * invHalfH
            val rowOff = localY * w
            for (x in 0 until w) {
                val c = pixels[rowOff + x]
                val a = c and 0xFF000000.toInt()
                if (a == 0) continue

                val nx = (x - cx) * invHalfW
                val rNorm = hypot(nx, ny) / 1.4142135f
                val theta = (rNorm * angleScale).coerceIn(0f, 1.42f)
                val cosTheta = cos(theta)
                val cos4 = cosTheta * cosTheta * cosTheta * cosTheta
                val gain = (1f - safeIntensity * (1f - cos4)).coerceIn(0.06f, 1f)

                val r = (((c shr 16) and 0xFF) * gain).toInt().coerceIn(0, 255)
                val g = (((c shr 8) and 0xFF) * gain).toInt().coerceIn(0, 255)
                val b = ((c and 0xFF) * gain).toInt().coerceIn(0, 255)
                pixels[rowOff + x] = a or (r shl 16) or (g shl 8) or b
            }
        }
    }

    @JvmOverloads
    fun buildHalationLeakBuffer(
        srcPixels: IntArray,
        w: Int,
        h: Int,
        state: OpticalLensState,
        monochrome: Boolean = false,
        refScale: Float = 1.0f
    ): IntArray {
        val intensity = state.intensity.coerceIn(0f, 1f)
        val spread = state.spread.coerceIn(0f, 1f)
        val threshold = (0.76f - spread * 0.18f).coerceIn(0.45f, 0.90f)

        val rCore = if (monochrome) 1.0f else 1.45f
        val gCore = if (monochrome) 1.0f else 0.16f
        val bCore = if (monochrome) 1.0f else 0.04f

        val rScat = if (monochrome) 0.85f else 1.25f
        val gScat = if (monochrome) 0.85f else 0.48f
        val bScat = if (monochrome) 0.85f else 0.10f

        val coreHi = AtharOpticalGlowEngine.isolateHighlights(srcPixels, threshold + 0.06f, 0.10f, rCore, gCore, bCore, squaredFalloff = true)
        val scatterHi = AtharOpticalGlowEngine.isolateHighlights(srcPixels, threshold - 0.04f, 0.14f, rScat, gScat, bScat, squaredFalloff = true)

        val coreRadius = max(2, (w * (0.010f + spread * 0.014f) * refScale).toInt())
        val scatterRadius = max(3, (w * (0.028f + spread * 0.036f) * refScale).toInt())

        val coreBlur = AtharOpticalGlowEngine.convolveGaussianBuffer(coreHi, w, h, coreRadius, passes = 3)
        val scatterBlur = AtharOpticalGlowEngine.convolveGaussianBuffer(scatterHi, w, h, scatterRadius, passes = 3)
        val combined = AtharOpticalGlowEngine.addArgbBuffers(coreBlur, scatterBlur)

        // Subtle organic corner film-gate light leak oriented by angleDegrees
        val rad = Math.toRadians(state.angleDegrees.toDouble())
        val leakOriginX = (0.5f + 0.52f * cos(rad).toFloat()) * w
        val leakOriginY = (0.5f + 0.52f * sin(rad).toFloat()) * h
        val leakRadius = max(w, h) * (0.42f + spread * 0.25f)
        val leakStrength = intensity * 0.42f

        for (y in 0 until h) {
            val rowOff = y * w
            for (x in 0 until w) {
                val c = combined[rowOff + x]
                var r = (((c shr 16) and 0xFF) * intensity).toInt()
                var g = (((c shr 8) and 0xFF) * intensity).toInt()
                var b = ((c and 0xFF) * intensity).toInt()

                if (!monochrome) {
                    val dist = hypot(x - leakOriginX, y - leakOriginY)
                    val t = (1f - dist / leakRadius).coerceIn(0f, 1f)
                    if (t > 0f) {
                        val ripple = 0.86f + 0.14f * sin((x + y) * 0.03f)
                        val lAmt = (t * t * leakStrength * ripple)
                        r = min(255, r + (lAmt * 255f).toInt())
                        g = min(255, g + (lAmt * 105f).toInt())
                        b = min(255, b + (lAmt * 22f).toInt())
                    }
                }
                val a = max(r, max(g, b)).coerceIn(0, 255)
                combined[rowOff + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return combined
    }

    @JvmOverloads
    fun buildPrismDiffractionBuffer(
        srcPixels: IntArray,
        w: Int,
        h: Int,
        state: OpticalLensState,
        monochrome: Boolean = false,
        refScale: Float = 1.0f
    ): IntArray {
        val intensity = state.intensity.coerceIn(0f, 1f)
        val spread = state.spread.coerceIn(0f, 1f)
        val highlights = AtharOpticalGlowEngine.isolateHighlights(
            srcPixels,
            threshold = (0.72f - spread * 0.16f).coerceIn(0.45f, 0.85f),
            knee = 0.12f,
            rMult = 1f, gMult = 1f, bMult = 1f,
            squaredFalloff = true
        )
        val blurRadius = max(2, (w * 0.015f * refScale).toInt())
        val baseHi = AtharOpticalGlowEngine.convolveGaussianBuffer(highlights, w, h, blurRadius, passes = 2)

        val rad = Math.toRadians(state.angleDegrees.toDouble())
        val dirX = cos(rad).toFloat()
        val dirY = sin(rad).toFloat()
        val maxShift = w * (0.035f + spread * 0.075f) * refScale

        // 6 spectral wavelength bands (Violet -> Blue -> Cyan -> Green -> Amber -> Red)
        val bandWeights = floatArrayOf(-1.0f, -0.6f, -0.2f, 0.2f, 0.6f, 1.0f)
        val bandR = floatArrayOf(0.65f, 0.15f, 0.05f, 0.25f, 1.00f, 1.00f)
        val bandG = floatArrayOf(0.15f, 0.45f, 0.85f, 0.92f, 0.72f, 0.18f)
        val bandB = floatArrayOf(1.00f, 1.00f, 0.95f, 0.32f, 0.08f, 0.10f)

        val out = IntArray(w * h)
        val bandCount = bandWeights.size

        for (y in 0 until h) {
            val rowOff = y * w
            for (x in 0 until w) {
                var accR = 0f; var accG = 0f; var accB = 0f
                for (k in 0 until bandCount) {
                    val shift = bandWeights[k] * maxShift
                    val sx = (x - dirX * shift).toInt()
                    val sy = (y - dirY * shift).toInt()
                    if (sx in 0 until w && sy in 0 until h) {
                        val sc = baseHi[sy * w + sx]
                        val luma = ((sc shr 16) and 0xFF) * 0.30f + ((sc shr 8) and 0xFF) * 0.59f + (sc and 0xFF) * 0.11f
                        if (luma > 1f) {
                            if (monochrome) {
                                accR += luma * 0.35f; accG += luma * 0.35f; accB += luma * 0.35f
                            } else {
                                accR += luma * bandR[k] * 0.42f
                                accG += luma * bandG[k] * 0.42f
                                accB += luma * bandB[k] * 0.42f
                            }
                        }
                    }
                }

                // Subtle localized edge caustic band along prism angle so even muted scenes read gracefully
                if (!monochrome) {
                    val nx = x / w.toFloat() - 0.5f
                    val ny = y / h.toFloat() - 0.5f
                    val proj = nx * dirX + ny * dirY
                    val edgeDist = abs(proj - 0.34f)
                    if (edgeDist < 0.18f) {
                        val bandT = (proj - 0.16f) / 0.36f
                        val falloff = (1f - edgeDist / 0.18f).pow(2f) * 38f
                        accR += falloff * (1f - bandT).coerceIn(0f, 1f)
                        accG += falloff * sin(bandT * PI.toFloat()).coerceIn(0f, 1f)
                        accB += falloff * bandT.coerceIn(0f, 1f)
                    }
                }

                val r = (accR * intensity).toInt().coerceIn(0, 255)
                val g = (accG * intensity).toInt().coerceIn(0, 255)
                val b = (accB * intensity).toInt().coerceIn(0, 255)
                val a = max(r, max(g, b))
                out[rowOff + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return out
    }

    @JvmOverloads
    fun buildStarburst4PtBuffer(
        srcPixels: IntArray,
        w: Int,
        h: Int,
        state: OpticalLensState,
        monochrome: Boolean = false,
        refScale: Float = 1.0f
    ): IntArray {
        val intensity = state.intensity.coerceIn(0f, 1f)
        val spread = state.spread.coerceIn(0f, 1f)
        val out = IntArray(w * h)
        if (intensity <= 0.01f || w <= 2 || h <= 2) return out

        val lumaGrid = FloatArray(w * h)
        for (i in srcPixels.indices) {
            val c = srcPixels[i]
            val a = (c ushr 24) and 0xFF
            if (a == 0) continue
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            lumaGrid[i] = ((0.299f * r + 0.587f * g + 0.114f * b) / 255f) * (a / 255f)
        }

        // Tile-based local specular peak detection (prevents flat white walls from flooding while catching every glint)
        val thresh = (0.82f - spread * 0.16f).coerceIn(0.55f, 0.92f)
        val tileSize = max(6, w / 48)
        data class StarPeak(val x: Int, val y: Int, val strength: Float)
        val peaks = ArrayList<StarPeak>(512)

        for (ty in 0 until h step tileSize) {
            val yEnd = min(h, ty + tileSize)
            for (tx in 0 until w step tileSize) {
                val xEnd = min(w, tx + tileSize)
                var bestScore = 0f
                var bestX = -1
                var bestY = -1
                for (y in ty until yEnd) {
                    val rowOff = y * w
                    for (x in tx until xEnd) {
                        val l = lumaGrid[rowOff + x]
                        if (l <= thresh) continue
                        val xL = max(0, x - 3); val xR = min(w - 1, x + 3)
                        val yT = max(0, y - 3); val yB = min(h - 1, y + 3)
                        val avgLocal = (
                            lumaGrid[yT * w + xL] + lumaGrid[yT * w + xR] +
                                lumaGrid[yB * w + xL] + lumaGrid[yB * w + xR]
                            ) * 0.25f
                        val excess = (l - avgLocal).coerceAtLeast(0f)
                        val absOver = ((l - thresh) / (1f - thresh).coerceAtLeast(0.05f)).coerceIn(0f, 1f)
                        // Small 4-neighbor centrality tiebreaker centers starburst on symmetric highlights
                        val x1L = max(0, x - 1); val x1R = min(w - 1, x + 1)
                        val y1T = max(0, y - 1); val y1B = min(h - 1, y + 1)
                        val avgImmediate = (
                            lumaGrid[y * w + x1L] + lumaGrid[y * w + x1R] +
                                lumaGrid[y1T * w + x] + lumaGrid[y1B * w + x]
                            ) * 0.25f
                        val score = excess * 1.4f + absOver * 0.55f + avgImmediate * 0.05f
                        if (score > bestScore) {
                            bestScore = score
                            bestX = x
                            bestY = y
                        }
                    }
                }
                if (bestX >= 0 && bestScore > 0.08f) {
                    peaks.add(StarPeak(bestX, bestY, bestScore.coerceIn(0f, 1.4f)))
                }
            }
        }

        if (peaks.isEmpty()) return out
        if (peaks.size > 1400) {
            peaks.sortByDescending { it.strength }
        }
        val limit = min(peaks.size, 1400)

        val rad1 = Math.toRadians(state.angleDegrees.toDouble())
        val rad2 = rad1 + PI / 2.0
        val dirsX = floatArrayOf(cos(rad1).toFloat(), -cos(rad1).toFloat(), cos(rad2).toFloat(), -cos(rad2).toFloat())
        val dirsY = floatArrayOf(sin(rad1).toFloat(), -sin(rad1).toFloat(), sin(rad2).toFloat(), -sin(rad2).toFloat())

        val rayLen = max(12, (w * (0.06f + spread * 0.14f) * refScale).toInt())
        val tau = rayLen * 0.34f
        val accR = FloatArray(w * h)
        val accG = FloatArray(w * h)
        val accB = FloatArray(w * h)

        for (pIdx in 0 until limit) {
            val peak = peaks[pIdx]
            val basePower = peak.strength * intensity * 235f

            // Hot core glow at peak center
            val cIdx = peak.y * w + peak.x
            accR[cIdx] += basePower
            accG[cIdx] += basePower * (if (monochrome) 1f else 0.94f)
            accB[cIdx] += basePower * (if (monochrome) 1f else 0.98f)

            for (dIdx in 0 until 4) {
                val dx = dirsX[dIdx]
                val dy = dirsY[dIdx]
                for (step in 1..rayLen) {
                    val fx = peak.x + dx * step
                    val fy = peak.y + dy * step
                    val ix = fx.toInt()
                    val iy = fy.toInt()
                    if (ix !in 0 until w - 1 || iy !in 0 until h - 1) break

                    val t = step.toFloat() / rayLen
                    val decay = exp(-step / tau) * (1f - t * 0.25f)
                    val energy = basePower * decay

                    // White-gold core -> signature purple-magenta-cyan diffraction tip tint
                    val rTint = if (monochrome) 1f else (1.0f - t * 0.30f)
                    val gTint = if (monochrome) 1f else (0.94f - t * 0.56f).coerceAtLeast(0.28f)
                    val bTint = if (monochrome) 1f else (0.86f + t * 0.14f)

                    val wx = fx - ix
                    val wy = fy - iy
                    val w00 = (1f - wx) * (1f - wy)
                    val w10 = wx * (1f - wy)
                    val w01 = (1f - wx) * wy
                    val w11 = wx * wy

                    val idx00 = iy * w + ix
                    accR[idx00] += energy * rTint * w00
                    accG[idx00] += energy * gTint * w00
                    accB[idx00] += energy * bTint * w00

                    accR[idx00 + 1] += energy * rTint * w10
                    accG[idx00 + 1] += energy * gTint * w10
                    accB[idx00 + 1] += energy * bTint * w10

                    accR[idx00 + w] += energy * rTint * w01
                    accG[idx00 + w] += energy * gTint * w01
                    accB[idx00 + w] += energy * bTint * w01

                    accR[idx00 + w + 1] += energy * rTint * w11
                    accG[idx00 + w + 1] += energy * gTint * w11
                    accB[idx00 + w + 1] += energy * bTint * w11
                }
            }
        }

        for (i in out.indices) {
            val r = accR[i].toInt().coerceIn(0, 255)
            val g = accG[i].toInt().coerceIn(0, 255)
            val b = accB[i].toInt().coerceIn(0, 255)
            if (r > 0 || g > 0 || b > 0) {
                val a = max(r, max(g, b))
                out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return out
    }
}
