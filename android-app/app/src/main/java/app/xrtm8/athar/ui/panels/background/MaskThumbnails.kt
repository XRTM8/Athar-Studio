package app.xrtm8.athar.ui.panels.background

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.model.AtharMaskDefinition
import app.xrtm8.athar.ui.theme.GlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object MaskThumbnailCache {
    private val cache = ConcurrentHashMap<String, Bitmap>()

    fun get(path: String): Bitmap? = cache[path]
    fun put(path: String, bmp: Bitmap) { cache[path] = bmp }
}

@Composable
fun MaskThumbnailView(
    maskDef: AtharMaskDefinition,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(maskDef.thumbFileName) {
        mutableStateOf(MaskThumbnailCache.get(maskDef.thumbFileName))
    }

    LaunchedEffect(maskDef.thumbFileName) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                AtharImageProcessor.decodeSampledAssetBitmap(context, maskDef.thumbFileName, 120, 120)
            }
            if (loaded != null) {
                MaskThumbnailCache.put(maskDef.thumbFileName, loaded)
                bitmap = loaded
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GlassSurface)
    ) {
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = maskDef.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
