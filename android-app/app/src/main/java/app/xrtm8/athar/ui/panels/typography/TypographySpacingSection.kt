package app.xrtm8.athar.ui.panels.typography

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*
import java.util.Locale

@Composable
fun TypographySpacingSection(
    lineSpacingMultiplier: Float,
    letterSpacing: Float,
    textOpacity: Float,
    isCurved: Boolean,
    curveAngle: Float,
    onLineSpacingChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleCurvedText: (Boolean) -> Unit,
    onCurveAngleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        StudioSlider(
            title = "تباعد الأسطر",
            value = lineSpacingMultiplier,
            onValueChange = onLineSpacingChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 1.0f..2.5f,
            step = 0.05f,
            valueDisplay = String.format(Locale.US, "%.2fx", lineSpacingMultiplier),
            icon = Icons.Default.FormatLineSpacing
        )

        Spacer(modifier = Modifier.height(6.dp))

        StudioSlider(
            title = "تباعد الحروف",
            value = letterSpacing,
            onValueChange = onLetterSpacingChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -0.05f..0.35f,
            step = 0.01f,
            valueDisplay = String.format(Locale.US, "%.2f", letterSpacing),
            icon = Icons.Default.SpaceBar
        )

        Spacer(modifier = Modifier.height(6.dp))

        StudioSlider(
            title = "شفافية النص",
            value = textOpacity,
            onValueChange = onOpacityChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0.1f..1.0f,
            step = 0.02f,
            valueDisplay = "${(textOpacity * 100).toInt()}%",
            icon = Icons.Default.Opacity
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Curved / Arc Path Typography
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
                checked = isCurved,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleCurvedText(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmoledBlack,
                    checkedTrackColor = CherryAccent,
                    uncheckedThumbColor = SubtleGray,
                    uncheckedTrackColor = GlassCard
                )
            )
        }

        if (isCurved) {
            Spacer(modifier = Modifier.height(6.dp))
            StudioSlider(
                title = "زاوية انحناء القوس",
                value = curveAngle,
                onValueChange = onCurveAngleChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = -180f..180f,
                step = 5f,
                valueDisplay = "${curveAngle.toInt()}°",
                icon = Icons.Default.LinearScale
            )
        }
    }
}
