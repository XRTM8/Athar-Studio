package app.xrtm8.athar.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.LuxuryGradientPreset
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.typography.AtharTextLayout
import app.xrtm8.athar.typography.CurvedTextRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Open Vector SVG exporter for Athar Studio.
 * Emits standards-compliant SVG with real CSS font family names, numeric weights,
 * multi-stop gradients, OpenType features, and converts custom/proprietary fonts
 * and curved Arabic paths to vector `<path>` outlines via `Paint.getTextPath()`.
 */
object AtharSvgExporter {

    fun generateSvgString(state: CanvasState): String = generateSvgString(state, context = null)

    fun generateSvgString(state: CanvasState, context: Context?): String {
        val ratio = state.aspectRatio.ratio ?: (9f / 16f)
        val width = 1080
        val height = (width / ratio).toInt()
        val density = width / 380f

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height" width="$width" height="$height">""")

        // Defs: Filters and Gradients
        sb.appendLine("  <defs>")
        for (layer in state.layers) {
            if (!layer.isVisible) continue
            val safeId = sanitizeId(layer.id)
            if (layer.shadowRadius > 0f) {
                val shadowColor = colorToHex(layer.shadowColorLong)
                val shadowAlpha = ((layer.shadowColorLong ushr 24) and 0xFF) / 255f
                sb.appendLine("""    <filter id="shadow_$safeId" x="-30%" y="-30%" width="160%" height="160%">""")
                sb.appendLine("""      <feDropShadow dx="${layer.shadowDx * density}" dy="${layer.shadowDy * density}" stdDeviation="${(layer.shadowRadius * density) / 2f}" flood-color="$shadowColor" flood-opacity="$shadowAlpha" />""")
                sb.appendLine("""    </filter>""")
            }
            val activeGrad = resolveActiveGradient(layer)
            if (activeGrad != LuxuryGradientPreset.NONE && !layer.isHollowText) {
                appendGradientDef(sb, safeId, activeGrad, layer.gradientAngle, layer.isRadialGradient)
            }
        }
        sb.appendLine("  </defs>")

        // Canvas Background
        if (!state.isTransparentBg) {
            val bgHex = if (state.solidBgColorLong != null) colorToHex(state.solidBgColorLong) else "#000000"
            sb.appendLine("""  <rect width="100%" height="100%" fill="$bgHex" />""")
        }

        // Text Layers
        for (layer in state.layers) {
            if (!layer.isVisible) continue
            val textToRender = if (layer.displayText.isNotEmpty()) layer.displayText else layer.rawText
            if (textToRender.isEmpty()) continue

            val safeId = sanitizeId(layer.id)
            val cx = (width * 0.5f) + (layer.normOffsetX * width)
            val cy = (height * 0.5f) + (layer.normOffsetY * height)
            val fontSize = layer.fontSize * density
            val effectiveWeight = if (layer.isBold && layer.fontWeight < 700) 700 else layer.fontWeight
            val activeGrad = resolveActiveGradient(layer)
            val fillValue = when {
                layer.isHollowText -> "none"
                activeGrad != LuxuryGradientPreset.NONE -> "url(#grad_$safeId)"
                layer.customTextColorLong != null -> colorToHex(layer.customTextColorLong)
                else -> "#FFFFFF"
            }
            val effectiveStroke = if (layer.isHollowText && layer.strokeWidth <= 0f) 2.0f else layer.strokeWidth
            val strokeAttr = if (effectiveStroke > 0f) {
                val strokeHex = colorToHex(layer.strokeColorLong)
                """stroke="$strokeHex" stroke-width="${effectiveStroke * density}" stroke-linejoin="round" stroke-linecap="round" paint-order="stroke fill""""
            } else ""
            val filterAttr = if (layer.shadowRadius > 0f) """filter="url(#shadow_$safeId)"""" else ""

            sb.appendLine("""  <g transform="translate($cx, $cy) rotate(${layer.rotationAngle}) scale(${layer.scaleFactor})" $filterAttr>""")

            val shouldOutline = context != null && (!layer.customFontPath.isNullOrBlank() || layer.isCurved)
            var outlined = false
            if (shouldOutline && context != null) {
                outlined = tryAppendVectorOutlines(
                    sb = sb,
                    context = context,
                    layer = layer,
                    textToRender = textToRender,
                    canvasWidth = width.toFloat(),
                    textSizePx = fontSize,
                    effectiveWeight = effectiveWeight,
                    fillValue = fillValue,
                    strokeAttr = strokeAttr
                )
            }

            if (!outlined) {
                appendTextElement(
                    sb = sb,
                    layer = layer,
                    textToRender = textToRender,
                    fontSize = fontSize,
                    effectiveWeight = effectiveWeight,
                    fillValue = fillValue,
                    strokeAttr = strokeAttr,
                    density = density
                )
            }

            sb.appendLine("""  </g>""")
        }

        sb.appendLine("</svg>")
        return sb.toString()
    }

    private fun tryAppendVectorOutlines(
        sb: StringBuilder,
        context: Context,
        layer: TextLayer,
        textToRender: String,
        canvasWidth: Float,
        textSizePx: Float,
        effectiveWeight: Int,
        fillValue: String,
        strokeAttr: String
    ): Boolean {
        val tf = AtharFontManager.getTypeface(context, layer.selectedFont, layer.customFontPath, effectiveWeight)
        val useFakeBold = layer.isBold && !AtharFontManager.hasRealWeight(context, layer.selectedFont, effectiveWeight, layer.customFontPath)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = textSizePx
            letterSpacing = layer.letterSpacing
            isUnderlineText = layer.isUnderline
            isFakeBoldText = useFakeBold
            textSkewX = if (layer.isItalic) -0.25f else 0f
            textAlign = when (layer.textAlign) {
                TextAlignOption.RIGHT -> Paint.Align.RIGHT
                TextAlignOption.CENTER -> Paint.Align.CENTER
                TextAlignOption.LEFT -> Paint.Align.LEFT
            }
            fontFeatureSettings = buildFontFeatureString(layer)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && layer.wordSpacing != 0f) {
                wordSpacing = (layer.wordSpacing / 380f) * canvasWidth
            }
        }
        val layout = AtharTextLayout.computeLayout(
            text = textToRender,
            paint = paint,
            lineSpacingMultiplier = layer.lineSpacingMultiplier,
            textAlign = layer.textAlign,
            maxWidth = canvasWidth * layer.boxWidthRatio.coerceIn(0.2f, 1.0f)
        )

        val combinedPath = Path()
        var hasData = false
        layout.lines.forEachIndexed { index, line ->
            if (line.isEmpty()) return@forEachIndexed
            val lineY = layout.startY + (index * layout.lineHeight)
            val linePath = if (layer.isCurved && abs(layer.curveAngle) >= 1f) {
                CurvedTextRenderer.buildCurvedOutlinePath(line, layout.lineX, lineY, paint, layer.curveAngle)
            } else {
                Path().also { p -> paint.getTextPath(line, 0, line.length, layout.lineX, lineY, p) }
            }
            if (linePath != null && !linePath.isEmpty) {
                combinedPath.fillType = linePath.fillType
                combinedPath.addPath(linePath)
                hasData = true
            }
        }
        if (!hasData) return false

        val d = pathToSvgData(combinedPath)
        if (d.isEmpty()) return false
        val fillRule = if (combinedPath.fillType == Path.FillType.EVEN_ODD) "evenodd" else "nonzero"
        sb.appendLine("""    <path d="$d" fill="$fillValue" fill-rule="$fillRule" opacity="${layer.textOpacity}" $strokeAttr aria-label="${escapeXml(textToRender)}" />""")
        return true
    }

    private fun appendTextElement(
        sb: StringBuilder,
        layer: TextLayer,
        textToRender: String,
        fontSize: Float,
        effectiveWeight: Int,
        fillValue: String,
        strokeAttr: String,
        density: Float
    ) {
        val textAnchor = when (layer.textAlign) {
            TextAlignOption.LEFT -> "start"
            TextAlignOption.RIGHT -> "end"
            TextAlignOption.CENTER -> "middle"
        }
        val cssFamily = resolveSvgFontFamily(layer)
        val features = buildFontFeatureString(layer)
        val extraAttrs = buildString {
            if (!features.isNullOrBlank()) append(""" font-feature-settings="${escapeXml(features)}"""")
            if (layer.wordSpacing != 0f) append(""" word-spacing="${layer.wordSpacing * density}px"""")
            if (layer.isUnderline) append(""" text-decoration="underline"""")
        }

        val lines = textToRender.split("\n")
        if (lines.size <= 1) {
            val escaped = escapeXml(textToRender)
            sb.appendLine("""    <text x="0" y="0" font-family="${escapeXml(cssFamily)}, sans-serif" font-size="$fontSize" font-weight="$effectiveWeight" font-style="${if (layer.isItalic) "italic" else "normal"}" fill="$fillValue" text-anchor="$textAnchor" direction="rtl" letter-spacing="${layer.letterSpacing}px" opacity="${layer.textOpacity}"$extraAttrs $strokeAttr>$escaped</text>""")
        } else {
            val lineHeight = fontSize * layer.lineSpacingMultiplier
            val startY = -((lines.size - 1) * lineHeight) / 2f + (fontSize * 0.35f)
            sb.appendLine("""    <text x="0" y="$startY" font-family="${escapeXml(cssFamily)}, sans-serif" font-size="$fontSize" font-weight="$effectiveWeight" font-style="${if (layer.isItalic) "italic" else "normal"}" fill="$fillValue" text-anchor="$textAnchor" direction="rtl" letter-spacing="${layer.letterSpacing}px" opacity="${layer.textOpacity}"$extraAttrs $strokeAttr>""")
            lines.forEachIndexed { idx, line ->
                val dy = if (idx == 0) 0f else lineHeight
                sb.appendLine("""      <tspan x="0" dy="$dy">${escapeXml(line)}</tspan>""")
            }
            sb.appendLine("""    </text>""")
        }
    }

    internal fun resolveSvgFontFamily(layer: TextLayer): String {
        val custom = layer.customFontName?.trim()
        if (!custom.isNullOrEmpty()) return custom
        return layer.selectedFont.cssFamilyName
    }

    private fun resolveActiveGradient(layer: TextLayer): LuxuryGradientPreset {
        if (layer.gradientPreset != LuxuryGradientPreset.NONE) return layer.gradientPreset
        if (layer.customTextColorLong != null) return LuxuryGradientPreset.NONE
        return when (layer.selectedEffect) {
            VisualEffect.LIQUID_CHROME -> LuxuryGradientPreset.TITANIUM_CHROME
            VisualEffect.CYBER_NEON -> LuxuryGradientPreset.CYBER_EMERALD
            VisualEffect.FROSTED_GLASS -> LuxuryGradientPreset.FROSTED_PEARL
            VisualEffect.MINIMAL_LUXURY, VisualEffect.CINEMATIC_SHADOW -> LuxuryGradientPreset.NONE
            else -> LuxuryGradientPreset.ANDALUSIAN_GOLD
        }
    }

    private fun appendGradientDef(
        sb: StringBuilder,
        safeId: String,
        preset: LuxuryGradientPreset,
        angleDeg: Float,
        isRadial: Boolean
    ) {
        if (preset.colors.isEmpty()) return
        if (isRadial) {
            sb.appendLine("""    <radialGradient id="grad_$safeId" cx="50%" cy="50%" r="55%">""")
            preset.colors.forEachIndexed { idx, col ->
                val offset = ((preset.stops.getOrElse(idx) { idx.toFloat() / (preset.colors.size - 1).coerceAtLeast(1) }) * 100f).toInt()
                val alpha = ((col ushr 24) and 0xFF) / 255f
                sb.appendLine("""      <stop offset="$offset%" stop-color="${colorToHex(col)}" stop-opacity="$alpha" />""")
            }
            sb.appendLine("    </radialGradient>")
        } else {
            val rad = Math.toRadians(angleDeg.toDouble())
            val dx = cos(rad) * 0.5
            val dy = sin(rad) * 0.5
            val x1 = ((0.5 - dx) * 100.0).toInt()
            val y1 = ((0.5 - dy) * 100.0).toInt()
            val x2 = ((0.5 + dx) * 100.0).toInt()
            val y2 = ((0.5 + dy) * 100.0).toInt()
            sb.appendLine("""    <linearGradient id="grad_$safeId" x1="$x1%" y1="$y1%" x2="$x2%" y2="$y2%">""")
            preset.colors.forEachIndexed { idx, col ->
                val offset = ((preset.stops.getOrElse(idx) { idx.toFloat() / (preset.colors.size - 1).coerceAtLeast(1) }) * 100f).toInt()
                val alpha = ((col ushr 24) and 0xFF) / 255f
                sb.appendLine("""      <stop offset="$offset%" stop-color="${colorToHex(col)}" stop-opacity="$alpha" />""")
            }
            sb.appendLine("    </linearGradient>")
        }
    }

    private fun buildFontFeatureString(layer: TextLayer): String? = buildString {
        if (layer.stylisticSet in 1..20) {
            append(String.format(Locale.US, "\"ss%02d\" 1, ", layer.stylisticSet))
        }
        if (layer.enableSwash) append("\"swsh\" 1, ")
        if (layer.enableDiscretionaryLigatures) append("\"dlig\" 1, ")
    }.trimEnd(',', ' ').ifEmpty { null }

    /**
     * Flattens an Android [Path] into an SVG `d` path string using adaptive chord-error
     * subdivision over [PathMeasure], keeping straight stems/kashidas minimal and curves crisp.
     */
    private fun pathToSvgData(path: Path, maxChordErrorPx: Float = 0.35f): String {
        val pm = PathMeasure(path, false)
        val out = StringBuilder()
        val pA = FloatArray(2)
        val pB = FloatArray(2)
        val pM = FloatArray(2)

        fun emitAdaptive(d0: Float, x0: Float, y0: Float, d1: Float, x1: Float, y1: Float, depth: Int) {
            if (depth < 6 && (d1 - d0) > 0.6f) {
                val dm = (d0 + d1) * 0.5f
                pm.getPosTan(dm, pM, null)
                val mx = pM[0]
                val my = pM[1]
                val dx = x1 - x0
                val dy = y1 - y0
                val segLen = hypot(dx, dy)
                val deviation = if (segLen > 1e-3f) {
                    abs(dy * mx - dx * my + x1 * y0 - y1 * x0) / segLen
                } else {
                    hypot(mx - x0, my - y0)
                }
                if (deviation > maxChordErrorPx) {
                    emitAdaptive(d0, x0, y0, dm, mx, my, depth + 1)
                    emitAdaptive(dm, mx, my, d1, x1, y1, depth + 1)
                    return
                }
            }
            out.append("L").append(fmt(x1)).append(",").append(fmt(y1))
        }

        do {
            val len = pm.length
            if (len <= 0.01f) continue
            val seedSteps = 8
            pm.getPosTan(0f, pA, null)
            out.append("M").append(fmt(pA[0])).append(",").append(fmt(pA[1]))
            var prevD = 0f
            var prevX = pA[0]
            var prevY = pA[1]
            for (i in 1..seedSteps) {
                val curD = (len * i) / seedSteps
                pm.getPosTan(curD, pB, null)
                emitAdaptive(prevD, prevX, prevY, curD, pB[0], pB[1], depth = 0)
                prevD = curD
                prevX = pB[0]
                prevY = pB[1]
            }
            out.append("Z")
        } while (pm.nextContour())

        return out.toString()
    }

    private fun fmt(v: Float): String = String.format(Locale.US, "%.2f", v)

    private fun sanitizeId(id: String): String = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    suspend fun exportSvg(
        context: Context,
        state: CanvasState,
        onShareRequested: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val svgContent = generateSvgString(state, context)
            val timestamp = System.currentTimeMillis()
            val filename = "Athar_Vector_${timestamp}.svg"

            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val shareFile = File(cacheDir, filename)
            FileOutputStream(shareFile).use { out ->
                out.write(svgContent.toByteArray(Charsets.UTF_8))
            }

            saveSvgToMediaStore(context, shareFile, filename)

            if (onShareRequested) {
                shareSvgFile(context, shareFile, filename)
            }

            Result.success("تم تصدير ملف SVG المتجهي بنجاح (قابل للتعديل في برامج التصميم)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveSvgToMediaStore(context: Context, sourceFile: File, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "image/svg+xml")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Athar")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("تعذر إنشاء ملف SVG في التنزيلات")

            context.contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val targetDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Athar"
            ).apply { mkdirs() }
            val targetFile = File(targetDir, filename)
            sourceFile.copyTo(targetFile, overwrite = true)
        }
    }

    private fun shareSvgFile(context: Context, shareFile: File, filename: String) {
        val uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", shareFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/svg+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, "مشاركة ملف SVG من أثر")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }

    private fun colorToHex(colorLong: Long): String {
        val r = ((colorLong ushr 16) and 0xFF).toString(16).padStart(2, '0')
        val g = ((colorLong ushr 8) and 0xFF).toString(16).padStart(2, '0')
        val b = (colorLong and 0xFF).toString(16).padStart(2, '0')
        return "#$r$g$b"
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
