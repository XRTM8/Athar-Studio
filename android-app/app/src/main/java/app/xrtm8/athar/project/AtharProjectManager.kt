package app.xrtm8.athar.project

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.typography.AtharFontManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * File I/O and Archive Manager for Athar Projects (.athar format).
 * Projects are bundled as compressed zip containers containing JSON schema,
 * thumbnail preview, custom fonts, and background media assets.
 */
object AtharProjectManager {

    private const val PROJECTS_DIR_NAME = "athar_projects"
    private const val THUMBS_DIR_NAME = "athar_thumbs"
    private const val ASSETS_DIR_NAME = "athar_project_assets"
    private const val MANIFEST_ENTRY_NAME = "project.json"
    private const val THUMBNAIL_ENTRY_NAME = "thumbnail.png"
    private const val BACKGROUND_ENTRY_NAME = "background.png"
    private const val MASK_ENTRY_NAME = "mask.png"
    internal const val CURRENT_SCHEMA_VERSION = 1

    private fun getProjectsDir(context: Context): File =
        File(context.filesDir, PROJECTS_DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun getThumbsDir(context: Context): File =
        File(context.filesDir, THUMBS_DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun getAssetsDir(context: Context): File =
        File(context.filesDir, ASSETS_DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun sanitizeProjectId(id: String?): String? {
        val clean = id?.trim() ?: return null
        if (clean.isEmpty() || clean.contains('/') || clean.contains('\\') || clean.contains("..")) return null
        return clean
    }

    private fun readExistingManifestTitle(projectFile: File): String? {
        if (!projectFile.exists()) return null
        return runCatching {
            ZipFile(projectFile).use { archive ->
                val manifest = archive.getEntry(MANIFEST_ENTRY_NAME) ?: return null
                val json = archive.getInputStream(manifest).bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
                json.optString("title", "").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    @JvmOverloads
    suspend fun saveProject(
        context: Context,
        state: CanvasState,
        projectTitle: String? = null,
        existingProjectId: String? = null
    ): AtharProjectMeta = withContext(Dispatchers.IO) {
        val safeExistingId = sanitizeProjectId(existingProjectId)
        val projectsDir = getProjectsDir(context)
        val thumbsDir = getThumbsDir(context)

        val projId = safeExistingId ?: "athar_${UUID.randomUUID()}"
        val projectFile = File(projectsDir, "$projId.athar")
        val thumbFile = File(thumbsDir, "$projId.png")

        val resolvedExistingTitle = if (projectTitle == null && safeExistingId != null) {
            readExistingManifestTitle(projectFile)
        } else null
        val fallbackLayerText = state.rawText.ifBlank {
            state.layers.firstOrNull { it.rawText.isNotBlank() }?.rawText ?: ""
        }
        val title = (projectTitle ?: resolvedExistingTitle ?: fallbackLayerText.trim().replace("\n", " ").take(25))
            .ifBlank { "مشروع بدون عنوان" }

        val thumbBitmap = AtharProjectThumbnailRenderer.renderThumbnail(context, state)
        val thumbBytes = ByteArrayOutputStream().use { baos ->
            thumbBitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
            baos.toByteArray()
        }
        thumbBitmap.recycle()

        val hasBg = state.backgroundImageUri != null

        val bundledFonts = LinkedHashMap<String, File>()
        state.layers.forEach { layer ->
            val path = layer.customFontPath?.takeIf { it.isNotBlank() } ?: return@forEach
            val fontFile = AtharFontManager.resolveExistingCustomFile(context, path) ?: return@forEach
            val safeName = fontFile.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            if (safeName.endsWith(".ttf", true) || safeName.endsWith(".otf", true)) {
                bundledFonts.putIfAbsent("fonts/$safeName", fontFile)
            }
        }

        val nowMs = System.currentTimeMillis()
        val json = JSONObject().apply {
            put("athar_schema_version", CURRENT_SCHEMA_VERSION)
            put("project_id", projId)
            put("title", title)
            put("created_at", nowMs)
            put("updated_at", nowMs)
            put("canvas", AtharProjectSerializer.serializeCanvasState(state, hasBg))
        }

        val pending = File.createTempFile("save-", ".part", projectsDir)
        val pendingThumb = File.createTempFile("thumb-", ".part", thumbsDir)
        try {
            ZipOutputStream(pending.outputStream()).use { archive ->
                fun writeEntry(name: String, bytes: ByteArray) {
                    archive.putNextEntry(ZipEntry(name))
                    archive.write(bytes)
                    archive.closeEntry()
                }
                fun writePhoto(name: String, uri: Uri, errorMessage: String) {
                    val stream = checkNotNull(context.contentResolver.openInputStream(uri)) { errorMessage }
                    stream.use {
                        archive.putNextEntry(ZipEntry(name))
                        it.copyTo(archive, bufferSize = 64 * 1024)
                        archive.closeEntry()
                    }
                }
                writeEntry(MANIFEST_ENTRY_NAME, json.toString(2).toByteArray(Charsets.UTF_8))
                writeEntry(THUMBNAIL_ENTRY_NAME, thumbBytes)
                state.backgroundImageUri?.let { writePhoto(BACKGROUND_ENTRY_NAME, it, "تعذرت قراءة صورة الخلفية") }
                state.maskState.customMaskUri?.takeIf { it.isNotBlank() }?.let { customUriStr ->
                    writePhoto(MASK_ENTRY_NAME, Uri.parse(customUriStr), "تعذرت قراءة صورة القناع المخصص")
                }
                state.collageState.cells.forEachIndexed { index, cell ->
                    cell.imageUri?.let { writePhoto("collage/$index", it, "تعذرت قراءة صورة الكولاج") }
                }
                bundledFonts.forEach { (entryName, fontFile) ->
                    if (fontFile.exists() && fontFile.canRead()) {
                        fontFile.inputStream().use { input ->
                            archive.putNextEntry(ZipEntry(entryName))
                            input.copyTo(archive, bufferSize = 64 * 1024)
                            archive.closeEntry()
                        }
                    }
                }
            }
            pendingThumb.writeBytes(thumbBytes)
            Files.move(pendingThumb.toPath(), thumbFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            Files.move(pending.toPath(), projectFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (e: Exception) {
            if (safeExistingId == null) {
                thumbFile.delete()
            }
            throw e
        } finally {
            pending.delete()
            pendingThumb.delete()
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
        cleanOldCache(context)
        val projectsDir = getProjectsDir(context)
        val thumbsDir = getThumbsDir(context)

        val files = projectsDir.listFiles { _, name -> name.endsWith(".athar", ignoreCase = true) } ?: emptyArray()

        files.mapNotNull { file ->
            try {
                val projId = file.nameWithoutExtension
                val thumbFile = File(thumbsDir, "$projId.png")

                var title = projId
                var createdAt = file.lastModified()

                ZipFile(file).use { archive ->
                    val manifest = archive.getEntry(MANIFEST_ENTRY_NAME) ?: return@mapNotNull null
                    val json = archive.getInputStream(manifest).bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
                    title = json.optString("title", title)
                    createdAt = json.optLong("created_at", createdAt)
                    if (!thumbFile.exists()) {
                        archive.getEntry(THUMBNAIL_ENTRY_NAME)?.let { thumbnail ->
                            archive.getInputStream(thumbnail).use { input ->
                                thumbFile.outputStream().use { input.copyTo(it) }
                            }
                        }
                    }
                }

                AtharProjectMeta(
                    id = projId,
                    title = title,
                    lastModified = createdAt,
                    thumbnailPath = thumbFile.absolutePath,
                    fileSizeBytes = file.length()
                )
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.lastModified }
    }

    @JvmOverloads
    suspend fun loadProjectFromStream(
        context: Context,
        inputStream: InputStream,
        assetScopePrefix: String? = null
    ): CanvasState = withContext(Dispatchers.IO) {
        var manifest: JSONObject? = null
        val photos = mutableMapOf<String, File>()
        val restoredFonts = mutableMapOf<String, String>()
        val assetsRoot = getAssetsDir(context)
        val safePrefix = sanitizeProjectId(assetScopePrefix) ?: "import"
        val sessionDir = File(assetsRoot, "proj_${safePrefix}_${UUID.randomUUID()}").apply { mkdirs() }

        try {
            ZipInputStream(inputStream).use { archive ->
                var entry = archive.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == MANIFEST_ENTRY_NAME) {
                        manifest = JSONObject(archive.bufferedReader(Charsets.UTF_8).readText())
                    } else if (name == BACKGROUND_ENTRY_NAME || name == MASK_ENTRY_NAME || name.matches(Regex("collage/[0-9]+"))) {
                        require(name !in photos) { "صورة مكررة في المشروع" }
                        val photo = File.createTempFile("photo-", ".img", sessionDir)
                        photos[name] = photo
                        photo.outputStream().use { archive.copyTo(it) }
                    } else if (name.startsWith("fonts/")) {
                        val rawFontName = name.substringAfter("fonts/")
                        require(
                            rawFontName.isNotBlank() &&
                                !rawFontName.contains('/') &&
                                !rawFontName.contains('\\') &&
                                !rawFontName.contains("..") &&
                                (rawFontName.endsWith(".ttf", true) || rawFontName.endsWith(".otf", true))
                        ) { "ملف خط غير صالح في المشروع" }
                        val tmpFont = File.createTempFile("font-", ".part", sessionDir)
                        try {
                            tmpFont.outputStream().use { archive.copyTo(it) }
                            AtharFontManager.importFontFileForProject(context, tmpFont, rawFontName)?.let { imported ->
                                restoredFonts[rawFontName] = imported.filePath
                            }
                        } finally {
                            tmpFont.delete()
                        }
                    }
                    entry = archive.nextEntry
                }
            }
            val rootManifest = requireNotNull(manifest) { "الملف المختار ليس مشروع أثر صالحًا" }
            val schemaVersion = rootManifest.optInt("athar_schema_version", 1)
            require(schemaVersion in 1..CURRENT_SCHEMA_VERSION) {
                "يتطلب هذا المشروع إصدارًا أحدث من تطبيق أثر (schema v$schemaVersion)"
            }
            val canvas = rootManifest.getJSONObject("canvas")
            fun remapCustomFontPath(obj: JSONObject) {
                val oldPath = obj.optString("custom_font_path", "")
                if (oldPath.isBlank()) return
                val oldFileName = oldPath.substringAfterLast('/').substringAfterLast('\\')
                val remapped = restoredFonts[oldFileName]
                    ?: AtharFontManager.resolveExistingCustomFile(context, oldPath)?.absolutePath
                if (!remapped.isNullOrBlank()) {
                    obj.put("custom_font_path", remapped)
                }
            }
            remapCustomFontPath(canvas)
            val layersArr = canvas.optJSONArray("layers")
            if (layersArr != null) {
                for (index in 0 until layersArr.length()) {
                    layersArr.optJSONObject(index)?.let { remapCustomFontPath(it) }
                }
            }
            val cells = canvas.optJSONArray("collage_cells")
            if (cells != null) {
                for (index in 0 until cells.length()) {
                    photos["collage/$index"]?.let { cells.getJSONObject(index).put("uri", Uri.fromFile(it).toString()) }
                }
            }
            photos[MASK_ENTRY_NAME]?.let {
                canvas.put("mask_custom_uri", Uri.fromFile(it).toString())
            }
            if (photos.isEmpty()) {
                sessionDir.deleteRecursively()
            }
            AtharProjectSerializer.deserializeCanvasState(canvas, photos[BACKGROUND_ENTRY_NAME]?.let { Uri.fromFile(it) })
        } catch (e: Exception) {
            sessionDir.deleteRecursively()
            throw e
        }
    }

    suspend fun loadProjectById(context: Context, projectId: String): CanvasState {
        val safeId = sanitizeProjectId(projectId) ?: throw IllegalArgumentException("معرف المشروع غير صالح")
        val projectsDir = getProjectsDir(context)
        val file = File(projectsDir, "$safeId.athar")
        if (!file.exists()) throw IllegalArgumentException("المشروع غير موجود")
        return FileInputStream(file).use { loadProjectFromStream(context, it, assetScopePrefix = safeId) }
    }

    suspend fun deleteProject(context: Context, projectId: String): Boolean = withContext(Dispatchers.IO) {
        val safeId = sanitizeProjectId(projectId) ?: return@withContext false
        val projectsDir = getProjectsDir(context)
        val thumbsDir = getThumbsDir(context)
        val assetsDir = getAssetsDir(context)
        val file = File(projectsDir, "$safeId.athar")
        val thumb = File(thumbsDir, "$safeId.png")

        assetsDir.listFiles { _, name ->
            name.contains(safeId) || name.startsWith("proj_${safeId}_")
        }?.forEach { entry ->
            entry.deleteRecursively()
        }

        val deletedFile = !file.exists() || file.delete()
        val deletedThumb = !thumb.exists() || thumb.delete()
        deletedFile && deletedThumb
    }

    suspend fun renameProject(context: Context, projectId: String, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val safeId = sanitizeProjectId(projectId) ?: return@withContext false
            val projectsDir = getProjectsDir(context)
            val file = File(projectsDir, "$safeId.athar")
            if (!file.exists()) return@withContext false

            val cleanTitle = newTitle.trim().take(40).ifBlank { "مشروع بدون عنوان" }
            val tempFile = File.createTempFile("rename-", ".part", projectsDir)
            try {
                var updated = false
                ZipInputStream(FileInputStream(file)).use { zis ->
                    ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == MANIFEST_ENTRY_NAME) {
                                val jsonStr = zis.bufferedReader(Charsets.UTF_8).readText()
                                val json = JSONObject(jsonStr)
                                json.put("title", cleanTitle)
                                zos.putNextEntry(ZipEntry(MANIFEST_ENTRY_NAME))
                                zos.write(json.toString(2).toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                                updated = true
                            } else {
                                zos.putNextEntry(ZipEntry(entry.name))
                                zis.copyTo(zos)
                                zos.closeEntry()
                            }
                            entry = zis.nextEntry
                        }
                    }
                }

                if (updated) Files.move(tempFile.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
                updated
            } finally {
                tempFile.delete()
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getCacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        fun dirSize(dir: File?): Long {
            if (dir == null || !dir.exists()) return 0L
            var size = 0L
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) dirSize(file) else file.length()
            }
            return size
        }
        dirSize(context.cacheDir)
    }

    suspend fun clearAppCache(context: Context): Long = withContext(Dispatchers.IO) {
        val before = getCacheSizeBytes(context)
        fun deleteRecursively(dir: File?) {
            if (dir == null || !dir.exists()) return
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) deleteRecursively(file)
                file.delete()
            }
        }
        deleteRecursively(context.cacheDir)
        pruneStaleExtractedAssets(context, maxAgeMs = 0L)
        val after = getCacheSizeBytes(context)
        (before - after).coerceAtLeast(0L)
    }

    suspend fun getExportableProjectFile(context: Context, projectId: String, title: String): File = withContext(Dispatchers.IO) {
        val safeId = sanitizeProjectId(projectId) ?: throw IllegalArgumentException("معرف المشروع غير صالح")
        val projectsDir = getProjectsDir(context)
        val srcFile = File(projectsDir, "$safeId.athar")
        val exportDir = File(context.getExternalFilesDir(null), "exported_projects")
        if (!exportDir.exists()) exportDir.mkdirs()

        val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val dstFile = File(exportDir, "$cleanTitle.athar")
        srcFile.copyTo(dstFile, overwrite = true)
        dstFile
    }

    /**
     * Deletes extracted temporary assets in filesDir/athar_project_assets older than [maxAgeMs]
     * that are not in [activeAssetPaths].
     */
    fun pruneStaleExtractedAssets(
        context: Context,
        maxAgeMs: Long = 24L * 3600L * 1000L,
        activeAssetPaths: Set<String> = emptySet()
    ) {
        runCatching {
            val now = System.currentTimeMillis()
            val assetsDir = getAssetsDir(context)
            assetsDir.listFiles()?.forEach { entry ->
                val isReferenced = activeAssetPaths.any { path ->
                    path.startsWith(entry.absolutePath)
                }
                if (!isReferenced && (maxAgeMs <= 0L || (now - entry.lastModified() > maxAgeMs))) {
                    entry.deleteRecursively()
                }
            }
        }
    }

    /**
     * Automated Cache Eviction Policy.
     * Deletes temporary files older than 24 hours in cacheDir/exports, cacheDir/updates,
     * and stale extracted asset sessions in athar_project_assets.
     */
    fun cleanOldCache(context: Context) {
        try {
            val now = System.currentTimeMillis()
            val oneDayMs = 24L * 3600L * 1000L

            val exportDir = File(context.cacheDir, "exports")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { f ->
                    if (now - f.lastModified() > oneDayMs) f.delete()
                }
            }

            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists()) {
                updatesDir.listFiles()?.forEach { f ->
                    if (now - f.lastModified() > oneDayMs) f.delete()
                }
            }

            pruneStaleExtractedAssets(context, maxAgeMs = oneDayMs)
        } catch (_: Exception) {
            // Safe ignore
        }
    }
}
