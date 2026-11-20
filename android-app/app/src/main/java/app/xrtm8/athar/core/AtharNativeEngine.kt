package app.xrtm8.athar.core

import android.util.Log
import app.xrtm8.athar.model.KashidaMode

object AtharNativeEngine {
    private const val TAG = "AtharNativeEngine"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("athar_core")
            isLoaded = true
            Log.i(TAG, "Athar native Rust core loaded successfully. Version: ${nativeGetVersion()}")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library athar_core not found or failed to load. Fallback pure-engine enabled.", e)
            isLoaded = false
        } catch (e: Throwable) {
            Log.w(TAG, "Error initializing athar_core. Fallback pure-engine enabled.", e)
            isLoaded = false
        }
    }

    fun isNativeReady(): Boolean = isLoaded

    fun getEngineVersion(): String {
        return if (isLoaded) {
            try {
                nativeGetVersion()
            } catch (e: Throwable) {
                "1.6.0-pure"
            }
        } else {
            "1.6.0-pure"
        }
    }

    fun applyKashida(
        text: String,
        stretchLevel: Int,
        mode: KashidaMode = KashidaMode.SMART,
        targetIndex: Int = -1
    ): String {
        if (stretchLevel <= 0 || text.isEmpty()) {
            return text
        }
        return PureArabicKashidaEngine.stretchText(text, stretchLevel, mode, targetIndex)
    }

    fun findOpportunities(text: String): List<PureArabicKashidaEngine.KashidaOpportunity> {
        return PureArabicKashidaEngine.findOpportunities(text)
    }

    fun getKashidaOpportunities(text: String): String {
        if (!isLoaded || text.isEmpty()) return "[]"
        return try {
            nativeFindOpportunitiesJson(text)
        } catch (e: Throwable) {
            "[]"
        }
    }

    // JNI Native Declarations
    @JvmStatic
    private external fun nativeGetVersion(): String

    @JvmStatic
    private external fun nativeApplyKashida(text: String, stretchLevel: Int, targetIndex: Long): String

    @JvmStatic
    private external fun nativeFindOpportunitiesJson(text: String): String
}
