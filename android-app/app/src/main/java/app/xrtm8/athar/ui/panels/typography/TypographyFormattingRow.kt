package app.xrtm8.athar.ui.panels.typography

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
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
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.ui.theme.*

@Composable
fun TypographyFormattingRow(
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    textAlign: TextAlignOption,
    hasMultiline: Boolean,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onTextAlignChange: (TextAlignOption) -> Unit,
    onJustifyLines: () -> Unit,
    onAutoBalanceBlock: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Equal-Width Arabic Paragraph Justification Button (if multiline)
        if (hasMultiline) {
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

        // Styling Chips: Bold, Italic, Underline + Paragraph Flow (Right/Center/Left)
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
                        .background(if (isBold) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isBold) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
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
                        tint = if (isBold) AmoledBlack else PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isItalic) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isItalic) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
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
                        tint = if (isItalic) AmoledBlack else PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isUnderline) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isUnderline) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
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
                        tint = if (isUnderline) AmoledBlack else PureWhite,
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
                    val isSelected = textAlign == align
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

        if (onAutoBalanceBlock != null && hasMultiline) {
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
    }
}
