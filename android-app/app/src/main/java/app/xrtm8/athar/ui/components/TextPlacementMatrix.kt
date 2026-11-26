package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.ui.theme.*
import kotlin.math.abs

/**
 * High-precision 9-Point Spatial Canvas Positioning Matrix + 4-Way Micro-Nudge D-Pad.
 */
@Composable
fun TextPlacementMatrix(
    normOffsetX: Float,
    normOffsetY: Float,
    onSnapPosition: (normX: Float, normY: Float, align: TextAlignOption) -> Unit,
    onNudgeOffset: (deltaX: Float, deltaY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
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
                text = "شبكة المحاذاة الفورية:",
                color = PureWhite,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "X: ${(normOffsetX * 100).toInt()}%  Y: ${(normOffsetY * 100).toInt()}%",
                color = CherryAccent,
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
                        Triple(0.0f, -0.30f, "أعلى" to TextAlignOption.CENTER),
                        Triple(-0.28f, -0.30f, "أعلى يسار" to TextAlignOption.LEFT)
                    ),
                    listOf(
                        Triple(0.28f, 0.0f, "يمين" to TextAlignOption.RIGHT),
                        Triple(0.0f, 0.0f, "مركز" to TextAlignOption.CENTER),
                        Triple(-0.28f, 0.0f, "يسار" to TextAlignOption.LEFT)
                    ),
                    listOf(
                        Triple(0.28f, 0.30f, "أسفل يمين" to TextAlignOption.RIGHT),
                        Triple(0.0f, 0.30f, "أسفل" to TextAlignOption.CENTER),
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
                            val isSnapped = abs(normOffsetX - x) < 0.05f &&
                                    abs(normOffsetY - y) < 0.05f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSnapped) CherryAccent else GlassBackground)
                                    .border(0.5.dp, if (isSnapped) CherryAccent else GlassBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSnapPosition(x, y, align)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSnapped) app.xrtm8.athar.ui.theme.AmoledWhite else PureWhite,
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
                            .border(0.5.dp, CherryAccent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSnapPosition(0f, 0f, TextAlignOption.CENTER)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", color = CherryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
}
