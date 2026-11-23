package app.xrtm8.athar.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import app.xrtm8.athar.model.*
import app.xrtm8.athar.ui.canvas.*
import app.xrtm8.athar.ui.theme.*

@Composable
fun CanvasView(
    state: CanvasState,
    modifier: Modifier = Modifier,
    onTransformFinished: ((normX: Float, normY: Float, scale: Float, rotation: Float) -> Unit)? = null,
    onPhotoFramingFinished: ((normX: Float, normY: Float, scale: Float) -> Unit)? = null,
    onCollageCellFramingFinished: ((cellId: String, normX: Float, normY: Float, scale: Float) -> Unit)? = null,
    onSelectLayer: ((String) -> Unit)? = null,
    onSelectCollageCell: ((String) -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current
    val currentState by rememberUpdatedState(state)
    val active = state.activeLayer

    var localNormX by remember(active.id) { mutableFloatStateOf(active.normOffsetX) }
    var localNormY by remember(active.id) { mutableFloatStateOf(active.normOffsetY) }
    var localScale by remember(active.id) { mutableFloatStateOf(active.scaleFactor) }
    var localRotation by remember(active.id) { mutableFloatStateOf(active.rotationAngle) }

    var localPhotoX by remember { mutableFloatStateOf(state.bgPhotoOffsetX) }
    var localPhotoY by remember { mutableFloatStateOf(state.bgPhotoOffsetY) }
    var localPhotoScale by remember { mutableFloatStateOf(state.bgPhotoScale) }

    val activeCollageCell = remember(state.collageState.activeCellId, state.collageState.cells) {
        state.collageState.cells.find { it.id == state.collageState.activeCellId } ?: state.collageState.cells.firstOrNull()
    }
    var localCellX by remember(activeCollageCell?.id) { mutableFloatStateOf(activeCollageCell?.photoOffsetX ?: 0f) }
    var localCellY by remember(activeCollageCell?.id) { mutableFloatStateOf(activeCollageCell?.photoOffsetY ?: 0f) }
    var localCellScale by remember(activeCollageCell?.id) { mutableFloatStateOf(activeCollageCell?.photoScale ?: 1f) }

    var canvasW by remember { mutableFloatStateOf(1080f) }
    var canvasH by remember { mutableFloatStateOf(1920f) }
    var isComparingBefore by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(active.id, active.normOffsetX, active.normOffsetY, active.scaleFactor, active.rotationAngle) {
        localNormX = active.normOffsetX; localNormY = active.normOffsetY
        localScale = active.scaleFactor; localRotation = active.rotationAngle
    }
    LaunchedEffect(state.bgPhotoOffsetX, state.bgPhotoOffsetY, state.bgPhotoScale) {
        localPhotoX = state.bgPhotoOffsetX; localPhotoY = state.bgPhotoOffsetY; localPhotoScale = state.bgPhotoScale
    }
    LaunchedEffect(activeCollageCell?.id, activeCollageCell?.photoOffsetX, activeCollageCell?.photoOffsetY, activeCollageCell?.photoScale) {
        localCellX = activeCollageCell?.photoOffsetX ?: 0f; localCellY = activeCollageCell?.photoOffsetY ?: 0f; localCellScale = activeCollageCell?.photoScale ?: 1f
    }

    val bitmaps = rememberCanvasBackgroundBitmap(context, state)
    val paints = rememberCanvasPaints(density)

    val isAmoledWhite = LocalAmoledWhite.current
    val canvasBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val ratio = state.aspectRatio.ratio

    val canvasModifier = Modifier
        .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.fillMaxSize())
        .clipToBounds()
        .border(0.5.dp, canvasBorder)

    val stateForBg = if (state.isFramingMode) {
        if (state.collageState.isEnabled && activeCollageCell != null) {
            val updated = state.collageState.cells.map {
                if (it.id == activeCollageCell.id) it.copy(photoOffsetX = localCellX, photoOffsetY = localCellY, photoScale = localCellScale) else it
            }
            state.copy(collageState = state.collageState.copy(cells = updated))
        } else {
            state.copy(bgPhotoOffsetX = localPhotoX, bgPhotoOffsetY = localPhotoY, bgPhotoScale = localPhotoScale)
        }
    } else {
        state
    }

    val canvasBg = if (state.isTransparentBg) Color.Transparent else if (isAmoledWhite) Color.White else AmoledBlack

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().clipToBounds().background(if (isAmoledWhite) WhiteSurface else AmoledBlack)
    ) {
        Canvas(
            modifier = canvasModifier
                .background(canvasBg)
                .pointerInput(state.isLocked, state.isFramingMode, active.id, activeCollageCell?.id) {
                    detectCanvasGestures(
                        isLocked = state.isLocked,
                        onTransform = { pan, zoom, rotate ->
                            isInteracting = true
                            if (state.isFramingMode) {
                                val cw = canvasW.coerceAtLeast(10f)
                                val ch = canvasH.coerceAtLeast(10f)
                                if (state.collageState.isEnabled && activeCollageCell != null) {
                                    val cellW = ((activeCollageCell.rightNorm - activeCollageCell.leftNorm) * cw).coerceAtLeast(10f)
                                    val cellH = ((activeCollageCell.bottomNorm - activeCollageCell.topNorm) * ch).coerceAtLeast(10f)
                                    localCellX = (localCellX - pan.x / (cellW * 0.5f)).coerceIn(-1f, 1f)
                                    localCellY = (localCellY - pan.y / (cellH * 0.5f)).coerceIn(-1f, 1f)
                                    localCellScale = (localCellScale * zoom).coerceIn(0.5f, 4.0f)
                                } else {
                                    localPhotoX = (localPhotoX - pan.x / (cw * 0.5f)).coerceIn(-1f, 1f)
                                    localPhotoY = (localPhotoY - pan.y / (ch * 0.5f)).coerceIn(-1f, 1f)
                                    localPhotoScale = (localPhotoScale * zoom).coerceIn(0.5f, 4.0f)
                                }
                            } else {
                                val r = CanvasTransformHelper.applyTransform(
                                    pan, zoom, rotate, canvasW, canvasH,
                                    localNormX, localNormY, localScale, localRotation
                                ) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                                localNormX = r.normX
                                localNormY = r.normY
                                localScale = r.scale
                                localRotation = r.rotation
                            }
                        },
                        onGestureEnd = {
                            isInteracting = false
                            if (state.isFramingMode) {
                                if (state.collageState.isEnabled && activeCollageCell != null) {
                                    onCollageCellFramingFinished?.invoke(activeCollageCell.id, localCellX, localCellY, localCellScale)
                                } else {
                                    onPhotoFramingFinished?.invoke(localPhotoX, localPhotoY, localPhotoScale)
                                }
                            } else {
                                onTransformFinished?.invoke(localNormX, localNormY, localScale, localRotation)
                            }
                        },
                        onTap = { offset ->
                            val hit = if (!state.isFramingMode) CanvasHitTester.findHitLayer(context, offset.x, offset.y, canvasW, canvasH, currentState, density) else null
                            if (hit != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectLayer?.invoke(hit.id)
                            } else if (state.collageState.isEnabled) {
                                val cellId = CanvasHitTester.findHitCollageCell(offset.x, offset.y, canvasW, canvasH, currentState)
                                if (cellId != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectCollageCell?.invoke(cellId)
                                }
                            }
                        },
                        onDoubleTap = { offset ->
                            if (!state.isFramingMode) {
                                val hit = CanvasHitTester.findHitLayer(context, offset.x, offset.y, canvasW, canvasH, currentState, density)
                                if (hit != null) onSelectLayer?.invoke(hit.id)
                            }
                            onDoubleTap?.invoke()
                        },
                        onHoldBeforeAfter = { holding ->
                            if (holding && (bitmaps.rawBitmap != null || state.solidBgColorLong != null)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isComparingBefore = true
                            } else {
                                isComparingBefore = false
                            }
                        }
                    )
                }
        ) {
            canvasW = size.width
            canvasH = size.height

            drawIntoCanvas { composeCanvas ->
                val native = composeCanvas.nativeCanvas

                if (isComparingBefore) {
                    val rawState = stateForBg.copy(
                        backgroundBlur = 0f, backgroundDim = 0f, backgroundBrightness = 0f,
                        backgroundContrast = 1.0f, backgroundSaturation = 1.0f, backgroundVignette = 0f,
                        backgroundGrayscale = false, filmPreset = FilmPreset.NONE, colorGradingEnabled = false,
                        gradientScrim = CanvasGradientScrim(isEnabled = false)
                    )
                    CanvasBackgroundRenderer.drawBackground(native, rawState, canvasW, canvasH, density, bitmaps.rawBitmap, paints.bgPaint)
                } else {
                    CanvasBackgroundRenderer.drawBackground(
                        native, stateForBg, canvasW, canvasH, density,
                        bitmaps.processedBitmap, paints.bgPaint, bitmaps.cellBitmaps,
                        bitmaps.maskBitmap
                    )

                    if (state.showGridGuides || state.isFramingMode) {
                        CanvasGuidesRenderer.drawGridGuides(native, canvasW, canvasH, paints.gridPaint)
                    }
                    if (isInteracting && !state.isFramingMode) {
                        CanvasGuidesRenderer.drawSnapGuides(native, canvasW, canvasH, localNormX, localNormY, paints.guidePaint)
                    }

                    state.layers.forEach { layer ->
                        val isCurr = (layer.id == active.id)
                        CanvasLayerTextRenderer.renderTextLayer(
                            nativeCanvas = native, context = context, layer = layer, isCurrentActive = isCurr,
                            isFramingMode = state.isFramingMode,
                            layerNormX = if (isCurr) localNormX else layer.normOffsetX,
                            layerNormY = if (isCurr) localNormY else layer.normOffsetY,
                            layerScale = if (isCurr) localScale else layer.scaleFactor,
                            layerRotation = if (isCurr) localRotation else layer.rotationAngle,
                            canvasWidth = canvasW, canvasHeight = canvasH, density = density,
                            paints = paints
                        )
                    }

                    if (!state.layers.any { it.isVisible && it.displayText.isNotBlank() } && !state.isFramingMode) {
                        CanvasLayerTextRenderer.drawEmptyPrompt(native, context, canvasW, canvasH, density, isAmoledWhite)
                    }
                }
            }
        }

        // Before Comparison Indicator Badge
        AnimatedVisibility(
            visible = isComparingBefore,
            enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = AmoledBlack.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, BurgundyAccent.copy(alpha = 0.8f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BurgundyAccent))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "الصورة الأصلية (قبل)", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
