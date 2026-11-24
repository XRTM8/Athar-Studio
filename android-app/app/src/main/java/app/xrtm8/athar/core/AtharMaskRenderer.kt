package app.xrtm8.athar.core

import android.graphics.*
import app.xrtm8.athar.model.AtharMaskState
import app.xrtm8.athar.model.BlackGradientPlacement
import kotlin.math.max

/**
 * Zero-allocation, hardware-accelerated Skia mask & atmospheric gradient renderer (Mask Studio 2.0).
 * Features:
 * - Mathematical procedural vector & optical Skia masks rendered at native canvas resolution (up to 4K).
 * - Full professional control pipeline: contrast, invert, solid tinting, luminance cutout, hue shift,
 *   spatial transforms (scale, offset X/Y, rotation, flips), and advanced blend modes.
 * - Hardware-accelerated atmospheric directional black gradients.
 */
object AtharMaskRenderer {

    private val reusableMatrix = Matrix()
    private val reusablePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val reusableGradientPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
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

    fun buildColorFilter(state: AtharMaskState): ColorFilter? {
        val hasAdjustments = state.hueShift != 0f ||
                state.contrast != 1.0f ||
                state.isInverted ||
                state.tintColorLong != null ||
                state.isLuminanceMode

        if (!hasAdjustments) return null

        val cm = ColorMatrix()

        // 1. Luminance mode (converts texture brightness to alpha/grayscale)
        if (state.isLuminanceMode) {
            val lumR = 0.299f
            val lumG = 0.587f
            val lumB = 0.114f
            cm.set(floatArrayOf(
                lumR, lumG, lumB, 0f, 0f,
                lumR, lumG, lumB, 0f, 0f,
                lumR, lumG, lumB, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        // 2. Custom Color Tint
        if (state.tintColorLong != null) {
            val tint = state.tintColorLong
            val r = ((tint shr 16) and 0xFF) / 255f
            val g = ((tint shr 8) and 0xFF) / 255f
            val b = (tint and 0xFF) / 255f
            val tintMatrix = ColorMatrix(floatArrayOf(
                r, 0f, 0f, 0f, 0f,
                0f, g, 0f, 0f, 0f,
                0f, 0f, b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(tintMatrix)
        }

        // 3. Hue Shift (only when not explicitly tinted)
        if (state.tintColorLong == null && state.hueShift != 0f) {
            val hueCm = ColorMatrix()
            applyHueRotation(hueCm, state.hueShift)
            cm.postConcat(hueCm)
        }

        // 4. Contrast
        if (state.contrast != 1.0f) {
            val c = state.contrast.coerceIn(0.5f, 2.5f)
            val t = (1f - c) * 0.5f * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)
        }

        // 5. Invert Mask
        if (state.isInverted) {
            val invertMatrix = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(invertMatrix)
        }

        return ColorMatrixColorFilter(cm)
    }

    @Synchronized
    fun drawMaskAndGradient(
        canvas: Canvas,
        state: AtharMaskState,
        width: Float,
        height: Float,
        maskBitmap: Bitmap?
    ) {
        if (!state.isEffectActive || width <= 0f || height <= 0f) return

        // 1. Draw Active Mask (Procedural Vector or Custom Bitmap)
        if (state.isMaskActive) {
            val procType = state.proceduralType

            // Configure reusable paint
            reusablePaint.alpha = (state.opacity.coerceIn(0f, 1f) * 255).toInt()
            reusablePaint.xfermode = PorterDuffXfermode(state.blendMode.porterDuffMode)
            reusablePaint.colorFilter = buildColorFilter(state)

            if (procType != null) {
                // Procedural Vector / Optical Mask (Infinite 4K resolution)
                reusableMatrix.reset()
                val scale = state.scale.coerceIn(0.5f, 3.0f)
                val maxShiftX = width * 0.5f
                val maxShiftY = height * 0.5f
                val transX = state.offsetX.coerceIn(-1f, 1f) * maxShiftX
                val transY = state.offsetY.coerceIn(-1f, 1f) * maxShiftY

                reusableMatrix.postScale(scale, scale, width * 0.5f, height * 0.5f)
                reusableMatrix.postTranslate(transX, transY)

                if (state.isFlippedH || state.isFlippedV) {
                    val sx = if (state.isFlippedH) -1f else 1f
                    val sy = if (state.isFlippedV) -1f else 1f
                    reusableMatrix.postScale(sx, sy, width * 0.5f, height * 0.5f)
                }

                if (state.rotationDegrees != 0) {
                    reusableMatrix.postRotate(state.rotationDegrees.toFloat(), width * 0.5f, height * 0.5f)
                }

                canvas.save()
                canvas.concat(reusableMatrix)
                AtharProceduralMaskRenderer.drawProceduralMask(canvas, procType, width, height, reusablePaint)
                canvas.restore()
            } else if (maskBitmap != null) {
                // Custom imported texture or legacy bitmap
                val bmpW = maskBitmap.width.toFloat()
                val bmpH = maskBitmap.height.toFloat()
                if (bmpW > 0 && bmpH > 0) {
                    reusableMatrix.reset()
                    val baseScale = max(width / bmpW, height / bmpH) * state.scale.coerceIn(0.5f, 3.0f)
                    val scaledW = bmpW * baseScale
                    val scaledH = bmpH * baseScale

                    val maxShiftX = width * 0.5f
                    val maxShiftY = height * 0.5f
                    val posX = (width - scaledW) / 2f + (state.offsetX.coerceIn(-1f, 1f) * maxShiftX)
                    val posY = (height - scaledH) / 2f + (state.offsetY.coerceIn(-1f, 1f) * maxShiftY)

                    reusableMatrix.postScale(baseScale, baseScale)
                    reusableMatrix.postTranslate(posX, posY)

                    if (state.isFlippedH || state.isFlippedV) {
                        val sx = if (state.isFlippedH) -1f else 1f
                        val sy = if (state.isFlippedV) -1f else 1f
                        reusableMatrix.postScale(sx, sy, width / 2f, height / 2f)
                    }

                    if (state.rotationDegrees != 0) {
                        reusableMatrix.postRotate(state.rotationDegrees.toFloat(), width / 2f, height / 2f)
                    }

                    canvas.drawBitmap(maskBitmap, reusableMatrix, reusablePaint)
                }
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
