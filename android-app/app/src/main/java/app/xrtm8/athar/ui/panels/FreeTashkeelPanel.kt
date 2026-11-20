package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.core.FreeTashkeelEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.LUXURY_SWATCHES
import app.xrtm8.athar.model.TashkeelItem
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

@Composable
fun FreeTashkeelPanel(
    state: CanvasState,
    onAddMark: (String) -> Unit,
    onRemoveMark: (String) -> Unit,
    onUpdateMark: (TashkeelItem) -> Unit,
    onNudgeMark: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val activeLayer = state.activeLayer
    val items = activeLayer.tashkeelItems
    var selectedItemId by remember(items) { mutableStateOf(items.firstOrNull()?.id) }

    val activeItem = items.find { it.id == selectedItemId } ?: items.firstOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "إضافة حركات تشكيل حرة",
            color = PureWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Symbols Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FreeTashkeelEngine.PRESET_TASHKEEL.forEach { def ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassCard)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddMark(def.symbol)
                        }
                ) {
                    Text(
                        text = def.symbol,
                        color = GoldAccent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isNotEmpty() && activeItem != null) {
            // Selected Items selector
            Text(
                text = "الحركات المضافة (${items.size}) - اختر للتعديل:",
                color = SubtleGray,
                fontSize = 11.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEach { item ->
                    val isSelected = (item.id == activeItem.id)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GoldAccent.copy(alpha = 0.2f) else GlassCard)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) GoldAccent else GlassBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedItemId = item.id
                            }
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = item.symbol,
                            color = if (isSelected) GoldAccent else PureWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Micro Nudge Controls & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تحريك دقيق بالحبة:",
                    color = MutedWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNudgeMark(activeItem.id, 0f, -0.05f)
                        },
                        modifier = Modifier.size(32.dp).background(GlassCard, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "أعلى", tint = GoldAccent)
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNudgeMark(activeItem.id, 0f, 0.05f)
                        },
                        modifier = Modifier.size(32.dp).background(GlassCard, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = GoldAccent)
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNudgeMark(activeItem.id, -0.05f, 0f)
                        },
                        modifier = Modifier.size(32.dp).background(GlassCard, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "يمين", tint = GoldAccent)
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNudgeMark(activeItem.id, 0.05f, 0f)
                        },
                        modifier = Modifier.size(32.dp).background(GlassCard, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "يسار", tint = GoldAccent)
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRemoveMark(activeItem.id)
                        },
                        modifier = Modifier.size(32.dp).background(Color(0x33FF3366), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scale Slider
            StudioSlider(
                title = "حجم الحركة",
                value = activeItem.scale,
                valueRange = 0.5f..2.5f,
                step = 0.05f,
                valueDisplay = String.format(java.util.Locale.US, "%.2fx", activeItem.scale),
                onValueChange = { newScale ->
                    onUpdateMark(activeItem.copy(scale = newScale))
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Color Swatches
            Text(text = "لون الحركة:", color = SubtleGray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LUXURY_SWATCHES.forEach { swatch ->
                    val isSelected = (activeItem.colorLong == swatch.argb)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(swatch.composeColor)
                            .border(if (isSelected) 2.dp else 0.5.dp, if (isSelected) GoldAccent else GlassBorder, CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateMark(activeItem.copy(colorLong = if (swatch.argb != 0L) swatch.argb else null))
                            }
                    )
                }
            }
        }
    }
}
