package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*
import java.util.Locale
import kotlin.math.abs

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
    onSnapPosition: (normX: Float, normY: Float, align: TextAlignOption?) -> Unit,
    onNudgeOffset: (deltaX: Float, deltaY: Float) -> Unit,
    modifier: Modifier = Modifier
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
            modifier = Modifier.fillMaxWidth().height(42.dp)
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
                    text = state.rawText.take(16),
                    color = GoldAccent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Unified 9-Point Spatial Positioning Matrix + Micro-Nudge D-Pad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassSurface)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "المحاذاة والتموضع الشامل (9-Point Matrix):",
                    color = PureWhite,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "X: ${(state.normOffsetX * 100).toInt()}%  Y: ${(state.normOffsetY * 100).toInt()}%",
                    color = GoldAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 9-Point Matrix Grid (3x3)
                Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val rows = listOf(
                        listOf(
                            Triple(0.28f, -0.30f, "أعلى يمين" to TextAlignOption.RIGHT),
                            Triple(0.0f, -0.30f, "أعلى وسط" to TextAlignOption.CENTER),
                            Triple(-0.28f, -0.30f, "أعلى يسار" to TextAlignOption.LEFT)
                        ),
                        listOf(
                            Triple(0.28f, 0.0f, "وسط يمين" to TextAlignOption.RIGHT),
                            Triple(0.0f, 0.0f, "مركز ✦" to TextAlignOption.CENTER),
                            Triple(-0.28f, 0.0f, "وسط يسار" to TextAlignOption.LEFT)
                        ),
                        listOf(
                            Triple(0.28f, 0.30f, "أسفل يمين" to TextAlignOption.RIGHT),
                            Triple(0.0f, 0.30f, "أسفل وسط" to TextAlignOption.CENTER),
                            Triple(-0.28f, 0.30f, "أسفل يسار" to TextAlignOption.LEFT)
                        )
                    )

                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { (x, y, meta) ->
                                val (label, align) = meta
                                val isSnapped = abs(state.normOffsetX - x) < 0.05f &&
                                        abs(state.normOffsetY - y) < 0.05f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSnapped) GoldAccent else GlassBackground)
                                        .border(0.5.dp, if (isSnapped) GoldAccent else GlassBorder, RoundedCornerShape(6.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSnapPosition(x, y, align)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSnapped) AmoledBlack else PureWhite,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSnapped) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right: 4-Way Micro-Nudge D-Pad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.85f)
                ) {
                    // Nudge Up
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlassBackground)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNudgeOffset(0f, -0.015f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "أعلى", tint = PureWhite, modifier = Modifier.size(18.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        // Right Nudge (Nudge X+)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassBackground)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNudgeOffset(0.015f, 0f)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "يمين", tint = PureWhite, modifier = Modifier.size(18.dp))
                        }

                        // Center Reset
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassBackground)
                                .border(0.5.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSnapPosition(0f, 0f, TextAlignOption.CENTER)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✦", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Left Nudge (Nudge X-)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassBackground)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNudgeOffset(-0.015f, 0f)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "يسار", tint = PureWhite, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Nudge Down
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GlassBackground)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNudgeOffset(0f, 0.015f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = PureWhite, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Arabic Font Families Carousel
        Text(text = "اختر الخط العربي الأصيل:", color = MutedWhite, fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArabicFontFamily.entries.forEach { font ->
                val isSelected = state.selectedFont == font
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) GoldAccent else GlassBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectFont(font)
                        }
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = font.specimen,
                            color = if (isSelected) AmoledBlack else GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = font.fontName,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Font Size Slider
        StudioSlider(
            title = "حجم الخط",
            value = state.fontSize,
            onValueChange = onFontSizeChange,
            valueRange = 24f..110f,
            step = 2f,
            valueDisplay = "${state.fontSize.toInt()} sp",
            icon = Icons.Default.FormatSize
        )

        Spacer(modifier = Modifier.height(6.dp))

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
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleBold()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleItalic()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleUnderline()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                            .padding(horizontal = 10.dp, vertical = 5.dp)
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

        Spacer(modifier = Modifier.height(6.dp))

        // 6. Text Opacity Slider
        StudioSlider(
            title = "شفافية النص",
            value = state.textOpacity,
            onValueChange = onOpacityChange,
            valueRange = 0.15f..1.0f,
            step = 0.05f,
            valueDisplay = "${(state.textOpacity * 100).toInt()}%",
            icon = Icons.Default.Opacity
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 7. Letter Spacing Slider
        StudioSlider(
            title = "تباعد الحروف (Letter Spacing)",
            value = state.letterSpacing,
            onValueChange = onLetterSpacingChange,
            valueRange = -0.05f..0.25f,
            step = 0.01f,
            valueDisplay = String.format(Locale.ENGLISH, "%.2f", state.letterSpacing)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 8. Line Spacing Slider
        StudioSlider(
            title = "تباعد الأسطر (Line Spacing)",
            value = state.lineSpacingMultiplier,
            onValueChange = onLineSpacingChange,
            valueRange = 1.0f..2.2f,
            step = 0.05f,
            valueDisplay = "${String.format(Locale.ENGLISH, "%.2f", state.lineSpacingMultiplier)}x"
        )
    }
}
