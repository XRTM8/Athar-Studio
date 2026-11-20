package app.xrtm8.athar.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Company-Grade Offline On-Device Subject Depth & Segmentation Engine.
 * 
 * Powered by Google ML Kit Neural Semantic Segmentation for human portraits/silhouettes,
 * paired with a robust localized saliency & edge gradient fallback for inanimate objects.
 * Produces smooth, anti-aliased, zero-jank foreground cutout masks for the "Text Behind Subject" effect.
 */
object SubjectDepthEngine {

    private var cachedSourceHash: Int = 0
    private var cachedSensitivity: Float = -1f
    private var cachedCutout: Bitmap? = null

    // Lazy initialization of Google ML Kit Neural Segmenter
    private val mlKitSegmenter: Segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .enableRawSizeMask()
            .build()
        Segmentation.getClient(options)
    }

    /**
     * Cleans up cached bitmaps to prevent memory leaks when studio unmounts.
     */
    fun clearCache() {
        val c = cachedCutout
        if (c != null && !c.isRecycled) {
            c.recycle()
        }
        cachedCutout = null
        cachedSourceHash = 0
        cachedSensitivity = -1f
    }

    /**
     * Generates a high-precision foreground cutout bitmap asynchronously.
     * Caches the result to guarantee 120Hz smooth canvas interactions during gestures.
     */
    suspend fun getSubjectCutout(
        source: Bitmap,
        sensitivity: Float = 0.5f
    ): Bitmap = withContext(Dispatchers.Default) {
        val srcHash = source.hashCode()
        val currentCutout = cachedCutout

        if (srcHash == cachedSourceHash && currentCutout != null && !currentCutout.isRecycled && cachedSensitivity == sensitivity) {
            return@withContext currentCutout
        }

        // Try Google ML Kit Neural Segmentation first
        val neuralCutout = tryRunNeuralSegmentation(source, sensitivity)
        val finalCutout = neuralCutout ?: runAdvancedSaliencySegmentation(source, sensitivity)

        // Update cache safely
        if (currentCutout != null && currentCutout != finalCutout && !currentCutout.isRecycled) {
            currentCutout.recycle()
        }
        cachedCutout = finalCutout
        cachedSourceHash = srcHash
        cachedSensitivity = sensitivity

        finalCutout
    }

    /**
     * Executes Google ML Kit Neural Network Segmentation.
     * Extracts pixel-accurate confidence values with sub-pixel feathering.
     */
    private fun tryRunNeuralSegmentation(source: Bitmap, sensitivity: Float): Bitmap? {
        return try {
            // Scale appropriately for rapid neural inference without sacrificing mask fidelity
            val maxDim = 1024
            val scale = if (max(source.width, source.height) > maxDim) {
                maxDim.toFloat() / max(source.width, source.height)
            } else 1.0f

            val procW = (source.width * scale).toInt().coerceAtLeast(100)
            val procH = (source.height * scale).toInt().coerceAtLeast(100)
            val scaledSource = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(source, procW, procH, true)
            } else {
                source
            }

            val inputImage = InputImage.fromBitmap(scaledSource, 0)
            val task = mlKitSegmenter.process(inputImage)
            val mask = Tasks.await(task, 4, TimeUnit.SECONDS)

            val maskWidth = mask.width
            val maskHeight = mask.height
            val buffer: ByteBuffer = mask.buffer
            buffer.rewind()

            // Analyze confidence distribution to verify a human subject was detected
            var maxConfidence = 0f
            val floatCount = maskWidth * maskHeight
            val confidences = FloatArray(floatCount)
            for (i in 0 until floatCount) {
                val conf = buffer.float
                confidences[i] = conf
                if (conf > maxConfidence) maxConfidence = conf
            }

            if (scaledSource != source && !scaledSource.isRecycled) {
                scaledSource.recycle()
            }

            // If no person detected (e.g. landscape or object), fall back to saliency engine
            if (maxConfidence < 0.25f) {
                return null
            }

            // Build anti-aliased cutout bitmap
            val cutout = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(cutout)

            // Create intermediate mask bitmap
            val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
            val maskPixels = IntArray(floatCount)

            // Dynamic threshold modulated by user sensitivity
            val baseThreshold = (0.75f - (sensitivity * 0.5f)).coerceIn(0.15f, 0.85f)
            val featherRange = 0.18f

            for (i in 0 until floatCount) {
                val conf = confidences[i]
                val alpha = when {
                    conf >= baseThreshold + featherRange -> 255
                    conf <= baseThreshold - featherRange -> 0
                    else -> {
                        val t = (conf - (baseThreshold - featherRange)) / (featherRange * 2f)
                        // Smooth cubic Hermite interpolation
                        val smoothT = t * t * (3f - 2f * t)
                        (smoothT * 255f).toInt().coerceIn(0, 255)
                    }
                }
                maskPixels[i] = (alpha shl 24) or 0x00FFFFFF
            }
            maskBitmap.setPixels(maskPixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

            // Draw source image masked by neural confidence
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val scaledMask = Bitmap.createScaledBitmap(maskBitmap, source.width, source.height, true)
            maskBitmap.recycle()

            canvas.drawBitmap(scaledMask, 0f, 0f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(source, 0f, 0f, paint)
            scaledMask.recycle()

            cutout
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Advanced Saliency & Edge Gradient Segmentation Engine.
     * Serves as seamless zero-crash fallback for non-human subjects or offline headless environments.
     */
    private fun runAdvancedSaliencySegmentation(source: Bitmap, sensitivity: Float): Bitmap {
        val sampleW = 280
        val sampleH = (sampleW * (source.height.toFloat() / source.width)).toInt().coerceIn(160, 480)
        val small = Bitmap.createScaledBitmap(source, sampleW, sampleH, true)

        val pixels = IntArray(sampleW * sampleH)
        small.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        // Multi-point perimeter background sampling
        var bgR = 0L; var bgG = 0L; var bgB = 0L; var bgCount = 0
        val borderPad = max(3, sampleW / 18)
        for (y in 0 until sampleH) {
            for (x in 0 until sampleW) {
                if (y < borderPad || y >= sampleH - borderPad || x < borderPad || x >= sampleW - borderPad) {
                    val p = pixels[y * sampleW + x]
                    bgR += (p shr 16) and 0xFF
                    bgG += (p shr 8) and 0xFF
                    bgB += p and 0xFF
                    bgCount++
                }
            }
        }
        val refR = (bgR / max(1, bgCount)).toInt()
        val refG = (bgG / max(1, bgCount)).toInt()
        val refB = (bgB / max(1, bgCount)).toInt()

        val maskPixels = IntArray(sampleW * sampleH)
        val centerX = sampleW / 2f
        val centerY = sampleH / 2f
        val maxDist = hypot(centerX, centerY)

        val baseThreshold = (40f + (1f - sensitivity) * 85f)

        for (y in 0 until sampleH) {
            for (x in 0 until sampleW) {
                val idx = y * sampleW + x
                val p = pixels[idx]
                val pr = (p shr 16) and 0xFF
                val pg = (p shr 8) and 0xFF
                val pb = p and 0xFF

                val dR = pr - refR
                val dG = pg - refG
                val dB = pb - refB
                val colorDist = sqrt((dR * dR + dG * dG + dB * dB).toDouble()).toFloat()

                val distFromCenter = hypot(x - centerX, y - centerY) / maxDist
                val centerWeight = exp(-2.2 * distFromCenter * distFromCenter).toFloat()
                val score = colorDist * (0.65f + 0.75f * centerWeight)

                val alpha = when {
                    score >= baseThreshold -> 255
                    score <= baseThreshold * 0.7f -> 0
                    else -> {
                        val t = (score - baseThreshold * 0.7f) / (baseThreshold * 0.3f)
                        (t * 255f).toInt().coerceIn(0, 255)
                    }
                }
                maskPixels[idx] = (alpha shl 24) or (p and 0x00FFFFFF)
            }
        }
        small.recycle()

        val maskSmall = Bitmap.createBitmap(sampleW, sampleH, Bitmap.Config.ARGB_8888)
        maskSmall.setPixels(maskPixels, 0, sampleW, 0, 0, sampleW, sampleH)

        val fullCutout = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fullCutout)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val scaledMask = Bitmap.createScaledBitmap(maskSmall, source.width, source.height, true)
        maskSmall.recycle()

        canvas.drawBitmap(scaledMask, 0f, 0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        scaledMask.recycle()

        return fullCutout
    }
}
