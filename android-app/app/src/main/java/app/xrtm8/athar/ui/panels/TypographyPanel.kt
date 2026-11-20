package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.ui.components.FontPickerCarousel
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*
import java.util.Locale

/**
 * Pure Arabic Typography & Font Styling Panel.
 * Focused on: Font selection, Font size, Calligraphic styles, Alignment, and Typographic Spacing.
 */
@Composable
fun TypographyPanel(
    state: CanvasState,
    onOpenTextEditor: () -> Unit,
    onSelectFont: (ArabicFontFamily) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onTextAlignChange: (TextAlignOption) -> Unit,
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
                    text = state.rawText.take(18),
                    color = GoldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Arabic Font Families Carousel with authentic specimen previews
        Text(
            text = "الخط العربي:",
            color = MutedWhite,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        FontPickerCarousel(
            selectedFont = state.selectedFont,
            onSelectFont = onSelectFont
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Font Size Slider
        StudioSlider(
            title = "حجم الخط",
            value = state.fontSize,
            onValueChange = onFontSizeChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 24f..110f,
            step = 2f,
            valueDisplay = "${state.fontSize.toInt()} sp",
            icon = Icons.Default.FormatSize
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Styling Chips: Bold, Italic, Underline + Paragraph Flow (Right/Center/Left)
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
                TextAlignOption.entries.forEach { align ->
                    val isSelected = state.textAlign == align
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GoldAccent else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTextAlignChange(align)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = align.label,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Advanced Spacing & Opacity
        StudioSlider(
            title = "شفافية النص",
            value = state.textOpacity,
            onValueChange = onOpacityChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0.15f..1.0f,
            step = 0.05f,
            valueDisplay = "${(state.textOpacity * 100).toInt()}%",
            icon = Icons.Default.Opacity
        )

        Spacer(modifier = Modifier.height(4.dp))

        StudioSlider(
            title = "تباعد الحروف",
            value = state.letterSpacing,
            onValueChange = onLetterSpacingChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -0.05f..0.25f,
            step = 0.01f,
            valueDisplay = String.format(Locale.ENGLISH, "%.2f", state.letterSpacing)
        )

        Spacer(modifier = Modifier.height(4.dp))

        StudioSlider(
            title = "تباعد الأسطر",
            value = state.lineSpacingMultiplier,
            onValueChange = onLineSpacingChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 1.0f..2.2f,
            step = 0.05f,
            valueDisplay = "${String.format(Locale.ENGLISH, "%.2f", state.lineSpacingMultiplier)}x"
        )
    }
}
