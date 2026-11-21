package app.xrtm8.athar.typography

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CustomFont
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.ConcurrentHashMap

object AtharFontManager {
    private val typefaceCache = ConcurrentHashMap<ArabicFontFamily, Typeface>()
    private val customTypefaceCache = ConcurrentHashMap<String, Typeface>()

    private const val CUSTOM_FONTS_DIR = "custom_fonts"

    private fun getCustomFontsDir(context: Context): File {
        val dir = File(context.filesDir, CUSTOM_FONTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTypeface(context: Context, font: ArabicFontFamily, customPath: String? = null): Typeface {
        if (!customPath.isNullOrBlank()) {
            val file = File(customPath)
            if (file.exists()) {
                val cached = customTypefaceCache[customPath]
                if (cached != null) return cached
                try {
                    val tf = Typeface.createFromFile(file)
                    customTypefaceCache[customPath] = tf
                    return tf
                } catch (_: Exception) {}
            }
        }

        return typefaceCache.computeIfAbsent(font) {
            try {
                Typeface.createFromAsset(context.assets, font.assetPath)
            } catch (e: Exception) {
                Typeface.SERIF
            }
        }
    }

    fun preload(context: Context) {
        ArabicFontFamily.entries.forEach { font ->
            getTypeface(context, font)
        }
    }

    fun listCustomFonts(context: Context): List<CustomFont> {
        val dir = getCustomFontsDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(".ttf", true) || name.endsWith(".otf", true) } ?: return emptyList()
        return files.map { file ->
            val cleanName = file.nameWithoutExtension
                .replace("_", " ")
                .replace("-", " ")
                .trim()
            CustomFont(
                id = file.name,
                name = cleanName.ifBlank { "خط مخصص" },
                filePath = file.absolutePath,
                dateAdded = file.lastModified()
            )
        }.sortedByDescending { it.dateAdded }
    }

    fun importFontFromUri(context: Context, uri: Uri): CustomFont? {
        return try {
            val dir = getCustomFontsDir(context)
            var fileName = "font_${System.currentTimeMillis()}"

            // Resolve real filename from content resolver if available
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val displayName = cursor.getString(nameIndex)
                            if (!displayName.isNullOrBlank()) {
                                fileName = displayName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            if (!fileName.endsWith(".ttf", true) && !fileName.endsWith(".otf", true)) {
                fileName = "$fileName.ttf"
            }

            val destFile = File(dir, fileName)
            val pending = File.createTempFile("import-", ".part", dir)
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return null
                stream.use { input -> pending.outputStream().use { input.copyTo(it) } }
                val tf = Typeface.Builder(pending).build() ?: return null
                synchronized(customTypefaceCache) {
                    Files.move(pending.toPath(), destFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
                    customTypefaceCache[destFile.absolutePath] = tf
                }
            } finally {
                pending.delete()
            }

            val displayName = destFile.nameWithoutExtension
                .replace("_", " ")
                .replace("-", " ")
                .trim()

            CustomFont(
                id = destFile.name,
                name = displayName.ifBlank { "خط مخصص" },
                filePath = destFile.absolutePath,
                dateAdded = destFile.lastModified()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deleteCustomFont(context: Context, fontId: String): Boolean {
        return try {
            val dir = getCustomFontsDir(context)
            val file = File(dir, fontId)
            customTypefaceCache.remove(file.absolutePath)
            file.delete()
        } catch (_: Exception) {
            false
        }
    }
}
