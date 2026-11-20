package app.xrtm8.athar.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.ui.theme.AmoledBlack

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

    // Smooth animated scale and rotation for fluid feel
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "animatedScale"
    )

    // Pre-allocated reusable Paint objects to avoid GC allocation during draw loop
    val basePaint = remember {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            textAlign = Paint.Align.CENTER
        }
    }
    val glowPaint = remember {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            textAlign = Paint.Align.CENTER
        }
    }
    val shadowPaint = remember {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            textAlign = Paint.Align.CENTER
        }
    }
    val strokePaint = remember {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            textAlign = Paint.Align.CENTER
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (state.isTransparentBg) AmoledBlack else AmoledBlack)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotate ->
                    offsetX += pan.x
                    offsetY += pan.y
                    scale = (scale * zoom).coerceIn(0.4f, 5.0f)
                    rotation += rotate
                    onTransformChange(offsetX, offsetY, scale, rotation)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (state.displayText.isEmpty()) return@Canvas

            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f + offsetX
            val centerY = canvasHeight / 2f + offsetY

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                nativeCanvas.save()

                nativeCanvas.translate(centerX, centerY)
                nativeCanvas.rotate(rotation)
                nativeCanvas.scale(animatedScale, animatedScale)

                val textSizePx = state.fontSize * density
                basePaint.textSize = textSizePx
                glowPaint.textSize = textSizePx
                shadowPaint.textSize = textSizePx
                strokePaint.textSize = textSizePx

                val lines = state.displayText.split("\n")
                val lineHeight = textSizePx * 1.3f
                val totalHeight = (lines.size - 1) * lineHeight
                val startY = -totalHeight / 2f

                // Render each line with proper calligraphic alignment
                lines.forEachIndexed { index, line ->
                    val lineY = startY + (index * lineHeight)

                    when (state.selectedEffect) {
                        VisualEffect.ROYAL_GOLD -> {
                            // 1. Ambient Gold Glow
                            glowPaint.color = android.graphics.Color.parseColor("#44E5C07B")
                            glowPaint.maskFilter = BlurMaskFilter(20f * density, BlurMaskFilter.Blur.NORMAL)
                            nativeCanvas.drawText(line, 0f, lineY, glowPaint)

                            // 2. Warm Shadow
                            shadowPaint.color = android.graphics.Color.parseColor("#88301B05")
                            shadowPaint.maskFilter = null
                            nativeCanvas.drawText(line, 0f, lineY + (5f * density), shadowPaint)

                            // 3. Royal Gold multi-stop gradient
                            basePaint.shader = LinearGradient(
                                0f, lineY - textSizePx * 0.8f,
                                0f, lineY + textSizePx * 0.2f,
                                intArrayOf(
                                    android.graphics.Color.parseColor("#FFF7DB"),
                                    android.graphics.Color.parseColor("#F5C369"),
                                    android.graphics.Color.parseColor("#D49438"),
                                    android.graphics.Color.parseColor("#FFE8A3"),
                                    android.graphics.Color.parseColor("#B0731E")
                                ),
                                floatArrayOf(0.0f, 0.25f, 0.55f, 0.75f, 1.0f),
                                Shader.TileMode.CLAMP
                            )
                            nativeCanvas.drawText(line, 0f, lineY, basePaint)
                        }

                        VisualEffect.LIQUID_CHROME -> {
                            // 1. Deep Specular Backdrop
                            shadowPaint.color = android.graphics.Color.parseColor("#66000000")
                            shadowPaint.maskFilter = null
                            nativeCanvas.drawText(line, 0f, lineY + (6f * density), shadowPaint)

                            // 2. Liquid Chrome Multi-stop Metal Shader
                            basePaint.shader = LinearGradient(
                                -textSizePx * 0.5f, lineY - textSizePx * 0.8f,
                                textSizePx * 0.5f, lineY + textSizePx * 0.2f,
                                intArrayOf(
                                    android.graphics.Color.parseColor("#FFFFFF"),
                                    android.graphics.Color.parseColor("#9EA5B0"),
                                    android.graphics.Color.parseColor("#181C22"),
                                    android.graphics.Color.parseColor("#FFFFFF"),
                                    android.graphics.Color.parseColor("#505866"),
                                    android.graphics.Color.parseColor("#DDE2EA")
                                ),
                                floatArrayOf(0.0f, 0.2f, 0.45f, 0.55f, 0.8f, 1.0f),
                                Shader.TileMode.CLAMP
                            )
                            nativeCanvas.drawText(line, 0f, lineY, basePaint)

                            // 3. Crisp chrome highlight rim
                            strokePaint.strokeWidth = 1.2f * density
                            strokePaint.shader = null
                            strokePaint.color = android.graphics.Color.parseColor("#88FFFFFF")
                            nativeCanvas.drawText(line, 0f, lineY, strokePaint)
                        }

                        VisualEffect.FROSTED_GLASS -> {
                            shadowPaint.color = android.graphics.Color.parseColor("#55000000")
                            shadowPaint.maskFilter = BlurMaskFilter(14f * density, BlurMaskFilter.Blur.NORMAL)
                            nativeCanvas.drawText(line, 0f, lineY + (4f * density), shadowPaint)

                            basePaint.shader = LinearGradient(
                                0f, lineY - textSizePx * 0.8f,
                                0f, lineY + textSizePx * 0.2f,
                                intArrayOf(
                                    android.graphics.Color.parseColor("#D9FFFFFF"),
                                    android.graphics.Color.parseColor("#33E2E8F0")
                                ),
                                null,
                                Shader.TileMode.CLAMP
                            )
                            nativeCanvas.drawText(line, 0f, lineY, basePaint)

                            strokePaint.strokeWidth = 1.0f * density
                            strokePaint.shader = null
                            strokePaint.color = android.graphics.Color.parseColor("#77FFFFFF")
                            nativeCanvas.drawText(line, 0f, lineY, strokePaint)
                        }

                        VisualEffect.CYBER_NEON -> {
                            glowPaint.color = android.graphics.Color.parseColor("#55FF007F")
                            glowPaint.maskFilter = BlurMaskFilter(28f * density, BlurMaskFilter.Blur.NORMAL)
                            nativeCanvas.drawText(line, 0f, lineY, glowPaint)

                            shadowPaint.color = android.graphics.Color.parseColor("#8800F5D4")
                            shadowPaint.maskFilter = BlurMaskFilter(10f * density, BlurMaskFilter.Blur.NORMAL)
                            nativeCanvas.drawText(line, 0f, lineY, shadowPaint)

                            basePaint.shader = null
                            basePaint.color = android.graphics.Color.WHITE
                            nativeCanvas.drawText(line, 0f, lineY, basePaint)
                        }

                        VisualEffect.CINEMATIC_SHADOW -> {
                            for (step in 1..6) {
                                shadowPaint.color = android.graphics.Color.argb((100 - step * 14).coerceAtLeast(10), 0, 0, 0)
                                shadowPaint.maskFilter = null
                                nativeCanvas.drawText(line, step * 1.5f * density, lineY + (step * 2.2f * density), shadowPaint)
                            }
                            basePaint.shader = null
                            basePaint.color = android.graphics.Color.parseColor("#F5F6F8")
                            nativeCanvas.drawText(line, 0f, lineY, basePaint)
                        }

                        VisualEffect.MINIMAL_LUXURY -> {
                            shadowPaint.color = android.graphics.Color.parseColor("#44000000")
                            shadowPaint.maskFilter = BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL)
                            nativeCanvas.drawText(line, 0f, lineY + (2.5f * density), shadowPaint)

                            basePaint.shader = null
                            basePaint.color = android.graphics.Color.WHITE
                            nativeCanvas.drawText(line, 0f, lineY, basePaint)
                        }
                    }
                }

                nativeCanvas.restore()
            }
        }
    }
}
