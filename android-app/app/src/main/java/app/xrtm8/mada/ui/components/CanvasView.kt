package app.xrtm8.mada.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import app.xrtm8.mada.model.CanvasState
import app.xrtm8.mada.model.VisualEffect

@Composable
fun CanvasView(
    state: CanvasState,
    modifier: Modifier = Modifier,
    onTransformChange: (offsetX: Float, offsetY: Float, scale: Float, rotation: Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(state.offsetX) }
    var offsetY by remember { mutableFloatStateOf(state.offsetY) }
    var scale by remember { mutableFloatStateOf(state.scaleFactor) }
    var rotation by remember { mutableFloatStateOf(state.rotationAngle) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (state.isTransparentBg) Color.Transparent else Color(0xFF0F1015))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotate ->
                    offsetX += pan.x
                    offsetY += pan.y
                    scale = (scale * zoom).coerceIn(0.5f, 4.0f)
                    rotation += rotate
                    onTransformChange(offsetX, offsetY, scale, rotation)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f + offsetX
            val centerY = canvasHeight / 2f + offsetY

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                nativeCanvas.save()

                // Apply user transform (pan, scale, rotation)
                nativeCanvas.translate(centerX, centerY)
                nativeCanvas.rotate(rotation)
                nativeCanvas.scale(scale, scale)

                val text = state.displayText
                val textSizePx = state.fontSize * density

                // Base text paint
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    isDither = true
                    textSize = textSizePx
                    textAlign = Paint.Align.CENTER
                }

                when (state.selectedEffect) {
                    VisualEffect.ROYAL_GOLD -> {
                        // 1. Warm gold ambient glow
                        val glowPaint = Paint(textPaint).apply {
                            color = android.graphics.Color.parseColor("#44E5C07B")
                            maskFilter = BlurMaskFilter(24f * density, BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawText(text, 0f, 0f, glowPaint)

                        // 2. Deep warm shadow
                        val shadowPaint = Paint(textPaint).apply {
                            color = android.graphics.Color.parseColor("#993A2408")
                        }
                        nativeCanvas.drawText(text, 0f, 6f * density, shadowPaint)

                        // 3. Multi-stop royal metallic gradient
                        val goldShader = LinearGradient(
                            0f, -textSizePx * 0.8f,
                            0f, textSizePx * 0.2f,
                            intArrayOf(
                                android.graphics.Color.parseColor("#FFF4D0"),
                                android.graphics.Color.parseColor("#F5C369"),
                                android.graphics.Color.parseColor("#D49438"),
                                android.graphics.Color.parseColor("#FFE8A3"),
                                android.graphics.Color.parseColor("#B0731E")
                            ),
                            floatArrayOf(0.0f, 0.25f, 0.55f, 0.75f, 1.0f),
                            Shader.TileMode.CLAMP
                        )
                        textPaint.shader = goldShader
                        nativeCanvas.drawText(text, 0f, 0f, textPaint)
                    }

                    VisualEffect.LIQUID_CHROME -> {
                        // 1. Sharp dark reflection base
                        val reflectionBase = Paint(textPaint).apply {
                            color = android.graphics.Color.parseColor("#55000000")
                        }
                        nativeCanvas.drawText(text, 0f, 8f * density, reflectionBase)

                        // 2. Liquid Chrome multi-stop silver gradient
                        val chromeShader = LinearGradient(
                            -textSizePx * 0.5f, -textSizePx * 0.8f,
                            textSizePx * 0.5f, textSizePx * 0.2f,
                            intArrayOf(
                                android.graphics.Color.parseColor("#FFFFFF"),
                                android.graphics.Color.parseColor("#9EA5B0"),
                                android.graphics.Color.parseColor("#1B2028"),
                                android.graphics.Color.parseColor("#FFFFFF"),
                                android.graphics.Color.parseColor("#505866"),
                                android.graphics.Color.parseColor("#CCD2DB")
                            ),
                            floatArrayOf(0.0f, 0.2f, 0.45f, 0.55f, 0.8f, 1.0f),
                            Shader.TileMode.CLAMP
                        )
                        textPaint.shader = chromeShader
                        nativeCanvas.drawText(text, 0f, 0f, textPaint)

                        // 3. Specular highlight outline
                        val strokePaint = Paint(textPaint).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 1.5f * density
                            shader = null
                            color = android.graphics.Color.parseColor("#99FFFFFF")
                        }
                        nativeCanvas.drawText(text, 0f, 0f, strokePaint)
                    }

                    VisualEffect.FROSTED_GLASS -> {
                        // Translucent glass fill with soft inner glow
                        val glassShadow = Paint(textPaint).apply {
                            color = android.graphics.Color.parseColor("#44000000")
                            maskFilter = BlurMaskFilter(16f * density, BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawText(text, 0f, 4f * density, glassShadow)

                        val glassShader = LinearGradient(
                            0f, -textSizePx * 0.8f,
                            0f, textSizePx * 0.2f,
                            intArrayOf(
                                android.graphics.Color.parseColor("#CCFFFFFF"),
                                android.graphics.Color.parseColor("#44E2E8F0")
                            ),
                            null,
                            Shader.TileMode.CLAMP
                        )
                        textPaint.shader = glassShader
                        nativeCanvas.drawText(text, 0f, 0f, textPaint)

                        // Sharp glass edge border
                        val borderPaint = Paint(textPaint).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 1.0f * density
                            shader = null
                            color = android.graphics.Color.parseColor("#88FFFFFF")
                        }
                        nativeCanvas.drawText(text, 0f, 0f, borderPaint)
                    }

                    VisualEffect.CYBER_NEON -> {
                        // Multi-layer intense glow
                        val outerGlow = Paint(textPaint).apply {
                            color = android.graphics.Color.parseColor("#66FF007F")
                            maskFilter = BlurMaskFilter(32f * density, BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawText(text, 0f, 0f, outerGlow)

                        val innerGlow = Paint(textPaint).apply {
                            color = android.graphics.Color.parseColor("#AA00F5D4")
                            maskFilter = BlurMaskFilter(12f * density, BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawText(text, 0f, 0f, innerGlow)

                        // White-hot core
                        textPaint.color = android.graphics.Color.WHITE
                        nativeCanvas.drawText(text, 0f, 0f, textPaint)
                    }

                    VisualEffect.CINEMATIC_SHADOW -> {
                        // Deep 3D projected shadow extrusion
                        for (step in 1..8) {
                            val shadowStep = Paint(textPaint).apply {
                                color = android.graphics.Color.argb((120 - step * 12).coerceAtLeast(10), 0, 0, 0)
                            }
                            nativeCanvas.drawText(text, step * 1.5f * density, step * 2.5f * density, shadowStep)
                        }
                        textPaint.color = android.graphics.Color.parseColor("#F3F4F6")
                        nativeCanvas.drawText(text, 0f, 0f, textPaint)
                    }

                    VisualEffect.MINIMAL_LUXURY -> {
                        val subtleShadow = Paint(textPaint).apply {
                            color = android.graphics.Color.parseColor("#33000000")
                            maskFilter = BlurMaskFilter(8f * density, BlurMaskFilter.Blur.NORMAL)
                        }
                        nativeCanvas.drawText(text, 0f, 3f * density, subtleShadow)
                        textPaint.color = android.graphics.Color.WHITE
                        nativeCanvas.drawText(text, 0f, 0f, textPaint)
                    }
                }

                nativeCanvas.restore()
            }
        }
    }
}
