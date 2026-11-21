package app.xrtm8.athar.ui.components

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import app.xrtm8.athar.model.CanvasGradientDirection
import app.xrtm8.athar.model.CanvasGradientScrim
import kotlin.math.max

/**
 * High-performance, hardware-accelerated Directional Gradient Scrim Renderer.
 * Draws subtle atmospheric fades over the canvas background (behind text or over photo):
 * - Bottom to Top, Top to Bottom, Left to Right, Right to Left, Radial
 * - Custom colors, opacities, and midpoint falloff
 * - Cached Shaders to eliminate memory allocation in 120Hz render loop
 */
object CanvasGradientScrimRenderer {

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private var lastWidth = -1f
    private var lastHeight = -1f
    private var lastDirection: CanvasGradientDirection? = null
    private var lastColorLong = -1L
    private var lastOpacity = -1f
    private var lastMidpoint = -1f
    private var cachedShader: Shader? = null

    @Synchronized
    fun drawGradientScrim(
        canvas: Canvas,
        width: Float,
        height: Float,
        scrim: CanvasGradientScrim
    ) {
        if (!scrim.isEnabled || scrim.opacity <= 0.01f || width <= 0f || height <= 0f) return

        if (cachedShader == null ||
            lastWidth != width ||
            lastHeight != height ||
            lastDirection != scrim.direction ||
            lastColorLong != scrim.colorLong ||
            lastOpacity != scrim.opacity ||
            lastMidpoint != scrim.midpoint
        ) {
            val baseColor = scrim.colorLong.toInt()
            val maxAlpha = (scrim.opacity.coerceIn(0f, 1f) * 255).toInt()
            val solidColor = (baseColor and 0x00FFFFFF) or (maxAlpha shl 24)
            val transparentColor = baseColor and 0x00FFFFFF
            val midAlpha = (maxAlpha * 0.5f).toInt()
            val midColor = (baseColor and 0x00FFFFFF) or (midAlpha shl 24)

            val colors = intArrayOf(solidColor, midColor, transparentColor)
            val stops = floatArrayOf(0f, scrim.midpoint.coerceIn(0.1f, 0.9f), 1f)

            cachedShader = when (scrim.direction) {
                CanvasGradientDirection.BOTTOM_TO_TOP -> {
                    LinearGradient(0f, height, 0f, 0f, colors, stops, Shader.TileMode.CLAMP)
                }
                CanvasGradientDirection.TOP_TO_BOTTOM -> {
                    LinearGradient(0f, 0f, 0f, height, colors, stops, Shader.TileMode.CLAMP)
                }
                CanvasGradientDirection.LEFT_TO_RIGHT -> {
                    LinearGradient(0f, 0f, width, 0f, colors, stops, Shader.TileMode.CLAMP)
                }
                CanvasGradientDirection.RIGHT_TO_LEFT -> {
                    LinearGradient(width, 0f, 0f, 0f, colors, stops, Shader.TileMode.CLAMP)
                }
                CanvasGradientDirection.RADIAL -> {
                    val radius = max(width, height) * 0.75f
                    RadialGradient(width * 0.5f, height * 0.5f, radius, colors, stops, Shader.TileMode.CLAMP)
                }
            }
            scrimPaint.shader = cachedShader
            lastWidth = width
            lastHeight = height
            lastDirection = scrim.direction
            lastColorLong = scrim.colorLong
            lastOpacity = scrim.opacity
            lastMidpoint = scrim.midpoint
        }

        canvas.drawRect(0f, 0f, width, height, scrimPaint)
    }
}
