package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.theme.*

/**
 * High-precision luxury studio slider with numeric badge, micro-increment buttons,
 * and haptic feedback for professional design control.
 */
@Composable
fun StudioSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueDisplay: String = String.format(java.util.Locale.US, "%.2f", value),
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    step: Float = (valueRange.endInclusive - valueRange.start) / 20f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Value Badge with micro-adjust buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassSurface)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                // Micro decrement button
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable {
                            val newVal = (value - step).coerceIn(valueRange.start, valueRange.endInclusive)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange(newVal)
                            onValueChangeFinished?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "إنقاص",
                        tint = if (value > valueRange.start) MutedWhite else SubtleGray,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = valueDisplay,
                    color = GoldAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Micro increment button
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable {
                            val newVal = (value + step).coerceIn(valueRange.start, valueRange.endInclusive)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange(newVal)
                            onValueChangeFinished?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "زيادة",
                        tint = if (value < valueRange.endInclusive) MutedWhite else SubtleGray,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Slider(
            value = value,
            onValueChange = { newVal ->
                if (steps > 0 && newVal.toInt() != value.toInt()) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onValueChange(newVal)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = GoldAccent,
                activeTrackColor = GoldAccent,
                inactiveTrackColor = GlassSurface,
                activeTickColor = AmoledBlack,
                inactiveTickColor = GlassBorder
            ),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
    }
}

@Composable
fun StudioSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float = (range.endInclusive - range.start) / 20f,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) = StudioSlider(
    title = title,
    value = value,
    valueRange = range,
    valueDisplay = String.format(java.util.Locale.US, "%.2f", value),
    step = step,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished
)
