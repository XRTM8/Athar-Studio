package app.xrtm8.athar.core

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import java.util.Random

/**
 * High-performance 35mm Analog Film Grain Engine.
 * Synthesizes organic Gaussian film grain using a seamless hardware-tiled
 * micro-texture with PorterDuff blending, ensuring zero runtime GC allocations.
 */
object FilmGrainEngine {

    private const val TILE_SIZE = 128
    private var cachedTile: Bitmap? = null
    private var cachedShader: BitmapShader? = null

    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        isFilterBitmap = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
    }

    private fun getGrainShader(): BitmapShader {
        val currentShader = cachedShader
        val currentTile = cachedTile
        if (currentShader != null && currentTile != null && !currentTile.isRecycled) {
            return currentShader
        }

        val tile = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(TILE_SIZE * TILE_SIZE)
        val random = Random(42L) // Consistent organic seed

        for (i in pixels.indices) {
            // Gaussian distributed luminance around neutral 128
            val gaussian = (random.nextGaussian() * 32.0).toInt().coerceIn(-120, 120)
            val v = (128 + gaussian).coerceIn(0, 255)
            pixels[i] = Color.argb(255, v, v, v)
        }
        tile.setPixels(pixels, 0, TILE_SIZE, 0, 0, TILE_SIZE, TILE_SIZE)

        val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        cachedTile = tile
        cachedShader = shader
        return shader
    }

    fun drawFilmGrain(
        canvas: Canvas,
        width: Float,
        height: Float,
        intensity: Float
    ) {
        if (intensity <= 0.01f) return

        val shader = getGrainShader()
        grainPaint.shader = shader
        grainPaint.alpha = (intensity.coerceIn(0f, 1f) * 160f).toInt().coerceIn(0, 255)

        canvas.drawRect(0f, 0f, width, height, grainPaint)
    }
}
