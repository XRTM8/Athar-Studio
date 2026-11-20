package app.xrtm8.athar.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject
import java.util.EnumMap

data class AtharFilterRecipe(
    val version: Int = 1,
    val name: String = "Athar Cinematic",
    val filmPresetId: String = "fuji_classic_chrome",
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val vignette: Float = 0f,
    val blur: Float = 0f,
    val scrimEnabled: Boolean = false,
    val scrimDirection: String = "BOTTOM_TO_TOP",
    val scrimColorLong: Long = 0xFF000000,
    val scrimOpacity: Float = 0.65f
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("athar_filter", version)
            put("name", name)
            put("film", filmPresetId)
            put("bright", brightness.toDouble())
            put("cont", contrast.toDouble())
            put("sat", saturation.toDouble())
            put("vig", vignette.toDouble())
            put("blur", blur.toDouble())
            put("scrim_en", scrimEnabled)
            put("scrim_dir", scrimDirection)
            put("scrim_col", scrimColorLong)
            put("scrim_op", scrimOpacity.toDouble())
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): AtharFilterRecipe? {
            return try {
                val obj = JSONObject(jsonStr)
                if (!obj.has("athar_filter") && !obj.has("film")) return null
                AtharFilterRecipe(
                    version = obj.optInt("athar_filter", 1),
                    name = obj.optString("name", "فلتر مخصص"),
                    filmPresetId = obj.optString("film", "none"),
                    brightness = obj.optDouble("bright", 0.0).toFloat(),
                    contrast = obj.optDouble("cont", 1.0).toFloat(),
                    saturation = obj.optDouble("sat", 1.0).toFloat(),
                    vignette = obj.optDouble("vig", 0.0).toFloat(),
                    blur = obj.optDouble("blur", 0.0).toFloat(),
                    scrimEnabled = obj.optBoolean("scrim_en", false),
                    scrimDirection = obj.optString("scrim_dir", "BOTTOM_TO_TOP"),
                    scrimColorLong = obj.optLong("scrim_col", 0xFF000000),
                    scrimOpacity = obj.optDouble("scrim_op", 0.65).toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Universal Filter Recipe QR Code Engine:
 * - Generates standalone QR code Bitmaps & Polarr/Lightroom style recipe cards
 * - Decodes QR codes from gallery images using ZXing
 */
object AtharPresetQRCodeManager {

    fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 1)
        }
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun generatePresetCardBitmap(recipe: AtharFilterRecipe): Bitmap {
        val width = 720
        val height = 1080
        val cardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cardBitmap)

        // Dark luxury background
        val bgPaint = Paint().apply { color = 0xFF0D0F14.toInt() }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Golden Card Border
        val borderPaint = Paint().apply {
            color = 0x55FFD700.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val cardRect = RectF(28f, 28f, width - 28f, height - 28f)
        canvas.drawRoundRect(cardRect, 32f, 32f, borderPaint)

        // Header
        val titlePaint = Paint().apply {
            color = 0xFFFFD700.toInt()
            textSize = 34f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("أَثَـر • ATHAR PRESET CARD", width / 2f, 100f, titlePaint)

        val subTitlePaint = Paint().apply {
            color = 0xFFCCCCCC.toInt()
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("خلطة فلتر سينمائي احترافي", width / 2f, 140f, subTitlePaint)

        // Preset Name Badge
        val badgePaint = Paint().apply {
            color = 0xFF1A1D24.toInt()
            isAntiAlias = true
        }
        val badgeRect = RectF(width * 0.15f, 175f, width * 0.85f, 235f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgePaint)

        val namePaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(recipe.name, width / 2f, 216f, namePaint)

        // QR Code in the center
        val qrSize = 360
        val qrBitmap = generateQrBitmap(recipe.toJson(), qrSize)
        val qrLeft = (width - qrSize) / 2f
        val qrTop = 270f

        // White padded background for QR
        val qrBgPaint = Paint().apply { color = Color.WHITE }
        val qrBgRect = RectF(qrLeft - 16f, qrTop - 16f, qrLeft + qrSize + 16f, qrTop + qrSize + 16f)
        canvas.drawRoundRect(qrBgRect, 20f, 20f, qrBgPaint)

        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
        qrBitmap.recycle()

        // Parameters Badges
        val paramY = 710f
        val paramPaint = Paint().apply {
            color = 0xFFE5C07B.toInt()
            textSize = 21f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("الفلتر: ${recipe.filmPresetId}  |  التباين: ${recipe.contrast}  |  التشبع: ${recipe.saturation}", width / 2f, paramY, paramPaint)

        // Footer Instructions
        val footerPaint = Paint().apply {
            color = 0xFF9E9E9E.toInt()
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("امسح هذا الباركود داخل تطبيق «أثر» لتطبيق الفلتر فوراً", width / 2f, 960f, footerPaint)
        canvas.drawText("Scan with Athar App to apply preset instantly", width / 2f, 995f, footerPaint)

        return cardBitmap
    }

    fun decodeRecipeFromBitmap(bitmap: Bitmap): AtharFilterRecipe? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val hints = EnumMap<com.google.zxing.DecodeHintType, Any>(com.google.zxing.DecodeHintType::class.java).apply {
                put(com.google.zxing.DecodeHintType.CHARACTER_SET, "UTF-8")
                put(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
            }

            val result = MultiFormatReader().decode(binaryBitmap, hints)
            AtharFilterRecipe.fromJson(result.text)
        } catch (e: Exception) {
            null
        }
    }
}
