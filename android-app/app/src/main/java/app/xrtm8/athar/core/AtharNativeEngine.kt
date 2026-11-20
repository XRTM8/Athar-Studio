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
            try {
                Log.i(TAG, "Athar native Rust core loaded successfully. Version: ${nativeGetVersion()}")
            } catch (_: Throwable) {
                // Ignore log if in unit test environment
            }
        } catch (_: Throwable) {
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

    private external fun nativeGetVersion(): String
}
