package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.ui.theme.AmoledBlack
import kotlin.math.abs

@Composable
fun CanvasView(
    state: CanvasState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var localOffsetX by remember { mutableFloatStateOf(state.offsetX) }
    var localOffsetY by remember { mutableFloatStateOf(state.offsetY) }
    var localScale by remember { mutableFloatStateOf(state.scaleFactor) }
    var localRotation by remember { mutableFloatStateOf(state.rotationAngle) }

    // Sync if external alignment is triggered (e.g. Align Center/Top/Bottom)
    LaunchedEffect(state.offsetX, state.offsetY, state.scaleFactor, state.rotationAngle) {
        localOffsetX = state.offsetX
        localOffsetY = state.offsetY
        localScale = state.scaleFactor
        localRotation = state.rotationAngle
    }

    // Load background image bitmap when URI changes
    var bgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(state.backgroundImageUri) {
        val uri = state.backgroundImageUri
        bgBitmap = if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Pre-allocated paint objects to guarantee 120Hz zero GC drops
    val basePaint = remember { Paint().apply { isAntiAlias = true; isDither = true; textAlign = Paint.Align.CENTER } }
    val glowPaint = remember { Paint().apply { isAntiAlias = true; isDither = true; textAlign = Paint.Align.CENTER } }
    val shadowPaint = remember { Paint().apply { isAntiAlias = true; isDither = true; textAlign = Paint.Align.CENTER } }
    val strokePaint = remember { Paint().apply { isAntiAlias = true; isDither = true; style = Paint.Style.STROKE; textAlign = Paint.Align.CENTER } }
    val guidePaint = remember { Paint().apply { isAntiAlias = true; color = android.graphics.Color.parseColor("#55E5C07B"); strokeWidth = 2f } }

    val ratio = state.aspectRatio.ratio
    val canvasModifier = if (ratio != null) {
        Modifier
            .aspectRatio(ratio)
            .clipToBounds()
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Canvas(
            modifier = canvasModifier
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
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas

                // 1. Draw Background Image (if loaded)
                if (bgBitmap != null && !state.isTransparentBg) {
                    val src = Rect(0, 0, bgBitmap!!.width, bgBitmap!!.height)
                    val dst = Rect(0, 0, canvasWidth.toInt(), canvasHeight.toInt())
                    nativeCanvas.drawBitmap(bgBitmap!!, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))

                    // 2. Dim Overlay for Text Legibility
                    if (state.backgroundDim > 0f) {
                        val alpha = (state.backgroundDim * 255).toInt().coerceIn(0, 240)
                        nativeCanvas.drawColor(android.graphics.Color.argb(alpha, 0, 0, 0))
                    }
                }

                // 3. Smart Snapping Guidelines (visual feedback when centered)
                val currentOx = localOffsetX
                val currentOy = localOffsetY
                if (abs(currentOx) < 18f) {
                    nativeCanvas.drawLine(canvasWidth / 2f, 0f, canvasWidth / 2f, canvasHeight, guidePaint)
                }
                if (abs(currentOy) < 18f) {
                    nativeCanvas.drawLine(0f, canvasHeight / 2f, canvasWidth, canvasHeight / 2f, guidePaint)
                }

                if (state.displayText.isEmpty()) return@drawIntoCanvas

                nativeCanvas.save()

                val centerX = canvasWidth / 2f + currentOx
                val centerY = canvasHeight / 2f + currentOy

                nativeCanvas.translate(centerX, centerY)
                nativeCanvas.rotate(localRotation)
                nativeCanvas.scale(localScale, localScale)

                // Set Typeface according to selected font
                val selectedTypeface = when (state.selectedFont) {
                    ArabicFontFamily.TRADITIONAL -> Typeface.SERIF
                    ArabicFontFamily.MODERN_SANS -> Typeface.SANS_SERIF
                    ArabicFontFamily.RUQAA -> Typeface.SERIF
                    ArabicFontFamily.KUFI -> Typeface.DEFAULT_BOLD
                    ArabicFontFamily.ELEGANT -> Typeface.DEFAULT
                    ArabicFontFamily.MONOSPACE -> Typeface.MONOSPACE
                }
                basePaint.typeface = selectedTypeface
                glowPaint.typeface = selectedTypeface
                shadowPaint.typeface = selectedTypeface
                strokePaint.typeface = selectedTypeface

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
