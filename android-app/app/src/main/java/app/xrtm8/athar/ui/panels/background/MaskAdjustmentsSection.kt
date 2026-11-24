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
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
 * Mask Studio 2.0 Precision Adjustments:
 * - Opacity, Contrast / Punch, Invert, Luminance Stencil, Custom Tinting,
 *   Scale, Offset (X, Y), Hue shift, Flip H/V, Rotation, and Extended Blend Modes.
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
            // 1. Opacity Slider
            StudioSlider(
                title = "كثافة وشفافية القناع",
                value = state.opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                valueDisplay = "${(state.opacity * 100).toInt()}%",
                icon = Icons.Default.Opacity,
                onValueChangeFinished = onSliderChangeFinished
            )

            // 2. Contrast & Punch Slider
            StudioSlider(
                title = "تباين القناع وحدته (Contrast)",
                value = state.contrast,
                onValueChange = onContrastChange,
                valueRange = 0.5f..2.5f,
                valueDisplay = String.format(java.util.Locale.US, "%.1fx", state.contrast),
                icon = Icons.Default.Contrast,
                onValueChangeFinished = onSliderChangeFinished
            )

            // 3. Quick Action Toggles: Invert Mask & Luminance Stencil Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Invert Mask Toggle
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleInvert()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isInverted) BurgundyAccent else StudioSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(0.5.dp, if (state.isInverted) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.InvertColors,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (state.isInverted) PureWhite else MutedWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.isInverted) "معكوس (Inverted)" else "عكس القناع",
                        fontSize = 11.sp,
                        color = if (state.isInverted) PureWhite else MutedWhite,
                        fontWeight = if (state.isInverted) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Luminance Stencil Toggle
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleLuminanceMode()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isLuminanceMode) BurgundyAccent else StudioSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(0.5.dp, if (state.isLuminanceMode) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (state.isLuminanceMode) PureWhite else MutedWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.isLuminanceMode) "استنسل شفاف" else "قناع استنسل",
                        fontSize = 11.sp,
                        color = if (state.isLuminanceMode) PureWhite else MutedWhite,
                        fontWeight = if (state.isLuminanceMode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // 4. Mask Tinting Palette
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "تلوين القناع (Mask Tint)",
                    color = MutedWhite,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Original / Untinted Chip
                    val isOriginal = state.tintColorLong == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isOriginal) BurgundyAccent else StudioSurface)
                            .border(0.5.dp, if (isOriginal) CherryAccent else GlassBorder, RoundedCornerShape(9.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTintColorChange(null)
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "طبيعي / أصلي",
                            color = if (isOriginal) PureWhite else MutedWhite,
                            fontSize = 10.5.sp,
                            fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Custom Color Picker Trigger
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(BurgundyAccent.copy(alpha = 0.2f))
                            .border(1.dp, BurgundyAccent, RoundedCornerShape(9.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCustomTintDialog = true
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BurgundyAccent, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "مخصص", color = BurgundyAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Curated Luxury Color Swatches
                    listOf(
                        0xFFFFFFFFL to "أبيض ناصع",
                        0xFF000000L to "أسود داكن",
                        0xFFFFD54FL to "ذهب ملكي",
                        0xFFFF8F00L to "كهرمان دافئ",
                        0xFF00E5FFL to "سيان كهربائي",
                        0xFF9E1B42L to "عنابي مخملي",
                        0xFF00E676L to "زمردي نقي"
                    ).forEach { (colorLong, name) ->
                        val isSelected = state.tintColorLong == colorLong
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) BurgundyAccent else StudioSurface)
                                .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(9.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTintColorChange(colorLong)
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorLong.toInt()))
                                    .border(0.5.dp, if (isSelected) PureWhite else MutedWhite, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = name,
                                color = if (isSelected) PureWhite else MutedWhite,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 5. Scale Slider
            StudioSlider(
                title = "حجم القناع وتكبيره",
                value = state.scale,
                onValueChange = onScaleChange,
                valueRange = 0.5f..3.0f,
                valueDisplay = String.format(java.util.Locale.US, "%.1fx", state.scale),
                icon = Icons.Default.ZoomIn,
                onValueChangeFinished = onSliderChangeFinished
            )

            // 6. Offset X Slider
            StudioSlider(
                title = "موضع القناع الأفقي (X)",
                value = state.offsetX,
                onValueChange = onOffsetXChange,
                valueRange = -1f..1f,
                valueDisplay = String.format(java.util.Locale.US, "%+.2f", state.offsetX),
                icon = Icons.Default.SwapHoriz,
                onValueChangeFinished = onSliderChangeFinished
            )

            // 7. Offset Y Slider
            StudioSlider(
                title = "موضع القناع الرأسي (Y)",
                value = state.offsetY,
                onValueChange = onOffsetYChange,
                valueRange = -1f..1f,
                valueDisplay = String.format(java.util.Locale.US, "%+.2f", state.offsetY),
                icon = Icons.Default.SwapVert,
                onValueChangeFinished = onSliderChangeFinished
            )

            // 8. Hue Shift Slider (Only when not tinted)
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

            // 9. Quick Transform Buttons: Flip H, Flip V, Rotate 90
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
                    Icon(imageVector = Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(15.dp))
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
                    Icon(imageVector = Icons.Default.VerticalAlignCenter, contentDescription = null, modifier = Modifier.size(15.dp))
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
                    Icon(imageVector = Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${state.rotationDegrees}°", fontSize = 11.sp, color = PureWhite)
                }
            }

            // 10. Blend Modes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "نمط دمج القناع السينمائي (Blend Mode)",
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

    if (showCustomTintDialog) {
        CustomColorPickerDialog(
            initialColorLong = state.tintColorLong ?: 0xFFFFD54FL,
            title = "منتقي لون القناع المخصص",
            onColorSelected = {
                onTintColorChange(it)
                showCustomTintDialog = false
            },
            onDismiss = { showCustomTintDialog = false }
        )
    }
}
