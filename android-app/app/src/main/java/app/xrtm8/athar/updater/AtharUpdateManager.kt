package app.xrtm8.athar.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import app.xrtm8.athar.BuildConfig
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
        private const val GITHUB_REPO = "XRTM8/Athar-Studio"
        private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

        data class ReleaseAsset(val name: String, val downloadUrl: String)

        /**
         * Intelligently selects the optimal APK asset matching device architecture.
         * Prioritizes ARM64-v8a native build on 64-bit devices, falling back to universal or first available APK.
         */
        fun selectBestApkAsset(
            assets: List<ReleaseAsset>,
            deviceAbis: Array<String>? = null
        ): String? {
            if (assets.isEmpty()) return null
            if (assets.size == 1) return assets.first().downloadUrl

            val supportedAbis = deviceAbis ?: Build.SUPPORTED_ABIS ?: emptyArray()
            val isArm64 = supportedAbis.any {
                it.contains("arm64", ignoreCase = true) || it.contains("aarch64", ignoreCase = true)
            }

            if (isArm64) {
                val arm64Asset = assets.find {
                    it.name.contains("arm64", ignoreCase = true) ||
                    it.name.contains("aarch64", ignoreCase = true) ||
                    it.name.contains("arm64-v8a", ignoreCase = true)
                }
                if (arm64Asset != null) {
                    try {
                        Log.i(TAG, "Selected native ARM64-v8a asset: ${arm64Asset.name}")
                    } catch (_: Throwable) {
                        // Safe in unit tests
                    }
                    return arm64Asset.downloadUrl
                }
            }

            val universalAsset = assets.find { it.name.contains("universal", ignoreCase = true) }
            if (universalAsset != null) {
                return universalAsset.downloadUrl
            }

            return assets.first().downloadUrl
        }
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

    suspend fun checkForUpdates(currentVersionName: String = BuildConfig.VERSION_NAME, showNotificationOnFound: Boolean = true): UpdateInfo? {
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

                    val apkAssets = ArrayList<ReleaseAsset>()
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            val dUrl = asset.optString("browser_download_url", "")
                            if (name.endsWith(".apk", ignoreCase = true) && dUrl.isNotEmpty()) {
                                apkAssets.add(ReleaseAsset(name, dUrl))
                            }
                        }
                    }

                    val downloadUrl = selectBestApkAsset(apkAssets) ?: ""
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
                val updatesDir = File(context.cacheDir, "updates")
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

                _downloadProgress.value = null

                // Verify package integrity and matching packageName before triggering installation
                val pm = context.packageManager
                val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                if (archiveInfo == null || archiveInfo.packageName != context.packageName) {
                    apkFile.delete()
                    Log.e(TAG, "Downloaded APK failed archive integrity check or package mismatch: ${archiveInfo?.packageName}")
                    return@withContext
                }

                installApk(apkFile)
            } catch (e: Exception) {
                Log.e(TAG, "Download and install failed: ${e.message}")
                _downloadProgress.value = null
            }
        }
    }

    private fun installApk(file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Launch installer failed: ${e.message}")
        }
    }

    fun dismissUpdate() {
        _updateState.value = null
    }
}
