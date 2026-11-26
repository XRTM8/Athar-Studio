package app.xrtm8.athar.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import app.xrtm8.athar.core.AtharBackgroundPipeline
import app.xrtm8.athar.core.CollageCoverFitMath
import app.xrtm8.athar.model.*
import app.xrtm8.athar.ui.canvas.*
import app.xrtm8.athar.ui.panels.background.CollageCanvasActionPills
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
    onDoubleTap: (() -> Unit)? = null,
    onPickCollageCellPhoto: ((String) -> Unit)? = null,
    onToggleFramingMode: ((Boolean) -> Unit)? = null,
    onResetCollageCellFraming: ((String) -> Unit)? = null
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
    var localPhotoScale by remember { mutableFloatStateOf(state.bgPhotoScale.coerceIn(1f, 4f)) }

    val activeCollageCell = remember(state.collageState.activeCellId, state.collageState.cells) {
        state.collageState.cells.find { it.id == state.collageState.activeCellId } ?: state.collageState.cells.firstOrNull()
    }
    var draggingCellId by remember { mutableStateOf<String?>(null) }
    var localCellX by remember { mutableFloatStateOf(activeCollageCell?.photoOffsetX ?: 0f) }
    var localCellY by remember { mutableFloatStateOf(activeCollageCell?.photoOffsetY ?: 0f) }
    var localCellScale by remember { mutableFloatStateOf((activeCollageCell?.photoScale ?: 1f).coerceIn(1f, 4f)) }
    var tappedCellId by remember { mutableStateOf<String?>(null) }
    var localQuickFraming by remember { mutableStateOf(false) }

    var canvasW by remember { mutableFloatStateOf(1080f) }
    var canvasH by remember { mutableFloatStateOf(1920f) }
    var isComparingBefore by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }

    val effectiveFramingMode = state.isFramingMode || localQuickFraming
    LaunchedEffect(state.isFramingMode) { if (state.isFramingMode) { localQuickFraming = false; tappedCellId = null } }
    LaunchedEffect(state.collageState.isEnabled) { if (!state.collageState.isEnabled) { tappedCellId = null; localQuickFraming = false } }
    LaunchedEffect(active.id, active.normOffsetX, active.normOffsetY, active.scaleFactor, active.rotationAngle) {
        localNormX = active.normOffsetX; localNormY = active.normOffsetY; localScale = active.scaleFactor; localRotation = active.rotationAngle
    }
    LaunchedEffect(state.bgPhotoOffsetX, state.bgPhotoOffsetY, state.bgPhotoScale) {
        localPhotoX = state.bgPhotoOffsetX; localPhotoY = state.bgPhotoOffsetY; localPhotoScale = state.bgPhotoScale.coerceIn(1f, 4f)
    }
    LaunchedEffect(activeCollageCell?.id, activeCollageCell?.photoOffsetX, activeCollageCell?.photoOffsetY, activeCollageCell?.photoScale) {
        if (!isInteracting) {
            localCellX = activeCollageCell?.photoOffsetX ?: 0f; localCellY = activeCollageCell?.photoOffsetY ?: 0f
            localCellScale = (activeCollageCell?.photoScale ?: 1f).coerceIn(1f, 4f)
        }
    }

    val bitmaps = rememberCanvasBackgroundBitmap(
        context, state, if (canvasH > 0f) canvasW / canvasH else null, isInteracting && effectiveFramingMode
    )
    val paints = rememberCanvasPaints(density)
    val isAmoledWhite = LocalAmoledWhite.current
    val ratio = state.aspectRatio.ratio

    val liveCellId = draggingCellId ?: activeCollageCell?.id
    val stateForBg = when {
        effectiveFramingMode && state.collageState.isEnabled && liveCellId != null -> {
            val updated = state.collageState.cells.map {
                if (it.id == liveCellId) it.copy(photoOffsetX = localCellX, photoOffsetY = localCellY, photoScale = localCellScale) else it
            }
            state.copy(isFramingMode = true, collageState = state.collageState.copy(cells = updated, activeCellId = liveCellId))
        }
        effectiveFramingMode -> state.copy(isFramingMode = true, bgPhotoOffsetX = localPhotoX, bgPhotoOffsetY = localPhotoY, bgPhotoScale = localPhotoScale)
        else -> state
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().clipToBounds().background(if (isAmoledWhite) WhiteSurface else Color(0xFF0C0E13))) {
        Canvas(
            modifier = Modifier
                .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.fillMaxSize())
                .clipToBounds()
                .border(1.dp, if (isAmoledWhite) WhiteBorder else Color(0xFF2B2F3A))
                .background(if (state.isTransparentBg) Color.Transparent else if (isAmoledWhite) Color.White else AmoledBlack)
                .pointerInput(state.isLocked && !effectiveFramingMode, effectiveFramingMode) {
                    detectCanvasGestures(
                        isLocked = currentState.isLocked && !effectiveFramingMode,
                        onTransformStart = { startPos ->
                            val st = currentState
                            if (effectiveFramingMode && st.collageState.isEnabled) {
                                val hitId = CanvasHitTester.findHitCollageCell(startPos.x, startPos.y, canvasW, canvasH, st)
                                val cell = st.collageState.cells.find { it.id == hitId } ?: st.collageState.cells.find { it.id == st.collageState.activeCellId } ?: st.collageState.cells.firstOrNull()
                                if (cell != null) {
                                    draggingCellId = cell.id; localCellX = cell.photoOffsetX; localCellY = cell.photoOffsetY
                                    localCellScale = cell.photoScale.coerceIn(1f, 4f)
                                    if (cell.id != st.collageState.activeCellId) onSelectCollageCell?.invoke(cell.id)
                                }
                            }
                        },
                        onTransform = { pan, zoom, rotate ->
                            isInteracting = true
                            val st = currentState
                            if (effectiveFramingMode) {
                                val cw = canvasW.coerceAtLeast(10f); val ch = canvasH.coerceAtLeast(10f)
                                if (st.collageState.isEnabled) {
                                    val cid = draggingCellId ?: st.collageState.activeCellId ?: st.collageState.cells.firstOrNull()?.id
                                    val cell = st.collageState.cells.find { it.id == cid } ?: return@detectCanvasGestures
                                    val b = CollageCoverFitMath.computeCellBounds(cell, cw, ch, st.collageState.spacingDp * AtharBackgroundPipeline.canvasDensity(cw))
                                    val bmp = bitmaps.cellBitmaps[cell.id]
                                    val step = CollageCoverFitMath.stepFramingTransform(
                                        bmp?.width?.toFloat() ?: (b[2] - b[0]), bmp?.height?.toFloat() ?: (b[3] - b[1]),
                                        b[0], b[1], b[2], b[3], localCellX, localCellY, localCellScale, pan.x, pan.y, zoom, cell.photoRotationDeg
                                    )
                                    localCellX = step.offsetX; localCellY = step.offsetY; localCellScale = step.scale
                                } else {
                                    val bmp = bitmaps.rawBitmap
                                    val step = CollageCoverFitMath.stepFramingTransform(
                                        bmp?.width?.toFloat() ?: cw, bmp?.height?.toFloat() ?: ch,
                                        0f, 0f, cw, ch, localPhotoX, localPhotoY, localPhotoScale, pan.x, pan.y, zoom
                                    )
                                    localPhotoX = step.offsetX; localPhotoY = step.offsetY; localPhotoScale = step.scale
                                }
                            } else {
                                val r = CanvasTransformHelper.applyTransform(pan, zoom, rotate, canvasW, canvasH, localNormX, localNormY, localScale, localRotation) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                localNormX = r.normX; localNormY = r.normY; localScale = r.scale; localRotation = r.rotation
                            }
                        },
                        onGestureEnd = {
                            isInteracting = false
                            val st = currentState
                            if (effectiveFramingMode && st.collageState.isEnabled) {
                                val cid = draggingCellId ?: st.collageState.activeCellId ?: st.collageState.cells.firstOrNull()?.id
                                draggingCellId = null
                                if (cid != null) onCollageCellFramingFinished?.invoke(cid, localCellX, localCellY, localCellScale)
                            } else if (effectiveFramingMode) {
                                onPhotoFramingFinished?.invoke(localPhotoX, localPhotoY, localPhotoScale)
                            } else {
                                onTransformFinished?.invoke(localNormX, localNormY, localScale, localRotation)
                            }
                        },
                        onTap = { offset ->
                            val st = currentState
                            val hit = if (!effectiveFramingMode) CanvasHitTester.findHitLayer(context, offset.x, offset.y, canvasW, canvasH, st, density) else null
                            if (hit != null) {
                                tappedCellId = null; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelectLayer?.invoke(hit.id)
                            } else if (st.collageState.isEnabled) {
                                val cell = st.collageState.cells.find { it.id == CanvasHitTester.findHitCollageCell(offset.x, offset.y, canvasW, canvasH, st) }
                                if (cell != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelectCollageCell?.invoke(cell.id)
                                    if (cell.imageUri == null) { tappedCellId = null; onPickCollageCellPhoto?.invoke(cell.id) }
                                    else if (!effectiveFramingMode) tappedCellId = cell.id
                                } else tappedCellId = null
                            } else tappedCellId = null
                        },
                        onDoubleTap = { offset ->
                            val st = currentState
                            if (effectiveFramingMode && st.collageState.isEnabled) {
                                val cellId = CanvasHitTester.findHitCollageCell(offset.x, offset.y, canvasW, canvasH, st) ?: st.collageState.activeCellId ?: st.collageState.cells.firstOrNull()?.id
                                if (cellId != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress); localCellX = 0f; localCellY = 0f; localCellScale = 1.0f
                                    onResetCollageCellFraming?.invoke(cellId) ?: onCollageCellFramingFinished?.invoke(cellId, 0f, 0f, 1.0f)
                                }
                            } else if (!effectiveFramingMode) {
                                val hit = CanvasHitTester.findHitLayer(context, offset.x, offset.y, canvasW, canvasH, st, density)
                                if (hit != null) {
                                    onSelectLayer?.invoke(hit.id)
                                    if (!hit.isLocked) onDoubleTap?.invoke()
                                } else if (!st.isLocked) {
                                    onDoubleTap?.invoke()
                                }
                            }
                        },
                        onHoldBeforeAfter = { holding ->
                            val st = currentState
                            if (holding && (bitmaps.rawBitmap != null || st.solidBgColorLong != null || st.collageState.isEnabled)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress); isComparingBefore = true
                            } else isComparingBefore = false
                        }
                    )
                }
        ) {
            canvasW = size.width; canvasH = size.height
            drawIntoCanvas { composeCanvas ->
                val native = composeCanvas.nativeCanvas
                if (isComparingBefore) {
                    CanvasBackgroundRenderer.drawBackground(
                        native, AtharBackgroundPipeline.createBeforeCompareState(stateForBg), canvasW, canvasH, density, bitmaps.rawBitmap, paints.bgPaint, bitmaps.cellBitmaps,
                        isAmoledWhite = isAmoledWhite
                    )
                } else {
                    CanvasBackgroundRenderer.drawBackground(
                        native, stateForBg, canvasW, canvasH, density, bitmaps.processedBitmap, paints.bgPaint,
                        bitmaps.cellBitmaps, bitmaps.maskBitmap, bitmaps.processedCollagePhotoLayer,
                        bitmaps.processedCollageBitmap, bitmaps.blurredCellBitmaps, isInteracting && effectiveFramingMode,
                        isAmoledWhite = isAmoledWhite
                    )
                    if (state.collageState.isEnabled && (effectiveFramingMode || tappedCellId != null)) {
                        CollageCanvasRenderer.drawActiveCellHighlight(
                            native, stateForBg.collageState, draggingCellId ?: tappedCellId ?: activeCollageCell?.id,
                            canvasW, canvasH, AtharBackgroundPipeline.canvasDensity(canvasW)
                        )
                    }
                    if (state.showGridGuides || effectiveFramingMode) CanvasGuidesRenderer.drawGridGuides(native, canvasW, canvasH, paints.gridPaint)
                    if (isInteracting && !effectiveFramingMode) CanvasGuidesRenderer.drawSnapGuides(native, canvasW, canvasH, localNormX, localNormY, paints.guidePaint)
                    state.layers.forEach { layer ->
                        val isCurr = (layer.id == active.id)
                        CanvasLayerTextRenderer.renderTextLayer(
                            native, context, layer, isCurr, effectiveFramingMode,
                            if (isCurr) localNormX else layer.normOffsetX, if (isCurr) localNormY else layer.normOffsetY,
                            if (isCurr) localScale else layer.scaleFactor, if (isCurr) localRotation else layer.rotationAngle,
                            canvasW, canvasH, density, paints
                        )
                    }
                    if (!state.layers.any { it.isVisible && it.displayText.isNotBlank() } && !effectiveFramingMode) {
                        CanvasLayerTextRenderer.drawEmptyPrompt(native, context, canvasW, canvasH, density, isAmoledWhite, paints)
                    }
                }
            }
        }
        CollageCanvasActionPills(
            isComparingBefore, effectiveFramingMode && state.collageState.isEnabled, tappedCellId, state.collageState.isEnabled,
            onEnterFraming = { tappedCellId = null; if (onToggleFramingMode != null) onToggleFramingMode(true) else localQuickFraming = true },
            onExitFraming = { localQuickFraming = false; onToggleFramingMode?.invoke(false) },
            onPickPhoto = onPickCollageCellPhoto?.let { pick -> { id: String -> tappedCellId = null; pick(id) } },
            onDismissPill = { tappedCellId = null },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp)
        )
    }
}
