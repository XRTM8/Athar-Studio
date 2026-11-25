package app.xrtm8.athar.ui.panels.background

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
 * Spatial geometry, hue shift, orientation, and blend-mode controls for Mask Studio 3.0.
 * Extracted from MaskAdjustmentsSection to honor ARCHITECTURE.md Small Units (~230 line cap).
 */
@Composable
fun MaskTransformControls(
    state: AtharMaskState,
    onScaleChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onHueShiftChange: (Float) -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onRotate90: () -> Unit,
    onBlendModeChange: (MaskBlendMode) -> Unit,
    onSliderChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Scale
        StudioSlider(
            title = "حجم القناع وتكبيره",
            value = state.scale,
            onValueChange = onScaleChange,
            valueRange = 0.5f..3.0f,
            valueDisplay = String.format(java.util.Locale.US, "%.1fx", state.scale),
            icon = Icons.Default.ZoomIn,
            onValueChangeFinished = onSliderChangeFinished
        )

        // Offset X
        StudioSlider(
            title = "موضع القناع الأفقي (X)",
            value = state.offsetX,
            onValueChange = onOffsetXChange,
            valueRange = -1f..1f,
            valueDisplay = String.format(java.util.Locale.US, "%+.2f", state.offsetX),
            icon = Icons.Default.SwapHoriz,
            onValueChangeFinished = onSliderChangeFinished
        )

        // Offset Y
        StudioSlider(
            title = "موضع القناع الرأسي (Y)",
            value = state.offsetY,
            onValueChange = onOffsetYChange,
            valueRange = -1f..1f,
            valueDisplay = String.format(java.util.Locale.US, "%+.2f", state.offsetY),
            icon = Icons.Default.SwapVert,
            onValueChangeFinished = onSliderChangeFinished
        )

        // Hue Shift (only when untinted)
        if (state.tintColorLong == null) {
            StudioSlider(
                title = "صبغة اللون الطيفية (Hue Shift)",
                value = state.hueShift,
                onValueChange = onHueShiftChange,
                valueRange = -180f..180f,
                valueDisplay = "${state.hueShift.toInt()}°",
                icon = Icons.Default.Palette,
                onValueChangeFinished = onSliderChangeFinished
            )
        }

        // Flip H, Flip V, Rotate 90
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaskActionToggleButton(
                active = state.isFlippedH,
                label = "قلب أفقي",
                icon = Icons.Default.Flip,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleFlipH() },
                modifier = Modifier.weight(1f)
            )
            MaskActionToggleButton(
                active = state.isFlippedV,
                label = "قلب رأسي",
                icon = Icons.Default.VerticalAlignCenter,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleFlipV() },
                modifier = Modifier.weight(1f)
            )
            MaskActionToggleButton(
                active = state.rotationDegrees != 0,
                label = "${state.rotationDegrees}°",
                icon = Icons.AutoMirrored.Filled.RotateRight,
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onRotate90() },
                modifier = Modifier.weight(1f)
            )
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
                            .padding(horizontal = 9.dp, vertical = 5.dp)
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

@Composable
internal fun MaskActionToggleButton(
    active: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = if (active) BurgundyAccent else StudioSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(0.5.dp, if (active) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (active) PureWhite else MutedWhite)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = if (active) PureWhite else MutedWhite, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}
