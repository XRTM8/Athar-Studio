package app.xrtm8.athar.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.xrtm8.athar.model.LayerBlendMode
import java.util.Locale

class CanvasPaintPool(density: Float) {
    val compositePaint = Paint()
    val basePaint = Paint().apply { isAntiAlias = true; isDither = true }
    val shadowPaint = Paint().apply { isAntiAlias = true; isDither = true }
    val strokePaint = Paint().apply { isAntiAlias = true; isDither = true; style = Paint.Style.STROKE }
    val guidePaint = Paint().apply { isAntiAlias = true; color = 0xAAFF3366.toInt(); strokeWidth = 2.0f }
    val gridPaint = Paint().apply { isAntiAlias = true; color = 0x33E02447.toInt(); strokeWidth = 1.5f }
    val selectBoxPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = 0x88E02447.toInt(); strokeWidth = 1.2f * density }
    val handlePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; color = 0xFFE02447.toInt(); strokeWidth = 2.4f * density; strokeCap = Paint.Cap.ROUND }
    val handleFillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL; color = 0xFFE02447.toInt() }
    val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    val shadowBlurFilterCache = mutableMapOf<Int, BlurMaskFilter>()
    val blendModeXfermode = mutableMapOf<LayerBlendMode, PorterDuffXfermode>()
    private val fontFeatureCache = HashMap<Int, String?>()

    val emptyPromptDashPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = 0x55E02447.toInt()
        strokeWidth = 1.4f * density
        pathEffect = DashPathEffect(floatArrayOf(8f * density, 6f * density), 0f)
    }
    val emptyPromptBgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val emptyPromptTextPaint = Paint().apply {
        isAntiAlias = true
        color = 0xCCE02447.toInt()
        textSize = 13.5f * density
        textAlign = Paint.Align.CENTER
    }
    val emptyPromptSubPaint = Paint().apply {
        isAntiAlias = true
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
    }
    val emptyPromptRect = RectF()

    fun getFontFeatureString(stylisticSet: Int, enableSwash: Boolean, enableDlig: Boolean): String? {
        val ss = if (stylisticSet in 1..20) stylisticSet else 0
        val key = (ss shl 2) or (if (enableSwash) 2 else 0) or (if (enableDlig) 1 else 0)
        if (key == 0) return null
        if (fontFeatureCache.containsKey(key)) return fontFeatureCache[key]
        val computed = buildString {
            if (ss > 0) append(String.format(Locale.US, "\"ss%02d\" 1, ", ss))
            if (enableSwash) append("\"swsh\" 1, ")
            if (enableDlig) append("\"dlig\" 1, ")
        }.trimEnd(',', ' ').ifEmpty { null }
        fontFeatureCache[key] = computed
        return computed
    }

    fun getEmptyPromptTextPaint(typeface: Typeface): Paint {
        if (emptyPromptTextPaint.typeface !== typeface) {
            emptyPromptTextPaint.typeface = typeface
        }
        return emptyPromptTextPaint
    }
}

@Composable
fun rememberCanvasPaints(density: Float): CanvasPaintPool {
    return remember(density) { CanvasPaintPool(density) }
}
