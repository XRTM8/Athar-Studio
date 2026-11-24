package app.xrtm8.athar.ui.panels.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import app.xrtm8.athar.model.CanvasGradientDirection
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.dialogs.CustomColorPickerDialog
import app.xrtm8.athar.ui.theme.*

@Composable
fun GradientScrimSection(
    state: CanvasState,
    onToggleGradientScrim: (Boolean) -> Unit,
    onGradientScrimDirectionChange: (CanvasGradientDirection) -> Unit,
    onGradientScrimColorChange: (Long) -> Unit,
    onGradientScrimOpacityChange: (Float) -> Unit,
    onGradientScrimMidpointChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var showCustomScrimColorDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تفعيل الظل المتدرج",
                    color = PureWhite,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "إسدال تدرج سينمائي يبرز النصوص فوق الصورة أو الخلفية",
                    color = SubtleGray,
                    fontSize = 10.sp
                )
            }

            Switch(
                checked = state.gradientScrim.isEnabled,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleGradientScrim(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = BurgundyAccent,
                    uncheckedTrackColor = StudioSurface
                )
            )
        }

        if (state.gradientScrim.isEnabled) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "اتجاه التدرج الظلي:", color = MutedWhite, fontSize = 11.5.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CanvasGradientDirection.entries.forEach { dir ->
                    val isSelected = state.gradientScrim.direction == dir
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) BurgundyAccent else GlassSurface)
                            .border(0.5.dp, if (isSelected) BurgundyAccent else GlassBorder, RoundedCornerShape(9.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onGradientScrimDirectionChange(dir)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = dir.label,
                            color = if (isSelected) PureWhite else MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "لون الظل:", color = MutedWhite, fontSize = 11.5.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Custom Color Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BurgundyAccent.copy(alpha = 0.2f))
                        .border(1.dp, BurgundyAccent, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showCustomScrimColorDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "لون ظل مخصص",
                        tint = BurgundyAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مخصص",
                        color = BurgundyAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                listOf(
                    0xFF000000L to "أسود داكن",
                    0xFF0A1128L to "كحلي ليلي",
                    0xFF1C1917L to "بني دافئ",
                    0xFF1A080EL to "عنابي فاخر",
                    0xFF081C15L to "زمردي داكن",
                    0xFF121418L to "رمادي فحمي"
                ).forEach { (colorLong, name) ->
                    val isSelected = state.gradientScrim.colorLong == colorLong
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BurgundyAccent else GlassSurface)
                            .border(0.5.dp, if (isSelected) BurgundyAccent else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onGradientScrimColorChange(colorLong)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong.toInt()))
                                .border(0.5.dp, if (isSelected) PureWhite else MutedWhite, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = name,
                            color = if (isSelected) PureWhite else MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            StudioSlider(
                title = "كثافة الظل",
                value = state.gradientScrim.opacity,
                onValueChange = onGradientScrimOpacityChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = 0.05f..1.0f,
                step = 0.05f,
                valueDisplay = "${(state.gradientScrim.opacity * 100).toInt()}%"
            )

            Spacer(modifier = Modifier.height(4.dp))

            StudioSlider(
                title = "نطاق امتداد الظل (المنتصف)",
                value = state.gradientScrim.midpoint,
                onValueChange = onGradientScrimMidpointChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = 0.1f..0.9f,
                step = 0.05f,
                valueDisplay = "${(state.gradientScrim.midpoint * 100).toInt()}%"
            )
        }

        if (showCustomScrimColorDialog) {
            CustomColorPickerDialog(
                initialColorLong = state.gradientScrim.colorLong,
                title = "منتقي لون الظل المتدرج",
                onColorSelected = { onGradientScrimColorChange(it) },
                onDismiss = { showCustomScrimColorDialog = false }
            )
        }
    }
}
