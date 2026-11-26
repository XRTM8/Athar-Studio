package app.xrtm8.athar.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import app.xrtm8.athar.core.AtharBackgroundPipeline
import app.xrtm8.athar.core.CollageCoverFitMath
import app.xrtm8.athar.model.CanvasState

/**
 * Hardware-accelerated canvas viewport background renderer using the shared
 * AtharBackgroundPipeline for 100% Preview-to-Export parity.
 */
object CanvasBackgroundRenderer {

    private var cachedCheckerboardDensity = 0f
    private var cachedCheckerboardLight = false
    private var cachedCheckerboardPaint: Paint? = null
    private val bgMatrix = Matrix()
    private val solidFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private fun getCheckerboardPaint(density: Float, isAmoledWhite: Boolean): Paint {
        val existing = cachedCheckerboardPaint
        if (existing != null && cachedCheckerboardDensity == density && cachedCheckerboardLight == isAmoledWhite) {
            return existing
        }
        val tileSize = (16f * density).toInt().coerceAtLeast(8)
        val tileBmp = Bitmap.createBitmap(tileSize * 2, tileSize * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(tileBmp)
        val p1 = Paint().apply { color = if (isAmoledWhite) 0xFFEAECEF.toInt() else 0xFF121418.toInt() }
        val p2 = Paint().apply { color = if (isAmoledWhite) 0xFFFFFFFF.toInt() else 0xFF1A1D24.toInt() }
        val s = tileSize.toFloat()

        c.drawRect(0f, 0f, s, s, p1)
        c.drawRect(s, 0f, s * 2, s, p2)
        c.drawRect(0f, s, s, s * 2, p2)
        c.drawRect(s, s, s * 2, s * 2, p1)

        val shader = BitmapShader(tileBmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        cachedCheckerboardDensity = density
        cachedCheckerboardLight = isAmoledWhite
        cachedCheckerboardPaint = paint
        return paint
    }

    fun drawBackground(
        nativeCanvas: Canvas,
        state: CanvasState,
        canvasWidth: Float,
        canvasHeight: Float,
        density: Float,
        bgBitmap: Bitmap?,
        bgPaint: Paint,
        cellBitmaps: Map<String, Bitmap> = emptyMap(),
        maskBitmap: Bitmap? = null,
        processedCollagePhotoLayer: Bitmap? = null,
        processedCollageBitmap: Bitmap? = null,
        blurredCellBitmaps: Map<String, Bitmap> = emptyMap(),
        isLiveFramingDrag: Boolean = false,
        isAmoledWhite: Boolean = false
    ) {
        if (state.isTransparentBg) {
            nativeCanvas.drawRect(0f, 0f, canvasWidth, canvasHeight, getCheckerboardPaint(density, isAmoledWhite))
            return
        }

        val worldDensity = AtharBackgroundPipeline.canvasDensity(canvasWidth)

        if (state.collageState.isEnabled) {
            val useFullBaked = !isLiveFramingDrag &&
                processedCollageBitmap != null &&
                !processedCollageBitmap.isRecycled

            val useMixedPhotoLayer = !isLiveFramingDrag &&
                !useFullBaked &&
                processedCollagePhotoLayer != null &&
                !processedCollagePhotoLayer.isRecycled

            if (useFullBaked && processedCollageBitmap != null) {
                bgPaint.colorFilter = null
                bgMatrix.reset()
                bgMatrix.setScale(
                    canvasWidth / processedCollageBitmap.width.coerceAtLeast(1),
                    canvasHeight / processedCollageBitmap.height.coerceAtLeast(1)
                )
                nativeCanvas.drawBitmap(processedCollageBitmap, bgMatrix, bgPaint)
            } else if (useMixedPhotoLayer && processedCollagePhotoLayer != null) {
                solidFillPaint.color = state.collageState.frameColorLong.toInt()
                nativeCanvas.drawRect(0f, 0f, canvasWidth, canvasHeight, solidFillPaint)
                CollageCanvasRenderer.drawEmptyPlaceholders(
                    canvas = nativeCanvas,
                    state = state.collageState,
                    width = canvasWidth,
                    height = canvasHeight,
                    density = worldDensity
                ) { cell ->
                    val b = cellBitmaps[cell.id]
                    b == null || b.isRecycled
                }
                bgPaint.colorFilter = AtharBackgroundPipeline.buildCompositeColorFilter(state, includeFilmMatrix = false)
                bgMatrix.reset()
                bgMatrix.setScale(
                    canvasWidth / processedCollagePhotoLayer.width.coerceAtLeast(1),
                    canvasHeight / processedCollagePhotoLayer.height.coerceAtLeast(1)
                )
                nativeCanvas.drawBitmap(processedCollagePhotoLayer, bgMatrix, bgPaint)
                bgPaint.colorFilter = null
            } else {
                bgPaint.colorFilter = AtharBackgroundPipeline.buildCompositeColorFilter(state, includeFilmMatrix = true)
                val activeCells = if (!isLiveFramingDrag && blurredCellBitmaps.isNotEmpty()) {
                    blurredCellBitmaps
                } else {
                    cellBitmaps
                }
                CollageCanvasRenderer.drawCollage(
                    canvas = nativeCanvas,
                    state = state.collageState,
                    width = canvasWidth,
                    height = canvasHeight,
                    density = worldDensity,
                    cellBitmaps = activeCells,
                    paint = bgPaint
                )
                bgPaint.colorFilter = null
            }
        } else if (bgBitmap != null && !bgBitmap.isRecycled) {
            bgPaint.colorFilter = if (AtharBackgroundPipeline.hasSinglePhotoRasterEffects(state)) {
                null
            } else {
                AtharBackgroundPipeline.buildCompositeColorFilter(state, includeFilmMatrix = false)
            }
            CollageCoverFitMath.populateMatrix(
                outMatrix = bgMatrix,
                srcW = bgBitmap.width.toFloat(),
                srcH = bgBitmap.height.toFloat(),
                dstLeft = 0f,
                dstTop = 0f,
                dstRight = canvasWidth,
                dstBottom = canvasHeight,
                offsetX = state.bgPhotoOffsetX,
                offsetY = state.bgPhotoOffsetY,
                scale = state.bgPhotoScale.coerceIn(1.0f, 4.0f)
            )
            nativeCanvas.drawBitmap(bgBitmap, bgMatrix, bgPaint)
            bgPaint.colorFilter = null
        } else {
            solidFillPaint.color = state.solidBgColorLong?.toInt() ?: Color.BLACK
            nativeCanvas.drawRect(0f, 0f, canvasWidth, canvasHeight, solidFillPaint)
        }

        AtharBackgroundPipeline.drawPostBackgroundOverlays(
            canvas = nativeCanvas,
            state = state,
            width = canvasWidth,
            height = canvasHeight,
            maskBitmap = maskBitmap
        )
    }
}
