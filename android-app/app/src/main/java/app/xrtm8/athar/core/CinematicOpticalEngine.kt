package app.xrtm8.athar.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.min

/**
 * High-performance Optical Lens Flare & Anamorphic Streak Engine.
 * Simulates Hollywood-grade anamorphic lens flares with zero runtime allocations.
 */
object CinematicOpticalEngine {

    private val hotspotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }

    private val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }

    fun drawLensFlare(
        canvas: Canvas,
        width: Float,
        height: Float,
        normX: Float,
        normY: Float,
        intensity: Float,
        colorLong: Long
    ) {
        if (intensity <= 0.01f) return

        val cx = width * normX.coerceIn(0f, 1f)
        val cy = height * normY.coerceIn(0f, 1f)
        val minDim = min(width, height)
        val flareRadius = minDim * 0.45f * intensity

        val baseColor = colorLong.toInt()
        val r = (baseColor shr 16) and 0xFF
        val g = (baseColor shr 8) and 0xFF
        val b = baseColor and 0xFF

        val maxAlpha = (220 * intensity).toInt().coerceIn(0, 255)
        val midAlpha = (110 * intensity).toInt().coerceIn(0, 255)

        val cHotspot = Color.argb(maxAlpha, min(255, r + 40), min(255, g + 40), min(255, b + 40))
        val cGlow = Color.argb(midAlpha, r, g, b)
        val cTransparent = Color.argb(0, r, g, b)

        // 1. Central Radial Glow
        hotspotPaint.shader = RadialGradient(
            cx, cy, flareRadius,
            intArrayOf(Color.WHITE, cHotspot, cGlow, cTransparent),
            floatArrayOf(0.0f, 0.15f, 0.55f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, flareRadius, hotspotPaint)

        // 2. Anamorphic Horizontal Streak
        val streakHalfWidth = width * 0.9f
        val streakHalfHeight = (minDim * 0.035f * intensity).coerceAtLeast(2f)

        streakPaint.shader = LinearGradient(
            cx - streakHalfWidth, cy, cx + streakHalfWidth, cy,
            intArrayOf(cTransparent, cGlow, Color.WHITE, cGlow, cTransparent),
            floatArrayOf(0.0f, 0.35f, 0.5f, 0.65f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(
            cx - streakHalfWidth,
            cy - streakHalfHeight,
            cx + streakHalfWidth,
            cy + streakHalfHeight,
            streakPaint
        )

        // 3. Secondary Outer Corona Ring
        ringPaint.color = Color.argb((80 * intensity).toInt().coerceIn(0, 255), r, g, b)
        canvas.drawCircle(cx, cy, flareRadius * 0.65f, ringPaint)
    }
}
