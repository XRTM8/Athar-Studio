package app.xrtm8.athar.ui.panels

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.*
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.components.TextGlassBackdropPanel
import app.xrtm8.athar.ui.theme.*

/**
 * Unified Luxury Style, Materials, Depth & Effects Panel.
 * Sub-segments:
 * 0 -> الألوان والشيدرز (Shaders & Swatches)
 * 1 -> الظلال والتحديد (Stroke & 3D Shadow)
 * 2 -> اللوح الزجاجي (Glass Plaque)
 */
@Composable
fun StyleEffectsPanel(
    state: CanvasState,
    onSelectEffect: ((VisualEffect) -> Unit)? = null,
    onSelectCustomColor: (Long?) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeColorChange: (Long) -> Unit,
    onShadowRadiusChange: (Float) -> Unit,
    onShadowDyChange: (Float) -> Unit,
    onShadowDxChange: (Float) -> Unit,
    onShadowColorChange: (Long) -> Unit,
    onToggleGlassBackdrop: (Boolean) -> Unit,
    onSelectGlassPreset: (GlassBackdropPreset) -> Unit,
    onGlassOpacityChange: (Float) -> Unit,
    onGlassCornerRadiusChange: (Float) -> Unit,
    onGlassPaddingChange: (Float) -> Unit,
    onBlendModeChange: (LayerBlendMode) -> Unit = {},
    extractedPalette: app.xrtm8.athar.core.AtharPaletteExtractor.ExtractedPalette? = null,
    onSelectGradientPreset: ((LuxuryGradientPreset) -> Unit)? = null,
    onGradientAngleChange: ((Float) -> Unit)? = null,
    onToggleRadialGradient: ((Boolean) -> Unit)? = null,
    onToggleHollowText: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Sub-segmented Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                0 to ("الألوان والخامات" to Icons.Default.AutoAwesome),
                1 to ("الظل والتحديد" to Icons.Default.Layers),
                2 to ("اللوح الزجاجي" to Icons.Default.FilterFrames)
            ).forEach { (idx, item) ->
                val (title, icon) = item
                val isSelected = subTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) GoldAccent else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            subTab = idx
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) AmoledBlack else MutedWhite,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = title,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = subTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut()
                }
            },
            label = "StyleEffectsSubTabs"
        ) { currentSubTab ->
            when (currentSubTab) {
                0 -> Column(modifier = Modifier.fillMaxWidth()) {
                    // Multi-Stop Luxury Gradients & Materials
                    if (onSelectGradientPreset != null) {
                        Text(text = "الخامات والتدرجات الفاخرة (Luxury Materials & Gradients):", color = MutedWhite, fontSize = 11.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LuxuryGradientPreset.entries.filter { it != LuxuryGradientPreset.NONE }.forEach { grad ->
                                val isSelected = state.gradientPreset == grad && state.customTextColorLong == null
                                val brushColors = grad.colors.map { Color(it.toInt()) }
                                val brush = Brush.horizontalGradient(brushColors)

                                Box(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GoldAccent else GlassSurface)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) GoldAccent else GlassBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectGradientPreset(if (isSelected) LuxuryGradientPreset.NONE else grad)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(brush)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = grad.labelAr,
                                            color = if (isSelected) AmoledBlack else PureWhite,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (state.gradientPreset != LuxuryGradientPreset.NONE) {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (onGradientAngleChange != null) {
                                StudioSlider(
                                    title = "زاوية التدرج اللوني",
                                    value = state.gradientAngle,
                                    onValueChange = onGradientAngleChange,
                                    onValueChangeFinished = onSliderChangeFinished,
                                    valueRange = 0f..360f,
                                    step = 5f,
                                    valueDisplay = "${state.gradientAngle.toInt()}°",
                                    icon = Icons.AutoMirrored.Filled.RotateRight
                                )
                            }
                            if (onToggleRadialGradient != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("نمط التدرج الشعاعي الدائري (Radial)", color = MutedWhite, fontSize = 11.sp)
                                    Switch(
                                        checked = state.isRadialGradient,
                                        onCheckedChange = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onToggleRadialGradient(it)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Luxury Color Swatches Palette
                    Text(text = "الألوان الجاهزة:", color = MutedWhite, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LUXURY_SWATCHES.forEach { swatch ->
                            val isSelected = state.customTextColorLong == swatch.argb
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectCustomColor(if (swatch.argb == 0L) null else swatch.argb)
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (swatch.argb == 0L) GoldAccent else Color(swatch.argb.toInt()))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) PureWhite else GlassBorder,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = swatch.name,
                                    color = if (isSelected) GoldAccent else SubtleGray,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (state.backgroundImageUri != null && extractedPalette != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "لوحة ألوان صورتك المستخرجة:", color = MutedWhite, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    "إضاءة" to extractedPalette.highlightLong,
                                    "تباين" to extractedPalette.accentLong,
                                    "مهيمن" to extractedPalette.dominantLong,
                                    "زجاج" to extractedPalette.glassLong,
                                    "ظل" to extractedPalette.shadowLong
                                ).forEach { (label, cLong) ->
                                    val isSelected = state.activeLayer.customTextColorLong == cLong
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectCustomColor(cLong)
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(cLong))
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.5.dp,
                                                    color = if (isSelected) GoldAccent else GlassBorder,
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(label, color = SubtleGray, fontSize = 9.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                    // Blend Mode Selector
                    Text(text = "نمط الدمج للطبقة (Blend Mode):", color = MutedWhite, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LayerBlendMode.entries.forEach { mode ->
                            val isSelected = state.blendMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBlendModeChange(mode)
                                },
                                label = { Text(mode.labelAr, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent,
                                    selectedLabelColor = AmoledBlack,
                                    containerColor = GlassCard,
                                    labelColor = MutedWhite
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                1 -> Column(modifier = Modifier.fillMaxWidth()) {
                    // v2.1.0 Hollow Outline Typography
                    if (onToggleHollowText != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CropFree, contentDescription = null, tint = if (state.isHollowText) GoldAccent else SubtleGray, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تفريغ الحروف (تحديد خارجي فقط / Hollow)", color = if (state.isHollowText) PureWhite else MutedWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = state.isHollowText,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleHollowText(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Stroke Width Slider
                    StudioSlider(
                        title = "سماكة التحديد",
                        value = state.strokeWidth,
                        onValueChange = onStrokeWidthChange,
                        onValueChangeFinished = onSliderChangeFinished,
                        valueRange = 0f..20f,
                        step = 1f,
                        valueDisplay = "${state.strokeWidth.toInt()} px",
                        icon = Icons.Default.BorderColor
                    )

                    // Stroke Color Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0xFF000000 to "أسود داكن",
                            0xFFFFFFFF to "أبيض ناصع",
                            0xFFFFD700 to "ذهب ملكي",
                            0xFFE0E0E0 to "فضي بلاتيني"
                        ).forEach { (colorLong, name) ->
                            val isSelected = state.strokeColorLong == colorLong
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GoldAccent else GlassSurface)
                                    .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onStrokeColorChange(colorLong)
                                    }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) AmoledBlack else PureWhite,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shadow Blur Radius Slider
                    StudioSlider(
                        title = "تشتت الظل",
                        value = state.shadowRadius,
                        onValueChange = onShadowRadiusChange,
                        onValueChangeFinished = onSliderChangeFinished,
                        valueRange = 0f..50f,
                        step = 2f,
                        valueDisplay = "${state.shadowRadius.toInt()} px",
                        icon = Icons.Default.BlurOn
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Directional 2D Shadow: Dy and Dx
                    StudioSlider(
                        title = "عمق الظل (رأسي)",
                        value = state.shadowDy,
                        onValueChange = onShadowDyChange,
                        onValueChangeFinished = onSliderChangeFinished,
                        valueRange = -15f..35f,
                        step = 1f,
                        valueDisplay = "${state.shadowDy.toInt()} px"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    StudioSlider(
                        title = "إزاحة الظل (أفقي)",
                        value = state.shadowDx,
                        onValueChange = onShadowDxChange,
                        onValueChangeFinished = onSliderChangeFinished,
                        valueRange = -25f..25f,
                        step = 1f,
                        valueDisplay = "${state.shadowDx.toInt()} px"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "لون الظل:", color = MutedWhite, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0xCC000000L to "ظل أسود",
                            0xAAFFD700L to "توهج ذهبي",
                            0xAA00FF9DL to "توهج زمردي",
                            0xAAFFFFFFL to "وهج أبيض",
                            0xAAFF3366L to "وهج ياقوتي"
                        ).forEach { (colorLong, name) ->
                            val isSelected = state.shadowColorLong == colorLong
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GoldAccent else GlassSurface)
                                    .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onShadowColorChange(colorLong)
                                    }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) AmoledBlack else PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                else -> Column(modifier = Modifier.fillMaxWidth()) {
                    TextGlassBackdropPanel(
                        state = state,
                        onToggleGlassBackdrop = onToggleGlassBackdrop,
                        onSelectPreset = onSelectGlassPreset,
                        onOpacityChange = onGlassOpacityChange,
                        onCornerRadiusChange = onGlassCornerRadiusChange,
                        onPaddingChange = onGlassPaddingChange,
                        onSliderChangeFinished = onSliderChangeFinished
                    )
                }
            }
        }
    }
}
