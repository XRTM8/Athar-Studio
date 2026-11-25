package app.xrtm8.athar.export

import android.content.Context
import android.graphics.pdf.PdfDocument
import app.xrtm8.athar.core.AtharBackgroundPipeline
import app.xrtm8.athar.model.CanvasState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Exports the current canvas state to a PDF page using Android's PdfDocument.
 */
object AtharPdfExporter {

    suspend fun exportPdf(
        context: Context,
        state: CanvasState,
        onShareRequested: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            val ratio = state.aspectRatio.ratio ?: (9f / 16f)
            val baseWidth = 1080
            val baseHeight = (baseWidth / ratio).toInt()

            val pageInfo = PdfDocument.PageInfo.Builder(baseWidth, baseHeight, 1).create()
            val page = document.startPage(pageInfo)

            if (!state.isTransparentBg) {
                AtharBackgroundPipeline.renderSoftwareBackground(context, page.canvas, state, baseWidth, baseHeight)
            }
            AtharExportManager.drawLayers(context, page.canvas, state.layers, baseWidth, baseHeight)
            document.finishPage(page)

            val filename = "Athar_Vector_${System.currentTimeMillis()}.pdf"
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val shareFile = File(cacheDir, filename)
            FileOutputStream(shareFile).use { out ->
                document.writeTo(out)
            }

            AtharDocumentFileHelper.saveDocumentToDownloads(context, shareFile, filename, "application/pdf")

            if (onShareRequested) {
                AtharDocumentFileHelper.shareDocumentFile(
                    context = context,
                    shareFile = shareFile,
                    filename = filename,
                    mimeType = "application/pdf",
                    chooserTitle = "مشاركة ملف PDF من أثر"
                )
            }

            Result.success("تم تصدير مستند PDF بنجاح إلى مجلد التنزيلات")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            document.close()
        }
    }
}
