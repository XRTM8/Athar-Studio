package app.xrtm8.athar.project

import android.content.Context

enum class ExportImageFormat(val label: String, val ext: String, val mime: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp")
}

data class AtharAppPreferences(
    val defaultFormat: ExportImageFormat = ExportImageFormat.JPEG,
    val defaultQuality: Int = 95,
    val hapticEnabled: Boolean = true,
    val autoGridGuides: Boolean = false,
    val autoSaveDrafts: Boolean = true
)

/**
 * Persistent user preferences for Athar Studio.
 */
object AtharAppSettings {

    private const val PREFS = "athar_app_settings"
    private const val KEY_FMT = "default_export_format"
    private const val KEY_QUALITY = "default_export_quality"
    private const val KEY_HAPTIC = "haptic_enabled"
    private const val KEY_GRID = "auto_grid_guides"
    private const val KEY_AUTOSAVE = "auto_save_drafts"

    fun load(context: Context): AtharAppPreferences {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fmtStr = p.getString(KEY_FMT, ExportImageFormat.JPEG.name) ?: ExportImageFormat.JPEG.name
        val fmt = try { ExportImageFormat.valueOf(fmtStr) } catch (_: Exception) { ExportImageFormat.JPEG }
        return AtharAppPreferences(
            defaultFormat = fmt,
            defaultQuality = p.getInt(KEY_QUALITY, 95).coerceIn(50, 100),
            hapticEnabled = p.getBoolean(KEY_HAPTIC, true),
            autoGridGuides = p.getBoolean(KEY_GRID, false),
            autoSaveDrafts = p.getBoolean(KEY_AUTOSAVE, true)
        )
    }

    fun save(context: Context, prefs: AtharAppPreferences) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_FMT, prefs.defaultFormat.name)
            .putInt(KEY_QUALITY, prefs.defaultQuality.coerceIn(50, 100))
            .putBoolean(KEY_HAPTIC, prefs.hapticEnabled)
            .putBoolean(KEY_GRID, prefs.autoGridGuides)
            .putBoolean(KEY_AUTOSAVE, prefs.autoSaveDrafts)
            .apply()
    }
}
