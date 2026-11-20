package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.ui.theme.AmoledBlack
import kotlin.math.abs

@Composable
fun CanvasView(
    state: CanvasState,
    modifier: Modifier = Modifier,
    onTransformChanged: ((normX: Float, normY: Float, scale: Float, rotation: Float) -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var localNormX by remember { mutableFloatStateOf(state.normOffsetX) }
    var localNormY by remember { mutableFloatStateOf(state.normOffsetY) }
    var localScale by remember { mutableFloatStateOf(state.scaleFactor) }
    var localRotation by remember { mutableFloatStateOf(state.rotationAngle) }

    var canvasW by remember { mutableFloatStateOf(1080f) }
    var canvasH by remember { mutableFloatStateOf(1920f) }

    // Sync when external alignment or reset is triggered
    LaunchedEffect(state.normOffsetX, state.normOffsetY, state.scaleFactor, state.rotationAngle) {
        localNormX = state.normOffsetX
        localNormY = state.normOffsetY
        localScale = state.scaleFactor
        localRotation = state.rotationAngle
    }

    // Load background image bitmap safely with memory limits
    var bgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(state.backgroundImageUri) {
        val uri = state.backgroundImageUri
        bgBitmap = if (uri != null) {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                var sample = 1
                while ((opts.outHeight / sample) > 1920 || (opts.outWidth / sample) > 1080) {
                    sample *= 2
                }
                val finalOpts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, finalOpts)
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Pre-allocated paint objects to guarantee 120Hz zero GC drops
    val basePaint = remember { Paint().apply { isAntiAlias = true; isDither = true } }
    val glowPaint = remember { Paint().apply { isAntiAlias = true; isDither = true } }
    val shadowPaint = remember { Paint().apply { isAntiAlias = true; isDither = true } }
    val strokePaint = remember { Paint().apply { isAntiAlias = true; isDither = true; style = Paint.Style.STROKE } }
    val guidePaint = remember { Paint().apply { isAntiAlias = true; color = android.graphics.Color.parseColor("#77E5C07B"); strokeWidth = 2.5f } }
    val bgPaint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }
    val grayMatrix = remember { ColorMatrix().apply { setSaturation(0f) } }

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
                .background(if (state.isTransparentBg) Color.Transparent else AmoledBlack)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            onDoubleTap?.invoke()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotate ->
                        val cw = canvasW.coerceAtLeast(10f)
                        val ch = canvasH.coerceAtLeast(10f)

                        localNormX += pan.x / cw
                        localNormY += pan.y / ch
                        localScale = (localScale * zoom).coerceIn(0.3f, 6.0f)
                        localRotation += rotate

                        // Magnetic Snap to Center
                        if (abs(localNormX) < 0.015f) localNormX = 0f
                        if (abs(localNormY) < 0.015f) localNormY = 0f

                        onTransformChanged?.invoke(localNormX, localNormY, localScale, localRotation)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            canvasW = canvasWidth
            canvasH = canvasHeight

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas

                // 1. Draw Background: Transparency Checkerboard or Image or Solid Color
                if (state.isTransparentBg) {
                    val tileSize = 24f * density
                    val darkTile = android.graphics.Color.parseColor("#0E0E0E")
                    val lightTile = android.graphics.Color.parseColor("#181818")
                    val tilePaint = Paint()
                    val cols = (canvasWidth / tileSize).toInt() + 1
                    val rows = (canvasHeight / tileSize).toInt() + 1
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            tilePaint.color = if ((r + c) % 2 == 0) darkTile else lightTile
                            nativeCanvas.drawRect(
                                c * tileSize,
                                r * tileSize,
                                (c + 1) * tileSize,
                                (r + 1) * tileSize,
                                tilePaint
                            )
                        }
                    }
                } else if (bgBitmap != null) {
                    if (state.backgroundGrayscale) {
                        bgPaint.colorFilter = ColorMatrixColorFilter(grayMatrix)
                    } else {
                        bgPaint.colorFilter = null
                    }
                    val bm = bgBitmap!!
                    val srcW = bm.width.toFloat()
                    val srcH = bm.height.toFloat()
                    val srcRatio = srcW / srcH
                    val dstRatio = canvasWidth / canvasHeight

                    val srcRect = if (srcRatio > dstRatio) {
                        val newW = (srcH * dstRatio).toInt()
                        val xOffset = ((srcW - newW) / 2).toInt()
                        Rect(xOffset, 0, xOffset + newW, srcH.toInt())
                    } else {
                        val newH = (srcW / dstRatio).toInt()
                        val yOffset = ((srcH - newH) / 2).toInt()
                        Rect(0, yOffset, srcW.toInt(), yOffset + newH)
                    }
                    val dstRect = Rect(0, 0, canvasWidth.toInt(), canvasHeight.toInt())
                    nativeCanvas.drawBitmap(bm, srcRect, dstRect, bgPaint)

                    // Dim Overlay for Text Legibility
                    if (state.backgroundDim > 0f) {
                        val alpha = (state.backgroundDim * 255).toInt().coerceIn(0, 240)
                        nativeCanvas.drawColor(android.graphics.Color.argb(alpha, 0, 0, 0))
                    }
                }

                // 2. Smart Snapping Guidelines
                if (abs(localNormX) < 0.005f) {
                    nativeCanvas.drawLine(canvasWidth / 2f, 0f, canvasWidth / 2f, canvasHeight, guidePaint)
                }
                if (abs(localNormY) < 0.005f) {
                    nativeCanvas.drawLine(0f, canvasHeight / 2f, canvasWidth, canvasHeight / 2f, guidePaint)
                }

                if (state.displayText.isEmpty()) return@drawIntoCanvas

                nativeCanvas.save()

                val centerX = canvasWidth / 2f + (canvasWidth * localNormX)
                val centerY = canvasHeight / 2f + (canvasHeight * localNormY)

                nativeCanvas.translate(centerX, centerY)
                nativeCanvas.rotate(localRotation)
                nativeCanvas.scale(localScale, localScale)

                // Load Authentic Arabic Typeface from assets cache
                val realTypeface = AtharFontManager.getTypeface(context, state.selectedFont)
                basePaint.typeface = realTypeface
                glowPaint.typeface = realTypeface
                shadowPaint.typeface = realTypeface
                strokePaint.typeface = realTypeface

                // Alignment
                val align = when (state.textAlign) {
                    TextAlignOption.RIGHT -> Paint.Align.RIGHT
                    TextAlignOption.CENTER -> Paint.Align.CENTER
                    TextAlignOption.LEFT -> Paint.Align.LEFT
                }
                basePaint.textAlign = align
                glowPaint.textAlign = align
                shadowPaint.textAlign = align
                strokePaint.textAlign = align

                val textSizePx = state.fontSize * density
                basePaint.textSize = textSizePx
                glowPaint.textSize = textSizePx
                shadowPaint.textSize = textSizePx
                strokePaint.textSize = textSizePx

                // Stroke Configuration
                if (state.strokeWidth > 0f) {
                    strokePaint.strokeWidth = state.strokeWidth * density
                    strokePaint.color = state.strokeColorLong.toInt()
                }

                val lines = state.displayText.split("\n")
                val lineHeight = textSizePx * state.lineSpacingMultiplier
                val totalHeight = (lines.size - 1) * lineHeight
                val startY = -totalHeight / 2f

                var maxLineWidth = 0f
                lines.forEach { line ->
                    val w = basePaint.measureText(line)
                    if (w > maxLineWidth) maxLineWidth = w
                }
                val halfBlockW = maxLineWidth / 2f
                val lineX = when (state.textAlign) {
                    TextAlignOption.RIGHT -> halfBlockW
                    TextAlignOption.CENTER -> 0f
                    TextAlignOption.LEFT -> -halfBlockW
                }

                lines.forEachIndexed { index, line ->
                    val lineY = startY + (index * lineHeight)

                    // Stroke Rendering
                    if (state.strokeWidth > 0f) {
                        nativeCanvas.drawText(line, lineX, lineY, strokePaint)
                    }

                    // Custom Color or Effect Shaders
                    if (state.customTextColorLong != null && state.customTextColorLong != 0L) {
                        basePaint.color = state.customTextColorLong.toInt()
                        basePaint.shader = null
                        if (state.shadowRadius > 0f) {
                            shadowPaint.color = android.graphics.Color.parseColor("#99000000")
                            shadowPaint.maskFilter = BlurMaskFilter(state.shadowRadius * density, BlurMaskFilter.Blur.NORMAL)
                            nativeCanvas.drawText(line, lineX, lineY + (state.shadowDy * density), shadowPaint)
                        }
                        nativeCanvas.drawText(line, lineX, lineY, basePaint)
                    } else {
                        when (state.selectedEffect) {
                            VisualEffect.ROYAL_GOLD -> {
                                glowPaint.color = android.graphics.Color.parseColor("#44E5C07B")
                                glowPaint.maskFilter = BlurMaskFilter(22f * density, BlurMaskFilter.Blur.NORMAL)
                                nativeCanvas.drawText(line, lineX, lineY, glowPaint)

                                shadowPaint.color = android.graphics.Color.parseColor("#99301B05")
                                shadowPaint.maskFilter = null
                                nativeCanvas.drawText(line, lineX, lineY + (state.shadowDy * density), shadowPaint)

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
                                nativeCanvas.drawText(line, lineX, lineY, basePaint)
                            }

                            VisualEffect.LIQUID_CHROME -> {
                                shadowPaint.color = android.graphics.Color.parseColor("#66000000")
                                shadowPaint.maskFilter = null
                                nativeCanvas.drawText(line, lineX, lineY + (state.shadowDy * density), shadowPaint)

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
                                nativeCanvas.drawText(line, lineX, lineY, basePaint)
                            }

                            VisualEffect.FROSTED_GLASS -> {
                                shadowPaint.color = android.graphics.Color.parseColor("#55000000")
                                shadowPaint.maskFilter = BlurMaskFilter(14f * density, BlurMaskFilter.Blur.NORMAL)
                                nativeCanvas.drawText(line, lineX, lineY + (state.shadowDy * density), shadowPaint)

                                basePaint.shader = LinearGradient(
                                    0f, lineY - textSizePx * 0.8f,
                                    0f, lineY + textSizePx * 0.2f,
                                    intArrayOf(
                                        android.graphics.Color.parseColor("#E6FFFFFF"),
                                        android.graphics.Color.parseColor("#33E2E8F0")
                                    ),
                                    null,
                                    Shader.TileMode.CLAMP
                                )
                                nativeCanvas.drawText(line, lineX, lineY, basePaint)
                            }

                            VisualEffect.CYBER_NEON -> {
                                glowPaint.color = android.graphics.Color.parseColor("#6600FF9D")
                                glowPaint.maskFilter = BlurMaskFilter(28f * density, BlurMaskFilter.Blur.NORMAL)
                                nativeCanvas.drawText(line, lineX, lineY, glowPaint)

                                shadowPaint.color = android.graphics.Color.parseColor("#88009955")
                                shadowPaint.maskFilter = BlurMaskFilter(12f * density, BlurMaskFilter.Blur.NORMAL)
                                nativeCanvas.drawText(line, lineX, lineY, shadowPaint)

                                basePaint.shader = null
                                basePaint.color = android.graphics.Color.WHITE
                                nativeCanvas.drawText(line, lineX, lineY, basePaint)
                            }

                            VisualEffect.CINEMATIC_SHADOW -> {
                                for (step in 1..6) {
                                    shadowPaint.color = android.graphics.Color.argb((110 - step * 16).coerceAtLeast(10), 0, 0, 0)
                                    shadowPaint.maskFilter = null
                                    nativeCanvas.drawText(line, lineX + (step * 1.5f * density), lineY + (step * 2.2f * density), shadowPaint)
                                }
                                basePaint.shader = null
                                basePaint.color = android.graphics.Color.parseColor("#F5F6F8")
                                nativeCanvas.drawText(line, lineX, lineY, basePaint)
                            }

                            VisualEffect.MINIMAL_LUXURY -> {
                                shadowPaint.color = android.graphics.Color.parseColor("#44000000")
                                shadowPaint.maskFilter = BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL)
                                nativeCanvas.drawText(line, lineX, lineY + (state.shadowDy * density), shadowPaint)

                                basePaint.shader = null
                                basePaint.color = android.graphics.Color.WHITE
                                nativeCanvas.drawText(line, lineX, lineY, basePaint)
                            }
                        }
                    }
                }

                nativeCanvas.restore()
            }
        }
    }
}
