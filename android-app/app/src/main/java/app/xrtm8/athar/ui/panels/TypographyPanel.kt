package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.ui.components.FontPickerCarousel
import app.xrtm8.athar.ui.components.KashidaModeSelector
import app.xrtm8.athar.ui.components.KashidaOpportunityChips
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*
import java.util.Locale

/**
 * Pure Arabic Typography & Font Styling Panel.
 * Focused on: Font selection, 6sp-100sp Font size, Line balancing / Justification,
 * Calligraphic styles, Alignment, and Typographic Spacing.
 */
@Composable
fun TypographyPanel(
    state: CanvasState,
    onOpenTextEditor: () -> Unit,
    onOpenFontBrowser: () -> Unit,
    onSelectFont: (ArabicFontFamily) -> Unit,
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
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Quick Text Editor Hero Trigger Bar
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenTextEditor()
            },
            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderActive),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تعديل النص والتشكيل",
                        color = PureWhite,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = state.rawText.take(18).ifBlank { "اكتب نصك هنا..." },
                    color = GoldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Arabic Font Header with "مكتبة الخطوط والمفضلة" Browser Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الخط العربي:",
                color = MutedWhite,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassSurface)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenFontBrowser()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "كافة الخطوط والمفضلة",
                    color = GoldAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        FontPickerCarousel(
            selectedFont = state.selectedFont,
            onSelectFont = onSelectFont
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Font Size Slider (Range from 6sp to 100sp)
        StudioSlider(
            title = "حجم الخط",
            value = state.fontSize,
            onValueChange = onFontSizeChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 6f..100f,
            step = 1f,
            valueDisplay = "${state.fontSize.toInt()} sp",
            icon = Icons.Default.FormatSize
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Equal-Width Arabic Paragraph Justification Button (if multiline)
        if (state.rawText.contains("\n")) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onJustifyLines()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatAlignJustify,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "موازنة أطوال الأسطر بالتساوي (كشيدة متزنة)",
                    color = PureWhite,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 5. Styling Chips: Bold, Italic, Underline + Paragraph Flow (Right/Center/Left)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Text Styling
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isBold) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (state.isBold) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleBold()
                        }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FormatBold,
                        contentDescription = "عريض",
                        tint = if (state.isBold) AmoledBlack else PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isItalic) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (state.isItalic) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleItalic()
                        }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FormatItalic,
                        contentDescription = "مائل",
                        tint = if (state.isItalic) AmoledBlack else PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isUnderline) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (state.isUnderline) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleUnderline()
                        }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FormatUnderlined,
                        contentDescription = "تسطير",
                        tint = if (state.isUnderline) AmoledBlack else PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Text Flow / RTL Alignment Options
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassSurface)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    TextAlignOption.RIGHT to Icons.AutoMirrored.Filled.FormatAlignRight,
                    TextAlignOption.CENTER to Icons.Default.FormatAlignCenter,
                    TextAlignOption.LEFT to Icons.AutoMirrored.Filled.FormatAlignLeft
                ).forEach { (align, icon) ->
                    val isSelected = state.textAlign == align
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GoldAccent else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTextAlignChange(align)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = align.label,
                            tint = if (isSelected) AmoledBlack else SubtleGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (onAutoBalanceBlock != null && state.displayText.contains("\n")) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassCard)
                    .border(0.5.dp, CherryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAutoBalanceBlock()
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatAlignJustify,
                        contentDescription = "محاذاة الأسطر بالمد",
                        tint = CherryAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "محاذاة الأسطر بالمد (ميزان الفقرة)",
                        color = CherryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Text Box Area & Multi-Line Word Wrapping Control (مساحة النص والتفاف الأسطر)
        StudioSlider(
            title = "عرض مساحة النص (التفاف الأسطر)",
            value = (state.boxWidthRatio * 100f).coerceIn(30f, 100f),
            onValueChange = { onBoxWidthRatioChange(it / 100f) },
            valueRange = 30f..100f,
            valueDisplay = "${((state.boxWidthRatio * 100f).toInt())}%",
            onValueChangeFinished = onSliderChangeFinished
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0.50f to "عمودي 50%",
                0.70f to "موجز 70%",
                0.85f to "متوازن 85%",
                1.00f to "كامل 100%"
            ).forEach { (ratio, label) ->
                val isSelected = kotlin.math.abs(state.boxWidthRatio - ratio) < 0.05f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CherryAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBoxWidthRatioChange(ratio)
                            onSliderChangeFinished?.invoke()
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) AmoledBlack else PureWhite,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // 6. Kashida & Calligraphic Elongation (المد والتطويل الكاليغرافي)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LinearScale,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "المد والتطويل الكاليغرافي",
                    color = PureWhite,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(GlassSurface)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStretchLevelChange(0)
                        onTargetIndexChange(-1)
                        onSliderChangeFinished?.invoke()
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "إلغاء المد",
                    color = SubtleGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        StudioSlider(
            title = "مستوى المد",
            value = state.stretchLevel.toFloat(),
            onValueChange = { onStretchLevelChange(it.toInt()) },
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..8f,
            step = 1f,
            steps = 7,
            valueDisplay = "${state.stretchLevel}x",
            icon = Icons.Default.LinearScale
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0 to "بدون",
                2 to "2x",
                4 to "4x",
                6 to "6x",
                8 to "8x"
            ).forEach { (lvl, title) ->
                val isSelected = state.stretchLevel == lvl
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStretchLevelChange(lvl)
                            onSliderChangeFinished?.invoke()
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) AmoledBlack else PureWhite,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "نمط توزيع المد:",
            color = MutedWhite,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        KashidaModeSelector(
            selectedMode = state.kashidaMode,
            onSelectMode = onKashidaModeChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "تمديد حرف محدد:",
                color = MutedWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        KashidaOpportunityChips(
            rawText = state.rawText,
            currentMode = state.kashidaMode,
            targetIndex = state.kashidaTargetIndex,
            onSelectOpportunity = onTargetIndexChange
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // 7. Typographic Spacing Sliders
        StudioSlider(
            title = "تباعد الأسطر",
            value = state.lineSpacingMultiplier,
            onValueChange = onLineSpacingChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 1.0f..2.5f,
            step = 0.05f,
            valueDisplay = String.format(Locale.US, "%.2fx", state.lineSpacingMultiplier),
            icon = Icons.Default.FormatLineSpacing
        )

        Spacer(modifier = Modifier.height(6.dp))

        StudioSlider(
            title = "تباعد الحروف",
            value = state.letterSpacing,
            onValueChange = onLetterSpacingChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -0.05f..0.35f,
            step = 0.01f,
            valueDisplay = String.format(Locale.US, "%.2f", state.letterSpacing),
            icon = Icons.Default.SpaceBar
        )

        Spacer(modifier = Modifier.height(6.dp))

        StudioSlider(
            title = "شفافية النص",
            value = state.textOpacity,
            onValueChange = onOpacityChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0.1f..1.0f,
            step = 0.02f,
            valueDisplay = "${(state.textOpacity * 100).toInt()}%",
            icon = Icons.Default.Opacity
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // 7. Curved / Arc Path Typography
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "تقويس النص (مسار قوسي)",
                    color = PureWhite,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ثني الكلمات على مسار دائري متزن",
                    color = SubtleGray,
                    fontSize = 10.5.sp
                )
            }

            Switch(
                checked = state.isCurved,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleCurvedText(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmoledBlack,
                    checkedTrackColor = GoldAccent,
                    uncheckedThumbColor = SubtleGray,
                    uncheckedTrackColor = GlassCard
                )
            )
        }

        if (state.isCurved) {
            Spacer(modifier = Modifier.height(6.dp))
            StudioSlider(
                title = "زاوية انحناء القوس",
                value = state.curveAngle,
                onValueChange = onCurveAngleChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = -180f..180f,
                step = 5f,
                valueDisplay = "${state.curveAngle.toInt()}°",
                icon = Icons.Default.LinearScale
            )
        }
    }
}
