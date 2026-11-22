package app.xrtm8.athar.ui.panels.background

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.BlackGradientPlacement
import app.xrtm8.athar.model.BlackGradientState
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

/**
 * Atmospheric Black Gradient Section:
 * High-legibility black atmospheric gradient behind typography (Bottom, Top, Left, Right, Radial, Bars).
 */
@Composable
fun BlackGradientSection(
    gradient: BlackGradientState,
    onToggle: (Boolean) -> Unit,
    onPlacementChange: (BlackGradientPlacement) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onCoverageChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gradient,
                    contentDescription = null,
                    tint = if (gradient.isEnabled) CherryAccent else SubtleGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تدرج الظلال الأسود (Black Gradient)",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Switch(
                checked = gradient.isEnabled,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = BurgundyAccent,
                    uncheckedTrackColor = StudioSurface
                )
            )
        }

        AnimatedVisibility(
            visible = gradient.isEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Placement / Direction Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "موضع واتجاه التدرج الأسود",
                        color = MutedWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BlackGradientPlacement.values().forEach { plc ->
                            val isSelected = gradient.placement == plc
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BurgundyAccent else StudioSurface)
                                    .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPlacementChange(plc)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = plc.label,
                                    color = if (isSelected) PureWhite else MutedWhite,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Black Gradient Opacity Slider
                StudioSlider(
                    title = "شفافية التدرج الأسود",
                    value = gradient.opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..1f,
                    valueDisplay = "${(gradient.opacity * 100).toInt()}%",
                    icon = Icons.Default.BrightnessMedium
                )

                // Black Gradient Coverage Slider
                StudioSlider(
                    title = "امتداد وتغطية التدرج",
                    value = gradient.coverage,
                    onValueChange = onCoverageChange,
                    valueRange = 0.20f..1.0f,
                    valueDisplay = "${(gradient.coverage * 100).toInt()}%",
                    icon = Icons.Default.LinearScale
                )
            }
        }
    }
}
