package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.core.CinematicOpticalEngine
import app.xrtm8.athar.core.FilmGrainEngine
import app.xrtm8.athar.core.SubjectDepthEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.LayerBlendMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.typography.AtharTextLayout
import app.xrtm8.athar.typography.CurvedTextRenderer
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.GlassBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun CanvasView(
    state: CanvasState,
    modifier: Modifier = Modifier,
    onTransformFinished: ((normX: Float, normY: Float, scale: Float, rotation: Float) -> Unit)? = null,
    onPhotoFramingFinished: ((normX: Float, normY: Float, scale: Float) -> Unit)? = null,
    onSelectLayer: ((String) -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    val active = state.activeLayer

    var localNormX by remember(active.id) { mutableFloatStateOf(active.normOffsetX) }
    var localNormY by remember(active.id) { mutableFloatStateOf(active.normOffsetY) }
    var localScale by remember(active.id) { mutableFloatStateOf(active.scaleFactor) }
    var localRotation by remember(active.id) { mutableFloatStateOf(active.rotationAngle) }

    var localPhotoX by remember { mutableFloatStateOf(state.bgPhotoOffsetX) }
    var localPhotoY by remember { mutableFloatStateOf(state.bgPhotoOffsetY) }
    var localPhotoScale by remember { mutableFloatStateOf(state.bgPhotoScale) }

    var canvasW by remember { mutableFloatStateOf(1080f) }
    var canvasH by remember { mutableFloatStateOf(1920f) }

    // Sync when external alignment or reset is triggered
    LaunchedEffect(active.id, active.normOffsetX, active.normOffsetY, active.scaleFactor, active.rotationAngle) {
        localNormX = active.normOffsetX
        localNormY = active.normOffsetY
        localScale = active.scaleFactor
        localRotation = active.rotationAngle
    }

    LaunchedEffect(state.bgPhotoOffsetX, state.bgPhotoOffsetY, state.bgPhotoScale) {
        localPhotoX = state.bgPhotoOffsetX
        localPhotoY = state.bgPhotoOffsetY
        localPhotoScale = state.bgPhotoScale
    }

    // Load background image bitmap safely with memory limits and zero leaks
    var rawBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBgBitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            val p = processedBgBitmap
            val r = rawBgBitmap
            if (p != null && p != r && !p.isRecycled) {
                p.recycle()
            }
            if (r != null && !r.isRecycled) {
                r.recycle()
            }
            SubjectDepthEngine.clearCache()
        }
    }

    var subjectCutoutBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(processedBgBitmap, state.isTextBehindSubject, state.subjectDepthSensitivity) {
        val bg = processedBgBitmap
        if (state.isTextBehindSubject && bg != null && !bg.isRecycled) {
            subjectCutoutBitmap = SubjectDepthEngine.getSubjectCutout(bg, state.subjectDepthSensitivity)
        } else {
            subjectCutoutBitmap = null
        }
    }

    LaunchedEffect(state.backgroundImageUri) {
        val uri = state.backgroundImageUri
        val oldRaw = rawBgBitmap
        val loaded = if (uri != null) {
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
        rawBgBitmap = loaded
        if (oldRaw != null && oldRaw != loaded && !oldRaw.isRecycled) {
            oldRaw.recycle()
        }
    }

    LaunchedEffect(rawBgBitmap, state.backgroundBlur) {
        val base = rawBgBitmap
        val oldProcessed = processedBgBitmap
        val next = if (base != null) {
            if (state.backgroundBlur > 0.5f) {
                withContext(Dispatchers.Default) {
                    AtharImageProcessor.fastPreviewBlur(base, state.backgroundBlur.toInt().coerceIn(1, 35))
                }
            } else {
                base
            }
        } else {
            null
        }
        processedBgBitmap = next
        if (oldProcessed != null && oldProcessed != base && oldProcessed != next && !oldProcessed.isRecycled) {
            oldProcessed.recycle()
        }
    }

    var isInteracting by remember { mutableStateOf(false) }
    val basePaint = remember { Paint().apply { isAntiAlias = true; isDither = true } }
    val glowPaint = remember { Paint().apply { isAntiAlias = true; isDither = true } }
    val shadowPaint = remember { Paint().apply { isAntiAlias = true; isDither = true } }
    val strokePaint = remember { Paint().apply { isAntiAlias = true; isDither = true; style = Paint.Style.STROKE } }
    val guidePaint = remember { Paint().apply { isAntiAlias = true; color = 0x99FFD700.toInt(); strokeWidth = 2.0f } }
    val gridPaint = remember { Paint().apply { isAntiAlias = true; color = 0x33E5C07B.toInt(); strokeWidth = 1.5f } }
    val selectBoxPaint = remember { Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = 0x77FFD700.toInt(); strokeWidth = 1.2f * density } }
    val handlePaint = remember { Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = 0xFFFFD700.toInt(); strokeWidth = 2.4f * density; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND } }
    val handleFillPaint = remember { Paint().apply { isAntiAlias = true; style = Paint.Style.FILL; color = 0xFFFFD700.toInt() } }
    val bgPaint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }
    val tashkeelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true; textAlign = Paint.Align.CENTER } }

    val neonGlowColor = remember { 0x6600FF9D.toInt() }
    val cinematicShadowColor = remember { 0xFFF5F6F8.toInt() }
    val neonBlurFilter = remember(density) {
        BlurMaskFilter((28f * density).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
    }

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

    val stateForBg = if (state.isFramingMode) {
        state.copy(
            bgPhotoOffsetX = localPhotoX,
            bgPhotoOffsetY = localPhotoY,
            bgPhotoScale = localPhotoScale
        )
    } else {
        state
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Canvas(
            modifier = canvasModifier
                .background(if (state.isTransparentBg) androidx.compose.ui.graphics.Color.Transparent else AmoledBlack)
                .pointerInput(state.isLocked, state.isFramingMode, active.id) {
                    detectCanvasGestures(
                        isLocked = state.isLocked,
                        onTransform = { pan, zoom, rotate ->
                            isInteracting = true
                            val cw = canvasW.coerceAtLeast(10f)
                            val ch = canvasH.coerceAtLeast(10f)

                            if (state.isFramingMode) {
                                localPhotoX = (localPhotoX - pan.x / (cw * 0.5f)).coerceIn(-1f, 1f)
                                localPhotoY = (localPhotoY - pan.y / (ch * 0.5f)).coerceIn(-1f, 1f)
                                localPhotoScale = (localPhotoScale * zoom).coerceIn(0.5f, 4.0f)
                            } else {
                                localNormX += pan.x / cw
                                localNormY += pan.y / ch
                                localScale = (localScale * zoom).coerceIn(0.25f, 6.0f)
                                localRotation = (localRotation + rotate) % 360f

                                var snapped = false
                                if (abs(localNormX) < 0.015f) {
                                    if (localNormX != 0f) snapped = true
                                    localNormX = 0f
                                }
                                if (abs(localNormY) < 0.015f) {
                                    if (localNormY != 0f) snapped = true
                                    localNormY = 0f
                                }

                                val rot360 = ((localRotation % 360f) + 360f) % 360f
                                for (cardinal in listOf(0f, 90f, 180f, 270f, 360f)) {
                                    if (abs(rot360 - cardinal) < 2.5f) {
                                        val target = if (cardinal == 360f) 0f else cardinal
                                        if (localRotation != target) snapped = true
                                        localRotation = target
                                        break
                                    }
                                }

                                if (snapped) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onGestureEnd = {
                            isInteracting = false
                            if (state.isFramingMode) {
                                onPhotoFramingFinished?.invoke(localPhotoX, localPhotoY, localPhotoScale)
                            } else {
                                onTransformFinished?.invoke(localNormX, localNormY, localScale, localRotation)
                            }
                        },
                        onTap = { tapOffset ->
                            if (!state.isFramingMode) {
                                val hit = findHitLayer(tapOffset.x, tapOffset.y, canvasW, canvasH, state, density)
                                if (hit != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectLayer?.invoke(hit.id)
                                }
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            if (!state.isFramingMode) {
                                val hit = findHitLayer(tapOffset.x, tapOffset.y, canvasW, canvasH, state, density)
                                if (hit != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectLayer?.invoke(hit.id)
                                    onDoubleTap?.invoke()
                                } else {
                                    onDoubleTap?.invoke()
                                }
                            } else {
                                onDoubleTap?.invoke()
                            }
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

                // 1. Draw Background Viewport with photo adjustments, framing, blur, and gradient scrim
                CanvasBackgroundRenderer.drawBackground(
                    nativeCanvas = nativeCanvas,
                    state = stateForBg,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    density = density,
                    bgBitmap = processedBgBitmap,
                    bgPaint = bgPaint
                )

                // 2. Smart Snapping & Golden Grid Guidelines
                if (state.showGridGuides || state.isFramingMode) {
                    nativeCanvas.drawLine(canvasWidth / 3f, 0f, canvasWidth / 3f, canvasHeight, gridPaint)
                    nativeCanvas.drawLine(canvasWidth * 2f / 3f, 0f, canvasWidth * 2f / 3f, canvasHeight, gridPaint)
                    nativeCanvas.drawLine(0f, canvasHeight / 3f, canvasWidth, canvasHeight / 3f, gridPaint)
                    nativeCanvas.drawLine(0f, canvasHeight * 2f / 3f, canvasWidth, canvasHeight * 2f / 3f, gridPaint)
                }

                if (isInteracting && !state.isFramingMode) {
                    if (abs(localNormX) < 0.005f) {
                        nativeCanvas.drawLine(canvasWidth / 2f, 0f, canvasWidth / 2f, canvasHeight, guidePaint)
                    }
                    if (abs(localNormY) < 0.005f) {
                        nativeCanvas.drawLine(0f, canvasHeight / 2f, canvasWidth, canvasHeight / 2f, guidePaint)
                    }
                }

                // 3. Render all Text Layers with Blend Modes, Curved Paths, Free Tashkeel & Subject Depth
                val renderTextLayer: (TextLayer) -> Unit = { layer ->
                    if (layer.isVisible && layer.displayText.isNotEmpty()) {
                        val isCurrentActive = (layer.id == active.id)

                        val layerNormX = if (isCurrentActive) localNormX else layer.normOffsetX
                        val layerNormY = if (isCurrentActive) localNormY else layer.normOffsetY
                        val layerScale = if (isCurrentActive) localScale else layer.scaleFactor
                        val layerRotation = if (isCurrentActive) localRotation else layer.rotationAngle

                        val realTypeface = AtharFontManager.getTypeface(context, layer.selectedFont)
                        val textSizePx = layer.fontSize * density
                        val alphaFloat = layer.textOpacity.coerceIn(0.15f, 1.0f)
                        val alphaInt = (alphaFloat * 255).toInt()
                        val alphaColorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(1f, 1f, 1f, alphaFloat) })

                        basePaint.typeface = realTypeface
                        glowPaint.typeface = realTypeface
                        shadowPaint.typeface = realTypeface
                        strokePaint.typeface = realTypeface

                        val align = when (layer.textAlign) {
                            TextAlignOption.RIGHT -> Paint.Align.RIGHT
                            TextAlignOption.CENTER -> Paint.Align.CENTER
                            TextAlignOption.LEFT -> Paint.Align.LEFT
                        }
                        basePaint.textAlign = align
                        glowPaint.textAlign = align
                        shadowPaint.textAlign = align
                        strokePaint.textAlign = align

                        basePaint.textSize = textSizePx
                        basePaint.letterSpacing = layer.letterSpacing
                        basePaint.isUnderlineText = layer.isUnderline
                        basePaint.isFakeBoldText = layer.isBold
                        basePaint.textSkewX = if (layer.isItalic) -0.25f else 0f
                        basePaint.alpha = alphaInt
                        basePaint.colorFilter = alphaColorFilter
                        basePaint.xfermode = if (layer.blendMode != LayerBlendMode.NORMAL) {
                            PorterDuffXfermode(layer.blendMode.porterDuffMode)
                        } else null

                        glowPaint.textSize = textSizePx
                        glowPaint.letterSpacing = layer.letterSpacing
                        glowPaint.textSkewX = basePaint.textSkewX

                        shadowPaint.textSize = textSizePx
                        shadowPaint.letterSpacing = layer.letterSpacing
                        shadowPaint.textSkewX = basePaint.textSkewX
                        shadowPaint.isFakeBoldText = layer.isBold

                        strokePaint.textSize = textSizePx
                        strokePaint.strokeJoin = Paint.Join.ROUND
                        strokePaint.strokeCap = Paint.Cap.ROUND
                        strokePaint.letterSpacing = layer.letterSpacing
                        strokePaint.isUnderlineText = layer.isUnderline
                        strokePaint.isFakeBoldText = layer.isBold
                        strokePaint.textSkewX = basePaint.textSkewX
                        strokePaint.alpha = alphaInt
                        strokePaint.colorFilter = alphaColorFilter

                        if (layer.strokeWidth > 0f) {
                            strokePaint.strokeWidth = layer.strokeWidth * density
                            strokePaint.color = layer.strokeColorLong.toInt()
                        }

                        val layerLayout = AtharTextLayout.computeLayout(
                            text = layer.displayText,
                            paint = basePaint,
                            lineSpacingMultiplier = layer.lineSpacingMultiplier,
                            textAlign = layer.textAlign
                        )

                        nativeCanvas.save()

                        val centerX = canvasWidth / 2f + (canvasWidth * layerNormX)
                        val centerY = canvasHeight / 2f + (canvasHeight * layerNormY)

                        nativeCanvas.translate(centerX, centerY)
                        nativeCanvas.rotate(layerRotation)
                        nativeCanvas.scale(layerScale, layerScale)

                        // Draw Frosted Glass Plaque
                        TextGlassBackdropRenderer.drawGlassBackdrop(
                            canvas = nativeCanvas,
                            layer = layer,
                            layout = layerLayout,
                            textSizePx = textSizePx,
                            density = density
                        )

                        val shadowDxPx = layer.shadowDx * density
                        val shadowDyPx = layer.shadowDy * density
                        val shadowBlurFilter = if (layer.shadowRadius > 0f) {
                            BlurMaskFilter((layer.shadowRadius * density).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                        } else null

                        layerLayout.lines.forEachIndexed { index, line ->
                            val lineY = layerLayout.startY + (index * layerLayout.lineHeight)

                            // 1. Draw 2D Directional Shadow with Blur & Custom Color
                            if (layer.shadowRadius > 0f) {
                                val sColor = layer.shadowColorLong.toInt()
                                val shadowAlpha = (((sColor ushr 24) and 0xFF) * alphaFloat).toInt()
                                shadowPaint.color = (sColor and 0x00FFFFFF) or (shadowAlpha shl 24)
                                shadowPaint.maskFilter = shadowBlurFilter
                                if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                                    CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX + shadowDxPx, lineY + shadowDyPx, shadowPaint, layer.curveAngle)
                                } else {
                                    nativeCanvas.drawText(line, layerLayout.lineX + shadowDxPx, lineY + shadowDyPx, shadowPaint)
                                }
                            }

                            // 2. Draw Stroke
                            if (layer.strokeWidth > 0f) {
                                if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                                    CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, strokePaint, layer.curveAngle)
                                } else {
                                    nativeCanvas.drawText(line, layerLayout.lineX, lineY, strokePaint)
                                }
                            }

                            // 3. Custom Color or Effect Shaders Fill
                            val customColor = layer.customTextColorLong
                            if (customColor != null && customColor != 0L) {
                                val baseColor = customColor.toInt()
                                basePaint.color = (baseColor and 0x00FFFFFF) or (alphaInt shl 24)
                                basePaint.shader = null
                                if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                                    CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, basePaint, layer.curveAngle)
                                } else {
                                    nativeCanvas.drawText(line, layerLayout.lineX, lineY, basePaint)
                                }
                            } else {
                                val effectShader = CanvasShaderFactory.createEffectShader(layer.selectedEffect, lineY, textSizePx)
                                if (effectShader != null) {
                                    basePaint.shader = effectShader
                                    if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                                        CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, basePaint, layer.curveAngle)
                                    } else {
                                        nativeCanvas.drawText(line, layerLayout.lineX, lineY, basePaint)
                                    }
                                } else {
                                    basePaint.shader = null
                                    when (layer.selectedEffect) {
                                        VisualEffect.CYBER_NEON -> {
                                            glowPaint.color = neonGlowColor
                                            glowPaint.maskFilter = neonBlurFilter
                                            if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                                                CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, glowPaint, layer.curveAngle)
                                            } else {
                                                nativeCanvas.drawText(line, layerLayout.lineX, lineY, glowPaint)
                                            }
                                            basePaint.color = (Color.WHITE and 0x00FFFFFF) or (alphaInt shl 24)
                                            if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                                                CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, basePaint, layer.curveAngle)
                                            } else {
                                                nativeCanvas.drawText(line, layerLayout.lineX, lineY, basePaint)
                                            }
                                        }
                                        VisualEffect.CINEMATIC_SHADOW -> {
                                            basePaint.color = (cinematicShadowColor and 0x00FFFFFF) or (alphaInt shl 24)
                                            if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                                                CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, basePaint, layer.curveAngle)
                                            } else {
                                                nativeCanvas.drawText(line, layerLayout.lineX, lineY, basePaint)
                                            }
                                        }
                                        else -> {
                                            basePaint.color = (Color.WHITE and 0x00FFFFFF) or (alphaInt shl 24)
                                            if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                                                CurvedTextRenderer.drawCurvedText(nativeCanvas, line, layerLayout.lineX, lineY, basePaint, layer.curveAngle)
                                            } else {
                                                nativeCanvas.drawText(line, layerLayout.lineX, lineY, basePaint)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Free Tashkeel Overlay Marks
                        if (layer.tashkeelItems.isNotEmpty()) {
                            tashkeelPaint.typeface = realTypeface
                            layer.tashkeelItems.forEach { tItem ->
                                tashkeelPaint.textSize = textSizePx * 0.75f * tItem.scale
                                val tColor = tItem.colorLong ?: layer.customTextColorLong ?: 0xFFFFFFFF
                                val baseC = tColor.toInt()
                                tashkeelPaint.color = (baseC and 0x00FFFFFF) or (alphaInt shl 24)
                                val tx = layerLayout.lineX + (tItem.normX * textSizePx * 2f)
                                val ty = layerLayout.startY + (tItem.normY * textSizePx * 2f)
                                nativeCanvas.drawText(tItem.symbol, tx, ty, tashkeelPaint)
                            }
                        }

                        // 5. Draw Selection Handles on the active layer
                        if (isCurrentActive && !state.isFramingMode) {
                            CanvasBoundingBoxRenderer.drawBoundingBox(
                                nativeCanvas = nativeCanvas,
                                textAlign = layer.textAlign,
                                layout = layerLayout,
                                textSizePx = textSizePx,
                                density = density,
                                selectBoxPaint = selectBoxPaint,
                                handlePaint = handlePaint,
                                handleFillPaint = handleFillPaint
                            )
                        }

                        nativeCanvas.restore()
                    }
                }

                // Render layers with dual-pass subject depth isolation if enabled
                val cutout = subjectCutoutBitmap
                if (state.isTextBehindSubject && cutout != null && !cutout.isRecycled) {
                    state.layers.filter { it.isBehindSubject }.forEach { renderTextLayer(it) }
                    CanvasBackgroundRenderer.drawCutoutForeground(
                        nativeCanvas = nativeCanvas,
                        state = stateForBg,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        cutoutBitmap = cutout,
                        paint = bgPaint
                    )
                    state.layers.filter { !it.isBehindSubject }.forEach { renderTextLayer(it) }
                } else {
                    state.layers.forEach { renderTextLayer(it) }
                }

                // 4. Optical Lens Flare & 35mm Film Grain Overlays
                if (state.lensFlareEnabled) {
                    CinematicOpticalEngine.drawLensFlare(
                        canvas = nativeCanvas,
                        width = canvasWidth,
                        height = canvasHeight,
                        normX = state.lensFlareX,
                        normY = state.lensFlareY,
                        intensity = state.lensFlareIntensity,
                        colorLong = state.lensFlareColorLong
                    )
                }

                if (state.filmGrainIntensity > 0f) {
                    FilmGrainEngine.drawFilmGrain(
                        canvas = nativeCanvas,
                        width = canvasWidth,
                        height = canvasHeight,
                        intensity = state.filmGrainIntensity
                    )
                }
            }
        }
    }
}

/**
 * Precision canvas hit-testing to detect which text layer the user tapped.
 * Accounts for canvas offset, scaling, rotation angle, and finger touch targets.
 */
private fun findHitLayer(
    touchX: Float,
    touchY: Float,
    canvasW: Float,
    canvasH: Float,
    state: CanvasState,
    density: Float
): TextLayer? {
    val pad = 28f * density
    // Search top-to-bottom in visual z-order
    for (layer in state.layers.asReversed()) {
        if (!layer.isVisible || layer.displayText.isEmpty()) continue

        val cx = canvasW / 2f + (layer.normOffsetX * canvasW)
        val cy = canvasH / 2f + (layer.normOffsetY * canvasH)
        val scale = layer.scaleFactor.coerceAtLeast(0.1f)

        val textSizePx = layer.fontSize * density
        val lines = layer.displayText.split("\n")
        val lineHeight = textSizePx * layer.lineSpacingMultiplier
        val totalH = kotlin.math.max(textSizePx, lines.size * lineHeight)
        val maxChars = lines.maxOfOrNull { it.length } ?: 1
        val totalW = kotlin.math.max(textSizePx * 2f, maxChars * textSizePx * 0.62f)

        val halfW = (totalW / 2f) * scale + pad
        val halfH = (totalH / 2f) * scale + pad

        val dx = touchX - cx
        val dy = touchY - cy

        val rad = Math.toRadians(-layer.rotationAngle.toDouble())
        val cosA = Math.cos(rad).toFloat()
        val sinA = Math.sin(rad).toFloat()
        val localX = dx * cosA - dy * sinA
        val localY = dx * sinA + dy * cosA

        if (abs(localX) <= halfW && abs(localY) <= halfH) {
            return layer
        }
    }
    return null
}
