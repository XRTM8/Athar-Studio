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
import app.xrtm8.athar.model.AtharMaskState
import app.xrtm8.athar.model.MaskBlendMode
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.dialogs.CustomColorPickerDialog
import app.xrtm8.athar.ui.theme.*

/**
 * Mask Studio 3.0 Optical & Color Adjustments (< 230 lines per ARCHITECTURE.md).
 * Spatial transform, hue shift, and blend mode controls live in MaskTransformControls.
 */
@Composable
fun MaskAdjustmentsSection(
    state: AtharMaskState,
    onOpacityChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onToggleInvert: () -> Unit,
    onTintColorChange: (Long?) -> Unit,
    onToggleLuminanceMode: () -> Unit,
    onBlendModeChange: (MaskBlendMode) -> Unit,
    onHueShiftChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onRotate90: () -> Unit,
    onSliderChangeFinished: () -> Unit = {},
    onSoftnessChange: (Float) -> Unit = {},
    onWarmthChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showCustomTintDialog by remember { mutableStateOf(false) }
    val isMaskActive = state.isEnabled && (!state.activeMaskId.isNullOrEmpty() || !state.customMaskUri.isNullOrEmpty())

    AnimatedVisibility(
        visible = isMaskActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StudioSlider(
                title = "كثافة وشفافية القناع",
                value = state.opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                valueDisplay = "${(state.opacity * 100).toInt()}%",
                icon = Icons.Default.Opacity,
                onValueChangeFinished = onSliderChangeFinished
            )

            StudioSlider(
                title = "نعومة الظل (Softness)",
                value = state.softness,
                onValueChange = onSoftnessChange,
                valueRange = 0f..1f,
                valueDisplay = "${(state.softness * 100).toInt()}%",
                icon = Icons.Default.BlurOn,
                onValueChangeFinished = onSliderChangeFinished
            )

            if (state.tintColorLong == null) {
                StudioSlider(
                    title = "دفء الإضاءة (Warmth)",
                    value = state.warmth,
                    onValueChange = onWarmthChange,
                    valueRange = 0f..1f,
                    valueDisplay = "${(state.warmth * 100).toInt()}%",
                    icon = Icons.Default.WbSunny,
                    onValueChangeFinished = onSliderChangeFinished
                )
            }

            StudioSlider(
                title = "تباين القناع وحدته (Contrast)",
                value = state.contrast,
                onValueChange = onContrastChange,
                valueRange = 0.5f..2.5f,
                valueDisplay = String.format(java.util.Locale.US, "%.1fx", state.contrast),
                icon = Icons.Default.Contrast,
                onValueChangeFinished = onSliderChangeFinished
            )

            // Invert & Luminance Stencil toggles
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MaskActionToggleButton(
                    active = state.isInverted,
                    label = if (state.isInverted) "معكوس (Inverted)" else "عكس القناع",
                    icon = Icons.Default.InvertColors,
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleInvert() },
                    modifier = Modifier.weight(1f)
                )
                MaskActionToggleButton(
                    active = state.isLuminanceMode,
                    label = if (state.isLuminanceMode) "استنسل شفاف" else "قناع استنسل",
                    icon = Icons.Default.Layers,
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleLuminanceMode() },
                    modifier = Modifier.weight(1f)
                )
            }

            // Mask Tint Palette
            MaskTintPaletteRow(
                selectedTint = state.tintColorLong,
                onSelectTint = onTintColorChange,
                onOpenPicker = { showCustomTintDialog = true }
            )

            MaskTransformControls(
                state = state,
                onScaleChange = onScaleChange,
                onOffsetXChange = onOffsetXChange,
                onOffsetYChange = onOffsetYChange,
                onHueShiftChange = onHueShiftChange,
                onToggleFlipH = onToggleFlipH,
                onToggleFlipV = onToggleFlipV,
                onRotate90 = onRotate90,
                onBlendModeChange = onBlendModeChange,
                onSliderChangeFinished = onSliderChangeFinished
            )
        }
    }

    if (showCustomTintDialog) {
        CustomColorPickerDialog(
            initialColorLong = state.tintColorLong ?: 0xFFFFD54FL,
            title = "منتقي لون القناع المخصص",
            onColorSelected = { onTintColorChange(it); showCustomTintDialog = false },
            onDismiss = { showCustomTintDialog = false }
        )
    }
}

@Composable
private fun MaskTintPaletteRow(
    selectedTint: Long?,
    onSelectTint: (Long?) -> Unit,
    onOpenPicker: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "تلوين القناع (Mask Tint)", color = MutedWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isOriginal = selectedTint == null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isOriginal) BurgundyAccent else StudioSurface)
                    .border(0.5.dp, if (isOriginal) CherryAccent else GlassBorder, RoundedCornerShape(9.dp))
                    .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelectTint(null) }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(text = "أصلي", color = if (isOriginal) PureWhite else MutedWhite, fontSize = 10.5.sp, fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Normal)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(BurgundyAccent.copy(alpha = 0.2f))
                    .border(1.dp, BurgundyAccent, RoundedCornerShape(9.dp))
                    .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onOpenPicker() }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BurgundyAccent, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "مخصص", color = BurgundyAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }

            listOf(
                0xFFFFFFFFL to "أبيض",
                0xFFFFD54FL to "ذهبي",
                0xFFFF8F00L to "كهرماني",
                0xFF00E5FFL to "سيان",
                0xFF9E1B42L to "عنابي",
                0xFF00E676L to "زمردي",
                0xFF000000L to "أسود"
            ).forEach { (colorLong, name) ->
                val isSelected = selectedTint == colorLong
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) BurgundyAccent else StudioSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(9.dp))
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelectTint(colorLong) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(colorLong.toInt())).border(0.5.dp, if (isSelected) PureWhite else MutedWhite, CircleShape))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = name, color = if (isSelected) PureWhite else MutedWhite, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
