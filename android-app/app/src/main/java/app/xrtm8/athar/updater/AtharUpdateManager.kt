package app.xrtm8.athar.updater

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
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
import java.security.MessageDigest

class AtharUpdateManager(private val context: Context) {
    companion object {
        private const val TAG = "AtharUpdateManager"
        private const val CHANNEL_ID = "athar_updates"
        private const val NOTIFICATION_ID = 1001
        const val GITHUB_REPO = "XRTM8/Athar-Studio"
        const val REPO_URL = "https://github.com/$GITHUB_REPO"
        private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

        data class ReleaseAsset(val name: String, val downloadUrl: String)

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
                    runCatching { Log.i(TAG, "Selected native ARM64-v8a asset: ${arm64Asset.name}") }
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

    fun isNewerVersion(remoteTag: String, localVersionName: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim().substringBefore('-')
        val cleanLocal = localVersionName.removePrefix("v").trim().substringBefore('-')
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

    suspend fun checkForUpdates(
        currentVersionName: String = BuildConfig.VERSION_NAME,
        showNotificationOnFound: Boolean = true
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(RELEASES_API_URL)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Athar-App-Android")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val tagName = json.optString("tag_name", "").trim()
                val body = json.optString("body", "تحسينات وإصلاحات")

                val apkAssets = ArrayList<ReleaseAsset>()
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        val dUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && dUrl.startsWith("https://")) {
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
                    _updateState.value = null
                }
            } else {
                // Zero-rate-limit fallback via GitHub web redirect
                val redirectConn = (URL("$REPO_URL/releases/latest").openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "HEAD"
                    connectTimeout = 7000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", "Athar-App-Android")
                }
                try {
                    val location = redirectConn.getHeaderField("Location").orEmpty()
                    val tagName = location.substringAfterLast("/tag/", "").trim()
                    if (tagName.isNotEmpty() && isNewerVersion(tagName, currentVersionName)) {
                        val cleanTag = tagName.removePrefix("v")
                        val updateInfo = UpdateInfo(
                            hasUpdate = true,
                            latestVersionName = cleanTag,
                            latestVersionCode = 0,
                            releaseNotes = "إصدار جديد متوفر على GitHub",
                            downloadUrl = "$REPO_URL/releases/latest/download/athar-$tagName.apk",
                            isMandatory = false
                        )
                        _updateState.value = updateInfo
                        if (showNotificationOnFound) showGentleNotification(updateInfo)
                        return@withContext updateInfo
                    } else {
                        _updateState.value = null
                    }
                } finally {
                    runCatching { redirectConn.disconnect() }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Update check completed without update or offline: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        null
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
            var activeConn: HttpURLConnection? = null
            val updatesDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val tempPartFile = File(updatesDir, "athar-latest.apk.part")
            val apkFile = File(updatesDir, "athar-latest.apk")

            try {
                require(downloadUrl.startsWith("https://")) { "يُرفض تنزيل التحديث عبر رابط غير آمن" }
                _downloadProgress.value = 0.05f

                var currentUrl = downloadUrl
                var redirects = 0
                while (true) {
                    require(currentUrl.startsWith("https://")) { "تم رفض إعادة توجيه غير آمنة (HTTP)" }
                    val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", "Athar-App-Android")
                        connect()
                    }
                    activeConn = conn

                    val code = conn.responseCode
                    if ((code == HttpURLConnection.HTTP_MOVED_PERM ||
                            code == HttpURLConnection.HTTP_MOVED_TEMP ||
                            code == HttpURLConnection.HTTP_SEE_OTHER ||
                            code == 307 || code == 308) && redirects < 5
                    ) {
                        val location = conn.getHeaderField("Location")
                        conn.disconnect()
                        activeConn = null
                        if (!location.isNullOrBlank()) {
                            currentUrl = URL(URL(currentUrl), location).toString()
                            redirects++
                            continue
                        }
                    }
                    check(code == HttpURLConnection.HTTP_OK) { "فشل تنزيل التحديث: HTTP $code" }
                    break
                }

                val conn = checkNotNull(activeConn)
                val fileLength = conn.contentLengthLong
                if (tempPartFile.exists()) tempPartFile.delete()
                if (apkFile.exists()) apkFile.delete()

                var total = 0L
                conn.inputStream.use { input ->
                    FileOutputStream(tempPartFile).use { output ->
                        val data = ByteArray(8192)
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0L) {
                                _downloadProgress.value = (total.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                            }
                            output.write(data, 0, count)
                        }
                        output.flush()
                    }
                }

                if (fileLength > 0L && total != fileLength) {
                    tempPartFile.delete()
                    Log.e(TAG, "Incomplete APK download: $total of $fileLength bytes")
                    return@withContext
                }
                check(tempPartFile.renameTo(apkFile)) { "تعذر تجهيز ملف الحزمة للتثبيت" }
                _downloadProgress.value = null

                if (!verifyDownloadedApkMatchesInstalledSignature(apkFile)) {
                    apkFile.delete()
                    Log.e(TAG, "Downloaded APK failed package or signing certificate verification")
                    return@withContext
                }

                installApk(apkFile)
            } catch (e: Exception) {
                tempPartFile.delete()
                Log.e(TAG, "Download and install failed: ${e.message}")
                _downloadProgress.value = null
            } finally {
                activeConn?.disconnect()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyDownloadedApkMatchesInstalledSignature(apkFile: File): Boolean {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
        if (archiveInfo.packageName != context.packageName) return false

        val installedInfo = runCatching { pm.getPackageInfo(context.packageName, flags) }.getOrNull() ?: return false
        val installedDigests = extractSigningSha256Digests(installedInfo)
        val archiveDigests = extractSigningSha256Digests(archiveInfo)
        if (installedDigests.isEmpty() || archiveDigests.isEmpty()) return false
        return archiveDigests.any { it in installedDigests }
    }

    @Suppress("DEPRECATION")
    private fun extractSigningSha256Digests(pkgInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pkgInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        } else {
            pkgInfo.signatures
        } ?: return emptySet()

        val md = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
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
