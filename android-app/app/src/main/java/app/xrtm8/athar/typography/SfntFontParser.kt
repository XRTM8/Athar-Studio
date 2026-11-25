package app.xrtm8.athar.typography

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * Pure-Kotlin OpenType / TrueType SFNT binary metadata parser.
 * Validates binary magic headers (`TTF` vs `OTF`), extracts real family/subfamily names,
 * OS/2 weights, variable `wght` axis bounds, GSUB features, and Arabic + Tatweel (`U+0640`) coverage.
 */
object SfntFontParser {

    data class SfntMetadata(
        val format: String,
        val extension: String,
        val familyName: String,
        val subfamilyName: String,
        val fullName: String,
        val weightClass: Int,
        val isVariable: Boolean,
        val minWeight: Int,
        val maxWeight: Int,
        val supportedWeights: List<Int>,
        val supportsArabic: Boolean,
        val supportsTatweel: Boolean,
        val openTypeFeatures: Set<String>,
        val sha256Short: String
    )

    private data class TableSlice(val offset: Int, val length: Int)

    private val STANDARD_STOPS = listOf(200, 300, 400, 500, 600, 700, 800, 900)
    private val ARABIC_PROBE_CODEPOINTS = intArrayOf(0x0627, 0x0628, 0x0644, 0x0645)

    fun isSupportedSfntHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val tag = u32(bytes, 0)
        return tag == 0x00010000L || tag == 0x74727565L || tag == 0x4F54544FL
    }

    fun parse(bytes: ByteArray): SfntMetadata? {
        if (bytes.size < 12) return null
        val sfntTag = u32(bytes, 0)
        val (format, extension) = when (sfntTag) {
            0x00010000L, 0x74727565L -> "TTF" to "ttf"
            0x4F54544FL -> "OTF" to "otf"
            else -> return null
        }

        val numTables = u16(bytes, 4)
        if (numTables <= 0 || bytes.size < 12 + numTables * 16) return null

        val tables = HashMap<String, TableSlice>(numTables)
        for (i in 0 until numTables) {
            val recOff = 12 + i * 16
            val tag = String(bytes, recOff, 4, StandardCharsets.ISO_8859_1)
            val offset = u32(bytes, recOff + 8)
            val length = u32(bytes, recOff + 12)
            if (offset >= 0 && length >= 0 && offset + length <= bytes.size) {
                tables[tag] = TableSlice(offset.toInt(), length.toInt())
            }
        }

        if (!tables.containsKey("head") || !tables.containsKey("cmap")) return null

        val names = tables["name"]?.let { parseNameTable(bytes, it) } ?: emptyMap()
        val rawFamily = (names[16]?.takeIf { it.isNotBlank() } ?: names[1].orEmpty()).trim()
        val subfamily = (names[17]?.takeIf { it.isNotBlank() } ?: names[2].orEmpty()).trim()
        val fullName = names[4].orEmpty().trim()
        val familyName = rawFamily.ifBlank { fullName }.ifBlank { "Custom Font" }

        val os2Weight = tables["OS/2"]?.let { parseOs2Weight(bytes, it) } ?: 400
        val weightClass = resolveEffectiveWeight(os2Weight, subfamily, fullName)

        val wghtBounds = tables["fvar"]?.let { parseFvarWghtBounds(bytes, it) }
        val isVariable = wghtBounds != null
        val minW = wghtBounds?.first ?: weightClass
        val maxW = wghtBounds?.second ?: weightClass
        val supportedWeights = if (isVariable) {
            val stops = STANDARD_STOPS.filter { it in minW..maxW }.toMutableList()
            if (stops.isEmpty()) stops.add(weightClass)
            stops.distinct().sorted()
        } else {
            listOf(weightClass)
        }

        val openTypeFeatures = tables["GSUB"]?.let { parseGsubFeatures(bytes, it) } ?: emptySet()
        val cmapSlice = tables["cmap"]!!
        val supportsArabic = ARABIC_PROBE_CODEPOINTS.any { hasGlyph(bytes, cmapSlice, it) }
        val supportsTatweel = hasGlyph(bytes, cmapSlice, 0x0640)

        return SfntMetadata(
            format = format,
            extension = extension,
            familyName = familyName,
            subfamilyName = subfamily,
            fullName = fullName,
            weightClass = weightClass,
            isVariable = isVariable,
            minWeight = minW,
            maxWeight = maxW,
            supportedWeights = supportedWeights,
            supportsArabic = supportsArabic,
            supportsTatweel = supportsTatweel,
            openTypeFeatures = openTypeFeatures,
            sha256Short = sha256Hex8(bytes)
        )
    }

    private fun parseNameTable(bytes: ByteArray, slice: TableSlice): Map<Int, String> {
        if (slice.length < 6) return emptyMap()
        val count = u16(bytes, slice.offset + 2)
        val stringStorageOff = slice.offset + u16(bytes, slice.offset + 4)
        if (slice.offset + 6 + count * 12 > slice.offset + slice.length) return emptyMap()

        val result = HashMap<Int, Pair<Int, String>>()
        for (i in 0 until count) {
            val rec = slice.offset + 6 + i * 12
            val platformId = u16(bytes, rec)
            val encodingId = u16(bytes, rec + 2)
            val languageId = u16(bytes, rec + 4)
            val nameId = u16(bytes, rec + 6)
            val strLen = u16(bytes, rec + 8)
            val strOff = stringStorageOff + u16(bytes, rec + 10)
            if (nameId !in intArrayOf(1, 2, 4, 16, 17)) continue
            if (strLen <= 0 || strOff < slice.offset || strOff + strLen > slice.offset + slice.length) continue

            val priority = when {
                platformId == 3 && (encodingId == 1 || encodingId == 10) && languageId == 0x0409 -> 100
                platformId == 3 && (encodingId == 1 || encodingId == 10) && (languageId and 0xFF) == 0x01 -> 95
                platformId == 3 && (encodingId == 1 || encodingId == 10) -> 80
                platformId == 0 -> 70
                platformId == 1 && languageId == 0 -> 50
                else -> 10
            }

            val decoded = when (platformId) {
                0, 3 -> String(bytes, strOff, strLen, StandardCharsets.UTF_16BE)
                else -> String(bytes, strOff, strLen, StandardCharsets.ISO_8859_1)
            }.replace("\u0000", "").trim()

            if (decoded.isNotEmpty()) {
                val existing = result[nameId]
                if (existing == null || priority > existing.first) {
                    result[nameId] = priority to decoded
                }
            }
        }
        return result.mapValues { it.value.second }
    }

    private fun parseOs2Weight(bytes: ByteArray, slice: TableSlice): Int {
        if (slice.length < 6) return 400
        val w = u16(bytes, slice.offset + 4)
        return if (w in 100..1000) w else 400
    }

    private fun resolveEffectiveWeight(os2Weight: Int, subfamily: String, fullName: String): Int {
        val hint = "$subfamily $fullName".lowercase()
        val inferred = when {
            "thin" in hint || "hairline" in hint -> 100
            "extralight" in hint || "extra light" in hint || "ultralight" in hint -> 200
            "light" in hint -> 300
            "medium" in hint -> 500
            "semibold" in hint || "semi bold" in hint || "demibold" in hint -> 600
            "extrabold" in hint || "extra bold" in hint || "ultrabold" in hint -> 800
            "black" in hint || "heavy" in hint -> 900
            "bold" in hint -> 700
            else -> null
        }
        val normalizedOs2 = when {
            os2Weight == 275 -> 200
            os2Weight < 150 -> 100
            else -> ((os2Weight / 100.0).roundToInt() * 100).coerceIn(100, 900)
        }
        return if (os2Weight == 400 && inferred != null) inferred else normalizedOs2
    }

    private fun parseFvarWghtBounds(bytes: ByteArray, slice: TableSlice): Pair<Int, Int>? {
        if (slice.length < 16) return null
        val axesOff = u16(bytes, slice.offset + 4)
        val axisCount = u16(bytes, slice.offset + 8)
        val axisSize = u16(bytes, slice.offset + 10)
        if (axisCount <= 0 || axisSize < 20) return null
        val start = slice.offset + axesOff
        if (start + axisCount * axisSize > slice.offset + slice.length) return null

        for (i in 0 until axisCount) {
            val aOff = start + i * axisSize
            val tag = String(bytes, aOff, 4, StandardCharsets.ISO_8859_1)
            if (tag == "wght") {
                val minVal = (i32(bytes, aOff + 4) / 65536.0).roundToInt().coerceIn(100, 1000)
                val maxVal = (i32(bytes, aOff + 12) / 65536.0).roundToInt().coerceIn(100, 1000)
                return minVal.coerceAtMost(maxVal) to maxVal.coerceAtLeast(minVal)
            }
        }
        return null
    }

    private fun parseGsubFeatures(bytes: ByteArray, slice: TableSlice): Set<String> {
        if (slice.length < 10) return emptySet()
        val featOff = slice.offset + u16(bytes, slice.offset + 6)
        if (featOff + 2 > slice.offset + slice.length) return emptySet()
        val featCount = u16(bytes, featOff)
        if (featOff + 2 + featCount * 6 > slice.offset + slice.length) return emptySet()

        val result = HashSet<String>()
        for (i in 0 until featCount) {
            val tag = String(bytes, featOff + 2 + i * 6, 4, StandardCharsets.ISO_8859_1)
            if (tag == "swsh" || tag == "dlig" || (tag.startsWith("ss") && tag.length == 4 && tag[2].isDigit() && tag[3].isDigit())) {
                result.add(tag)
            }
        }
        return result
    }

    private fun hasGlyph(bytes: ByteArray, slice: TableSlice, cp: Int): Boolean {
        if (slice.length < 4) return false
        val numSubtables = u16(bytes, slice.offset + 2)
        if (slice.offset + 4 + numSubtables * 8 > slice.offset + slice.length) return false

        var chosenSubOffset = -1
        var bestPriority = -1
        for (i in 0 until numSubtables) {
            val rec = slice.offset + 4 + i * 8
            val pid = u16(bytes, rec)
            val eid = u16(bytes, rec + 2)
            val relOff = u32(bytes, rec + 4).toInt()
            if (relOff < 0 || slice.offset + relOff + 4 > slice.offset + slice.length) continue
            val fmt = u16(bytes, slice.offset + relOff)
            if (fmt != 4 && fmt != 12) continue
            val priority = when {
                pid == 3 && eid == 10 && fmt == 12 -> 100
                pid == 0 && eid == 4 && fmt == 12 -> 95
                pid == 3 && eid == 1 -> 85
                pid == 0 -> 75
                else -> -1
            }
            if (priority > bestPriority) {
                bestPriority = priority
                chosenSubOffset = slice.offset + relOff
            }
        }
        if (chosenSubOffset < 0) return false

        val format = u16(bytes, chosenSubOffset)
        return when (format) {
            4 -> hasGlyphFormat4(bytes, chosenSubOffset, slice.offset + slice.length, cp)
            12 -> hasGlyphFormat12(bytes, chosenSubOffset, slice.offset + slice.length, cp)
            else -> false
        }
    }

    private fun hasGlyphFormat4(bytes: ByteArray, subOff: Int, limit: Int, cp: Int): Boolean {
        if (cp > 0xFFFF || subOff + 16 > limit) return false
        val segCountX2 = u16(bytes, subOff + 6)
        val segCount = segCountX2 / 2
        val endBase = subOff + 14
        val startBase = endBase + segCountX2 + 2
        val deltaBase = startBase + segCountX2
        val rangeBase = deltaBase + segCountX2
        if (rangeBase + segCountX2 > limit) return false

        for (i in 0 until segCount) {
            val endCode = u16(bytes, endBase + i * 2)
            val startCode = u16(bytes, startBase + i * 2)
            if (cp in startCode..endCode) {
                val idDelta = i16(bytes, deltaBase + i * 2)
                val idRangeOffset = u16(bytes, rangeBase + i * 2)
                val gid = if (idRangeOffset == 0) {
                    (cp + idDelta) and 0xFFFF
                } else {
                    val gidPos = rangeBase + i * 2 + idRangeOffset + (cp - startCode) * 2
                    if (gidPos + 2 > limit) return false
                    val raw = u16(bytes, gidPos)
                    if (raw == 0) 0 else (raw + idDelta) and 0xFFFF
                }
                return gid != 0
            }
        }
        return false
    }

    private fun hasGlyphFormat12(bytes: ByteArray, subOff: Int, limit: Int, cp: Int): Boolean {
        if (subOff + 16 > limit) return false
        val numGroups = u32(bytes, subOff + 12).toInt().coerceAtLeast(0)
        val groupBase = subOff + 16
        if (groupBase + numGroups.toLong() * 12L > limit.toLong()) return false

        var lo = 0
        var hi = numGroups - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val gOff = groupBase + mid * 12
            val startCode = u32(bytes, gOff)
            val endCode = u32(bytes, gOff + 4)
            when {
                cp < startCode -> hi = mid - 1
                cp > endCode -> lo = mid + 1
                else -> {
                    val startGid = u32(bytes, gOff + 8)
                    return (startGid + (cp - startCode)) != 0L
                }
            }
        }
        return false
    }

    private fun sha256Hex8(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return buildString(8) {
            for (i in 0 until 4) {
                append(((digest[i].toInt() ushr 4) and 0xF).toString(16))
                append((digest[i].toInt() and 0xF).toString(16))
            }
        }
    }

    private fun u16(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    private fun i16(b: ByteArray, off: Int): Int =
        ByteBuffer.wrap(b, off, 2).order(ByteOrder.BIG_ENDIAN).short.toInt()

    private fun u32(b: ByteArray, off: Int): Long =
        ((b[off].toLong() and 0xFF) shl 24) or
            ((b[off + 1].toLong() and 0xFF) shl 16) or
            ((b[off + 2].toLong() and 0xFF) shl 8) or
            (b[off + 3].toLong() and 0xFF)

    private fun i32(b: ByteArray, off: Int): Int =
        ByteBuffer.wrap(b, off, 4).order(ByteOrder.BIG_ENDIAN).int
}
