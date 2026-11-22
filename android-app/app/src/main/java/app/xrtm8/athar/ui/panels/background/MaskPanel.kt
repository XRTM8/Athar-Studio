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
import app.xrtm8.athar.model.*
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

/**
 * PicsArt-Grade Mask Studio & Atmospheric Black Gradient Panel.
 * Supports organic light leaks, prism bokeh, architectural shadows, and paper/glass textures
 * with precision positioning, scaling, flipping, 360° hue shifting, and directional black gradients.
 */
@Composable
fun MaskPanel(
    state: AtharMaskState,
    onSelectMask: (String?) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (MaskBlendMode) -> Unit,
    onHueShiftChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onRotate90: () -> Unit,
    onToggleBlackGradient: (Boolean) -> Unit,
    onBlackGradientOpacityChange: (Float) -> Unit,
    onBlackGradientPlacementChange: (BlackGradientPlacement) -> Unit,
    onBlackGradientCoverageChange: (Float) -> Unit,
    onResetMask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf(MaskCategory.ALL) }

    val filteredMasks = remember(selectedCategory) {
        if (selectedCategory == MaskCategory.ALL) {
            AtharMaskCatalog.ALL_MASKS
        } else {
            AtharMaskCatalog.ALL_MASKS.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header with Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نظام الأقنعة والتدرج الأسود (خلف النص)",
                color = PureWhite,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
            if (state.isEffectActive) {
                Text(
                    text = "إعادة تعيين",
                    color = CherryAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResetMask()
                    }
                )
            }
        }

        // 2. Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MaskCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BurgundyAccent else StudioSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = cat
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.label,
                        color = if (isSelected) PureWhite else MutedWhite,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 3. Mask Thumbnails Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "None" option
            val isNoneSelected = state.activeMaskId == null || !state.isEnabled
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isNoneSelected) BurgundyAccent.copy(alpha = 0.35f) else StudioSurface)
                    .border(
                        width = if (isNoneSelected) 1.5.dp else 0.5.dp,
                        color = if (isNoneSelected) CherryAccent else GlassBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMask(null)
                    }
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = if (isNoneSelected) CherryAccent else SubtleGray,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "بدون",
                    color = if (isNoneSelected) PureWhite else MutedWhite,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Mask Items
            filteredMasks.forEach { maskDef ->
                val isSelected = state.isEnabled && state.activeMaskId == maskDef.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BurgundyAccent.copy(alpha = 0.45f) else StudioSurface)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) CherryAccent else GlassBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectMask(maskDef.id)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    val icon = when (maskDef.category) {
                        MaskCategory.LIGHT_LEAKS -> Icons.Default.WbSunny
                        MaskCategory.PRISM_BOKEH -> Icons.Default.AutoAwesome
                        MaskCategory.SHADOWS -> Icons.Default.FilterFrames
                        MaskCategory.TEXTURES -> Icons.Default.Texture
                        MaskCategory.PAPER_GLASS -> Icons.Default.Layers
                        else -> Icons.Default.Flare
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CherryAccent.copy(alpha = 0.25f) else GlassSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) CherryAccent else MutedWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = maskDef.title,
                        color = if (isSelected) PureWhite else MutedWhite,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }

        // 4. Mask Fine-Tuning Controls (When a mask is selected)
        AnimatedVisibility(
            visible = state.isEnabled && !state.activeMaskId.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Opacity Slider
                StudioSlider(
                    title = "شفافية القناع",
                    value = state.opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..1f,
                    valueDisplay = "${(state.opacity * 100).toInt()}%",
                    icon = Icons.Default.Opacity
                )

                // Scale Slider
                StudioSlider(
                    title = "حجم القناع وتكبيره",
                    value = state.scale,
                    onValueChange = onScaleChange,
                    valueRange = 0.5f..2.5f,
                    valueDisplay = String.format(java.util.Locale.US, "%.1fx", state.scale),
                    icon = Icons.Default.ZoomIn
                )

                // Offset X Slider
                StudioSlider(
                    title = "موضع القناع الأفقي (X)",
                    value = state.offsetX,
                    onValueChange = onOffsetXChange,
                    valueRange = -1f..1f,
                    valueDisplay = String.format(java.util.Locale.US, "%+.2f", state.offsetX),
                    icon = Icons.Default.SwapHoriz
                )

                // Offset Y Slider
                StudioSlider(
                    title = "موضع القناع الرأسي (Y)",
                    value = state.offsetY,
                    onValueChange = onOffsetYChange,
                    valueRange = -1f..1f,
                    valueDisplay = String.format(java.util.Locale.US, "%+.2f", state.offsetY),
                    icon = Icons.Default.SwapVert
                )

                // Hue Shift Slider
                StudioSlider(
                    title = "صبغة اللون (Hue Shift)",
                    value = state.hueShift,
                    onValueChange = onHueShiftChange,
                    valueRange = -180f..180f,
                    valueDisplay = "${state.hueShift.toInt()}°",
                    icon = Icons.Default.Palette
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
                        modifier = Modifier.weight(1f).border(0.5.dp, if (state.isFlippedH) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
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
                        modifier = Modifier.weight(1f).border(0.5.dp, if (state.isFlippedV) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
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
                        modifier = Modifier.weight(1f).border(0.5.dp, if (state.rotationDegrees != 0) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${state.rotationDegrees}°", fontSize = 11.sp, color = PureWhite)
                    }
                }

                // Blend Modes
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "نمط دمج القناع (Blend Mode)", color = PureWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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

        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

        // 5. Atmospheric Black Gradient Section (التدرج الأسود المخصص)
        val grad = state.blackGradient
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Gradient,
                        contentDescription = null,
                        tint = if (grad.isEnabled) CherryAccent else SubtleGray,
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
                    checked = grad.isEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleBlackGradient(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureWhite,
                        checkedTrackColor = BurgundyAccent,
                        uncheckedTrackColor = StudioSurface
                    )
                )
            }

            AnimatedVisibility(
                visible = grad.isEnabled,
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
                                val isSelected = grad.placement == plc
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BurgundyAccent else StudioSurface)
                                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onBlackGradientPlacementChange(plc)
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
                        value = grad.opacity,
                        onValueChange = onBlackGradientOpacityChange,
                        valueRange = 0f..1f,
                        valueDisplay = "${(grad.opacity * 100).toInt()}%",
                        icon = Icons.Default.BrightnessMedium
                    )

                    // Black Gradient Coverage Slider
                    StudioSlider(
                        title = "امتداد وتغطية التدرج",
                        value = grad.coverage,
                        onValueChange = onBlackGradientCoverageChange,
                        valueRange = 0.20f..1.0f,
                        valueDisplay = "${(grad.coverage * 100).toInt()}%",
                        icon = Icons.Default.LinearScale
                    )
                }
            }
        }
    }
}
