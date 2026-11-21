package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.LayerBlendMode
import app.xrtm8.athar.ui.canvas.CanvasGuidesRenderer
import app.xrtm8.athar.ui.canvas.CanvasHitTester
import app.xrtm8.athar.ui.canvas.CanvasLayerTextRenderer
import app.xrtm8.athar.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Clean Canvas Viewport Composable.
 * Orchestrates:
 * - Canvas viewport geometry and touch gesture tracking
 * - Background rendering via CanvasBackgroundRenderer
 * - Guides and snapping via CanvasGuidesRenderer
 * - Text layer rendering via CanvasLayerTextRenderer
 * - Touch hit-testing via CanvasHitTester
 */
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
    var wasSnapped by remember { mutableStateOf(false) }
    val basePaint = remember { Paint().apply { isAntiAlias = true; isDither = true } }
    val shadowPaint = remember { Paint().apply { isAntiAlias = true; isDither = true } }
    val strokePaint = remember { Paint().apply { isAntiAlias = true; isDither = true; style = Paint.Style.STROKE } }
    val guidePaint = remember { Paint().apply { isAntiAlias = true; color = 0xAAFF3366.toInt(); strokeWidth = 2.0f } }
    val gridPaint = remember { Paint().apply { isAntiAlias = true; color = 0x33E02447.toInt(); strokeWidth = 1.5f } }
    val selectBoxPaint = remember { Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = 0x88E02447.toInt(); strokeWidth = 1.2f * density } }
    val handlePaint = remember { Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = 0xFFE02447.toInt(); strokeWidth = 2.4f * density; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND } }
    val handleFillPaint = remember { Paint().apply { isAntiAlias = true; style = Paint.Style.FILL; color = 0xFFE02447.toInt() } }
    val bgPaint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }

    val colorFilterCache = remember { mutableMapOf<Int, ColorMatrixColorFilter>() }
    val shadowBlurFilterCache = remember { mutableMapOf<Int, BlurMaskFilter>() }
    val blendModeXfermode = remember { mutableMapOf<LayerBlendMode, PorterDuffXfermode>() }

    val isAmoledWhite = LocalAmoledWhite.current
    val canvasBorder = if (isAmoledWhite) WhiteBorder else GlassBorder

    val ratio = state.aspectRatio.ratio
    val canvasModifier = Modifier
        .fillMaxSize()
        .then(
            if (ratio != null) {
                Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = false)
            } else {
                Modifier
            }
        )
        .clipToBounds()
        .border(0.5.dp, canvasBorder)

    val stateForBg = if (state.isFramingMode) {
        state.copy(
            bgPhotoOffsetX = localPhotoX,
            bgPhotoOffsetY = localPhotoY,
            bgPhotoScale = localPhotoScale
        )
    } else {
        state
    }

    val canvasBackground = if (state.isTransparentBg) {
        androidx.compose.ui.graphics.Color.Transparent
    } else if (isAmoledWhite) {
        androidx.compose.ui.graphics.Color.White
    } else {
        AmoledBlack
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(if (isAmoledWhite) WhiteSurface else AmoledBlack)
    ) {
        Canvas(
            modifier = canvasModifier
                .background(canvasBackground)
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

                                if (snapped && !wasSnapped) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                wasSnapped = snapped
                            }
                        },
                        onGestureEnd = {
                            isInteracting = false
                            wasSnapped = false
                            if (state.isFramingMode) {
                                onPhotoFramingFinished?.invoke(localPhotoX, localPhotoY, localPhotoScale)
                            } else {
                                onTransformFinished?.invoke(localNormX, localNormY, localScale, localRotation)
                            }
                        },
                        onTap = { tapOffset ->
                            if (!state.isFramingMode) {
                                val hit = CanvasHitTester.findHitLayer(tapOffset.x, tapOffset.y, canvasW, canvasH, state, density)
                                if (hit != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectLayer?.invoke(hit.id)
                                }
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            if (!state.isFramingMode) {
                                val hit = CanvasHitTester.findHitLayer(tapOffset.x, tapOffset.y, canvasW, canvasH, state, density)
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

                // 2. Alignment & Snapping Guides
                if (state.showGridGuides || state.isFramingMode) {
                    CanvasGuidesRenderer.drawGridGuides(nativeCanvas, canvasWidth, canvasHeight, gridPaint)
                }

                if (isInteracting && !state.isFramingMode) {
                    CanvasGuidesRenderer.drawSnapGuides(nativeCanvas, canvasWidth, canvasHeight, localNormX, localNormY, guidePaint)
                }

                // 3. Render all Text Layers
                state.layers.forEach { layer ->
                    val isCurrentActive = (layer.id == active.id)
                    val layerNormX = if (isCurrentActive) localNormX else layer.normOffsetX
                    val layerNormY = if (isCurrentActive) localNormY else layer.normOffsetY
                    val layerScale = if (isCurrentActive) localScale else layer.scaleFactor
                    val layerRotation = if (isCurrentActive) localRotation else layer.rotationAngle

                    CanvasLayerTextRenderer.renderTextLayer(
                        nativeCanvas = nativeCanvas,
                        context = context,
                        layer = layer,
                        isCurrentActive = isCurrentActive,
                        isFramingMode = state.isFramingMode,
                        layerNormX = layerNormX,
                        layerNormY = layerNormY,
                        layerScale = layerScale,
                        layerRotation = layerRotation,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        density = density,
                        isAmoledWhite = isAmoledWhite,
                        basePaint = basePaint,
                        shadowPaint = shadowPaint,
                        strokePaint = strokePaint,
                        selectBoxPaint = selectBoxPaint,
                        handlePaint = handlePaint,
                        handleFillPaint = handleFillPaint,
                        colorFilterCache = colorFilterCache,
                        shadowBlurFilterCache = shadowBlurFilterCache,
                        blendModeXfermode = blendModeXfermode
                    )
                }

                // 4. Draw Empty Prompt if no visible text
                val hasAnyText = state.layers.any { it.isVisible && it.displayText.isNotBlank() }
                if (!hasAnyText && !state.isFramingMode) {
                    CanvasLayerTextRenderer.drawEmptyPrompt(
                        nativeCanvas = nativeCanvas,
                        context = context,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        density = density,
                        isAmoledWhite = isAmoledWhite
                    )
                }
            }
        }
    }
}
