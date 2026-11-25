package app.xrtm8.athar.typography

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CustomFont
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.ConcurrentHashMap

object AtharFontManager {

    private data class FontCacheKey(val font: ArabicFontFamily, val weight: Int)
    private data class CustomCacheKey(val path: String, val weight: Int)

    private val typefaceCache = ConcurrentHashMap<FontCacheKey, Typeface>()
    private val customTypefaceCache = ConcurrentHashMap<CustomCacheKey, Typeface>()
    private val customMetaCache = ConcurrentHashMap<String, CustomFont>()
    private val composeFamilyCache = ConcurrentHashMap<Typeface, FontFamily>()

    private const val CUSTOM_FONTS_DIR = "custom_fonts"
    private const val META_FILE_NAME = "custom_fonts_meta.json"
    private const val MAX_FONT_FILE_BYTES = 25L * 1024L * 1024L

    fun getCustomFontsDir(context: Context): File {
        val dir = File(context.filesDir, CUSTOM_FONTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTypeface(
        context: Context,
        font: ArabicFontFamily,
        customPath: String? = null,
        fontWeight: Int = 400
    ): Typeface {
        if (!customPath.isNullOrBlank()) {
            val resolvedFile = resolveExistingCustomFile(context, customPath)
            if (resolvedFile != null) {
                val allCustom = ensureCustomMetaLoaded(context)
                val currentMeta = allCustom.find { it.filePath == resolvedFile.absolutePath }
                val targetFile = if (currentMeta != null && !currentMeta.isVariable) {
                    val siblingPath = findSiblingVariantPath(allCustom, currentMeta, fontWeight)
                    if (siblingPath != null) File(siblingPath).takeIf { it.exists() } ?: resolvedFile else resolvedFile
                } else {
                    resolvedFile
                }
                val isVariable = currentMeta?.isVariable == true
                val clampedWeight = if (isVariable && currentMeta != null) {
                    fontWeight.coerceIn(
                        currentMeta.supportedWeights.minOrNull() ?: 100,
                        currentMeta.supportedWeights.maxOrNull() ?: 900
                    )
                } else {
                    currentMeta?.weightClass ?: 400
                }
                val cacheKey = CustomCacheKey(targetFile.absolutePath, clampedWeight)
                customTypefaceCache[cacheKey]?.let { return it }
                try {
                    val tf = if (isVariable) {
                        Typeface.Builder(targetFile)
                            .setFontVariationSettings("'wght' $clampedWeight")
                            .build() ?: Typeface.createFromFile(targetFile)
                    } else {
                        Typeface.createFromFile(targetFile)
                    }
                    customTypefaceCache[cacheKey] = tf
                    return tf
                } catch (_: Exception) {
                }
            }
        }

        val resolvedWeight = font.resolveWeight(fontWeight)
        val cacheKey = FontCacheKey(font, resolvedWeight)
        return typefaceCache.computeIfAbsent(cacheKey) {
            try {
                if (font.isVariable) {
                    Typeface.Builder(context.assets, font.assetPath)
                        .setFontVariationSettings("'wght' $resolvedWeight")
                        .build() ?: Typeface.createFromAsset(context.assets, font.assetPath)
                } else {
                    val assetPath = font.assetPathForWeight(resolvedWeight)
                    Typeface.createFromAsset(context.assets, assetPath)
                }
            } catch (_: Exception) {
                try {
                    Typeface.createFromAsset(context.assets, font.assetPath)
                } catch (_: Exception) {
                    Typeface.SERIF
                }
            }
        }
    }

    fun getComposeFontFamily(
        context: Context,
        font: ArabicFontFamily,
        fontWeight: Int = font.defaultWeight
    ): FontFamily {
        val tf = getTypeface(context, font, customPath = null, fontWeight = fontWeight)
        return composeFamilyCache.computeIfAbsent(tf) { FontFamily(it) }
    }

    fun getComposeFontFamily(
        context: Context,
        customFont: CustomFont,
        fontWeight: Int = customFont.weightClass
    ): FontFamily {
        val tf = getTypeface(context, ArabicFontFamily.CAIRO, customPath = customFont.filePath, fontWeight = fontWeight)
        return composeFamilyCache.computeIfAbsent(tf) { FontFamily(it) }
    }

    fun hasRealWeight(
        context: Context,
        font: ArabicFontFamily,
        targetWeight: Int,
        customPath: String? = null
    ): Boolean {
        if (!customPath.isNullOrBlank()) {
            val resolvedFile = resolveExistingCustomFile(context, customPath) ?: return false
            val allCustom = ensureCustomMetaLoaded(context)
            val meta = allCustom.find { it.filePath == resolvedFile.absolutePath } ?: return false
            if (meta.isVariable) {
                val minW = meta.supportedWeights.minOrNull() ?: 100
                val maxW = meta.supportedWeights.maxOrNull() ?: 900
                return targetWeight in minW..maxW
            }
            if (targetWeight >= 600) {
                val variants = buildFamilyVariants(allCustom, meta)
                return variants.keys.any { it >= 600 }
            }
            return true
        }
        return font.hasRealWeight(targetWeight)
    }

    fun getSupportedWeights(
        context: Context,
        font: ArabicFontFamily,
        customPath: String? = null
    ): List<Int> {
        if (!customPath.isNullOrBlank()) {
            val resolvedFile = resolveExistingCustomFile(context, customPath)
            if (resolvedFile != null) {
                val allCustom = ensureCustomMetaLoaded(context)
                val meta = allCustom.find { it.filePath == resolvedFile.absolutePath }
                if (meta != null) {
                    return if (meta.isVariable) meta.supportedWeights
                    else buildFamilyVariants(allCustom, meta).keys.sorted()
                }
            }
        }
        return font.supportedWeights
    }

    fun getSupportedOpenTypeFeatures(
        context: Context,
        font: ArabicFontFamily,
        customPath: String? = null
    ): Set<String> {
        if (!customPath.isNullOrBlank()) {
            val resolvedFile = resolveExistingCustomFile(context, customPath)
            if (resolvedFile != null) {
                val meta = ensureCustomMetaLoaded(context).find { it.filePath == resolvedFile.absolutePath }
                if (meta != null) return meta.supportedOpenTypeFeatures
            }
        }
        return font.supportedOpenTypeFeatures
    }

    fun supportsTatweel(
        context: Context,
        font: ArabicFontFamily,
        customPath: String? = null
    ): Boolean {
        if (!customPath.isNullOrBlank()) {
            val resolvedFile = resolveExistingCustomFile(context, customPath)
            if (resolvedFile != null) {
                val meta = ensureCustomMetaLoaded(context).find { it.filePath == resolvedFile.absolutePath }
                if (meta != null) return meta.supportsTatweel
            }
        }
        return font.supportsTatweel
    }

    fun preload(context: Context) {
        ArabicFontFamily.entries.forEach { font ->
            getTypeface(context, font, fontWeight = font.defaultWeight)
        }
    }

    fun listCustomFonts(context: Context): List<CustomFont> {
        val raw = ensureCustomMetaLoaded(context)
        return raw.map { cf ->
            if (cf.isVariable) cf
            else {
                val variants = buildFamilyVariants(raw, cf)
                cf.copy(
                    supportedWeights = variants.keys.sorted(),
                    weightVariants = variants
                )
            }
        }.sortedByDescending { it.dateAdded }
    }

    fun importFontFromUri(context: Context, uri: Uri): CustomFont? {
        return try {
            val dir = getCustomFontsDir(context)
            var rawBaseName = "font_${System.currentTimeMillis()}"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val displayName = cursor.getString(nameIndex)
                            if (!displayName.isNullOrBlank()) {
                                rawBaseName = displayName
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }

            val pending = File.createTempFile("import-", ".part", dir)
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return null
                stream.use { input ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    pending.outputStream().use { out ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_FONT_FILE_BYTES) return null
                            out.write(buffer, 0, read)
                        }
                    }
                }
                installValidatedFontFile(context, pending, rawBaseName)
            } finally {
                pending.delete()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Installs a font extracted from a `.athar` project archive into `custom_fonts/`.
     * Validates SFNT binary headers + `Typeface.Builder` and deduplicates by SHA-256 content hash.
     */
    fun importFontFileForProject(context: Context, sourceFile: File, originalFileName: String): CustomFont? {
        return try {
            if (!sourceFile.exists() || sourceFile.length() > MAX_FONT_FILE_BYTES) return null
            installValidatedFontFile(context, sourceFile, originalFileName)
        } catch (_: Exception) {
            null
        }
    }

    private fun installValidatedFontFile(context: Context, sourceFile: File, rawFileName: String): CustomFont? {
        val dir = getCustomFontsDir(context)
        val bytes = sourceFile.readBytes()
        val parsed = SfntFontParser.parse(bytes) ?: return null
        val tf = Typeface.Builder(sourceFile).build() ?: return null

        val existing = ensureCustomMetaLoaded(context)
        existing.find { it.sha256Short == parsed.sha256Short && File(it.filePath).exists() }?.let { match ->
            return match
        }

        val cleanStem = rawFileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBeforeLast('.')
            .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_')
            .ifBlank { parsed.familyName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]+"), "_") }
            .ifBlank { "custom_font" }
            .take(48)

        val storageFileName = "${cleanStem}_${parsed.sha256Short}.${parsed.extension}"
        val destFile = File(dir, storageFileName)

        val cleanDisplayName = parsed.familyName
            .replace("_", " ")
            .trim()
            .ifBlank { cleanStem.replace("_", " ") }
            .ifBlank { "خط مخصص" }

        synchronized(customTypefaceCache) {
            Files.move(sourceFile.toPath(), destFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            val cacheKey = CustomCacheKey(destFile.absolutePath, parsed.weightClass)
            customTypefaceCache[cacheKey] = tf
            val created = CustomFont(
                id = destFile.name,
                name = cleanDisplayName,
                filePath = destFile.absolutePath,
                dateAdded = destFile.lastModified(),
                familyName = parsed.familyName,
                format = parsed.format,
                weightClass = parsed.weightClass,
                isVariable = parsed.isVariable,
                supportedWeights = parsed.supportedWeights,
                weightVariants = mapOf(parsed.weightClass to destFile.absolutePath),
                supportsArabic = parsed.supportsArabic,
                supportsTatweel = parsed.supportsTatweel,
                supportedOpenTypeFeatures = parsed.openTypeFeatures,
                sha256Short = parsed.sha256Short
            )
            customMetaCache[destFile.name] = created
            persistMetaIndex(context)
            return created
        }
    }

    fun deleteCustomFont(context: Context, fontId: String): Boolean {
        return try {
            val dir = getCustomFontsDir(context)
            val file = File(dir, fontId)
            synchronized(customTypefaceCache) {
                val abs = file.absolutePath
                customTypefaceCache.keys.filter { it.path == abs }.forEach {
                    val removed = customTypefaceCache.remove(it)
                    if (removed != null) composeFamilyCache.remove(removed)
                }
                customMetaCache.remove(fontId)
                val deleted = file.delete()
                persistMetaIndex(context)
                deleted
            }
        } catch (_: Exception) {
            false
        }
    }

    fun resolveExistingCustomFile(context: Context, customPath: String): File? {
        val direct = File(customPath)
        if (direct.exists() && direct.isFile) return direct
        val fallbackName = customPath.substringAfterLast('/').substringAfterLast('\\')
        if (fallbackName.isNotBlank()) {
            val local = File(getCustomFontsDir(context), fallbackName)
            if (local.exists() && local.isFile) return local
        }
        return null
    }

    private fun findSiblingVariantPath(all: List<CustomFont>, current: CustomFont, targetWeight: Int): String? {
        val variants = buildFamilyVariants(all, current)
        if (variants.isEmpty()) return null
        return variants[targetWeight]
            ?: variants.entries.minByOrNull { kotlin.math.abs(it.key - targetWeight) }?.value
    }

    private fun buildFamilyVariants(all: List<CustomFont>, current: CustomFont): Map<Int, String> {
        val normalizedFamily = normalizeFamilyKey(current.familyName)
        val siblings = all.filter { normalizeFamilyKey(it.familyName) == normalizedFamily && !it.isVariable }
        if (siblings.isEmpty()) return mapOf(current.weightClass to current.filePath)
        val map = LinkedHashMap<Int, String>()
        siblings.sortedBy { it.dateAdded }.forEach { map[it.weightClass] = it.filePath }
        return map
    }

    private fun normalizeFamilyKey(name: String): String =
        name.lowercase().replace(Regex("[\\s_-]+"), "")

    private fun ensureCustomMetaLoaded(context: Context): List<CustomFont> {
        val dir = getCustomFontsDir(context)
        val files = dir.listFiles { _, name -> name.endsWith(".ttf", true) || name.endsWith(".otf", true) }
            ?: return emptyList()

        if (customMetaCache.isEmpty()) {
            loadMetaIndex(dir)
        }

        val activeIds = files.map { it.name }.toSet()
        var dirty = false
        customMetaCache.keys.filter { it !in activeIds }.forEach {
            customMetaCache.remove(it)
            dirty = true
        }

        for (file in files) {
            if (!customMetaCache.containsKey(file.name)) {
                val parsed = try { SfntFontParser.parse(file.readBytes()) } catch (_: Exception) { null }
                val fallbackName = file.nameWithoutExtension.replace("_", " ").replace("-", " ").trim().ifBlank { "خط مخصص" }
                val cf = if (parsed != null) {
                    CustomFont(
                        id = file.name,
                        name = parsed.familyName.ifBlank { fallbackName },
                        filePath = file.absolutePath,
                        dateAdded = file.lastModified(),
                        familyName = parsed.familyName.ifBlank { fallbackName },
                        format = parsed.format,
                        weightClass = parsed.weightClass,
                        isVariable = parsed.isVariable,
                        supportedWeights = parsed.supportedWeights,
                        weightVariants = mapOf(parsed.weightClass to file.absolutePath),
                        supportsArabic = parsed.supportsArabic,
                        supportsTatweel = parsed.supportsTatweel,
                        supportedOpenTypeFeatures = parsed.openTypeFeatures,
                        sha256Short = parsed.sha256Short
                    )
                } else {
                    CustomFont(
                        id = file.name,
                        name = fallbackName,
                        filePath = file.absolutePath,
                        dateAdded = file.lastModified()
                    )
                }
                customMetaCache[file.name] = cf
                dirty = true
            }
        }
        if (dirty) persistMetaIndex(context)
        return customMetaCache.values.toList()
    }

    private fun loadMetaIndex(dir: File) {
        val metaFile = File(dir, META_FILE_NAME)
        if (!metaFile.exists()) return
        try {
            val arr = JSONArray(metaFile.readText(Charsets.UTF_8))
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id", "").trim()
                if (id.isEmpty()) continue
                val file = File(dir, id)
                if (!file.exists()) continue
                val weightsArr = o.optJSONArray("weights")
                val weights = if (weightsArr != null) {
                    (0 until weightsArr.length()).map { weightsArr.optInt(it, 400) }.ifEmpty { listOf(o.optInt("weight_class", 400)) }
                } else listOf(o.optInt("weight_class", 400))
                val otArr = o.optJSONArray("ot_features")
                val otSet = if (otArr != null) {
                    (0 until otArr.length()).mapNotNull { otArr.optString(it, "").ifBlank { null } }.toSet()
                } else emptySet()
                val wc = o.optInt("weight_class", 400)
                customMetaCache[id] = CustomFont(
                    id = id,
                    name = o.optString("name", id),
                    filePath = file.absolutePath,
                    dateAdded = o.optLong("date_added", file.lastModified()),
                    familyName = o.optString("family_name", o.optString("name", id)),
                    format = o.optString("format", if (id.endsWith(".otf", true)) "OTF" else "TTF"),
                    weightClass = wc,
                    isVariable = o.optBoolean("is_variable", false),
                    supportedWeights = weights,
                    weightVariants = mapOf(wc to file.absolutePath),
                    supportsArabic = o.optBoolean("supports_arabic", true),
                    supportsTatweel = o.optBoolean("supports_tatweel", true),
                    supportedOpenTypeFeatures = otSet,
                    sha256Short = o.optString("sha256", "")
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun persistMetaIndex(context: Context) {
        try {
            val dir = getCustomFontsDir(context)
            val metaFile = File(dir, META_FILE_NAME)
            val arr = JSONArray()
            customMetaCache.values.forEach { cf ->
                arr.put(JSONObject().apply {
                    put("id", cf.id)
                    put("name", cf.name)
                    put("family_name", cf.familyName)
                    put("format", cf.format)
                    put("weight_class", cf.weightClass)
                    put("is_variable", cf.isVariable)
                    put("weights", JSONArray(cf.supportedWeights))
                    put("supports_arabic", cf.supportsArabic)
                    put("supports_tatweel", cf.supportsTatweel)
                    put("ot_features", JSONArray(cf.supportedOpenTypeFeatures.toList()))
                    put("sha256", cf.sha256Short)
                    put("date_added", cf.dateAdded)
                })
            }
            val tmp = File.createTempFile("meta-", ".part", dir)
            try {
                tmp.writeText(arr.toString(), Charsets.UTF_8)
                Files.move(tmp.toPath(), metaFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            } finally {
                tmp.delete()
            }
        } catch (_: Exception) {
        }
    }
}
