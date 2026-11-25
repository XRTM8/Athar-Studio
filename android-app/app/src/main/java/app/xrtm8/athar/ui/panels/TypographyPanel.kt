package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.ui.panels.typography.*
import app.xrtm8.athar.ui.theme.GlassBorder

/**
 * Clean Typography & Calligraphy Panel Coordinator.
 * Modularized into small, focused sub-units in app.xrtm8.athar.ui.panels.typography.*:
 * - TypographyEditorTrigger
 * - TypographyFontSection
 * - TypographyMetricsSection
 * - TypographyFormattingRow
 * - TypographyKashidaSection
 * - TypographySpacingSection
 */
@Composable
fun TypographyPanel(
    state: CanvasState,
    onOpenTextEditor: () -> Unit,
    onOpenFontBrowser: () -> Unit,
    onSelectFont: (ArabicFontFamily) -> Unit,
    onFontWeightChange: (Int) -> Unit = {},
    onFontSizeChange: (Float) -> Unit,
    onJustifyLines: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onTextAlignChange: (TextAlignOption) -> Unit,
    onBoxWidthRatioChange: (Float) -> Unit = {},
    onStretchLevelChange: (Int) -> Unit = {},
    onKashidaModeChange: (KashidaMode) -> Unit = {},
    onTargetIndexChange: (Int) -> Unit = {},
    onToggleCurvedText: (Boolean) -> Unit = {},
    onCurveAngleChange: (Float) -> Unit = {},
    onAutoBalanceBlock: (() -> Unit)? = null,
    onStylisticSetChange: (Int) -> Unit = {},
    onToggleSwash: (Boolean) -> Unit = {},
    onToggleDiscretionaryLigatures: (Boolean) -> Unit = {},
    onWordSpacingChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val supportedFeatures = remember(state.selectedFont, state.customFontPath) {
        AtharFontManager.getSupportedOpenTypeFeatures(context, state.selectedFont, state.customFontPath)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Text Editor Dialog Trigger
        TypographyEditorTrigger(
            rawText = state.rawText,
            onOpenTextEditor = onOpenTextEditor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Font Selection & Library Carousel
        TypographyFontSection(
            selectedFont = state.selectedFont,
            onSelectFont = onSelectFont,
            onOpenFontBrowser = onOpenFontBrowser,
            fontWeight = state.activeLayer.fontWeight,
            isBold = state.isBold,
            customFontPath = state.customFontPath,
            customFontName = state.activeLayer.customFontName,
            onFontWeightChange = onFontWeightChange
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Formatting, Alignment & Multiline Justification
        TypographyFormattingRow(
            isBold = state.isBold,
            isItalic = state.isItalic,
            isUnderline = state.isUnderline,
            textAlign = state.textAlign,
            hasMultiline = state.rawText.contains("\n"),
            onToggleBold = onToggleBold,
            onToggleItalic = onToggleItalic,
            onToggleUnderline = onToggleUnderline,
            onTextAlignChange = onTextAlignChange,
            onJustifyLines = onJustifyLines,
            onAutoBalanceBlock = onAutoBalanceBlock
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Font Size & Text Box Bounding Width (Line Wrapping)
        TypographyMetricsSection(
            fontSize = state.fontSize,
            boxWidthRatio = state.boxWidthRatio,
            onFontSizeChange = onFontSizeChange,
            onBoxWidthRatioChange = onBoxWidthRatioChange,
            onSliderChangeFinished = onSliderChangeFinished
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // 5. Kashida, Stretch Levels & Opportunity Placement
        TypographyKashidaSection(
            rawText = state.rawText,
            stretchLevel = state.stretchLevel,
            kashidaMode = state.kashidaMode,
            kashidaTargetIndex = state.kashidaTargetIndex,
            onStretchLevelChange = onStretchLevelChange,
            onKashidaModeChange = onKashidaModeChange,
            onTargetIndexChange = onTargetIndexChange,
            onSliderChangeFinished = onSliderChangeFinished
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // 6. Typographic Spacing & Curved Text
        TypographySpacingSection(
            lineSpacingMultiplier = state.lineSpacingMultiplier,
            letterSpacing = state.letterSpacing,
            textOpacity = state.textOpacity,
            isCurved = state.isCurved,
            curveAngle = state.curveAngle,
            onLineSpacingChange = onLineSpacingChange,
            onLetterSpacingChange = onLetterSpacingChange,
            onOpacityChange = onOpacityChange,
            onToggleCurvedText = onToggleCurvedText,
            onCurveAngleChange = onCurveAngleChange,
            onSliderChangeFinished = onSliderChangeFinished
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // 7. OpenType Stylistic Sets, Swash & Micro-Spacing
        TypographyOpenTypeSection(
            stylisticSet = state.stylisticSet,
            enableSwash = state.enableSwash,
            enableDiscretionaryLigatures = state.enableDiscretionaryLigatures,
            wordSpacing = state.wordSpacing,
            onStylisticSetChange = onStylisticSetChange,
            onToggleSwash = onToggleSwash,
            onToggleDiscretionaryLigatures = onToggleDiscretionaryLigatures,
            onWordSpacingChange = onWordSpacingChange,
            onSliderChangeFinished = onSliderChangeFinished,
            supportedFeatures = supportedFeatures
        )
    }
}
