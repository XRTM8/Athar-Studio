package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.RadialGradient
import android.graphics.Shader
import app.xrtm8.athar.model.LuxuryGradientPreset
import app.xrtm8.athar.model.VisualEffect
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * High-performance Skia shader generator for metallic, luxury, and multi-stop vector gradients.
 * Pre-compiled ARGB integer color tables eliminate all string parsing and array allocation churn at 120Hz.
 */
object CanvasShaderFactory {

    private val GOLD_COLORS = intArrayOf(
        0xFFFFF7DB.toInt(),
        0xFFF5C369.toInt(),
        0xFFD49438.toInt(),
        0xFFFFE8A3.toInt(),
        0xFFB0731E.toInt()
    )
    private val GOLD_STOPS = floatArrayOf(0.0f, 0.25f, 0.55f, 0.75f, 1.0f)

    private val CHROME_COLORS = intArrayOf(
        0xFFFFFFFF.toInt(),
        0xFF9EA5B0.toInt(),
        0xFF181C22.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF505866.toInt(),
        0xFFDDE2EA.toInt()
    )
    private val CHROME_STOPS = floatArrayOf(0.0f, 0.2f, 0.45f, 0.55f, 0.8f, 1.0f)

    private val GLASS_COLORS = intArrayOf(
        0xE6FFFFFF.toInt(),
        0x33E2E8F0.toInt()
    )

    fun createEffectShader(
        effect: VisualEffect,
        lineY: Float,
        textSizePx: Float
    ): Shader? {
        return when (effect) {
            VisualEffect.ROYAL_GOLD -> {
                LinearGradient(
                    0f, lineY - textSizePx * 0.85f,
                    0f, lineY + textSizePx * 0.25f,
                    GOLD_COLORS,
                    GOLD_STOPS,
                    Shader.TileMode.CLAMP
                )
            }

            VisualEffect.LIQUID_CHROME -> {
                LinearGradient(
                    -textSizePx * 0.5f, lineY - textSizePx * 0.85f,
                    textSizePx * 0.5f, lineY + textSizePx * 0.25f,
                    CHROME_COLORS,
                    CHROME_STOPS,
                    Shader.TileMode.CLAMP
                )
            }

            VisualEffect.FROSTED_GLASS -> {
                LinearGradient(
                    0f, lineY - textSizePx * 0.85f,
                    0f, lineY + textSizePx * 0.25f,
                    GLASS_COLORS,
                    null,
                    Shader.TileMode.CLAMP
                )
            }

            else -> null
        }
    }

    /**
     * Generates a 360° rotatable multi-stop vector gradient shader.
     */
    fun createMultiStopGradientShader(
        preset: LuxuryGradientPreset,
        angle: Float,
        isRadial: Boolean,
        centerX: Float,
        centerY: Float,
        radiusPx: Float
    ): Shader? {
        if (preset == LuxuryGradientPreset.NONE || preset.colors.isEmpty()) return null

        val colors = IntArray(preset.colors.size) { i -> preset.colors[i].toInt() }
        val stops = preset.stops.toFloatArray()

        return if (isRadial) {
            RadialGradient(
                centerX, centerY,
                radiusPx.coerceAtLeast(10f),
                colors, stops,
                Shader.TileMode.CLAMP
            )
        } else {
            val rad = Math.toRadians(angle.toDouble())
            val dx = (cos(rad) * radiusPx).toFloat()
            val dy = (sin(rad) * radiusPx).toFloat()
            LinearGradient(
                centerX - dx, centerY - dy,
                centerX + dx, centerY + dy,
                colors, stops,
                Shader.TileMode.CLAMP
            )
        }
    }

    /**
     * Generates a GPU hardware-accelerated clipping texture shader.
     */
    fun createTextureShader(
        textureBitmap: Bitmap,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ): Shader {
        val shader = BitmapShader(textureBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val matrix = Matrix().apply {
            postScale(scale.coerceIn(0.1f, 10f), scale.coerceIn(0.1f, 10f))
            postTranslate(offsetX, offsetY)
        }
        shader.setLocalMatrix(matrix)
        return shader
    }
}
