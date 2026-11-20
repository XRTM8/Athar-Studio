package app.xrtm8.mada.updater

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
import app.xrtm8.mada.MainActivity
import app.xrtm8.mada.model.UpdateInfo
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

class MadaUpdateManager(private val context: Context) {
    companion object {
        private const val TAG = "MadaUpdateManager"
        private const val CHANNEL_ID = "mada_updates"
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
            val name = "تحديثات تطبيق مَــدَى"
            val descriptionText = "إشعارات اختيارية عند توفر إصدارات وتحديثات جديدة للتطبيق"
            val importance = NotificationManager.IMPORTANCE_LOW // Non-intrusive / gentle
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(false)
                enableVibration(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun checkForUpdates(currentVersionCode: Int = 1, showNotificationOnFound: Boolean = true): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(RELEASES_API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "Mada-App-Android")

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)

                    val tagName = json.optString("tag_name", "v1.0.0")
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

                    // Parse version from tag e.g. "v1.1.0" or "1.1.0"
                    val cleanTag = tagName.removePrefix("v")
                    val remoteVersionCode = parseVersionCode(cleanTag)

                    if (remoteVersionCode > currentVersionCode && downloadUrl.isNotEmpty()) {
                        val updateInfo = UpdateInfo(
                            hasUpdate = true,
                            latestVersionName = cleanTag,
                            latestVersionCode = remoteVersionCode,
                            releaseNotes = body,
                            downloadUrl = downloadUrl,
                            isMandatory = false
                        )
                        _updateState.value = updateInfo

                        if (showNotificationOnFound) {
                            showGentleNotification(updateInfo)
                        }
                        return@withContext updateInfo
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Update check completed without update or network is offline: ${e.message}")
            }
            null
        }
    }

    private fun parseVersionCode(versionName: String): Int {
        return try {
            val parts = versionName.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            1
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
            .setContentTitle("يتوفر إصدار جديد من مَــدَى (${updateInfo.latestVersionName})")
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
                val url = URL(downloadUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.connect()

                val fileLength = conn.contentLength
                val updatesDir = File(context.getExternalFilesDir(null), "updates")
                if (!updatesDir.exists()) updatesDir.mkdirs()

                val apkFile = File(updatesDir, "mada-latest.apk")
                if (apkFile.exists()) apkFile.delete()

                val input = conn.inputStream
                val output = FileOutputStream(apkFile)

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
                output.close()
                input.close()

                _downloadProgress.value = 1.0f

                // Launch package installer
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
                "app.xrtm8.mada.fileprovider",
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
