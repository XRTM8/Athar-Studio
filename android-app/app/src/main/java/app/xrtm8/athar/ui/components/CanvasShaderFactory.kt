package app.xrtm8.athar.ui.components

import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import app.xrtm8.athar.model.LuxuryGradientPreset
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Skia shader factory with pre-compiled color/stop tables and a bounded LRU cache
 * so interactive pan/zoom/drag frames do not allocate Shader objects on every draw.
 */
object CanvasShaderFactory {

    private data class ShaderKey(
        val presetOrdinal: Int,
        val angleTenths: Int,
        val isRadial: Boolean,
        val cxQ: Int,
        val cyQ: Int,
        val radiusQ: Int
    )

    private const val MAX_SHADERS = 64

    private val tableCache = ConcurrentHashMap<LuxuryGradientPreset, Pair<IntArray, FloatArray>>()

    private val shaderCache = Collections.synchronizedMap(
        object : LinkedHashMap<ShaderKey, Shader>(MAX_SHADERS, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ShaderKey, Shader>?): Boolean {
                return size > MAX_SHADERS
            }
        }
    )

    fun createMultiStopGradientShader(
        preset: LuxuryGradientPreset,
        angle: Float,
        isRadial: Boolean,
        centerX: Float,
        centerY: Float,
        radiusPx: Float
    ): Shader? {
        if (preset == LuxuryGradientPreset.NONE || preset.colors.isEmpty()) return null

        val clampedRadius = radiusPx.coerceAtLeast(10f)
        val key = ShaderKey(
            presetOrdinal = preset.ordinal,
            angleTenths = if (isRadial) 0 else (((angle % 360f) + 360f) % 360f * 10f).roundToInt(),
            isRadial = isRadial,
            cxQ = (centerX * 2f).roundToInt(),
            cyQ = (centerY * 2f).roundToInt(),
            radiusQ = (clampedRadius * 2f).roundToInt()
        )
        shaderCache[key]?.let { return it }

        val (colors, stops) = tableCache.computeIfAbsent(preset) {
            IntArray(preset.colors.size) { i -> preset.colors[i].toInt() } to preset.stops.toFloatArray()
        }

        val shader = if (isRadial) {
            RadialGradient(
                centerX, centerY,
                clampedRadius,
                colors, stops,
                Shader.TileMode.CLAMP
            )
        } else {
            val rad = Math.toRadians(angle.toDouble())
            val dx = (cos(rad) * clampedRadius).toFloat()
            val dy = (sin(rad) * clampedRadius).toFloat()
            LinearGradient(
                centerX - dx, centerY - dy,
                centerX + dx, centerY + dy,
                colors, stops,
                Shader.TileMode.CLAMP
            )
        }
        shaderCache[key] = shader
        return shader
    }
}
