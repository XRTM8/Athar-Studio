package app.xrtm8.athar.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.xrtm8.athar.model.CanvasState
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-fidelity vector PDF exporter for Athar Studio (< 150 lines).
 * Records exact typography, vector paths, layers, and compositions
 * into a print-ready vector PDF using Android's native PdfDocument.
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
                AtharExportManager.drawBackground(context, page.canvas, state, baseWidth, baseHeight)
            }
            AtharExportManager.drawLayers(context, page.canvas, state.layers, baseWidth, baseHeight)
            document.finishPage(page)

            val timestamp = System.currentTimeMillis()
            val filename = "Athar_Vector_${timestamp}.pdf"

            // Save copy in cache for immediate sharing
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val shareFile = File(cacheDir, filename)
            FileOutputStream(shareFile).use { out ->
                document.writeTo(out)
            }

            // Save to Public MediaStore / Downloads
            savePdfToMediaStore(context, shareFile, filename)

            if (onShareRequested) {
                sharePdfFile(context, shareFile, filename)
            }

            Result.success("تم تصدير مستند PDF المتجهي بنجاح (جاهز للطباعة بدقة عالية)")
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            document.close()
        }
    }

    private fun savePdfToMediaStore(context: Context, sourceFile: File, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Athar")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("تعذر إنشاء ملف PDF في التنزيلات")

            context.contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val targetDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Athar"
            ).apply { mkdirs() }
            val targetFile = File(targetDir, filename)
            sourceFile.copyTo(targetFile, overwrite = true)
        }
    }

    private fun sharePdfFile(context: Context, shareFile: File, filename: String) {
        val uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", shareFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, "مشاركة ملف PDF من أثر")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }
}
