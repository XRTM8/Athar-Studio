package app.xrtm8.athar.core

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.LruCache
import app.xrtm8.athar.model.AtharMaskCatalog
import app.xrtm8.athar.model.AtharMaskDefinition
import app.xrtm8.athar.model.AtharMaskState
import app.xrtm8.athar.model.BlackGradientPlacement
import app.xrtm8.athar.model.MaskBlendMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Hardware-accelerated & 4K-parity Skia mask & atmospheric gradient renderer (Mask Studio 3.0).
 * Features:
 * - Procedural perspective sunlight gobos & vector frames at infinite resolution (LRU-cached blur).
 * - High-res studio WebP smoke, botanical, and caustic textures capped at <= 1440px to prevent 4K OOM.
 * - Full control pipeline: opacity, contrast, softness, warmth, invert, tint, luminance cutout,
 *   hue shift, API-level-aware blend modes (including SOFT_LIGHT & COLOR_DODGE), and correct
 *   centered transforms (scale/flip/rotate BEFORE pan translate).
 */
object AtharMaskRenderer {

    const val MAX_MASK_ASSET_DIM_PX = 1440

    private val reusableMatrix by lazy(LazyThreadSafetyMode.NONE) { Matrix() }
    private val reusablePaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    }
    private val reusableGradientPaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.FILL
        }
    }
    private val blurredBitmapCache by lazy(LazyThreadSafetyMode.NONE) { object : LruCache<String, Bitmap>(2) {} }

    /**
     * Returns true only when [maskDef] requires decoding a WebP bitmap from assets.
     * Procedural masks ignore asset files and render directly via AtharProceduralMaskRenderer.
     */
    fun shouldDecodeAssetBitmap(maskDef: AtharMaskDefinition?): Boolean {
        return maskDef != null && maskDef.requiresAssetBitmap
    }

    /**
     * Capped asset mask decoder (<= 1440px) so 4K exports upscale smoothly with bilinear filtering
     * and stay under ~8 MB RAM.
     */
    fun decodeCappedAssetBitmap(
        context: Context,
        maskDef: AtharMaskDefinition?,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        if (!shouldDecodeAssetBitmap(maskDef)) return null
        val def = maskDef ?: return null
        val reqW = targetWidth.coerceIn(64, MAX_MASK_ASSET_DIM_PX)
        val reqH = targetHeight.coerceIn(64, MAX_MASK_ASSET_DIM_PX)
        return AtharImageProcessor.decodeSampledAssetBitmap(context, def.assetFileName, reqW, reqH)
    }

    /**
     * Capped custom user mask decoder (<= 1440px) for safe memory usage during 4K export.
     */
    fun decodeCappedUriBitmap(
        context: Context,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        val reqW = targetWidth.coerceIn(64, MAX_MASK_ASSET_DIM_PX)
        val reqH = targetHeight.coerceIn(64, MAX_MASK_ASSET_DIM_PX)
        return AtharImageProcessor.decodeSampledBitmap(context, uri, reqW, reqH)
    }

    private fun applyHueRotation(cm: ColorMatrix, degrees: Float) {
        cm.reset()
        if (degrees == 0f) return
        val rad = Math.toRadians(degrees.toDouble())
        val cosVal = Math.cos(rad).toFloat()
        val sinVal = Math.sin(rad).toFloat()
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        val mat = floatArrayOf(
            lumR + cosVal * (1f - lumR) + sinVal * (-lumR),
            lumG + cosVal * (-lumG) + sinVal * (-lumG),
            lumB + cosVal * (-lumB) + sinVal * (1f - lumB),
            0f, 0f,
            lumR + cosVal * (-lumR) + sinVal * 0.143f,
            lumG + cosVal * (1f - lumG) + sinVal * 0.140f,
            lumB + cosVal * (-lumB) + sinVal * (-0.283f),
            0f, 0f,
            lumR + cosVal * (-lumR) + sinVal * (-(1f - lumR)),
            lumG + cosVal * (-lumG) + sinVal * lumG,
            lumB + cosVal * (1f - lumB) + sinVal * lumB,
            0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        cm.set(mat)
    }

    fun buildColorFilter(state: AtharMaskState): ColorFilter? {
        val isProcedural = state.proceduralType != null
        val defWarmth = AtharMaskCatalog.getById(state.activeMaskId)?.defaultWarmth ?: 0.35f
        val warmthDelta = if (isProcedural || state.tintColorLong != null) 0f else (state.warmth - defWarmth)

        val hasAdjustments = state.hueShift != 0f ||
                state.contrast != 1.0f ||
                state.isInverted ||
                state.tintColorLong != null ||
                state.isLuminanceMode ||
                kotlin.math.abs(warmthDelta) > 0.02f

        if (!hasAdjustments) return null

        val cm = ColorMatrix()

        // 1. Luminance mode (converts texture brightness to grayscale)
        if (state.isLuminanceMode) {
            val lumR = 0.299f
            val lumG = 0.587f
            val lumB = 0.114f
            cm.set(floatArrayOf(
                lumR, lumG, lumB, 0f, 0f,
                lumR, lumG, lumB, 0f, 0f,
                lumR, lumG, lumB, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        // 2. Custom Color Tint
        if (state.tintColorLong != null) {
            val tint = state.tintColorLong
            val r = ((tint shr 16) and 0xFF) / 255f
            val g = ((tint shr 8) and 0xFF) / 255f
            val b = (tint and 0xFF) / 255f
            val tintMatrix = ColorMatrix(floatArrayOf(
                r, 0f, 0f, 0f, 0f,
                0f, g, 0f, 0f, 0f,
                0f, 0f, b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(tintMatrix)
        }

        // 3. Hue Shift (only when not explicitly tinted)
        if (state.tintColorLong == null && state.hueShift != 0f) {
            val hueCm = ColorMatrix()
            applyHueRotation(hueCm, state.hueShift)
            cm.postConcat(hueCm)
        }

        // 4. Warmth temperature curve for raster WebP / custom masks (relative to neutral defaultWarmth)
        if (kotlin.math.abs(warmthDelta) > 0.02f) {
            val isMultiplyLike = state.blendMode == MaskBlendMode.MULTIPLY || state.blendMode == MaskBlendMode.DARKEN
            val rScale: Float
            val gScale: Float
            val bScale: Float
            if (isMultiplyLike) {
                // Keep sunlit white near 1.0 while gently shifting blue/warm balance
                rScale = if (warmthDelta < 0f) (1f + warmthDelta * 0.08f).coerceIn(0.88f, 1.0f) else 1.0f
                gScale = 1.0f - kotlin.math.abs(warmthDelta) * 0.04f
                bScale = if (warmthDelta > 0f) (1f - warmthDelta * 0.14f).coerceIn(0.84f, 1.0f) else 1.0f
            } else {
                rScale = (1f + warmthDelta * 0.22f).coerceIn(0.75f, 1.25f)
                gScale = (1f + warmthDelta * 0.02f).coerceIn(0.88f, 1.06f)
                bScale = (1f - warmthDelta * 0.34f).coerceIn(0.55f, 1.28f)
            }
            cm.postConcat(ColorMatrix(floatArrayOf(
                rScale, 0f, 0f, 0f, 0f,
                0f, gScale, 0f, 0f, 0f,
                0f, 0f, bScale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }

        // 5. Contrast
        if (state.contrast != 1.0f) {
            val c = state.contrast.coerceIn(0.5f, 2.5f)
            val t = (1f - c) * 0.5f * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)
        }

        // 6. Invert Mask
        if (state.isInverted) {
            val invertMatrix = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(invertMatrix)
        }

        return ColorMatrixColorFilter(cm)
    }

    private fun getSoftenedRasterBitmap(src: Bitmap, state: AtharMaskState): Bitmap {
        val defSoftness = AtharMaskCatalog.getById(state.activeMaskId)?.defaultSoftness ?: 0.0f
        val extraSoft = (state.softness - defSoftness).coerceAtLeast(0f)
        if (extraSoft <= 0.04f || src.isRecycled) return src

        val radius = (extraSoft * 24f).roundToInt().coerceIn(0, 24)
        if (radius < 2) return src

        val cacheKey = "${System.identityHashCode(src)}_${src.width}x${src.height}_r$radius"
        val cached = blurredBitmapCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) return cached

        // Downscale to <= 720px for fast blur and low RAM footprint
        val maxDim = 720f
        val scale = min(1f, maxDim / max(src.width, src.height).toFloat())
        val pw = (src.width * scale).roundToInt().coerceAtLeast(16)
        val ph = (src.height * scale).roundToInt().coerceAtLeast(16)
        val work = if (scale < 0.98f) Bitmap.createScaledBitmap(src, pw, ph, true) else src
        val scaledRadius = (radius * scale).roundToInt().coerceIn(1, 24)
        val blurred = AtharImageProcessor.fastStackBlur(work, scaledRadius)
        if (work != src && work != blurred && !work.isRecycled) {
            work.recycle()
        }
        blurredBitmapCache.put(cacheKey, blurred)
        return blurred
    }

    @Synchronized
    fun drawMaskAndGradient(
        canvas: Canvas,
        state: AtharMaskState,
        width: Float,
        height: Float,
        maskBitmap: Bitmap?
    ) {
        if (!state.isEffectActive || width <= 0f || height <= 0f) return

        // 1. Draw Active Mask (Procedural Vector/Gobo or WebP/Custom Bitmap)
        if (state.isMaskActive) {
            val procType = state.proceduralType

            reusablePaint.reset()
            reusablePaint.isAntiAlias = true
            reusablePaint.isFilterBitmap = true
            reusablePaint.isDither = true
            reusablePaint.alpha = (state.opacity.coerceIn(0f, 1f) * 255).toInt()
            state.blendMode.applyToPaint(reusablePaint)
            reusablePaint.colorFilter = buildColorFilter(state)

            val cx = width * 0.5f
            val cy = height * 0.5f
            val scale = state.scale.coerceIn(0.5f, 3.0f)
            val flipX = if (state.isFlippedH) -1f else 1f
            val flipY = if (state.isFlippedV) -1f else 1f
            val transX = state.offsetX.coerceIn(-1f, 1f) * (width * 0.5f)
            val transY = state.offsetY.coerceIn(-1f, 1f) * (height * 0.5f)

            if (procType != null) {
                // Transform order: scale+flip around center -> rotate around center -> pan translate
                reusableMatrix.reset()
                reusableMatrix.postScale(scale * flipX, scale * flipY, cx, cy)
                if (state.rotationDegrees != 0) {
                    reusableMatrix.postRotate(state.rotationDegrees.toFloat(), cx, cy)
                }
                reusableMatrix.postTranslate(transX, transY)

                canvas.save()
                canvas.concat(reusableMatrix)
                AtharProceduralMaskRenderer.drawProceduralMask(
                    canvas = canvas,
                    type = procType,
                    width = width,
                    height = height,
                    paint = reusablePaint,
                    softness = state.softness,
                    warmth = state.warmth
                )
                canvas.restore()
            } else if (maskBitmap != null && !maskBitmap.isRecycled) {
                val drawBmp = getSoftenedRasterBitmap(maskBitmap, state)
                val bmpW = drawBmp.width.toFloat()
                val bmpH = drawBmp.height.toFloat()
                if (bmpW > 0f && bmpH > 0f) {
                    val baseScale = max(width / bmpW, height / bmpH) * scale
                    // Center at origin -> scale+flip -> rotate -> translate to canvas center + pan
                    reusableMatrix.reset()
                    reusableMatrix.postTranslate(-bmpW * 0.5f, -bmpH * 0.5f)
                    reusableMatrix.postScale(baseScale * flipX, baseScale * flipY)
                    if (state.rotationDegrees != 0) {
                        reusableMatrix.postRotate(state.rotationDegrees.toFloat())
                    }
                    reusableMatrix.postTranslate(cx + transX, cy + transY)

                    canvas.drawBitmap(drawBmp, reusableMatrix, reusablePaint)
                }
            }
        }

        // 2. Draw Atmospheric Black Gradient Scrim if active
        if (state.blackGradient.isEffectActive) {
            val grad = state.blackGradient
            val cov = grad.coverage.coerceIn(0.15f, 1.0f)

            reusableGradientPaint.reset()
            reusableGradientPaint.isAntiAlias = true
            reusableGradientPaint.isDither = true
            reusableGradientPaint.style = Paint.Style.FILL
            reusableGradientPaint.alpha = (grad.opacity.coerceIn(0f, 1f) * 255).toInt()

            val shader: Shader = when (grad.placement) {
                BlackGradientPlacement.BOTTOM -> LinearGradient(
                    0f, height * (1f - cov), 0f, height,
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
                )
                BlackGradientPlacement.TOP -> LinearGradient(
                    0f, height * cov, 0f, 0f,
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
                )
                BlackGradientPlacement.LEFT -> LinearGradient(
                    width * cov, 0f, 0f, 0f,
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
                )
                BlackGradientPlacement.RIGHT -> LinearGradient(
                    width * (1f - cov), 0f, width, 0f,
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP
                )
                BlackGradientPlacement.RADIAL -> RadialGradient(
                    width / 2f, height / 2f, max(width, height) * 0.75f * cov,
                    intArrayOf(Color.TRANSPARENT, Color.BLACK),
                    floatArrayOf(0.15f, 1.0f), Shader.TileMode.CLAMP
                )
                BlackGradientPlacement.VERTICAL_SPLIT -> LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(Color.BLACK, Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                    floatArrayOf(0f, cov * 0.40f, 1f - (cov * 0.40f), 1f), Shader.TileMode.CLAMP
                )
            }

            reusableGradientPaint.shader = shader
            canvas.drawRect(0f, 0f, width, height, reusableGradientPaint)
        }
    }
}
