package app.xrtm8.athar.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import app.xrtm8.athar.core.AtharBackgroundPipeline
import app.xrtm8.athar.core.AtharColorMixEngine
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.model.AtharMaskCatalog
import app.xrtm8.athar.model.CanvasState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max

data class CanvasBitmapHolder(
    val rawBitmap: Bitmap?,
    val processedBitmap: Bitmap?,
    val cellBitmaps: Map<String, Bitmap>,
    val maskBitmap: Bitmap? = null,
    val blurredCellBitmaps: Map<String, Bitmap> = emptyMap(),
    val processedCollagePhotoLayer: Bitmap? = null,
    val processedCollageBitmap: Bitmap? = null
)

@Composable
fun rememberCanvasBackgroundBitmap(
    context: Context,
    state: CanvasState,
    viewportAspectRatio: Float? = null,
    isLiveFramingDrag: Boolean = false
): CanvasBitmapHolder {
    var rawBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var maskBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(state.maskState.isEnabled, state.maskState.activeMaskId, state.maskState.customMaskUri) {
        val oldMask = maskBitmap
        if (!state.maskState.isEnabled) {
            maskBitmap = null
            if (oldMask != null && !oldMask.isRecycled) oldMask.recycle()
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            val customUriStr = state.maskState.customMaskUri
            if (!customUriStr.isNullOrEmpty()) {
                runCatching { AtharImageProcessor.decodeSampledBitmap(context, android.net.Uri.parse(customUriStr), 1080, 1920) }.getOrNull()
            } else {
                val def = AtharMaskCatalog.getById(state.maskState.activeMaskId)
                if (def != null && def.proceduralType == null) {
                    AtharImageProcessor.decodeSampledAssetBitmap(context, def.assetFileName, 1080, 1920)
                } else null
            }
        }
        maskBitmap = loaded
        if (oldMask != null && oldMask != loaded && !oldMask.isRecycled) oldMask.recycle()
    }

    val cellPhotos = remember(state.collageState.cells) {
        state.collageState.cells.mapNotNull { cell -> cell.imageUri?.let { cell.id to it } }
    }
    var cellBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    LaunchedEffect(cellPhotos) {
        val oldMap = cellBitmaps
        val newMap = withContext(Dispatchers.IO) {
            cellPhotos.mapNotNull { (id, uri) ->
                AtharImageProcessor.decodeSampledBitmap(context, uri, 1080, 1920)?.let { id to it }
            }.toMap()
        }
        cellBitmaps = newMap
        oldMap.forEach { (id, bmp) -> if (newMap[id] != bmp && !bmp.isRecycled) bmp.recycle() }
    }

    LaunchedEffect(state.backgroundImageUri) {
        val oldBg = rawBgBitmap
        val loaded = state.backgroundImageUri?.let { withContext(Dispatchers.IO) { AtharImageProcessor.decodeSampledBitmap(context, it, 1080, 1920) } }
        rawBgBitmap = loaded
        if (oldBg != null && oldBg != loaded && !oldBg.isRecycled) oldBg.recycle()
    }

    // Single-photo multi-stage cache with 540px proxy -> 140ms -> 1080px settle
    var singleStageBlur by remember { mutableStateOf<Bitmap?>(null) }
    var singleStageMix by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(rawBgBitmap, state.backgroundBlur, state.collageState.isEnabled) {
        val base = rawBgBitmap
        singleStageBlur = when {
            base == null || state.collageState.isEnabled -> null
            state.backgroundBlur <= 0.5f -> base
            else -> withContext(Dispatchers.Default) { AtharImageProcessor.fastPreviewBlur(base, state.backgroundBlur.toInt().coerceIn(1, 35)) }
        }
    }

    LaunchedEffect(singleStageBlur, state.colorMixState, state.collageState.isEnabled) {
        val base = singleStageBlur
        if (base == null || base.isRecycled || state.collageState.isEnabled) { singleStageMix = null; return@LaunchedEffect }
        if (!AtharBackgroundPipeline.hasColorMix(state)) { singleStageMix = base; return@LaunchedEffect }
        runTwoTierBitmapPass(base, 540) { input ->
            AtharColorMixEngine.processBitmap(input, state.colorMixState)
        }.also { singleStageMix = it }
        val maxSide = max(base.width, base.height)
        if (maxSide > 540) {
            delay(140L)
            singleStageMix = withContext(Dispatchers.Default) { AtharColorMixEngine.processBitmap(base, state.colorMixState) }
        }
    }

    LaunchedEffect(singleStageMix, state.cinematicGlowState, state.filmGrainState, state.glitchEffectState, state.collageState.isEnabled) {
        val base = singleStageMix
        if (base == null || base.isRecycled || state.collageState.isEnabled) { processedBgBitmap = null; return@LaunchedEffect }
        if (!state.cinematicGlowState.isEnabled && !state.filmGrainState.isEnabled && !state.glitchEffectState.isEnabled) {
            processedBgBitmap = base; return@LaunchedEffect
        }
        processedBgBitmap = runTwoTierBitmapPass(base, 540) { input ->
            AtharBackgroundPipeline.renderSinglePhotoHeavyEffects(input, state, includeColorMix = false)
        }
        if (max(base.width, base.height) > 540) {
            delay(140L)
            processedBgBitmap = withContext(Dispatchers.Default) {
                AtharBackgroundPipeline.renderSinglePhotoHeavyEffects(base, state, includeColorMix = false)
            }
        }
    }

    // Collage blur-only fast hardware path
    var blurredCellBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    val collageNeedsHeavy = state.collageState.isEnabled && !state.isTransparentBg &&
        AtharBackgroundPipeline.hasHeavyCollageCompositeEffects(state)

    LaunchedEffect(cellBitmaps, state.backgroundBlur, state.collageState.isEnabled, state.isTransparentBg, collageNeedsHeavy) {
        val old = blurredCellBitmaps
        if (!state.collageState.isEnabled || state.isTransparentBg || collageNeedsHeavy || state.backgroundBlur <= 0.5f) {
            blurredCellBitmaps = emptyMap()
            old.values.forEach { if (!it.isRecycled) it.recycle() }
            return@LaunchedEffect
        }
        val radius = state.backgroundBlur.toInt().coerceIn(1, 35)
        val next = withContext(Dispatchers.Default) {
            cellBitmaps.mapValues { (_, bmp) -> if (bmp.isRecycled) bmp else AtharImageProcessor.fastPreviewBlur(bmp, radius) }
        }
        blurredCellBitmaps = next
        old.forEach { (id, bmp) -> if (next[id] != bmp && cellBitmaps[id] != bmp && !bmp.isRecycled) bmp.recycle() }
    }

    // Collage multi-stage cache (Stage 1: PhotoLayer+Blur -> Stage 2: ColorMix -> Stage 3: Film/Grain/Glow/Glitch)
    val effectiveRatio = state.aspectRatio.ratio ?: viewportAspectRatio ?: (9f / 16f)
    var collageStageBase by remember { mutableStateOf<Bitmap?>(null) }
    var collageStageMix by remember { mutableStateOf<Bitmap?>(null) }
    var processedCollagePhotoLayer by remember { mutableStateOf<Bitmap?>(null) }
    var processedCollageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(
        cellBitmaps, state.collageState.layout, state.collageState.cells,
        state.collageState.spacingDp, state.collageState.cornerRadiusDp,
        state.backgroundBlur, effectiveRatio, collageNeedsHeavy, isLiveFramingDrag
    ) {
        if (!collageNeedsHeavy) { collageStageBase = null; return@LaunchedEffect }
        if (isLiveFramingDrag) return@LaunchedEffect
        val (pw, ph) = AtharBackgroundPipeline.computePreviewSize(effectiveRatio, 540)
        collageStageBase = withContext(Dispatchers.Default) {
            AtharBackgroundPipeline.assembleCollagePhotoLayer(pw, ph, state, isPreview = true, { cell, _, _ -> cellBitmaps[cell.id] }, false)
        }
        delay(140L)
        val (fw, fh) = AtharBackgroundPipeline.computePreviewSize(effectiveRatio, 1080)
        collageStageBase = withContext(Dispatchers.Default) {
            AtharBackgroundPipeline.assembleCollagePhotoLayer(fw, fh, state, isPreview = true, { cell, _, _ -> cellBitmaps[cell.id] }, false)
        }
    }

    LaunchedEffect(collageStageBase, state.colorMixState, collageNeedsHeavy) {
        val base = collageStageBase
        if (base == null || base.isRecycled || !collageNeedsHeavy) { collageStageMix = null; return@LaunchedEffect }
        if (!AtharBackgroundPipeline.hasColorMix(state)) { collageStageMix = base; return@LaunchedEffect }
        collageStageMix = runTwoTierBitmapPass(base, 540) { input ->
            AtharColorMixEngine.processBitmap(input, state.colorMixState)
        }
        if (max(base.width, base.height) > 540) {
            delay(140L)
            collageStageMix = withContext(Dispatchers.Default) { AtharColorMixEngine.processBitmap(base, state.colorMixState) }
        }
    }

    val needsRasterFinal = state.cinematicGlowState.isEnabled || state.filmGrainState.isEnabled || state.glitchEffectState.isEnabled
    LaunchedEffect(
        collageStageMix, needsRasterFinal, collageNeedsHeavy,
        state.collageState.frameColorLong, state.collageState.cells, state.collageState.spacingDp, state.collageState.cornerRadiusDp,
        state.filmPreset, state.colorGradingEnabled,
        state.liftRed, state.liftGreen, state.liftBlue, state.gammaRed, state.gammaGreen, state.gammaBlue, state.gainRed, state.gainGreen, state.gainBlue,
        state.backgroundDim, state.backgroundBrightness, state.backgroundContrast, state.backgroundSaturation, state.backgroundGrayscale,
        state.cinematicGlowState, state.filmGrainState, state.glitchEffectState
    ) {
        val mixed = collageStageMix
        if (mixed == null || mixed.isRecycled || !collageNeedsHeavy) {
            processedCollagePhotoLayer = null; processedCollageBitmap = null; return@LaunchedEffect
        }
        if (!needsRasterFinal) {
            processedCollagePhotoLayer = mixed; processedCollageBitmap = null; return@LaunchedEffect
        }
        processedCollagePhotoLayer = null
        processedCollageBitmap = runTwoTierBitmapPass(mixed, 540) { input ->
            AtharBackgroundPipeline.renderCollageCompositeFromMixedLayer(input, state) { cell -> cellBitmaps[cell.id] == null }
        }
        if (max(mixed.width, mixed.height) > 540) {
            delay(140L)
            processedCollageBitmap = withContext(Dispatchers.Default) {
                AtharBackgroundPipeline.renderCollageCompositeFromMixedLayer(mixed, state) { cell -> cellBitmaps[cell.id] == null }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (maskBitmap?.isRecycled == false) maskBitmap?.recycle()
            if (rawBgBitmap?.isRecycled == false) rawBgBitmap?.recycle()
            cellBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
            blurredCellBitmaps.values.forEach { if (!it.isRecycled && !cellBitmaps.containsValue(it)) it.recycle() }
        }
    }

    return CanvasBitmapHolder(
        rawBitmap = rawBgBitmap,
        processedBitmap = processedBgBitmap,
        cellBitmaps = cellBitmaps,
        maskBitmap = maskBitmap,
        blurredCellBitmaps = blurredCellBitmaps,
        processedCollagePhotoLayer = processedCollagePhotoLayer,
        processedCollageBitmap = processedCollageBitmap
    )
}

private suspend fun runTwoTierBitmapPass(
    base: Bitmap,
    proxyMaxDim: Int,
    transform: (Bitmap) -> Bitmap
): Bitmap = withContext(Dispatchers.Default) {
    val maxSide = max(base.width, base.height)
    if (maxSide <= proxyMaxDim) return@withContext transform(base)
    val scale = proxyMaxDim.toFloat() / maxSide
    val scaled = Bitmap.createScaledBitmap(
        base,
        (base.width * scale).toInt().coerceAtLeast(16),
        (base.height * scale).toInt().coerceAtLeast(16),
        true
    )
    val result = transform(scaled)
    if (result !== scaled && !scaled.isRecycled) scaled.recycle()
    result
}
