package app.xrtm8.athar.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import app.xrtm8.athar.core.AtharColorMixEngine
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.model.CanvasState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CanvasBitmapHolder(
    val rawBitmap: Bitmap?,
    val processedBitmap: Bitmap?,
    val cellBitmaps: Map<String, Bitmap>,
    val maskBitmap: Bitmap? = null
)

@Composable
fun rememberCanvasBackgroundBitmap(
    context: Context,
    state: CanvasState
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
        val customUriStr = state.maskState.customMaskUri
        val maskId = state.maskState.activeMaskId
        val loaded = withContext(Dispatchers.IO) {
            if (!customUriStr.isNullOrEmpty()) {
                try {
                    AtharImageProcessor.decodeSampledBitmap(context, android.net.Uri.parse(customUriStr), 1080, 1920)
                } catch (e: Exception) {
                    null
                }
            } else if (!maskId.isNullOrEmpty()) {
                val def = app.xrtm8.athar.model.AtharMaskCatalog.getById(maskId)
                if (def != null) {
                    AtharImageProcessor.decodeSampledAssetBitmap(context, def.assetFileName, 1080, 1920)
                } else null
            } else {
                null
            }
        }
        maskBitmap = loaded
        if (oldMask != null && oldMask != loaded && !oldMask.isRecycled) {
            oldMask.recycle()
        }
    }

    val cellPhotos = state.collageState.cells.mapNotNull { cell -> cell.imageUri?.let { cell.id to it } }
    var cellBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }

    LaunchedEffect(cellPhotos) {
        val oldMap = cellBitmaps
        val newMap = withContext(Dispatchers.IO) {
            cellPhotos.mapNotNull { (id, uri) ->
                val bmp = AtharImageProcessor.decodeSampledBitmap(context, uri, 1080, 1920)
                bmp?.let { id to it }
            }.toMap()
        }
        cellBitmaps = newMap
        oldMap.forEach { (id, bmp) ->
            if (newMap[id] != bmp && !bmp.isRecycled) {
                bmp.recycle()
            }
        }
    }

    LaunchedEffect(state.backgroundImageUri) {
        val oldBg = rawBgBitmap
        val uri = state.backgroundImageUri
        val loaded = uri?.let { withContext(Dispatchers.IO) { AtharImageProcessor.decodeSampledBitmap(context, it, 1080, 1920) } }
        rawBgBitmap = loaded
        if (oldBg != null && oldBg != loaded && !oldBg.isRecycled) {
            oldBg.recycle()
        }
    }

    LaunchedEffect(rawBgBitmap, state.backgroundBlur, state.colorMixState, state.cinematicGlowState, state.filmGrainState, state.glitchEffectState) {
        val base = rawBgBitmap
        val next = if (base != null) {
            withContext(Dispatchers.Default) {
                var current = if (state.backgroundBlur > 0.5f) {
                    AtharImageProcessor.fastPreviewBlur(base, state.backgroundBlur.toInt().coerceIn(1, 35))
                } else {
                    base
                }

                if (state.colorMixState.isEnabled && !state.colorMixState.isDefault) {
                    val mixed = AtharColorMixEngine.processBitmap(current, state.colorMixState)
                    if (current != base && current != mixed && !current.isRecycled) {
                        current.recycle()
                    }
                    current = mixed
                }

                if (state.cinematicGlowState.isEnabled) {
                    val glowed = app.xrtm8.athar.core.AtharOpticalGlowEngine.processBitmap(current, state.cinematicGlowState)
                    if (current != base && current != glowed && !current.isRecycled) {
                        current.recycle()
                    }
                    current = glowed
                }

                if (state.filmGrainState.isEnabled) {
                    val grained = app.xrtm8.athar.core.AtharFilmGrainEngine.processBitmap(current, state.filmGrainState)
                    if (current != base && current != grained && !current.isRecycled) {
                        current.recycle()
                    }
                    current = grained
                }

                if (state.glitchEffectState.isEnabled) {
                    val glitched = app.xrtm8.athar.core.AtharGlitchEngine.processBitmap(current, state.glitchEffectState)
                    if (current != base && current != glitched && !current.isRecycled) {
                        current.recycle()
                    }
                    current = glitched
                }

                current
            }
        } else {
            null
        }
        val oldProcessed = processedBgBitmap
        processedBgBitmap = next
        if (oldProcessed != null && oldProcessed != next && oldProcessed != base && !oldProcessed.isRecycled) {
            oldProcessed.recycle()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (maskBitmap?.isRecycled == false) maskBitmap?.recycle()
            if (rawBgBitmap?.isRecycled == false) rawBgBitmap?.recycle()
            if (processedBgBitmap?.isRecycled == false && processedBgBitmap != rawBgBitmap) {
                processedBgBitmap?.recycle()
            }
            cellBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    return CanvasBitmapHolder(
        rawBitmap = rawBgBitmap,
        processedBitmap = processedBgBitmap,
        cellBitmaps = cellBitmaps,
        maskBitmap = maskBitmap
    )
}
