package app.xrtm8.athar.ui.components

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import app.xrtm8.athar.model.VisualEffect

/**
 * High-performance Skia shader generator for metallic and luxury visual effects.
 */
object CanvasShaderFactory {

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
                    intArrayOf(
                        Color.parseColor("#FFF7DB"),
                        Color.parseColor("#F5C369"),
                        Color.parseColor("#D49438"),
                        Color.parseColor("#FFE8A3"),
                        Color.parseColor("#B0731E")
                    ),
                    floatArrayOf(0.0f, 0.25f, 0.55f, 0.75f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }

            VisualEffect.LIQUID_CHROME -> {
                LinearGradient(
                    -textSizePx * 0.5f, lineY - textSizePx * 0.85f,
                    textSizePx * 0.5f, lineY + textSizePx * 0.25f,
                    intArrayOf(
                        Color.parseColor("#FFFFFF"),
                        Color.parseColor("#9EA5B0"),
                        Color.parseColor("#181C22"),
                        Color.parseColor("#FFFFFF"),
                        Color.parseColor("#505866"),
                        Color.parseColor("#DDE2EA")
                    ),
                    floatArrayOf(0.0f, 0.2f, 0.45f, 0.55f, 0.8f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }

            VisualEffect.FROSTED_GLASS -> {
                LinearGradient(
                    0f, lineY - textSizePx * 0.85f,
                    0f, lineY + textSizePx * 0.25f,
                    intArrayOf(
                        Color.parseColor("#E6FFFFFF"),
                        Color.parseColor("#33E2E8F0")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }

            else -> null
        }
    }
}
