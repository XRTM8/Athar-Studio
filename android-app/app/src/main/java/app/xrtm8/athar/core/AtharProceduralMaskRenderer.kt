package app.xrtm8.athar.core

import android.graphics.*
import app.xrtm8.athar.model.ProceduralMaskType
import kotlin.math.*

/**
 * Athar Mask Studio 2.0 - High-Performance Procedural Vector & Optical Mask Engine.
 *
 * Replaces low-resolution raster artifacts with mathematically computed Skia shaders,
 * vector paths, and optical gradients. Rendered at native canvas resolution (up to 4K Skia vector)
 * with zero memory leaks, zero asset dependencies, and 120Hz smooth performance.
 */
object AtharProceduralMaskRenderer {

    private val reusablePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val reusablePath = Path()
    private val reusableRect = RectF()
    private val reusableShaderMatrix = Matrix()

    /**
     * Draws the selected procedural mask onto [canvas] with exact [width] and [height].
     * The incoming [paint] already contains the configured alpha, blend mode, and color matrix filters.
     */
    fun drawProceduralMask(
        canvas: Canvas,
        type: ProceduralMaskType,
        width: Float,
        height: Float,
        paint: Paint
    ) {
        if (width <= 0f || height <= 0f) return

        when (type) {
            ProceduralMaskType.WINDOW_BLINDS -> drawWindowBlinds(canvas, width, height, paint)
            ProceduralMaskType.FRENCH_WINDOW -> drawFrenchWindow(canvas, width, height, paint)
            ProceduralMaskType.BOTANICAL_PALM -> drawBotanicalPalm(canvas, width, height, paint)
            ProceduralMaskType.GEOMETRIC_ARCH -> drawGeometricArch(canvas, width, height, paint)
            ProceduralMaskType.OPTICAL_SUNBURST -> drawOpticalSunburst(canvas, width, height, paint)
            ProceduralMaskType.ANAMORPHIC_STREAK -> drawAnamorphicStreak(canvas, width, height, paint)
            ProceduralMaskType.PRISM_DISPERSION -> drawPrismDispersion(canvas, width, height, paint)
            ProceduralMaskType.GOLDEN_FLARE -> drawGoldenFlare(canvas, width, height, paint)
            ProceduralMaskType.FILM_BORDER_35MM -> drawFilmBorder35mm(canvas, width, height, paint)
            ProceduralMaskType.STUDIO_PASSEPARTOUT -> drawStudioPassepartout(canvas, width, height, paint)
            ProceduralMaskType.VECTOR_HALFTONE -> drawVectorHalftone(canvas, width, height, paint)
            ProceduralMaskType.PRECISION_SCANLINES -> drawPrecisionScanlines(canvas, width, height, paint)
        }
    }

    // ==========================================
    // 1. Venetian Blinds (ستائر شرائح)
    // ==========================================
    private fun drawWindowBlinds(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val slatCount = 9
        val slatHeight = height / slatCount.toFloat()
        val slatShader = LinearGradient(
            0f, 0f, 0f, slatHeight,
            intArrayOf(
                0xEE000000.toInt(),
                0xEE000000.toInt(),
                0x22000000,
                0x00000000,
                0x22000000
            ),
            floatArrayOf(0f, 0.40f, 0.52f, 0.88f, 1f),
            Shader.TileMode.REPEAT
        )
        // Angle the slats slightly (-12 degrees) for dramatic indoor window light
        reusableShaderMatrix.reset()
        reusableShaderMatrix.postRotate(-12f, width * 0.5f, height * 0.5f)
        slatShader.setLocalMatrix(reusableShaderMatrix)

        paint.shader = slatShader
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }

    // ==========================================
    // 2. French Window Frame (إطار نافذة مقسم)
    // ==========================================
    private fun drawFrenchWindow(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val frameThickness = min(width, height) * 0.08f
        val dividerThickness = min(width, height) * 0.045f

        // Draw soft ambient darkening around the window panes
        val ambientShader = RadialGradient(
            width * 0.5f, height * 0.5f, max(width, height) * 0.7f,
            intArrayOf(0x00000000, 0x88000000.toInt(), 0xCC000000.toInt()),
            floatArrayOf(0.3f, 0.75f, 1.0f),
            Shader.TileMode.CLAMP
        )

        paint.shader = ambientShader
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null

        // Cross Bars: Center vertical mullion + 2 horizontal transoms
        paint.color = 0xDD000000.toInt()

        // Outer border frame
        canvas.drawRect(0f, 0f, width, frameThickness, paint)
        canvas.drawRect(0f, height - frameThickness, width, height, paint)
        canvas.drawRect(0f, 0f, frameThickness, height, paint)
        canvas.drawRect(width - frameThickness, 0f, width, height, paint)

        // Vertical central mullion
        val centerX = width * 0.5f
        canvas.drawRect(centerX - dividerThickness * 0.5f, 0f, centerX + dividerThickness * 0.5f, height, paint)

        // Horizontal transoms at 1/3 and 2/3
        val y1 = height * 0.333f
        val y2 = height * 0.667f
        canvas.drawRect(0f, y1 - dividerThickness * 0.5f, width, y1 + dividerThickness * 0.5f, paint)
        canvas.drawRect(0f, y2 - dividerThickness * 0.5f, width, y2 + dividerThickness * 0.5f, paint)
    }

    // ==========================================
    // 3. Botanical Palm Leaf (ظلال أوراق استوائية)
    // ==========================================
    private fun drawBotanicalPalm(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        reusablePath.reset()

        // Central graceful stem curving from top-right towards bottom-left
        val startX = width * 0.95f
        val startY = height * 0.05f
        val endX = width * 0.25f
        val endY = height * 0.85f
        val ctrlX = width * 0.45f
        val ctrlY = height * 0.35f

        // Draw multiple paired tropical palm fronds with tapered bezier curves
        val frondPairs = 9
        for (i in 0 until frondPairs) {
            val t = (i + 1) / (frondPairs + 1).toFloat()
            // Stem point at t (quadratic bezier)
            val oneMinusT = 1f - t
            val px = oneMinusT * oneMinusT * startX + 2f * oneMinusT * t * ctrlX + t * t * endX
            val py = oneMinusT * oneMinusT * startY + 2f * oneMinusT * t * ctrlY + t * t * endY

            val frondLen = (width * 0.40f) * (1f - abs(t - 0.4f) * 0.8f)

            // Right frond
            reusablePath.moveTo(px, py)
            val rTipX = px - frondLen * 0.85f
            val rTipY = py + frondLen * 0.55f
            reusablePath.quadTo(px - frondLen * 0.4f, py - frondLen * 0.15f, rTipX, rTipY)
            reusablePath.quadTo(px - frondLen * 0.5f, py + frondLen * 0.25f, px, py)

            // Left frond
            reusablePath.moveTo(px, py)
            val lTipX = px + frondLen * 0.45f
            val lTipY = py - frondLen * 0.65f
            reusablePath.quadTo(px + frondLen * 0.1f, py - frondLen * 0.35f, lTipX, lTipY)
            reusablePath.quadTo(px + frondLen * 0.3f, py - frondLen * 0.15f, px, py)
        }

        // Stem thickness
        reusablePath.moveTo(startX, startY)
        reusablePath.quadTo(ctrlX, ctrlY, endX, endY)

        val palmShader = RadialGradient(
            width * 0.7f, height * 0.3f, max(width, height) * 0.85f,
            intArrayOf(0xEE000000.toInt(), 0xAA000000.toInt(), 0x33000000),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = palmShader
        canvas.drawPath(reusablePath, paint)
        paint.shader = null
    }

    // ==========================================
    // 4. Architectural Arches (أقواس معمارية)
    // ==========================================
    private fun drawGeometricArch(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        reusablePath.reset()

        val archWidth = width * 0.38f
        val archTop = height * 0.20f
        val archLeft = width * 0.10f

        // Draw primary Roman/Moorish arch silhouette
        reusablePath.moveTo(0f, 0f)
        reusablePath.lineTo(width, 0f)
        reusablePath.lineTo(width, height)
        reusablePath.lineTo(0f, height)
        reusablePath.close()

        // Cut out the illuminated arch window
        val archRect = RectF(archLeft, archTop, archLeft + archWidth, archTop + archWidth)
        reusablePath.moveTo(archLeft, height)
        reusablePath.lineTo(archLeft, archTop + archWidth * 0.5f)
        reusablePath.arcTo(archRect, 180f, 180f, false)
        reusablePath.lineTo(archLeft + archWidth, height)
        reusablePath.close()

        val archShader = LinearGradient(
            0f, 0f, width, height,
            intArrayOf(0xDD000000.toInt(), 0x99000000.toInt(), 0x44000000),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = archShader
        canvas.drawPath(reusablePath, paint)
        paint.shader = null
    }

    // ==========================================
    // 5. Optical Sunburst (تسريب شمسي نقي)
    // ==========================================
    private fun drawOpticalSunburst(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val cx = width * 0.90f
        val cy = height * 0.10f
        val radius = max(width, height) * 0.85f

        val sunShader = RadialGradient(
            cx, cy, radius,
            intArrayOf(
                0xFFFFFFFF.toInt(), // Pure white core
                0xFFFFE082.toInt(), // Golden warmth
                0xFFFFB74D.toInt(), // Amber glow
                0xFFFF7043.toInt(), // Crimson flare
                0x99AB47BC.toInt(), // Chromatic purple edge
                0x00000000          // Transparent falloff
            ),
            floatArrayOf(0f, 0.08f, 0.25f, 0.48f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = sunShader
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }

    // ==========================================
    // 6. Anamorphic Flare Streak (شريط توهج سينمائي)
    // ==========================================
    private fun drawAnamorphicStreak(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val centerY = height * 0.45f
        val streakHalfHeight = height * 0.12f

        // Horizontal laser streak in CinemaScope electric cyan/blue
        val streakShader = LinearGradient(
            0f, centerY - streakHalfHeight,
            0f, centerY + streakHalfHeight,
            intArrayOf(
                0x00000000,
                0x4400E5FF,
                0xFF00E5FF.toInt(),
                0xFFFFFFFF.toInt(), // White hot filament core
                0xFF00E5FF.toInt(),
                0x4400E5FF,
                0x00000000
            ),
            floatArrayOf(0f, 0.35f, 0.47f, 0.50f, 0.53f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = streakShader
        canvas.drawRect(0f, centerY - streakHalfHeight, width, centerY + streakHalfHeight, paint)
        paint.shader = null
    }

    // ==========================================
    // 7. Prism Optical Dispersion (انكسار موشوري)
    // ==========================================
    private fun drawPrismDispersion(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val prismShader = LinearGradient(
            0f, 0f, width * 0.75f, height * 0.75f,
            intArrayOf(
                0x00000000,
                0xBB8E24AA.toInt(), // Violet
                0xBB1E88E5.toInt(), // Blue
                0xBB00ACC1.toInt(), // Cyan
                0xBB43A047.toInt(), // Green
                0xBBFDD835.toInt(), // Yellow
                0xBBFB8C00.toInt(), // Orange
                0xBBE53935.toInt(), // Red
                0x00000000
            ),
            floatArrayOf(0f, 0.20f, 0.30f, 0.40f, 0.50f, 0.60f, 0.70f, 0.80f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = prismShader
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }

    // ==========================================
    // 8. Golden Hour Flare (وهج الساعة الذهبية)
    // ==========================================
    private fun drawGoldenFlare(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val cx = width * 0.15f
        val cy = height * 0.05f
        val radius = max(width, height) * 0.95f

        val goldenShader = RadialGradient(
            cx, cy, radius,
            intArrayOf(
                0xFFFFF8E1.toInt(), // Warm morning sunlight
                0xFFFFD54F.toInt(), // Rich Gold
                0xFFFF8F00.toInt(), // Warm Amber
                0x88E65100.toInt(), // Deep Sunset Orange
                0x00000000
            ),
            floatArrayOf(0f, 0.18f, 0.42f, 0.70f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = goldenShader
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }

    // ==========================================
    // 9. 35mm Film Frame & Sprockets (إطار فيلم 35mm)
    // ==========================================
    private fun drawFilmBorder35mm(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val barHeight = height * 0.11f
        paint.color = 0xFA050507.toInt()

        // Top and Bottom black matte bands
        canvas.drawRect(0f, 0f, width, barHeight, paint)
        canvas.drawRect(0f, height - barHeight, width, height, paint)

        // Sprocket perforations (cutouts filled with semi-transparent background tone)
        val sprocketW = min(width, height) * 0.038f
        val sprocketH = barHeight * 0.48f
        val sprocketRadius = sprocketW * 0.28f
        val spacing = sprocketW * 2.1f
        val count = (width / spacing).toInt()
        val startX = (width - (count * spacing)) * 0.5f

        val punchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0x11FFFFFF // Subtle transparency perforation
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }

        for (i in 0..count) {
            val sx = startX + i * spacing
            // Top sprocket
            val topRect = RectF(sx, (barHeight - sprocketH) * 0.5f, sx + sprocketW, (barHeight + sprocketH) * 0.5f)
            canvas.drawRoundRect(topRect, sprocketRadius, sprocketRadius, punchPaint)

            // Bottom sprocket
            val botY = height - barHeight + (barHeight - sprocketH) * 0.5f
            val botRect = RectF(sx, botY, sx + sprocketW, botY + sprocketH)
            canvas.drawRoundRect(botRect, sprocketRadius, sprocketRadius, punchPaint)
        }
    }

    // ==========================================
    // 10. Studio Passepartout Mat (باسبارتو استوديو)
    // ==========================================
    private fun drawStudioPassepartout(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val marginX = width * 0.08f
        val marginY = height * 0.08f

        paint.color = 0xEE12141A.toInt()

        // 4 outer border blocks leaving inner aperture open
        canvas.drawRect(0f, 0f, width, marginY, paint)
        canvas.drawRect(0f, height - marginY, width, height, paint)
        canvas.drawRect(0f, marginY, marginX, height - marginY, paint)
        canvas.drawRect(width - marginX, marginY, width, height - marginY, paint)

        // Soft inner shadow on top & left edge of aperture
        val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0x66000000
        }
        canvas.drawRect(marginX, marginY, width - marginX, height - marginY, bevelPaint)
    }

    // ==========================================
    // 11. Vector Halftone Screen (تنقيط مطبعي نقي)
    // ==========================================
    private fun drawVectorHalftone(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val step = max(18f, width / 24f)
        val cols = (width / step).toInt() + 1
        val rows = (height / step).toInt() + 1
        val maxRadius = step * 0.42f

        paint.color = 0xCC000000.toInt()
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val maxDist = hypot(centerX, centerY)

        for (c in 0 until cols) {
            val x = c * step
            for (r in 0 until rows) {
                val y = r * step
                val dist = hypot(x - centerX, y - centerY)
                val factor = (dist / maxDist).coerceIn(0.05f, 1f)
                val dotRadius = maxRadius * factor
                canvas.drawCircle(x, y, dotRadius, paint)
            }
        }
    }

    // ==========================================
    // 12. Analog CRT Scanlines (خطوط مسح تلفاز)
    // ==========================================
    private fun drawPrecisionScanlines(canvas: Canvas, width: Float, height: Float, paint: Paint) {
        val lineHeight = 4f
        val scanlineShader = LinearGradient(
            0f, 0f, 0f, lineHeight,
            intArrayOf(0x00000000, 0x00000000, 0xAA000000.toInt(), 0xAA000000.toInt()),
            floatArrayOf(0f, 0.49f, 0.50f, 1f),
            Shader.TileMode.REPEAT
        )
        paint.shader = scanlineShader
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null
    }

    // ==========================================
    // Ultra-Fast Vector Thumbnail Generator
    // ==========================================
    fun generateThumbnailBitmap(type: ProceduralMaskType, size: Int = 120): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark photographic surface backdrop
        val bgPaint = Paint().apply {
            color = 0xFF141720.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            style = Paint.Style.FILL
        }

        drawProceduralMask(canvas, type, size.toFloat(), size.toFloat(), thumbPaint)
        return bitmap
    }
}
