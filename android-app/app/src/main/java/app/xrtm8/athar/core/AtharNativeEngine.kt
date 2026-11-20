package app.xrtm8.athar.core

import app.xrtm8.athar.model.KashidaMode

/**
 * Pure Kotlin Athar Calligraphy & Typography Engine.
 * High performance, zero native binary dependencies, fully universal on all Android ABIs.
 */
object AtharNativeEngine {
    private const val ENGINE_VERSION = "2.0.0-pure"

    fun isNativeReady(): Boolean = true

    fun getEngineVersion(): String = ENGINE_VERSION

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
}
