package app.xrtm8.mada.core

import android.util.Log

object MadaNativeEngine {
    private const val TAG = "MadaNativeEngine"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("mada_core")
            isLoaded = true
            Log.i(TAG, "Mada native Rust core loaded successfully. Version: ${nativeGetVersion()}")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library mada_core not found or failed to load. Fallback enabled.", e)
            isLoaded = false
        }
    }

    fun isNativeReady(): Boolean = isLoaded

    fun getEngineVersion(): String {
        return if (isLoaded) {
            try {
                nativeGetVersion()
            } catch (e: Throwable) {
                "1.0.0-managed"
            }
        } else {
            "1.0.0-managed"
        }
    }

    fun applyKashida(text: String, stretchLevel: Int, targetIndex: Long = -1L): String {
        if (!isLoaded || stretchLevel <= 0 || text.isEmpty()) {
            return fallbackKashida(text, stretchLevel)
        }
        return try {
            nativeApplyKashida(text, stretchLevel, targetIndex)
        } catch (e: Throwable) {
            Log.e(TAG, "Error in nativeApplyKashida, using fallback", e)
            fallbackKashida(text, stretchLevel)
        }
    }

    fun getKashidaOpportunities(text: String): String {
        if (!isLoaded || text.isEmpty()) return "[]"
        return try {
            nativeFindOpportunitiesJson(text)
        } catch (e: Throwable) {
            "[]"
        }
    }

    private fun fallbackKashida(text: String, stretchLevel: Int): String {
        if (stretchLevel <= 0 || text.length < 2) return text
        val tatweel = "ـ".repeat(stretchLevel)
        // Simple dual-joining letters fallback
        val dualJoining = setOf('ب', 'ت', 'ث', 'ج', 'ح', 'خ', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ع', 'غ', 'ف', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'ي')
        val sb = StringBuilder()
        for (i in 0 until text.length) {
            sb.append(text[i])
            if (i < text.length - 1 && text[i] in dualJoining) {
                sb.append(tatweel)
                break
            }
        }
        return sb.toString()
    }

    // JNI Native Declarations
    @JvmStatic
    private external fun nativeGetVersion(): String

    @JvmStatic
    private external fun nativeApplyKashida(text: String, stretchLevel: Int, targetIndex: Long): String

    @JvmStatic
    private external fun nativeFindOpportunitiesJson(text: String): String
}
