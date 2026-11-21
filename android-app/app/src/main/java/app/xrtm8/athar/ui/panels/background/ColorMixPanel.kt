package app.xrtm8.athar.ui.panels.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
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
import app.xrtm8.athar.model.ColorMixChannel
import app.xrtm8.athar.model.ColorMixState
import app.xrtm8.athar.ui.theme.*

@Composable
fun ColorMixPanel(
    state: ColorMixState,
    onChannelAdjust: (ColorMixChannel, hue: Float, sat: Float, lum: Float) -> Unit,
    onIsolateChannel: (ColorMixChannel?) -> Unit,
    onMonochromeChannel: (ColorMixChannel?) -> Unit,
    onResetChannel: (ColorMixChannel) -> Unit,
    onResetAll: () -> Unit,
    onSliderChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedChannel by remember { mutableStateOf(ColorMixChannel.RED) }
    val adj = state.getAdjustment(selectedChannel)

    val isIsolated = state.isolatedChannel == selectedChannel
    val isMonochrome = state.monochromeChannel == selectedChannel

    Column(modifier = modifier.fillMaxWidth()) {
        // Channel Selector: 8 Lightroom Color Swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorMixChannel.values().forEach { channel ->
                val isSelected = (selectedChannel == channel)
                val hasAdj = !state.getAdjustment(channel).isDefault || state.isolatedChannel == channel || state.monochromeChannel == channel
                val swatchColor = Color(channel.swatchColorLong)

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) StudioSurfaceElevated else Color.Transparent)
                        .border(
                            width = if (isSelected) 2.dp else if (hasAdj) 1.dp else 0.dp,
                            color = if (isSelected) BurgundyAccent else if (hasAdj) swatchColor.copy(alpha = 0.6f) else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedChannel = channel
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 20.dp else 16.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Channel Label & Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(selectedChannel.swatchColorLong))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "قناة اللون: ${selectedChannel.labelAr}",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Isolate / Color Pop Toggle
                FilterChip(
                    selected = isIsolated,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onIsolateChannel(if (isIsolated) null else selectedChannel)
                    },
                    label = { Text(if (isIsolated) "معزول ✓" else "عزل اللون", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BurgundyAccent,
                        selectedLabelColor = PureWhite
                    )
                )

                // Monochrome Tint Toggle
                FilterChip(
                    selected = isMonochrome,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMonochromeChannel(if (isMonochrome) null else selectedChannel)
                    },
                    label = { Text(if (isMonochrome) "أحادي ✓" else "لون موحد", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BurgundyAccent,
                        selectedLabelColor = PureWhite
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Hue Shift Slider (-50 .. +50)
        ColorMixSlider(
            title = "تدرج اللون (Hue)",
            value = adj.hueShift,
            valueRange = -50f..50f,
            unit = "°",
            onValueChange = { onChannelAdjust(selectedChannel, it, adj.satShift, adj.lumShift) },
            onValueChangeFinished = onSliderChangeFinished
        )

        // 2. Saturation Slider (-100 .. +100)
        ColorMixSlider(
            title = "تشبع اللون (Saturation)",
            value = adj.satShift,
            valueRange = -100f..100f,
            unit = "%",
            onValueChange = { onChannelAdjust(selectedChannel, adj.hueShift, it, adj.lumShift) },
            onValueChangeFinished = onSliderChangeFinished
        )

        // 3. Luminance Slider (-100 .. +100)
        ColorMixSlider(
            title = "إضاءة اللون (Luminance)",
            value = adj.lumShift,
            valueRange = -100f..100f,
            unit = "%",
            onValueChange = { onChannelAdjust(selectedChannel, adj.hueShift, adj.satShift, it) },
            onValueChangeFinished = onSliderChangeFinished
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Channel Reset & Reset All Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onResetChannel(selectedChannel)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MutedWhite, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إعادة ضبط القناة", color = MutedWhite, fontSize = 11.sp)
            }

            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onResetAll()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("إعادة ضبط كل الألوان", color = BurgundyAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ColorMixSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, color = MutedWhite, fontSize = 11.5.sp)
            Text(
                text = "${if (value > 0) "+" else ""}${value.toInt()}$unit",
                color = PureWhite,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = BurgundyAccent,
                activeTrackColor = BurgundyAccent,
                inactiveTrackColor = StudioSurfaceElevated
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
