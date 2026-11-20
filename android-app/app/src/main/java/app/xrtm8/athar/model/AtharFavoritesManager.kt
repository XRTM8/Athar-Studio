package app.xrtm8.athar.model

import android.content.Context
import android.content.SharedPreferences

/**
 * High-performance persistence manager for favorite Fonts and Filters.
 * Uses atomic SharedPreferences storage for instant read and write.
 */
object AtharFavoritesManager {
    private const val PREFS_NAME = "athar_favorites_prefs"
    private const val KEY_FAVORITE_FONTS = "favorite_fonts"
    private const val KEY_FAVORITE_FILTERS = "favorite_filters"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFavoriteFonts(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_FAVORITE_FONTS, emptySet()) ?: emptySet()
    }

    fun isFontFavorite(context: Context, fontName: String): Boolean {
        return getFavoriteFonts(context).contains(fontName)
    }

    fun toggleFontFavorite(context: Context, fontName: String): Boolean {
        val current = getFavoriteFonts(context).toMutableSet()
        val isFav = if (current.contains(fontName)) {
            current.remove(fontName)
            false
        } else {
            current.add(fontName)
            true
        }
        getPrefs(context).edit().putStringSet(KEY_FAVORITE_FONTS, current).apply()
        return isFav
    }

    fun getFavoriteFilters(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_FAVORITE_FILTERS, emptySet()) ?: emptySet()
    }

    fun isFilterFavorite(context: Context, filterId: String): Boolean {
        return getFavoriteFilters(context).contains(filterId)
    }

    fun toggleFilterFavorite(context: Context, filterId: String): Boolean {
        val current = getFavoriteFilters(context).toMutableSet()
        val isFav = if (current.contains(filterId)) {
            current.remove(filterId)
            false
        } else {
            current.add(filterId)
            true
        }
        getPrefs(context).edit().putStringSet(KEY_FAVORITE_FILTERS, current).apply()
        return isFav
    }
}
