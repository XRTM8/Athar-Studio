package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
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
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.theme.*

/**
 * Professional 3-Way Color Grading Studio Component:
 * - Lift (Shadows / المناطق المعتمة)
 * - Gamma (Midtones / النغمات المتوسطة)
 * - Gain (Highlights / الإضاءات العالية)
 * Real-time Skia 5x4 ColorMatrix mathematical grading.
 */
@Composable
fun ColorGradingWheelsPanel(
    state: CanvasState,
    onToggleColorGrading: (Boolean) -> Unit,
    onLiftChange: (r: Float, g: Float, b: Float) -> Unit,
    onGammaChange: (r: Float, g: Float, b: Float) -> Unit,
    onGainChange: (r: Float, g: Float, b: Float) -> Unit,
    onResetColorGrading: () -> Unit,
    onSliderChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Master Activation Switch & Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = if (state.colorGradingEnabled) CherryAccent else SubtleGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "التدريج اللوني السينمائي (Lift / Gamma / Gain)",
                    color = if (state.colorGradingEnabled) PureWhite else MutedWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.colorGradingEnabled) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onResetColorGrading()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "إعادة ضبط",
                            tint = SubtleGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Switch(
                    checked = state.colorGradingEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleColorGrading(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = app.xrtm8.athar.ui.theme.AmoledWhite,
                        checkedTrackColor = CherryAccent,
                        uncheckedThumbColor = SubtleGray,
                        uncheckedTrackColor = GlassSurface
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        if (state.colorGradingEnabled) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Lift (Shadows)
            ColorToneChannelSection(
                title = "🌒 الظلال (Lift / Shadows)",
                red = state.liftRed,
                green = state.liftGreen,
                blue = state.liftBlue,
                onValuesChange = onLiftChange,
                onSliderChangeFinished = onSliderChangeFinished
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Gamma (Midtones)
            ColorToneChannelSection(
                title = "🌓 النغمات الوسطى (Gamma / Midtones)",
                red = state.gammaRed,
                green = state.gammaGreen,
                blue = state.gammaBlue,
                onValuesChange = onGammaChange,
                onSliderChangeFinished = onSliderChangeFinished
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Gain (Highlights)
            ColorToneChannelSection(
                title = "🌔 الإضاءات العالية (Gain / Highlights)",
                red = state.gainRed,
                green = state.gainGreen,
                blue = state.gainBlue,
                onValuesChange = onGainChange,
                onSliderChangeFinished = onSliderChangeFinished
            )
        }
    }
}

@Composable
private fun ColorToneChannelSection(
    title: String,
    red: Float,
    green: Float,
    blue: Float,
    onValuesChange: (r: Float, g: Float, b: Float) -> Unit,
    onSliderChangeFinished: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "R:${(red * 100).toInt()}% G:${(green * 100).toInt()}% B:${(blue * 100).toInt()}%",
                color = CherryAccent,
                fontSize = 9.5.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Red Balance Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "أحمر", color = Color(0xFFFF5252), fontSize = 9.sp, modifier = Modifier.width(30.dp))
            Slider(
                value = red,
                onValueChange = { onValuesChange(it, green, blue) },
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = -0.5f..0.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF5252),
                    activeTrackColor = Color(0xFFFF5252),
                    inactiveTrackColor = GlassBorder
                ),
                modifier = Modifier.weight(1f).height(20.dp)
            )
        }

        // Green Balance Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "أخضر", color = Color(0xFF69F0AE), fontSize = 9.sp, modifier = Modifier.width(30.dp))
            Slider(
                value = green,
                onValueChange = { onValuesChange(red, it, blue) },
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = -0.5f..0.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF69F0AE),
                    activeTrackColor = Color(0xFF69F0AE),
                    inactiveTrackColor = GlassBorder
                ),
                modifier = Modifier.weight(1f).height(20.dp)
            )
        }

        // Blue Balance Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "أزرق", color = Color(0xFF448AFF), fontSize = 9.sp, modifier = Modifier.width(30.dp))
            Slider(
                value = blue,
                onValueChange = { onValuesChange(red, green, it) },
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = -0.5f..0.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF448AFF),
                    activeTrackColor = Color(0xFF448AFF),
                    inactiveTrackColor = GlassBorder
                ),
                modifier = Modifier.weight(1f).height(20.dp)
            )
        }
    }
}
