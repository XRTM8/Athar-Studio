package app.xrtm8.athar.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import app.xrtm8.athar.MainActivity
import app.xrtm8.athar.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AtharUpdateManager(private val context: Context) {
    companion object {
        private const val TAG = "AtharUpdateManager"
        private const val CHANNEL_ID = "athar_updates"
        private const val NOTIFICATION_ID = 1001
        private const val GITHUB_REPO = "XRTM8/DESIGN"
        private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    }

    private val _updateState = MutableStateFlow<UpdateInfo?>(null)
    val updateState: StateFlow<UpdateInfo?> = _updateState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تحديثات تطبيق أَثَــر"
            val descriptionText = "إشعارات اختيارية عند توفر إصدارات وتحديثات جديدة للتطبيق"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(false)
                enableVibration(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if a remote version tag is strictly newer than current local version using Semantic Versioning.
     */
    fun isNewerVersion(remoteTag: String, localVersionName: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim()
        val cleanLocal = localVersionName.removePrefix("v").trim()
        if (cleanRemote == cleanLocal) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    suspend fun checkForUpdates(currentVersionName: String = "1.8.1", showNotificationOnFound: Boolean = true): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(RELEASES_API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "Athar-App-Android")

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)

                    val tagName = json.optString("tag_name", "").trim()
                    val body = json.optString("body", "تحسينات جديدة وتأثيرات إضافية")

                    var downloadUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    val cleanTag = tagName.removePrefix("v")

                    if (isNewerVersion(tagName, currentVersionName) && downloadUrl.isNotEmpty()) {
                        val updateInfo = UpdateInfo(
                            hasUpdate = true,
                            latestVersionName = cleanTag,
                            latestVersionCode = 0,
                            releaseNotes = body,
                            downloadUrl = downloadUrl,
                            isMandatory = false
                        )
                        _updateState.value = updateInfo

                        if (showNotificationOnFound) {
                            showGentleNotification(updateInfo)
                        }
                        return@withContext updateInfo
                    } else {
                        // Already up to date! Clear any stale update state
                        _updateState.value = null
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Update check completed without update or offline: ${e.message}")
            }
            null
        }
    }

    private fun showGentleNotification(updateInfo: UpdateInfo) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_UPDATE_DIALOG", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("يتوفر إصدار جديد من أَثَــر (${updateInfo.latestVersionName})")
            .setContentText("انقر للاطلاع على التحديثات والميزات الجديدة (اختياري)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    suspend fun downloadAndInstallApk(downloadUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                _downloadProgress.value = 0.05f
                var currentUrl = downloadUrl
                var conn: HttpURLConnection
                var redirects = 0

                while (true) {
                    val url = URL(currentUrl)
                    conn = url.openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.setRequestProperty("User-Agent", "Athar-App-Android")
                    conn.connect()

                    val code = conn.responseCode
                    if ((code == HttpURLConnection.HTTP_MOVED_PERM ||
                                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                                code == HttpURLConnection.HTTP_SEE_OTHER ||
                                code == 307 || code == 308) && redirects < 5
                    ) {
                        val location = conn.getHeaderField("Location")
                        if (!location.isNullOrBlank()) {
                            currentUrl = location
                            redirects++
                            conn.disconnect()
                            continue
                        }
                    }
                    break
                }

                val fileLength = conn.contentLength
                val updatesDir = File(context.getExternalFilesDir(null), "updates")
                if (!updatesDir.exists()) updatesDir.mkdirs()

                val apkFile = File(updatesDir, "athar-latest.apk")
                if (apkFile.exists()) apkFile.delete()

                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val data = ByteArray(8192)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                _downloadProgress.value = (total.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                            }
                            output.write(data, 0, count)
                        }
                        output.flush()
                    }
                }

                _downloadProgress.value = 1.0f
                installApk(apkFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download update APK", e)
                _downloadProgress.value = null
            }
        }
    }

    private fun installApk(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "app.xrtm8.athar.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer", e)
        }
    }

    fun dismissUpdate() {
        _updateState.value = null
    }
}
