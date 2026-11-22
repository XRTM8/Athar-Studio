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
    val cellBitmaps: Map<String, Bitmap>
)

@Composable
fun rememberCanvasBackgroundBitmap(
    context: Context,
    state: CanvasState
): CanvasBitmapHolder {
    var rawBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBgBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cellPhotos = state.collageState.cells.mapNotNull { cell -> cell.imageUri?.let { cell.id to it } }
    val cellBitmaps by produceState(emptyMap<String, Bitmap>(), cellPhotos) {
        value = withContext(Dispatchers.IO) {
            cellPhotos.mapNotNull { (id, uri) ->
                AtharImageProcessor.decodeSampledBitmap(context, uri, 1080, 1920)?.let { id to it }
            }.toMap()
        }
    }

    LaunchedEffect(state.backgroundImageUri) {
        val uri = state.backgroundImageUri
        rawBgBitmap = uri?.let { withContext(Dispatchers.IO) { AtharImageProcessor.decodeSampledBitmap(context, it, 1080, 1920) } }
    }

    LaunchedEffect(rawBgBitmap, state.backgroundBlur, state.colorMixState, state.cinematicGlowState, state.filmGrainState) {
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

                current
            }
        } else {
            null
        }
        processedBgBitmap = next
    }

    return CanvasBitmapHolder(rawBitmap = rawBgBitmap, processedBitmap = processedBgBitmap, cellBitmaps = cellBitmaps)
}
