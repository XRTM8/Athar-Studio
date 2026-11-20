package app.xrtm8.athar.project

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import app.xrtm8.athar.model.CanvasState
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

/**
 * File I/O and Archive Manager for Athar Projects (.athar format).
 * Projects are bundled as compressed zip containers containing JSON schema,
 * full-fidelity thumbnails, and background media assets.
 */
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

        // 1. Generate Thumbnail Bitmap via dedicated renderer
        val thumbBitmap = AtharProjectThumbnailRenderer.renderThumbnail(context, state)
        val thumbBytes = ByteArrayOutputStream().use { baos ->
            thumbBitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
            baos.toByteArray()
        }
        thumbBitmap.recycle()
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
            put("canvas", AtharProjectSerializer.serializeCanvasState(state, bgBytes != null))
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
                        cleanOldCache(context)
                        val bgDir = File(context.cacheDir, "athar_bg_cache")
                        if (!bgDir.exists()) bgDir.mkdirs()
                        val bgFile = File(bgDir, "bg_${System.currentTimeMillis()}.png")
                        
                        // Zip Slip Security Prevention
                        if (!bgFile.canonicalPath.startsWith(bgDir.canonicalPath + File.separator)) {
                            throw SecurityException("محاولة اختراق تراجعي للمسار: ${entry.name}")
                        }
                        
                        FileOutputStream(bgFile).use { out -> zis.copyTo(out) }
                        extractedBgUri = Uri.fromFile(bgFile)
                    }
                }
                entry = zis.nextEntry
            }
        }

        val validJsonStr = jsonStr ?: throw IllegalArgumentException("الملف المختار ليس مشروع أَثَــر صالح")
        val json = JSONObject(validJsonStr)
        val c = json.getJSONObject("canvas")
        AtharProjectSerializer.deserializeCanvasState(c, extractedBgUri)
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

    /**
     * Automated Cache Eviction Policy.
     * Prevents unbounded storage growth in athar_bg_cache:
     * - Deletes files older than 48 hours
     * - Caps directory size at 50MB max, evicting oldest files first
     */
    fun cleanOldCache(context: Context) {
        try {
            val bgDir = File(context.cacheDir, "athar_bg_cache")
            if (!bgDir.exists()) return
            val maxSizeBytes = 50L * 1024L * 1024L // 50MB
            val files = bgDir.listFiles() ?: return
            val now = System.currentTimeMillis()
            val twoDaysMs = 48L * 3600L * 1000L

            var totalSize = 0L
            for (f in files) {
                if (now - f.lastModified() > twoDaysMs) {
                    f.delete()
                } else {
                    totalSize += f.length()
                }
            }

            if (totalSize > maxSizeBytes) {
                val sorted = files.filter { it.exists() }.sortedBy { it.lastModified() }
                for (f in sorted) {
                    if (totalSize <= maxSizeBytes) break
                    val len = f.length()
                    if (f.delete()) {
                        totalSize -= len
                    }
                }
            }
        } catch (e: Exception) {
            // Safe ignore
        }
    }
}
