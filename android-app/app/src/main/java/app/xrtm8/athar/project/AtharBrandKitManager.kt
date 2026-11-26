package app.xrtm8.athar.project

import android.content.Context
import org.json.JSONArray

/**
 * Local Brand Kit persistence for saving custom creator color swatches.
 */
object AtharBrandKitManager {

    private const val PREFS_NAME = "athar_brand_kit"
    private const val KEY_COLORS = "brand_colors_json"

    fun loadBrandColors(context: Context): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_COLORS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getLong(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addBrandColor(context: Context, colorLong: Long): List<Long> {
        val current = loadBrandColors(context).toMutableList()
        current.remove(colorLong)
        current.add(0, colorLong)
        val trimmed = current.take(16)
        saveColors(context, trimmed)
        return trimmed
    }

    fun removeBrandColor(context: Context, colorLong: Long): List<Long> {
        val updated = loadBrandColors(context).filter { it != colorLong }
        saveColors(context, updated)
        return updated
    }

    private fun saveColors(context: Context, colors: List<Long>) {
        val arr = JSONArray()
        colors.forEach { arr.put(it) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COLORS, arr.toString())
            .apply()
    }
}
