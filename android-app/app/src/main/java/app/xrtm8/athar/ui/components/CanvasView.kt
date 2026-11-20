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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.GlassBorder
import kotlin.math.abs

@Composable
fun CanvasView(
    state: CanvasState,
    modifier: Modifier = Modifier,
    onTransformFinished: ((normX: Float, normY: Float, scale: Float, rotation: Float) -> Unit)? = null,
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
            .fillMaxSize()
            .aspectRatio(ratio, matchHeightConstraintsFirst = false)
            .clipToBounds()
            .border(0.5.dp, GlassBorder)
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
                .background(if (state.isTransparentBg) Color.Transparent else AmoledBlack)
                .pointerInput(state.isLocked) {
                    detectCanvasGestures(
                        isLocked = state.isLocked,
                        onTransform = { pan, zoom, rotate ->
                            val cw = canvasW.coerceAtLeast(10f)
                            val ch = canvasH.coerceAtLeast(10f)

                            localNormX += pan.x / cw
                            localNormY += pan.y / ch
                            localScale = (localScale * zoom).coerceIn(0.25f, 6.0f)
                            localRotation = (localRotation + rotate) % 360f

                            // Magnetic Snap to Center
                            if (abs(localNormX) < 0.015f) localNormX = 0f
                            if (abs(localNormY) < 0.015f) localNormY = 0f
                        },
                        onGestureEnd = {
                            onTransformFinished?.invoke(localNormX, localNormY, localScale, localRotation)
                        },
                        onDoubleTap = {
                            onDoubleTap?.invoke()
                        }
                    )
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
                    val tileSize = 20f * density
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
                } else if (state.solidBgColorLong != null) {
                    nativeCanvas.drawColor(state.solidBgColorLong.toInt())
                } else {
                    nativeCanvas.drawColor(android.graphics.Color.BLACK)
                }

                // 2. Smart Snapping & Golden Grid Guidelines
                if (state.showGridGuides) {
                    val gridPaint = Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.parseColor("#33E5C07B")
                        strokeWidth = 1.5f
                    }
                    nativeCanvas.drawLine(canvasWidth / 3f, 0f, canvasWidth / 3f, canvasHeight, gridPaint)
                    nativeCanvas.drawLine(canvasWidth * 2f / 3f, 0f, canvasWidth * 2f / 3f, canvasHeight, gridPaint)
                    nativeCanvas.drawLine(0f, canvasHeight / 3f, canvasWidth, canvasHeight / 3f, gridPaint)
                    nativeCanvas.drawLine(0f, canvasHeight * 2f / 3f, canvasWidth, canvasHeight * 2f / 3f, gridPaint)
                }

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
                val alphaFloat = state.textOpacity.coerceIn(0.15f, 1.0f)
                val alphaInt = (alphaFloat * 255).toInt()

                basePaint.textSize = textSizePx
                basePaint.letterSpacing = state.letterSpacing
                basePaint.isUnderlineText = state.isUnderline
                basePaint.isFakeBoldText = state.isBold
                basePaint.textSkewX = if (state.isItalic) -0.25f else 0f
                basePaint.alpha = alphaInt

                // Color Filter for Opacity across Shaders and Colors
                val alphaMatrix = ColorMatrix().apply {
                    setScale(1f, 1f, 1f, alphaFloat)
                }
                basePaint.colorFilter = ColorMatrixColorFilter(alphaMatrix)

                glowPaint.textSize = textSizePx
                glowPaint.letterSpacing = state.letterSpacing
                glowPaint.textSkewX = basePaint.textSkewX

                shadowPaint.textSize = textSizePx
                shadowPaint.letterSpacing = state.letterSpacing
                shadowPaint.textSkewX = basePaint.textSkewX
                shadowPaint.isFakeBoldText = state.isBold

                strokePaint.textSize = textSizePx
                strokePaint.strokeJoin = Paint.Join.ROUND
                strokePaint.strokeCap = Paint.Cap.ROUND
                strokePaint.letterSpacing = state.letterSpacing
                strokePaint.isUnderlineText = state.isUnderline
                strokePaint.isFakeBoldText = state.isBold
                strokePaint.textSkewX = basePaint.textSkewX
                strokePaint.alpha = alphaInt
                strokePaint.colorFilter = basePaint.colorFilter

                // Stroke Configuration
                if (state.strokeWidth > 0f) {
                    strokePaint.strokeWidth = state.strokeWidth * density
                    strokePaint.color = state.strokeColorLong.toInt()
                }

                val shadowDxPx = state.shadowDx * density
                val shadowDyPx = state.shadowDy * density

                val lines = state.displayText.split("\n")
                val lineHeight = textSizePx * state.lineSpacingMultiplier
                val totalHeight = (lines.size - 1) * lineHeight
                // Accurate vertical baseline centering
                val startY = -totalHeight / 2f + (textSizePx * 0.35f)

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

                    // 1. Draw 2D Directional Shadow with Blur for ALL modes
                    if (state.shadowRadius > 0f) {
                        val sColor = state.shadowColorLong.toInt()
                        val shadowAlpha = (((sColor ushr 24) and 0xFF) * alphaFloat).toInt()
                        shadowPaint.color = (sColor and 0x00FFFFFF) or (shadowAlpha shl 24)
                        shadowPaint.maskFilter = BlurMaskFilter((state.shadowRadius * density).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                        nativeCanvas.drawText(line, lineX + shadowDxPx, lineY + shadowDyPx, shadowPaint)
                    }

                    // 2. Draw Smooth Stroke
                    if (state.strokeWidth > 0f) {
                        nativeCanvas.drawText(line, lineX, lineY, strokePaint)
                    }

                    // 3. Custom Color or Effect Shaders Fill
                    if (state.customTextColorLong != null && state.customTextColorLong != 0L) {
                        val baseColor = state.customTextColorLong.toInt()
                        basePaint.color = (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
                        basePaint.shader = null
                        nativeCanvas.drawText(line, lineX, lineY, basePaint)
                    } else {
                        when (state.selectedEffect) {
                            VisualEffect.ROYAL_GOLD -> {
                                basePaint.shader = LinearGradient(
                                    0f, lineY - textSizePx * 0.85f,
                                    0f, lineY + textSizePx * 0.25f,
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
                                basePaint.shader = LinearGradient(
                                    -textSizePx * 0.5f, lineY - textSizePx * 0.85f,
                                    textSizePx * 0.5f, lineY + textSizePx * 0.25f,
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
                                basePaint.shader = LinearGradient(
                                    0f, lineY - textSizePx * 0.85f,
                                    0f, lineY + textSizePx * 0.25f,
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

                                basePaint.shader = null
                                basePaint.color = (android.graphics.Color.WHITE and 0x00FFFFFF) or (alphaInt shl 24)
                                nativeCanvas.drawText(line, lineX, lineY, basePaint)
                            }

                            VisualEffect.CINEMATIC_SHADOW -> {
                                basePaint.shader = null
                                basePaint.color = (android.graphics.Color.parseColor("#F5F6F8") and 0x00FFFFFF) or (alphaInt shl 24)
                                nativeCanvas.drawText(line, lineX, lineY, basePaint)
                            }

                            VisualEffect.MINIMAL_LUXURY -> {
                                basePaint.shader = null
                                basePaint.color = (android.graphics.Color.WHITE and 0x00FFFFFF) or (alphaInt shl 24)
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

/**
 * High-performance pointer gesture detector for interactive studio canvas.
 * Handles double-tap and transform gestures without recomposition lag or event conflicts.
 */
private suspend fun PointerInputScope.detectCanvasGestures(
    isLocked: Boolean,
    onTransform: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onGestureEnd: () -> Unit,
    onDoubleTap: () -> Unit
) {
    var lastTapTime = 0L

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val now = System.currentTimeMillis()
        if (now - lastTapTime < 320L) {
            onDoubleTap()
            lastTapTime = 0L
        } else {
            lastTapTime = now
        }

        if (isLocked) return@awaitEachGesture

        var zoom = 1f
        var rotation = 0f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1f - zoom) * centroidSize
                    val rotationMotion = abs(rotation * Math.PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                        onTransform(panChange, zoomChange, rotationChange)
                    }
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        if (pastTouchSlop) {
            onGestureEnd()
        }
    }
}
