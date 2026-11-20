package app.xrtm8.athar.typography

import android.content.Context
import android.graphics.Typeface
import app.xrtm8.athar.model.ArabicFontFamily
import java.util.concurrent.ConcurrentHashMap

object AtharFontManager {
    private val typefaceCache = ConcurrentHashMap<ArabicFontFamily, Typeface>()

    fun getTypeface(context: Context, font: ArabicFontFamily): Typeface {
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
}
