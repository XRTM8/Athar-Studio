package app.xrtm8.athar.typography

import android.graphics.Paint
import app.xrtm8.athar.model.TextAlignOption
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Standardized Typography Layout & Metrics calculation.
 * Ensures 100% pixel-perfect mathematical equivalence across:
 * - Interactive Canvas Viewport (CanvasView)
 * - Vector Export Engine (AtharExportManager)
 * - Frosted Glass Plaque (TextGlassBackdropRenderer)
 * - Studio Bounding Box (CanvasBoundingBoxRenderer)
 * - Project Thumbnail Generator (AtharProjectThumbnailRenderer)
 *
 * Performance: Features a thread-safe LRU layout cache to eliminate string splitting
 * and paint text measuring overhead during 120Hz gesture transformations (pan/rotate/zoom).
 */
data class TypographyLayoutResult(
    val lines: List<String>,
    val lineHeight: Float,
    val totalHeight: Float,
    val startY: Float,
    val maxLineWidth: Float,
    val halfBlockW: Float,
    val lineX: Float
)

object AtharTextLayout {

    private data class LayoutKey(
        val text: String,
        val textSize: Float,
        val typefaceHash: Int,
        val letterSpacing: Float,
        val lineSpacingMultiplier: Float,
        val textAlign: TextAlignOption,
        val maxWidth: Float
    )

    private const val MAX_CACHE_ENTRIES = 128

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<LayoutKey, TypographyLayoutResult>(MAX_CACHE_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LayoutKey, TypographyLayoutResult>?): Boolean {
                return size > MAX_CACHE_ENTRIES
            }
        }
    )

    fun computeLayout(
        text: String,
        paint: Paint? = null,
        lineSpacingMultiplier: Float = 1.0f,
        textAlign: TextAlignOption = TextAlignOption.CENTER,
        fallbackTextSize: Float = 32f,
        maxWidth: Float = Float.MAX_VALUE,
        measureText: ((String) -> Float)? = null
    ): TypographyLayoutResult {
        val textSize = try {
            paint?.textSize ?: fallbackTextSize
        } catch (_: Throwable) {
            fallbackTextSize
        }
        val typefaceHash = try {
            paint?.typeface?.hashCode() ?: 0
        } catch (_: Throwable) {
            0
        }
        val letterSpacing = try {
            paint?.letterSpacing ?: 0f
        } catch (_: Throwable) {
            0f
        }

        val key = LayoutKey(
            text = text,
            textSize = textSize,
            typefaceHash = typefaceHash,
            letterSpacing = letterSpacing,
            lineSpacingMultiplier = lineSpacingMultiplier,
            textAlign = textAlign,
            maxWidth = maxWidth
        )

        val cached = cache[key]
        if (cached != null) {
            return cached
        }

        val measurer: (String) -> Float = measureText ?: { line ->
            try {
                val measured = paint?.measureText(line) ?: 0f
                if (measured > 0f) measured else (line.length * 15f)
            } catch (_: Throwable) {
                line.length * 15f
            }
        }

        val rawLines = text.split("\n")
        val lines = if (maxWidth > 0f && maxWidth < Float.MAX_VALUE) {
            val wrapped = ArrayList<String>()
            for (rawLine in rawLines) {
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty()) {
                    wrapped.add("")
                    continue
                }
                if (measurer(trimmed) <= maxWidth) {
                    wrapped.add(trimmed)
                } else {
                    val words = trimmed.split(Regex("\\s+"))
                    var currentLine = StringBuilder()
                    for (word in words) {
                        if (word.isEmpty()) continue
                        if (currentLine.isEmpty()) {
                            currentLine.append(word)
                        } else {
                            val candidate = currentLine.toString() + " " + word
                            if (measurer(candidate) <= maxWidth) {
                                currentLine.append(" ").append(word)
                            } else {
                                wrapped.add(currentLine.toString())
                                currentLine = StringBuilder(word)
                            }
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        wrapped.add(currentLine.toString())
                    }
                }
            }
            wrapped.ifEmpty { listOf("") }
        } else {
            rawLines
        }

        val lineHeight = textSize * lineSpacingMultiplier
        val totalHeight = (lines.size - 1) * lineHeight
        val startY = -totalHeight / 2f + (textSize * 0.35f)

        var maxLineWidth = 0f
        lines.forEach { line ->
            val w = measurer(line)
            if (w > maxLineWidth) maxLineWidth = w
        }
        val halfBlockW = maxLineWidth / 2f
        val lineX = when (textAlign) {
            TextAlignOption.RIGHT -> halfBlockW
            TextAlignOption.CENTER -> 0f
            TextAlignOption.LEFT -> -halfBlockW
        }

        val result = TypographyLayoutResult(
            lines = lines,
            lineHeight = lineHeight,
            totalHeight = totalHeight,
            startY = startY,
            maxLineWidth = maxLineWidth,
            halfBlockW = halfBlockW,
            lineX = lineX
        )

        cache[key] = result
        return result
    }

    fun clearCache() {
        cache.clear()
    }
}
