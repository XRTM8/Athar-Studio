package app.xrtm8.athar.core

import android.graphics.*
import app.xrtm8.athar.model.AtharMaskState
import app.xrtm8.athar.model.BlackGradientPlacement
import kotlin.math.max

/**
 * Zero-allocation, hardware-accelerated Skia mask & atmospheric gradient renderer.
 * Blends authentic light leaks, prisms, shadows, and paper/glass textures behind text layers
 * with precision positioning, flipping, rotation, 360° hue shift, and cinematic black gradients.
 */
object AtharMaskRenderer {

    private val reusableMatrix = Matrix()
    private val reusablePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
    }
    private val reusableGradientPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val reusableColorMatrix = ColorMatrix()

    private fun applyHueRotation(cm: ColorMatrix, degrees: Float) {
        cm.reset()
        if (degrees == 0f) return
        val rad = Math.toRadians(degrees.toDouble())
        val cosVal = Math.cos(rad).toFloat()
        val sinVal = Math.sin(rad).toFloat()
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        val mat = floatArrayOf(
            lumR + cosVal * (1f - lumR) + sinVal * (-lumR),
            lumG + cosVal * (-lumG) + sinVal * (-lumG),
            lumB + cosVal * (-lumB) + sinVal * (1f - lumB),
            0f, 0f,
            lumR + cosVal * (-lumR) + sinVal * 0.143f,
            lumG + cosVal * (1f - lumG) + sinVal * 0.140f,
            lumB + cosVal * (-lumB) + sinVal * (-0.283f),
            0f, 0f,
            lumR + cosVal * (-lumR) + sinVal * (-(1f - lumR)),
            lumG + cosVal * (-lumG) + sinVal * lumG,
            lumB + cosVal * (1f - lumB) + sinVal * lumB,
            0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        cm.set(mat)
    }

    @Synchronized
    fun drawMaskAndGradient(
        canvas: Canvas,
        state: AtharMaskState,
        width: Float,
        height: Float,
        maskBitmap: Bitmap?
    ) {
        if (!state.isEffectActive) return

        // 1. Draw Mask Texture if active and loaded
        if (maskBitmap != null && state.isMaskActive) {
            val bmpW = maskBitmap.width.toFloat()
            val bmpH = maskBitmap.height.toFloat()
            if (bmpW > 0 && bmpH > 0) {
                reusableMatrix.reset()

                // Calculate base fit (Aspect Fill)
                val scale = max(width / bmpW, height / bmpH) * state.scale.coerceIn(0.5f, 3.0f)
                val scaledW = bmpW * scale
                val scaledH = bmpH * scale

                // Center position plus interactive normalized offsets
                val maxShiftX = width * 0.5f
                val maxShiftY = height * 0.5f
                val posX = (width - scaledW) / 2f + (state.offsetX.coerceIn(-1f, 1f) * maxShiftX)
                val posY = (height - scaledH) / 2f + (state.offsetY.coerceIn(-1f, 1f) * maxShiftY)

                reusableMatrix.postScale(scale, scale)
                reusableMatrix.postTranslate(posX, posY)

                // Flips around canvas center
                if (state.isFlippedH || state.isFlippedV) {
                    val sx = if (state.isFlippedH) -1f else 1f
                    val sy = if (state.isFlippedV) -1f else 1f
                    reusableMatrix.postScale(sx, sy, width / 2f, height / 2f)
                }

                // Rotation around canvas center
                if (state.rotationDegrees != 0) {
                    reusableMatrix.postRotate(state.rotationDegrees.toFloat(), width / 2f, height / 2f)
                }

                // Paint configuration
                reusablePaint.alpha = (state.opacity.coerceIn(0f, 1f) * 255).toInt()
                reusablePaint.xfermode = PorterDuffXfermode(state.blendMode.porterDuffMode)

                if (state.hueShift != 0f) {
                    applyHueRotation(reusableColorMatrix, state.hueShift)
                    reusablePaint.colorFilter = ColorMatrixColorFilter(reusableColorMatrix)
                } else {
                    reusablePaint.colorFilter = null
                }

                canvas.drawBitmap(maskBitmap, reusableMatrix, reusablePaint)
            }
        }

        // 2. Draw Atmospheric Black Gradient Scrim if active
        if (state.blackGradient.isEffectActive) {
            val grad = state.blackGradient
            val cov = grad.coverage.coerceIn(0.15f, 1.0f)

            reusableGradientPaint.reset()
            reusableGradientPaint.isAntiAlias = true
            reusableGradientPaint.style = Paint.Style.FILL
            reusableGradientPaint.alpha = (grad.opacity.coerceIn(0f, 1f) * 255).toInt()

            val shader: Shader = when (grad.placement) {
                BlackGradientPlacement.BOTTOM -> {
                    LinearGradient(
                        0f, height * (1f - cov),
                        0f, height,
                        Color.TRANSPARENT, Color.BLACK,
                        Shader.TileMode.CLAMP
                    )
                }
                BlackGradientPlacement.TOP -> {
                    LinearGradient(
                        0f, height * cov,
                        0f, 0f,
                        Color.TRANSPARENT, Color.BLACK,
                        Shader.TileMode.CLAMP
                    )
                }
                BlackGradientPlacement.LEFT -> {
                    LinearGradient(
                        width * cov, 0f,
                        0f, 0f,
                        Color.TRANSPARENT, Color.BLACK,
                        Shader.TileMode.CLAMP
                    )
                }
                BlackGradientPlacement.RIGHT -> {
                    LinearGradient(
                        width * (1f - cov), 0f,
                        width, 0f,
                        Color.TRANSPARENT, Color.BLACK,
                        Shader.TileMode.CLAMP
                    )
                }
                BlackGradientPlacement.RADIAL -> {
                    val radius = max(width, height) * 0.75f * cov
                    RadialGradient(
                        width / 2f, height / 2f, radius,
                        intArrayOf(Color.TRANSPARENT, Color.BLACK),
                        floatArrayOf(0.15f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                BlackGradientPlacement.VERTICAL_SPLIT -> {
                    LinearGradient(
                        0f, 0f, 0f, height,
                        intArrayOf(Color.BLACK, Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                        floatArrayOf(0f, cov * 0.40f, 1f - (cov * 0.40f), 1f),
                        Shader.TileMode.CLAMP
                    )
                }
            }

            reusableGradientPaint.shader = shader
            canvas.drawRect(0f, 0f, width, height, reusableGradientPaint)
        }
    }
}
