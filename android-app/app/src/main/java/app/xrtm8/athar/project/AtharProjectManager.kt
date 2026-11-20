package app.xrtm8.athar.project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import app.xrtm8.athar.core.AtharNativeEngine
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.typography.AtharFontManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object AtharProjectManager {

    private const val PROJECTS_DIR_NAME = "athar_projects"
    private const val THUMBS_DIR_NAME = "athar_thumbs"
    private const val MANIFEST_ENTRY_NAME = "project.json"
    private const val THUMBNAIL_ENTRY_NAME = "thumbnail.png"
    private const val BACKGROUND_ENTRY_NAME = "background.png"

    private fun getProjectsDir(context: Context): File {
        val dir = File(context.filesDir, PROJECTS_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getThumbsDir(context: Context): File {
        val dir = File(context.filesDir, THUMBS_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun saveProject(
        context: Context,
        state: CanvasState,
        projectTitle: String? = null
    ): AtharProjectMeta = withContext(Dispatchers.IO) {
        val projId = "athar_${System.currentTimeMillis()}"
        val title = (projectTitle ?: state.rawText.trim().replace("\n", " ").take(25)).ifBlank { "مشروع بدون عنوان" }

        val projectsDir = getProjectsDir(context)
        val thumbsDir = getThumbsDir(context)

        val projectFile = File(projectsDir, "$projId.athar")
        val thumbFile = File(thumbsDir, "$projId.png")

        // 1. Generate Thumbnail Bitmap
        val thumbBitmap = renderThumbnail(context, state)
        val thumbBytes = ByteArrayOutputStream().use { baos ->
            thumbBitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
            baos.toByteArray()
        }
        FileOutputStream(thumbFile).use { it.write(thumbBytes) }

        // 2. Read Background Image Bytes if exists
        var bgBytes: ByteArray? = null
        if (state.backgroundImageUri != null) {
            try {
                context.contentResolver.openInputStream(state.backgroundImageUri)?.use { input ->
                    bgBytes = input.readBytes()
                }
            } catch (e: Exception) {
                // Ignore if failed to read
            }
        }

        // 3. Create Manifest JSON
        val json = JSONObject().apply {
            put("athar_schema_version", 1)
            put("project_id", projId)
            put("title", title)
            put("created_at", System.currentTimeMillis())

            val canvasJson = JSONObject().apply {
                put("raw_text", state.rawText)
                put("display_text", state.displayText)
                put("stretch_level", state.stretchLevel)
                put("kashida_mode", state.kashidaMode.name)
                put("kashida_target_index", state.kashidaTargetIndex)
                put("selected_font", state.selectedFont.name)
                put("selected_effect", state.selectedEffect.name)
                put("font_size", state.fontSize.toDouble())
                put("custom_color", state.customTextColorLong ?: 0L)
                put("stroke_width", state.strokeWidth.toDouble())
                put("stroke_color", state.strokeColorLong)
                put("shadow_radius", state.shadowRadius.toDouble())
                put("shadow_dx", state.shadowDx.toDouble())
                put("shadow_dy", state.shadowDy.toDouble())
                put("shadow_color", state.shadowColorLong)
                put("text_align", state.textAlign.name)
                put("line_spacing", state.lineSpacingMultiplier.toDouble())
                put("letter_spacing", state.letterSpacing.toDouble())
                put("text_opacity", state.textOpacity.toDouble())
                put("is_bold", state.isBold)
                put("is_italic", state.isItalic)
                put("is_underline", state.isUnderline)
                put("is_locked", state.isLocked)
                put("show_grid_guides", state.showGridGuides)
                put("aspect_ratio", state.aspectRatio.name)
                put("solid_bg_color", state.solidBgColorLong ?: 0L)
                put("has_background_photo", bgBytes != null)
                put("background_dim", state.backgroundDim.toDouble())
                put("background_grayscale", state.backgroundGrayscale)
                put("rotation_angle", state.rotationAngle.toDouble())
                put("scale_factor", state.scaleFactor.toDouble())
                put("norm_offset_x", state.normOffsetX.toDouble())
                put("norm_offset_y", state.normOffsetY.toDouble())
                put("is_transparent_bg", state.isTransparentBg)
            }
            put("canvas", canvasJson)
        }

        // 4. Bundle into ZIP Archive (.athar)
        ZipOutputStream(FileOutputStream(projectFile)).use { zos ->
            // Write project.json
            zos.putNextEntry(ZipEntry(MANIFEST_ENTRY_NAME))
            zos.write(json.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Write thumbnail.png
            zos.putNextEntry(ZipEntry(THUMBNAIL_ENTRY_NAME))
            zos.write(thumbBytes)
            zos.closeEntry()

            // Write background.png if exists
            if (bgBytes != null) {
                zos.putNextEntry(ZipEntry(BACKGROUND_ENTRY_NAME))
                zos.write(bgBytes)
                zos.closeEntry()
            }
        }

        AtharProjectMeta(
            id = projId,
            title = title,
            lastModified = System.currentTimeMillis(),
            thumbnailPath = thumbFile.absolutePath,
            fileSizeBytes = projectFile.length()
        )
    }

    suspend fun listProjects(context: Context): List<AtharProjectMeta> = withContext(Dispatchers.IO) {
        val projectsDir = getProjectsDir(context)
        val thumbsDir = getThumbsDir(context)

        val files = projectsDir.listFiles { _, name -> name.endsWith(".athar", ignoreCase = true) } ?: emptyArray()

        files.mapNotNull { file ->
            try {
                val projId = file.nameWithoutExtension
                val thumbFile = File(thumbsDir, "$projId.png")

                var title = projId
                var createdAt = file.lastModified()

                // Inspect Zip Header without extracting everything
                ZipInputStream(FileInputStream(file)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == MANIFEST_ENTRY_NAME) {
                            val jsonStr = zis.bufferedReader(Charsets.UTF_8).readText()
                            val json = JSONObject(jsonStr)
                            title = json.optString("title", title)
                            createdAt = json.optLong("created_at", createdAt)
                        } else if (entry.name == THUMBNAIL_ENTRY_NAME && !thumbFile.exists()) {
                            FileOutputStream(thumbFile).use { out -> zis.copyTo(out) }
                        }
                        entry = zis.nextEntry
                    }
                }

                AtharProjectMeta(
                    id = projId,
                    title = title,
                    lastModified = createdAt,
                    thumbnailPath = thumbFile.absolutePath,
                    fileSizeBytes = file.length()
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.lastModified }
    }

    suspend fun loadProjectFromStream(context: Context, inputStream: InputStream): CanvasState = withContext(Dispatchers.IO) {
        var jsonStr: String? = null
        var extractedBgUri: Uri? = null

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    MANIFEST_ENTRY_NAME -> {
                        jsonStr = zis.bufferedReader(Charsets.UTF_8).readText()
                    }
                    BACKGROUND_ENTRY_NAME -> {
                        val bgDir = File(context.cacheDir, "athar_bg_cache")
                        if (!bgDir.exists()) bgDir.mkdirs()
                        val bgFile = File(bgDir, "bg_${System.currentTimeMillis()}.png")
                        FileOutputStream(bgFile).use { out -> zis.copyTo(out) }
                        extractedBgUri = Uri.fromFile(bgFile)
                    }
                }
                entry = zis.nextEntry
            }
        }

        if (jsonStr == null) {
            throw IllegalArgumentException("الملف المختار ليس مشروع أَثَــر صالح")
        }

        val json = JSONObject(jsonStr!!)
        val c = json.getJSONObject("canvas")

        val rawText = c.optString("raw_text", "أَثَــر")
        val stretchLevel = c.optInt("stretch_level", 3)
        val kashidaModeStr = c.optString("kashida_mode", KashidaMode.SMART.name)
        val kashidaMode = try { KashidaMode.valueOf(kashidaModeStr) } catch (e: Exception) { KashidaMode.SMART }
        val kashidaTargetIndex = c.optInt("kashida_target_index", -1)
        val selectedFontStr = c.optString("selected_font", ArabicFontFamily.AREF_RUQAA.name)
        val selectedEffectStr = c.optString("selected_effect", VisualEffect.ROYAL_GOLD.name)
        val aspectRatioStr = c.optString("aspect_ratio", CanvasAspectRatio.STORY_9_16.name)
        val textAlignStr = c.optString("text_align", TextAlignOption.CENTER.name)

        val font = try { ArabicFontFamily.valueOf(selectedFontStr) } catch (e: Exception) { ArabicFontFamily.AREF_RUQAA }
        val effect = try { VisualEffect.valueOf(selectedEffectStr) } catch (e: Exception) { VisualEffect.ROYAL_GOLD }
        val aspect = try { CanvasAspectRatio.valueOf(aspectRatioStr) } catch (e: Exception) { CanvasAspectRatio.STORY_9_16 }
        val align = try { TextAlignOption.valueOf(textAlignStr) } catch (e: Exception) { TextAlignOption.CENTER }

        val customColor = c.optLong("custom_color", 0L)
        val solidBg = c.optLong("solid_bg_color", 0L)

        // Native Kashida calculation
        val stretched = AtharNativeEngine.applyKashida(rawText, stretchLevel, kashidaMode, kashidaTargetIndex)

        CanvasState(
            rawText = rawText,
            displayText = stretched,
            stretchLevel = stretchLevel,
            kashidaMode = kashidaMode,
            kashidaTargetIndex = kashidaTargetIndex,
            selectedFont = font,
            selectedEffect = effect,
            fontSize = c.optDouble("font_size", 52.0).toFloat(),
            customTextColorLong = if (customColor != 0L) customColor else null,
            strokeWidth = c.optDouble("stroke_width", 0.0).toFloat(),
            strokeColorLong = c.optLong("stroke_color", 0xFF000000),
            shadowRadius = c.optDouble("shadow_radius", 16.0).toFloat(),
            shadowDx = c.optDouble("shadow_dx", 0.0).toFloat(),
            shadowDy = c.optDouble("shadow_dy", 6.0).toFloat(),
            shadowColorLong = c.optLong("shadow_color", 0xAA000000),
            textAlign = align,
            lineSpacingMultiplier = c.optDouble("line_spacing", 1.35).toFloat(),
            letterSpacing = c.optDouble("letter_spacing", 0.0).toFloat(),
            textOpacity = c.optDouble("text_opacity", 1.0).toFloat(),
            isBold = c.optBoolean("is_bold", false),
            isItalic = c.optBoolean("is_italic", false),
            isUnderline = c.optBoolean("is_underline", false),
            isLocked = c.optBoolean("is_locked", false),
            showGridGuides = c.optBoolean("show_grid_guides", false),
            aspectRatio = aspect,
            solidBgColorLong = if (solidBg != 0L) solidBg else null,
            backgroundImageUri = extractedBgUri,
            backgroundDim = c.optDouble("background_dim", 0.25).toFloat(),
            backgroundGrayscale = c.optBoolean("background_grayscale", false),
            rotationAngle = c.optDouble("rotation_angle", 0.0).toFloat(),
            scaleFactor = c.optDouble("scale_factor", 1.0).toFloat(),
            normOffsetX = c.optDouble("norm_offset_x", 0.0).toFloat(),
            normOffsetY = c.optDouble("norm_offset_y", 0.0).toFloat(),
            isTransparentBg = c.optBoolean("is_transparent_bg", false)
        )
    }

    suspend fun loadProjectById(context: Context, projectId: String): CanvasState {
        val projectsDir = getProjectsDir(context)
        val file = File(projectsDir, "$projectId.athar")
        if (!file.exists()) throw IllegalArgumentException("المشروع غير موجود")
        return loadProjectFromStream(context, FileInputStream(file))
    }

    suspend fun deleteProject(context: Context, projectId: String): Boolean = withContext(Dispatchers.IO) {
        val projectsDir = getProjectsDir(context)
        val thumbsDir = getThumbsDir(context)
        val file = File(projectsDir, "$projectId.athar")
        val thumb = File(thumbsDir, "$projectId.png")
        file.delete() && thumb.delete()
    }

    suspend fun getExportableProjectFile(context: Context, projectId: String, title: String): File = withContext(Dispatchers.IO) {
        val projectsDir = getProjectsDir(context)
        val srcFile = File(projectsDir, "$projectId.athar")
        val exportDir = File(context.getExternalFilesDir(null), "exported_projects")
        if (!exportDir.exists()) exportDir.mkdirs()

        val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val dstFile = File(exportDir, "$cleanTitle.athar")
        srcFile.copyTo(dstFile, overwrite = true)
        dstFile
    }

    private fun renderThumbnail(context: Context, state: CanvasState): Bitmap {
        val width = 480
        val height = when (state.aspectRatio) {
            CanvasAspectRatio.STORY_9_16 -> (width * 16f / 9f).toInt()
            CanvasAspectRatio.SQUARE_1_1 -> width
            CanvasAspectRatio.PORTRAIT_4_5 -> (width * 5f / 4f).toInt()
            CanvasAspectRatio.FREE -> (width * 16f / 9f).toInt()
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!state.isTransparentBg) {
            var photoDrawn = false
            if (state.backgroundImageUri != null) {
                try {
                    context.contentResolver.openInputStream(state.backgroundImageUri)?.use { stream ->
                        val srcBm = BitmapFactory.decodeStream(stream)
                        if (srcBm != null) {
                            val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
                            if (state.backgroundGrayscale) {
                                val cm = ColorMatrix().apply { setSaturation(0f) }
                                bgPaint.colorFilter = ColorMatrixColorFilter(cm)
                            }
                            val srcW = srcBm.width.toFloat()
                            val srcH = srcBm.height.toFloat()
                            val srcRatio = srcW / srcH
                            val dstRatio = width.toFloat() / height.toFloat()

                            val srcRect = if (srcRatio > dstRatio) {
                                val newW = (srcH * dstRatio).toInt()
                                val xOffset = ((srcW - newW) / 2).toInt()
                                Rect(xOffset, 0, xOffset + newW, srcH.toInt())
                            } else {
                                val newH = (srcW / dstRatio).toInt()
                                val yOffset = ((srcH - newH) / 2).toInt()
                                Rect(0, yOffset, srcW.toInt(), yOffset + newH)
                            }
                            canvas.drawBitmap(srcBm, srcRect, Rect(0, 0, width, height), bgPaint)
                            photoDrawn = true
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            if (!photoDrawn) {
                if (state.solidBgColorLong != null) {
                    canvas.drawColor(state.solidBgColorLong.toInt())
                } else {
                    canvas.drawColor(android.graphics.Color.BLACK)
                }
            } else if (state.backgroundDim > 0f) {
                val dimAlpha = (state.backgroundDim * 255).toInt().coerceIn(0, 240)
                canvas.drawColor(android.graphics.Color.argb(dimAlpha, 0, 0, 0))
            }
        }

        val scaledTextSize = (state.fontSize / 380f) * width
        val realTypeface = AtharFontManager.getTypeface(context, state.selectedFont)

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            textSize = scaledTextSize
            letterSpacing = state.letterSpacing
            isUnderlineText = state.isUnderline
            isFakeBoldText = state.isBold
            textSkewX = if (state.isItalic) -0.25f else 0f
            alpha = (state.textOpacity * 255).toInt().coerceIn(20, 255)
            textAlign = when (state.textAlign) {
                TextAlignOption.RIGHT -> Paint.Align.RIGHT
                TextAlignOption.CENTER -> Paint.Align.CENTER
                TextAlignOption.LEFT -> Paint.Align.LEFT
            }
            typeface = realTypeface
        }

        if (state.customTextColorLong != null && state.customTextColorLong != 0L) {
            paint.color = state.customTextColorLong.toInt()
        } else {
            when (state.selectedEffect) {
                VisualEffect.ROYAL_GOLD -> {
                    paint.shader = LinearGradient(
                        0f, -paint.textSize * 0.8f,
                        0f, paint.textSize * 0.2f,
                        intArrayOf(
                            android.graphics.Color.parseColor("#FFF7DB"),
                            android.graphics.Color.parseColor("#F5C369"),
                            android.graphics.Color.parseColor("#D49438"),
                            android.graphics.Color.parseColor("#FFE8A3")
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                VisualEffect.CYBER_NEON -> paint.color = android.graphics.Color.parseColor("#00FF9D")
                else -> paint.color = android.graphics.Color.WHITE
            }
        }

        val centerX = width * 0.5f + (width * state.normOffsetX)
        val centerY = height * 0.5f + (height * state.normOffsetY)

        val lines = state.displayText.split("\n")
        val lineHeight = paint.textSize * state.lineSpacingMultiplier
        val totalHeight = (lines.size - 1) * lineHeight
        val startY = -totalHeight / 2f + (paint.textSize * 0.32f)

        var maxLineWidth = 0f
        lines.forEach { line ->
            val w = paint.measureText(line)
            if (w > maxLineWidth) maxLineWidth = w
        }
        val halfBlockW = maxLineWidth / 2f
        val lineX = when (state.textAlign) {
            TextAlignOption.RIGHT -> halfBlockW
            TextAlignOption.CENTER -> 0f
            TextAlignOption.LEFT -> -halfBlockW
        }

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(state.rotationAngle)
        canvas.scale(state.scaleFactor, state.scaleFactor)

        lines.forEachIndexed { index, line ->
            val lineY = startY + (index * lineHeight)
            canvas.drawText(line, lineX, lineY, paint)
        }
        canvas.restore()

        return bitmap
    }
}
