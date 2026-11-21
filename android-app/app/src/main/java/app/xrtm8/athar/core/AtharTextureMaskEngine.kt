package app.xrtm8.athar.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import app.xrtm8.athar.model.TextureMaskPreset
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

/**
 * High-performance, zero-slop procedural luxury texture engine.
 * Generates compact, tileable high-resolution material textures for clipping mask typography.
 * Textures are cached in memory as reusable Bitmaps, eliminating garbage collection churn.
 */
object AtharTextureMaskEngine {

    private const val TILE_SIZE = 512
    private val textureCache = ConcurrentHashMap<TextureMaskPreset, Bitmap>()

    fun getTexture(context: Context, preset: TextureMaskPreset, customUri: String? = null): Bitmap? {
        if (preset == TextureMaskPreset.NONE) return null

        if (preset == TextureMaskPreset.CUSTOM_PHOTO && !customUri.isNullOrEmpty()) {
            return loadCustomPhoto(context, customUri)
        }

        return textureCache.getOrPut(preset) {
            generatePresetTexture(preset)
        }
    }

    private fun generatePresetTexture(preset: TextureMaskPreset): Bitmap {
        val bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (preset) {
            TextureMaskPreset.GOLD_LEAF -> {
                // 1. Rich 24K gold base gradient
                paint.shader = LinearGradient(
                    0f, 0f, TILE_SIZE.toFloat(), TILE_SIZE.toFloat(),
                    intArrayOf(
                        0xFF4A3500.toInt(),
                        0xFFD4AF37.toInt(),
                        0xFFFFF2A3.toInt(),
                        0xFFF3C644.toInt(),
                        0xFF996515.toInt(),
                        0xFFFFEDB3.toInt(),
                        0xFF5E3A00.toInt()
                    ),
                    floatArrayOf(0f, 0.2f, 0.35f, 0.55f, 0.75f, 0.88f, 1f),
                    Shader.TileMode.MIRROR
                )
                canvas.drawRect(0f, 0f, TILE_SIZE.toFloat(), TILE_SIZE.toFloat(), paint)

                // 2. Micro metallic leaf striations
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                for (y in 0 until TILE_SIZE step 6) {
                    val alpha = (40 + (sin(y * 0.1) * 25)).toInt().coerceIn(15, 75)
                    paint.color = Color.argb(alpha, 255, 245, 200)
                    canvas.drawLine(0f, y.toFloat(), TILE_SIZE.toFloat(), y + (sin(y * 0.05) * 8).toFloat(), paint)
                }
            }

            TextureMaskPreset.CARRARA_MARBLE -> {
                // 1. Pristine off-white Carrara marble base
                paint.shader = LinearGradient(
                    0f, 0f, 0f, TILE_SIZE.toFloat(),
                    intArrayOf(0xFFF9F9FB.toInt(), 0xFFEDEFEF.toInt(), 0xFFF2F4F5.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, TILE_SIZE.toFloat(), TILE_SIZE.toFloat(), paint)

                // 2. Organic gray marble veins
                paint.shader = null
                paint.style = Paint.Style.STROKE
                val veinColors = listOf(0x446B7280.toInt(), 0x2A9CA3AF.toInt(), 0x334B5563.toInt())
                for (i in 0 until 5) {
                    paint.color = veinColors[i % veinColors.size]
                    paint.strokeWidth = (2.0f + (i % 3) * 1.5f)
                    var curX = (i * 95f)
                    var curY = 0f
                    while (curY < TILE_SIZE) {
                        val nextX = curX + (sin(curY * 0.04) * 25).toFloat()
                        val nextY = curY + 20f
                        canvas.drawLine(curX, curY, nextX, nextY, paint)
                        curX = nextX
                        curY = nextY
                    }
                }
            }

            TextureMaskPreset.DAMASCUS_PARCHMENT -> {
                // 1. Ancient warm manuscript parchment base
                paint.shader = RadialGradient(
                    TILE_SIZE / 2f, TILE_SIZE / 2f, TILE_SIZE * 0.7f,
                    intArrayOf(0xFFFBF4E2.toInt(), 0xFFEAD8B7.toInt(), 0xFFC9B086.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, TILE_SIZE.toFloat(), TILE_SIZE.toFloat(), paint)

                // 2. Pulp fibers and vintage specks
                paint.shader = null
                paint.style = Paint.Style.FILL
                for (i in 0 until 120) {
                    val px = (i * 37) % TILE_SIZE
                    val py = (i * 53) % TILE_SIZE
                    val size = (1.2f + (i % 3) * 0.8f)
                    paint.color = Color.argb(45, 120, 80, 40)
                    canvas.drawCircle(px.toFloat(), py.toFloat(), size, paint)
                }
            }

            TextureMaskPreset.EMERALD_MALACHITE -> {
                // 1. Deep emerald malachite stone bands
                paint.shader = LinearGradient(
                    0f, 0f, TILE_SIZE.toFloat(), TILE_SIZE * 0.6f,
                    intArrayOf(
                        0xFF06281E.toInt(),
                        0xFF0D5C3A.toInt(),
                        0xFF00E676.toInt(),
                        0xFF0A3D27.toInt(),
                        0xFF1DE9B6.toInt(),
                        0xFF041C15.toInt()
                    ),
                    floatArrayOf(0f, 0.25f, 0.45f, 0.65f, 0.85f, 1f),
                    Shader.TileMode.MIRROR
                )
                canvas.drawRect(0f, 0f, TILE_SIZE.toFloat(), TILE_SIZE.toFloat(), paint)

                // 2. Concentric mineral bands
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.0f
                for (r in 40 until TILE_SIZE step 14) {
                    val alpha = (50 + (sin(r * 0.08) * 35)).toInt().coerceIn(20, 95)
                    paint.color = Color.argb(alpha, 167, 255, 235)
                    canvas.drawCircle(TILE_SIZE * 0.3f, TILE_SIZE * 0.4f, r.toFloat(), paint)
                }
            }

            TextureMaskPreset.BRUSHED_TITANIUM -> {
                // 1. Industrial platinum/titanium base
                paint.shader = LinearGradient(
                    0f, 0f, 0f, TILE_SIZE.toFloat(),
                    intArrayOf(0xFF33373B.toInt(), 0xFF7D8691.toInt(), 0xFFCFD8DC.toInt(), 0xFF455A64.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, TILE_SIZE.toFloat(), TILE_SIZE.toFloat(), paint)

                // 2. High density horizontal brushing
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.0f
                for (y in 0 until TILE_SIZE step 2) {
                    val alpha = (25 + (sin(y * 0.25) * 20)).toInt().coerceIn(10, 50)
                    paint.color = if (y % 4 == 0) Color.argb(alpha, 255, 255, 255) else Color.argb(alpha, 0, 0, 0)
                    canvas.drawLine(0f, y.toFloat(), TILE_SIZE.toFloat(), y.toFloat(), paint)
                }
            }

            else -> {
                // Transparent fallback
                canvas.drawColor(Color.TRANSPARENT)
            }
        }

        return bitmap
    }

    private fun loadCustomPhoto(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            var sample = 1
            while ((opts.outHeight / sample) > 1024 || (opts.outWidth / sample) > 1024) {
                sample *= 2
            }
            val finalOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, finalOpts)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        textureCache.values.forEach { if (!it.isRecycled) it.recycle() }
        textureCache.clear()
    }
}
