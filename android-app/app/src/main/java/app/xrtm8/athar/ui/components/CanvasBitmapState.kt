package app.xrtm8.athar.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import app.xrtm8.athar.core.AtharColorMixEngine
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.model.CanvasState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CanvasBitmapHolder(
    val rawBitmap: Bitmap?,
    val processedBitmap: Bitmap?
)

@Composable
fun rememberCanvasBackgroundBitmap(
    context: Context,
    state: CanvasState
): CanvasBitmapHolder {
    var rawBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBgBitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            val p = processedBgBitmap
            val r = rawBgBitmap
            if (p != null && p != r && !p.isRecycled) p.recycle()
            if (r != null && !r.isRecycled) r.recycle()
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

    LaunchedEffect(rawBgBitmap, state.backgroundBlur, state.colorMixState) {
        val base = rawBgBitmap
        val oldProcessed = processedBgBitmap
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
                current
            }
        } else {
            null
        }
        processedBgBitmap = next
        if (oldProcessed != null && oldProcessed != base && oldProcessed != next && !oldProcessed.isRecycled) {
            oldProcessed.recycle()
        }
    }

    return CanvasBitmapHolder(rawBitmap = rawBgBitmap, processedBitmap = processedBgBitmap)
}
