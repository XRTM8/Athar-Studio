package app.xrtm8.athar.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * Shared MediaStore / legacy Downloads persistence and FileProvider sharing helper
 * for exported vector documents (PDF, SVG).
 */
internal object AtharDocumentFileHelper {

    fun saveDocumentToDownloads(
        context: Context,
        sourceFile: File,
        filename: String,
        mimeType: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Athar")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("تعذر إنشاء الملف في التنزيلات")

            try {
                val output = checkNotNull(context.contentResolver.openOutputStream(uri)) {
                    "تعذر فتح مجرى الكتابة للملف"
                }
                output.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                check(context.contentResolver.update(uri, values, null, null) == 1) {
                    "تعذر إتمام حفظ الملف في التنزيلات"
                }
            } catch (e: Exception) {
                runCatching { context.contentResolver.delete(uri, null, null) }
                throw e
            }
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

    fun shareDocumentFile(
        context: Context,
        shareFile: File,
        filename: String,
        mimeType: String,
        chooserTitle: String
    ) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, chooserTitle)
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }
}
