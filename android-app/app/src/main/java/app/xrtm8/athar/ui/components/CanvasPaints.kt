package app.xrtm8.athar.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.xrtm8.athar.model.LayerBlendMode

class CanvasPaintPool(density: Float) {
    val basePaint = Paint().apply { isAntiAlias = true; isDither = true }
    val shadowPaint = Paint().apply { isAntiAlias = true; isDither = true }
    val strokePaint = Paint().apply { isAntiAlias = true; isDither = true; style = Paint.Style.STROKE }
    val guidePaint = Paint().apply { isAntiAlias = true; color = 0xAAFF3366.toInt(); strokeWidth = 2.0f }
    val gridPaint = Paint().apply { isAntiAlias = true; color = 0x33E02447.toInt(); strokeWidth = 1.5f }
    val selectBoxPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = 0x88E02447.toInt(); strokeWidth = 1.2f * density }
    val handlePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = 0xFFE02447.toInt(); strokeWidth = 2.4f * density; strokeCap = Paint.Cap.ROUND }
    val handleFillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL; color = 0xFFE02447.toInt() }
    val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    val colorFilterCache = mutableMapOf<Int, ColorMatrixColorFilter>()
    val shadowBlurFilterCache = mutableMapOf<Int, BlurMaskFilter>()
    val blendModeXfermode = mutableMapOf<LayerBlendMode, PorterDuffXfermode>()
}

@Composable
fun rememberCanvasPaints(density: Float): CanvasPaintPool {
    return remember(density) { CanvasPaintPool(density) }
}
