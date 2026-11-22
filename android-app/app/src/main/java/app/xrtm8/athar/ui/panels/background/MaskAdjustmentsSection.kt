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
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.AtharMaskState
import app.xrtm8.athar.model.MaskBlendMode
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

/**
 * Precision adjustments for active mask:
 * Opacity, Scale, Offset (X, Y), Hue shift, Flip H/V, Rotation, and Blend Mode.
 */
@Composable
fun MaskAdjustmentsSection(
    state: AtharMaskState,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (MaskBlendMode) -> Unit,
    onHueShiftChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onRotate90: () -> Unit,
    onSliderChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isMaskActive = state.isEnabled && (!state.activeMaskId.isNullOrEmpty() || !state.customMaskUri.isNullOrEmpty())

    AnimatedVisibility(
        visible = isMaskActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Opacity Slider
            StudioSlider(
                title = "شفافية القناع",
                value = state.opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                valueDisplay = "${(state.opacity * 100).toInt()}%",
                icon = Icons.Default.Opacity,
                onValueChangeFinished = onSliderChangeFinished
            )

            // Scale Slider
            StudioSlider(
                title = "حجم القناع وتكبيره",
                value = state.scale,
                onValueChange = onScaleChange,
                valueRange = 0.5f..2.5f,
                valueDisplay = String.format(java.util.Locale.US, "%.1fx", state.scale),
                icon = Icons.Default.ZoomIn,
                onValueChangeFinished = onSliderChangeFinished
            )

            // Offset X Slider
            StudioSlider(
                title = "موضع القناع الأفقي (X)",
                value = state.offsetX,
                onValueChange = onOffsetXChange,
                valueRange = -1f..1f,
                valueDisplay = String.format(java.util.Locale.US, "%+.2f", state.offsetX),
                icon = Icons.Default.SwapHoriz,
                onValueChangeFinished = onSliderChangeFinished
            )

            // Offset Y Slider
            StudioSlider(
                title = "موضع القناع الرأسي (Y)",
                value = state.offsetY,
                onValueChange = onOffsetYChange,
                valueRange = -1f..1f,
                valueDisplay = String.format(java.util.Locale.US, "%+.2f", state.offsetY),
                icon = Icons.Default.SwapVert,
                onValueChangeFinished = onSliderChangeFinished
            )

            // Hue Shift Slider (Only when mask is active)
            StudioSlider(
                title = "صبغة اللون (Hue Shift)",
                value = state.hueShift,
                onValueChange = onHueShiftChange,
                valueRange = -180f..180f,
                valueDisplay = "${state.hueShift.toInt()}°",
                icon = Icons.Default.Palette,
                onValueChangeFinished = onSliderChangeFinished
            )

            // Quick Transform Buttons: Flip H, Flip V, Rotate 90
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFlipH()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isFlippedH) BurgundyAccent else StudioSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(0.5.dp, if (state.isFlippedH) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "قلب أفقي", fontSize = 11.sp, color = PureWhite)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFlipV()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isFlippedV) BurgundyAccent else StudioSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(0.5.dp, if (state.isFlippedV) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.Default.VerticalAlignCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "قلب رأسي", fontSize = 11.sp, color = PureWhite)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRotate90()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.rotationDegrees != 0) BurgundyAccent else StudioSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(0.5.dp, if (state.rotationDegrees != 0) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${state.rotationDegrees}°", fontSize = 11.sp, color = PureWhite)
                }
            }

            // Blend Modes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "نمط دمج القناع (Blend Mode)",
                    color = PureWhite,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MaskBlendMode.values().forEach { mode ->
                        val isSelected = state.blendMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BurgundyAccent else StudioSurface)
                                .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBlendModeChange(mode)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) PureWhite else MutedWhite,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
