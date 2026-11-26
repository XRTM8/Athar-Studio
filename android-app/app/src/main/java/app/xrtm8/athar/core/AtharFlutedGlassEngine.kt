package app.xrtm8.athar.core

import android.graphics.Bitmap
import app.xrtm8.athar.model.FlutedGlassState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Physical Fluted / Ribbed Glass Engine Gen 2:
 * - Cylindrical lens horizontal/angled refraction with bilinear pixel sampling
 * - Chromatic dispersion along rib edges (independent R/B optical shift)
 * - Realistic convex crest specular highlights and inter-rib shadow ridges
 * - Optional partial split coverage with adjustable edge softness & glass edge highlight
 * - Strip-based row processing to minimize 4K heap allocation
 */
object AtharFlutedGlassEngine {

    private const val STRIP_ROWS = 64

    @JvmOverloads
    fun processBitmap(
        source: Bitmap,
        state: FlutedGlassState,
        inPlace: Boolean = false
    ): Bitmap {
        if (!state.isEnabled) return source
        val w = source.width
        val h = source.height
        if (w <= 1 || h <= 1) return source

        val target = if (inPlace && source.isMutable) {
            source
        } else {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }

        if (abs(state.angleDegrees) < 0.5f) {
            processVerticalStrips(source, target, w, h, state)
        } else {
            processAngledStrips(source, target, w, h, state)
        }
        return target
    }

    private fun processVerticalStrips(
        source: Bitmap,
        target: Bitmap,
        w: Int,
        h: Int,
        state: FlutedGlassState
    ) {
        val table = buildVerticalLookup(w, state)
        val dispersion = state.chromaticDispersion.coerceIn(0f, 1f)
        val hasDispersion = dispersion > 0.01f
        val srcStrip = IntArray(w * min(STRIP_ROWS, h))
        val outStrip = IntArray(w * min(STRIP_ROWS, h))

        var yStart = 0
        while (yStart < h) {
            val rows = min(STRIP_ROWS, h - yStart)
            source.getPixels(srcStrip, 0, w, 0, yStart, w, rows)
            for (r in 0 until rows) {
                val rowBase = r * w
                for (x in 0 until w) {
                    val alphaCov = table.coverageAlpha[x]
                    if (alphaCov <= 0.001f) {
                        outStrip[rowBase + x] = srcStrip[rowBase + x]
                        continue
                    }
                    val disp = table.xOffset[x]
                    val sampled = if (hasDispersion) {
                        val shift = disp * 0.30f * dispersion
                        val rPx = sampleRowHorizontal(srcStrip, rowBase, w, x + disp + shift)
                        val gPx = sampleRowHorizontal(srcStrip, rowBase, w, x + disp)
                        val bPx = sampleRowHorizontal(srcStrip, rowBase, w, x + disp - shift)
                        (gPx and 0xFF00FF00.toInt()) or (rPx and 0x00FF0000) or (bPx and 0x000000FF)
                    } else {
                        sampleRowHorizontal(srcStrip, rowBase, w, x + disp)
                    }
                    val lit = applyGlassLighting(sampled, table.lightDelta[x])
                    outStrip[rowBase + x] = if (alphaCov >= 0.999f) lit else lerpArgb(srcStrip[rowBase + x], lit, alphaCov)
                }
            }
            target.setPixels(outStrip, 0, w, 0, yStart, w, rows)
            yStart += rows
        }
    }

    private fun sampleRowHorizontal(buf: IntArray, rowBase: Int, w: Int, sampleX: Float): Int {
        val sx = sampleX.coerceIn(0f, (w - 1).toFloat())
        val x0 = floor(sx).toInt()
        val x1 = min(x0 + 1, w - 1)
        return lerpArgb(buf[rowBase + x0], buf[rowBase + x1], sx - x0)
    }

    private fun processAngledStrips(
        source: Bitmap,
        target: Bitmap,
        w: Int,
        h: Int,
        state: FlutedGlassState
    ) {
        val ribCount = state.ribCount.coerceIn(6, 120)
        val ribWidth = (w.toFloat() / ribCount).coerceAtLeast(4f)
        val intensity = state.intensity.coerceIn(0f, 1f)
        val specular = state.specular.coerceIn(0f, 1f)
        val dispersion = state.chromaticDispersion.coerceIn(0f, 1f)
        val maxDisp = ribWidth * 0.55f * intensity
        val margin = ceil(maxDisp * (1f + 0.35f * dispersion)).toInt().coerceIn(1, h)

        val rad = Math.toRadians(state.angleDegrees.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val cx = w * 0.5f
        val cy = h * 0.5f

        val cov = state.coverage.coerceIn(0.1f, 1.0f)
        val hasPartial = cov < 0.999f
        val spanProj = abs(w * cosA) + abs(h * sinA)
        val halfSpan = spanProj * 0.5f
        val feather = (state.edgeSoftness.coerceIn(0f, 1f) * 0.14f).coerceAtLeast(0.0001f)

        val outStrip = IntArray(w * min(STRIP_ROWS, h))
        var yStart = 0
        while (yStart < h) {
            val rows = min(STRIP_ROWS, h - yStart)
            val readTop = max(0, yStart - margin)
            val readBottom = min(h, yStart + rows + margin)
            val readH = readBottom - readTop
            val srcBuf = IntArray(w * readH)
            source.getPixels(srcBuf, 0, w, 0, readTop, w, readH)

            for (r in 0 until rows) {
                val y = yStart + r
                val dy = y - cy
                val outBase = r * w
                for (x in 0 until w) {
                    val dx = x - cx
                    val proj = dx * cosA + dy * sinA
                    val origPx = srcBuf[(y - readTop) * w + x]
                    var covAlpha = 1f
                    if (hasPartial) {
                        val normPos = ((proj + halfSpan) / spanProj.coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val dist = if (state.coverageFromStart) (cov - normPos) else (normPos - (1f - cov))
                        covAlpha = if (state.edgeSoftness <= 0.01f) {
                            if (dist >= 0f) 1f else 0f
                        } else {
                            ((dist / feather) + 0.5f).coerceIn(0f, 1f)
                        }
                        if (covAlpha <= 0.001f) {
                            outStrip[outBase + x] = origPx
                            continue
                        }
                    }

                    val u = computeRibPhase(proj + halfSpan, ribWidth)
                    val disp = computeRibDisplacement(u, maxDisp)
                    val light = computeRibLighting(u, specular)

                    val sampled = if (dispersion > 0.01f) {
                        val shift = disp * 0.30f * dispersion
                        val rPx = sampleBilinear(srcBuf, w, readTop, readH, x + (disp + shift) * cosA, y + (disp + shift) * sinA)
                        val gPx = sampleBilinear(srcBuf, w, readTop, readH, x + disp * cosA, y + disp * sinA)
                        val bPx = sampleBilinear(srcBuf, w, readTop, readH, x + (disp - shift) * cosA, y + (disp - shift) * sinA)
                        (gPx and 0xFF00FF00.toInt()) or (rPx and 0x00FF0000) or (bPx and 0x000000FF)
                    } else {
                        sampleBilinear(srcBuf, w, readTop, readH, x + disp * cosA, y + disp * sinA)
                    }
                    val lit = applyGlassLighting(sampled, light)
                    outStrip[outBase + x] = if (covAlpha >= 0.999f) lit else lerpArgb(origPx, lit, covAlpha)
                }
            }
            target.setPixels(outStrip, 0, w, 0, yStart, w, rows)
            yStart += rows
        }
    }

    /**
     * Pure buffer pass (100% JVM unit-testable without Android Bitmap).
     */
    fun applyFlutedGlassToBuffer(
        srcPixels: IntArray,
        outPixels: IntArray,
        w: Int,
        h: Int,
        state: FlutedGlassState
    ) {
        if (!state.isEnabled || w <= 1 || h <= 1) {
            if (srcPixels !== outPixels) {
                System.arraycopy(srcPixels, 0, outPixels, 0, min(srcPixels.size, outPixels.size))
            }
            return
        }
        val table = buildVerticalLookup(w, state)
        val dispersion = state.chromaticDispersion.coerceIn(0f, 1f)
        val hasDispersion = dispersion > 0.01f

        for (y in 0 until h) {
            val rowBase = y * w
            for (x in 0 until w) {
                val alphaCov = table.coverageAlpha[x]
                if (alphaCov <= 0.001f) {
                    outPixels[rowBase + x] = srcPixels[rowBase + x]
                    continue
                }
                val disp = table.xOffset[x]
                val sampled = if (hasDispersion) {
                    val shift = disp * 0.30f * dispersion
                    val rPx = sampleRowHorizontal(srcPixels, rowBase, w, x + disp + shift)
                    val gPx = sampleRowHorizontal(srcPixels, rowBase, w, x + disp)
                    val bPx = sampleRowHorizontal(srcPixels, rowBase, w, x + disp - shift)
                    (gPx and 0xFF00FF00.toInt()) or (rPx and 0x00FF0000) or (bPx and 0x000000FF)
                } else {
                    sampleRowHorizontal(srcPixels, rowBase, w, x + disp)
                }
                val lit = applyGlassLighting(sampled, table.lightDelta[x])
                outPixels[rowBase + x] = if (alphaCov >= 0.999f) lit else lerpArgb(srcPixels[rowBase + x], lit, alphaCov)
            }
        }
    }

    private class VerticalLookup(
        val xOffset: FloatArray,
        val lightDelta: FloatArray,
        val coverageAlpha: FloatArray
    )

    private fun buildVerticalLookup(w: Int, state: FlutedGlassState): VerticalLookup {
        val ribCount = state.ribCount.coerceIn(6, 120)
        val ribWidth = (w.toFloat() / ribCount).coerceAtLeast(4f)
        val intensity = state.intensity.coerceIn(0f, 1f)
        val specular = state.specular.coerceIn(0f, 1f)
        val maxDisp = ribWidth * 0.55f * intensity
        val cov = state.coverage.coerceIn(0.1f, 1.0f)
        val hasPartial = cov < 0.999f
        val splitX = if (state.coverageFromStart) (w * cov) else (w * (1f - cov))
        val featherPx = (w * 0.14f * state.edgeSoftness.coerceIn(0f, 1f)).coerceAtLeast(1f)

        val xOffset = FloatArray(w)
        val lightDelta = FloatArray(w)
        val coverageAlpha = FloatArray(w)

        for (x in 0 until w) {
            val covAlpha = if (!hasPartial) {
                1f
            } else {
                val dist = if (state.coverageFromStart) (splitX - x) else (x - splitX)
                if (state.edgeSoftness <= 0.01f) {
                    if (dist >= 0f) 1f else 0f
                } else {
                    ((dist / featherPx) + 0.5f).coerceIn(0f, 1f)
                }
            }
            coverageAlpha[x] = covAlpha
            if (covAlpha <= 0.001f) continue

            val u = computeRibPhase(x.toFloat(), ribWidth)
            xOffset[x] = computeRibDisplacement(u, maxDisp)
            var light = computeRibLighting(u, specular)

            if (hasPartial && state.edgeSoftness <= 0.05f && abs(x - splitX) <= 1.5f) {
                light += 0.25f * specular
            }
            lightDelta[x] = light
        }
        return VerticalLookup(xOffset, lightDelta, coverageAlpha)
    }

    private fun computeRibPhase(coord: Float, ribWidth: Float): Float {
        val m = ((coord % ribWidth) + ribWidth) % ribWidth
        return (m / ribWidth) * 2f - 1f
    }

    private fun computeRibDisplacement(u: Float, maxDisp: Float): Float {
        val wave = sin(u * PI).toFloat() * 0.72f + (u * (1f - abs(u))) * 0.85f
        return wave * maxDisp
    }

    private fun computeRibLighting(u: Float, specular: Float): Float {
        if (specular <= 0.001f) return 0f
        val crestDist = u - 0.52f
        val highlight = exp(-(crestDist * crestDist) / 0.032f) * 0.34f
        val valley = if (abs(u) > 0.78f) -((abs(u) - 0.78f) / 0.22f) * 0.22f else 0f
        val ambientSheen = 0.03f
        return (highlight + valley + ambientSheen) * specular
    }

    private fun sampleBilinear(
        buf: IntArray,
        w: Int,
        readTop: Int,
        readH: Int,
        sx: Float,
        sy: Float
    ): Int {
        val clampedX = sx.coerceIn(0f, (w - 1).toFloat())
        val x0 = floor(clampedX).toInt()
        val x1 = min(x0 + 1, w - 1)
        val localY = (sy - readTop).coerceIn(0f, (readH - 1).toFloat())
        val y0 = floor(localY).toInt().coerceIn(0, readH - 1)
        val y1 = min(y0 + 1, readH - 1)
        val fx = clampedX - x0
        val fy = localY - y0

        val c00 = buf[y0 * w + x0]
        val c10 = buf[y0 * w + x1]
        val c01 = buf[y1 * w + x0]
        val c11 = buf[y1 * w + x1]

        return lerpArgb(lerpArgb(c00, c10, fx), lerpArgb(c01, c11, fx), fy)
    }

    private fun lerpArgb(c0: Int, c1: Int, t: Float): Int {
        if (t <= 0f) return c0
        if (t >= 1f) return c1
        val a0 = (c0 ushr 24) and 0xFF
        val r0 = (c0 ushr 16) and 0xFF
        val g0 = (c0 ushr 8) and 0xFF
        val b0 = c0 and 0xFF

        val a1 = (c1 ushr 24) and 0xFF
        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF

        val a = (a0 + (a1 - a0) * t).toInt().coerceIn(0, 255)
        val r = (r0 + (r1 - r0) * t).toInt().coerceIn(0, 255)
        val g = (g0 + (g1 - g0) * t).toInt().coerceIn(0, 255)
        val b = (b0 + (b1 - b0) * t).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun applyGlassLighting(argb: Int, delta: Float): Int {
        if (abs(delta) < 0.002f) return argb
        val a = (argb ushr 24) and 0xFF
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF

        val shift = (delta * 255f).toInt()
        val nr = (r + shift).coerceIn(0, 255)
        val ng = (g + shift).coerceIn(0, 255)
        val nb = (b + shift).coerceIn(0, 255)
        return (a shl 24) or (nr shl 16) or (ng shl 8) or nb
    }
}
