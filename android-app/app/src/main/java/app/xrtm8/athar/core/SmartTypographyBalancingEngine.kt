package app.xrtm8.athar.core

import app.xrtm8.athar.typography.ArabicParagraphJustifier

/**
 * Optical Typographic Block Balancing Engine.
 * Consolidated into [ArabicParagraphJustifier.balanceArabicBlock].
 */
object SmartTypographyBalancingEngine {

    fun balanceArabicBlock(text: String): String {
        return ArabicParagraphJustifier.balanceArabicBlock(text)
    }
}
