package app.xrtm8.athar.ui.components

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
import androidx.compose.runtime.LaunchedEffect
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

/**
 * High-Performance 120 FPS Canvas View.
 *
 * Implements Jetpack Compose Draw-Phase Isolation: Touch translation, scaling,
 * and rotation are read strictly inside the Draw Phase, completely skipping
 * the Recomposition and Layout phases for zero-overhead, buttery-smooth 120Hz gesture response.
 */
@Composable
fun CanvasView(
    state: CanvasState,
    modifier: Modifier = Modifier
) {
    // Local transform states read strictly in Draw phase
    var localOffsetX by remember { mutableFloatStateOf(state.offsetX) }
    var localOffsetY by remember { mutableFloatStateOf(state.offsetY) }
    var localScale by remember { mutableFloatStateOf(state.scaleFactor) }
    var localRotation by remember { mutableFloatStateOf(state.rotationAngle) }

    // Sync if external reset is requested (e.g. from reset button)
    LaunchedEffect(state.offsetX, state.offsetY, state.scaleFactor, state.rotationAngle) {
        if (state.offsetX == 0f && state.offsetY == 0f && state.scaleFactor == 1.0f && state.rotationAngle == 0f) {
            localOffsetX = 0f
            localOffsetY = 0f
            localScale = 1.0f
            localRotation = 0f
        }
    }

    // Pre-allocated reusable Paint objects to avoid garbage collection during 120Hz draw loops
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
            .background(AmoledBlack)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotate ->
                    localOffsetX += pan.x
                    localOffsetY += pan.y
                    localScale = (localScale * zoom).coerceIn(0.35f, 5.0f)
                    localRotation += rotate
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (state.displayText.isEmpty()) return@Canvas

            // Access local transforms here inside the DrawScope (Phase 3)
            val currentOx = localOffsetX
            val currentOy = localOffsetY
            val currentSc = localScale
            val currentRot = localRotation

            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f + currentOx
            val centerY = canvasHeight / 2f + currentOy

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                nativeCanvas.save()

                nativeCanvas.translate(centerX, centerY)
                nativeCanvas.rotate(currentRot)
                nativeCanvas.scale(currentSc, currentSc)

                val textSizePx = state.fontSize * density
                basePaint.textSize = textSizePx
                glowPaint.textSize = textSizePx
                shadowPaint.textSize = textSizePx
                strokePaint.textSize = textSizePx

                val lines = state.displayText.split("\n")
                val lineHeight = textSizePx * 1.32f
                val totalHeight = (lines.size - 1) * lineHeight
                val startY = -totalHeight / 2f

                lines.forEachIndexed { index, line ->
                    val lineY = startY + (index * lineHeight)

                    when (state.selectedEffect) {
                        VisualEffect.ROYAL_GOLD -> {
                            glowPaint.color = android.graphics.Color.parseColor("#44E5C07B")
                            glowPaint.maskFilter = BlurMaskFilter(20f * density, BlurMaskFilter.Blur.NORMAL)
                            nativeCanvas.drawText(line, 0f, lineY, glowPaint)

                            shadowPaint.color = android.graphics.Color.parseColor("#88301B05")
                            shadowPaint.maskFilter = null
                            nativeCanvas.drawText(line, 0f, lineY + (5f * density), shadowPaint)

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
                            shadowPaint.color = android.graphics.Color.parseColor("#66000000")
                            shadowPaint.maskFilter = null
                            nativeCanvas.drawText(line, 0f, lineY + (6f * density), shadowPaint)

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
