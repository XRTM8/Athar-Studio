package app.xrtm8.athar.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.TextLayer
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Open Vector SVG exporter for Athar Studio (< 220 lines).
 * Generates standards-compliant SVG with typography styling,
 * transforms, RTL text orientation, and filter effects for Figma,
 * Illustrator, and web integration.
 */
object AtharSvgExporter {

    fun generateSvgString(state: CanvasState): String {
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
            if (layer.shadowRadius > 0f) {
                val shadowColor = colorToHex(layer.shadowColorLong)
                val shadowAlpha = ((layer.shadowColorLong ushr 24) and 0xFF) / 255f
                sb.appendLine("""    <filter id="shadow_${layer.id}" x="-30%" y="-30%" width="160%" height="160%">""")
                sb.appendLine("""      <feDropShadow dx="${layer.shadowDx * density}" dy="${layer.shadowDy * density}" stdDeviation="${(layer.shadowRadius * density) / 2f}" flood-color="$shadowColor" flood-opacity="$shadowAlpha" />""")
                sb.appendLine("""    </filter>""")
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

            val cx = (width * 0.5f) + (layer.normOffsetX * width)
            val cy = (height * 0.5f) + (layer.normOffsetY * height)
            val fontSize = layer.fontSize * density
            val fillHex = if (layer.customTextColorLong != null) colorToHex(layer.customTextColorLong) else "#FFFFFF"
            val textAnchor = when (layer.textAlign) {
                TextAlignOption.LEFT -> "start"
                TextAlignOption.RIGHT -> "end"
                TextAlignOption.CENTER -> "middle"
            }
            val filterAttr = if (layer.shadowRadius > 0f) """filter="url(#shadow_${layer.id})"""" else ""
            val strokeAttr = if (layer.strokeWidth > 0f) {
                val strokeHex = colorToHex(layer.strokeColorLong)
                """stroke="$strokeHex" stroke-width="${layer.strokeWidth * density}" paint-order="stroke fill""""
            } else ""

            val escaped = escapeXml(textToRender)

            sb.appendLine("""  <g transform="translate($cx, $cy) rotate(${layer.rotationAngle}) scale(${layer.scaleFactor})" $filterAttr>""")
            sb.appendLine("""    <text x="0" y="0" font-family="${escapeXml(layer.selectedFont.fontName)}, sans-serif" font-size="$fontSize" font-weight="${if (layer.isBold) "bold" else "normal"}" font-style="${if (layer.isItalic) "italic" else "normal"}" fill="$fillHex" text-anchor="$textAnchor" direction="rtl" letter-spacing="${layer.letterSpacing}px" opacity="${layer.textOpacity}" $strokeAttr>$escaped</text>""")
            sb.appendLine("""  </g>""")
        }

        sb.appendLine("</svg>")
        return sb.toString()
    }

    suspend fun exportSvg(
        context: Context,
        state: CanvasState,
        onShareRequested: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val svgContent = generateSvgString(state)
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
