package app.xrtm8.athar.ui.panels.effects

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
import androidx.compose.material.icons.filled.Add
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
import app.xrtm8.athar.core.AtharPaletteExtractor
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.CustomGradientState
import app.xrtm8.athar.model.CustomGradientStop
import app.xrtm8.athar.model.LUXURY_SWATCHES
import app.xrtm8.athar.model.LayerBlendMode
import app.xrtm8.athar.model.LuxuryGradientPreset
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.dialogs.CustomColorPickerDialog
import app.xrtm8.athar.ui.theme.*

@Composable
fun ColorsMaterialsSection(
    state: CanvasState,
    onSelectCustomColor: (Long?) -> Unit,
    onBlendModeChange: (LayerBlendMode) -> Unit,
    extractedPalette: AtharPaletteExtractor.ExtractedPalette? = null,
    onSelectGradientPreset: ((LuxuryGradientPreset) -> Unit)? = null,
    onGradientAngleChange: ((Float) -> Unit)? = null,
    onToggleRadialGradient: ((Boolean) -> Unit)? = null,
    onCustomGradientChange: ((CustomGradientState) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var editingStopIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Multi-Stop Luxury Gradients & Materials
        if (onSelectGradientPreset != null) {
            Text(text = "الخامات والتدرجات الفاخرة:", color = MutedWhite, fontSize = 11.5.sp)
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
                            .background(if (isSelected) CherryAccent else GlassSurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) CherryAccent else GlassBorder,
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
                                color = PureWhite,
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
                            colors = SwitchDefaults.colors(checkedThumbColor = app.xrtm8.athar.ui.theme.AmoledWhite, checkedTrackColor = CherryAccent),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Custom Multi-Stop Gradient Builder (2-4 Stops)
        if (onCustomGradientChange != null) {
            val cg = state.activeLayer.customGradient
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تدرج حر مخصص (2–4 ألوان)", color = if (cg.isEnabled) PureWhite else MutedWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = cg.isEnabled,
                    onCheckedChange = { enabled ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCustomGradientChange(cg.copy(isEnabled = enabled))
                        onSliderChangeFinished?.invoke()
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AmoledWhite, checkedTrackColor = CherryAccent),
                    modifier = Modifier.height(24.dp)
                )
            }
            if (cg.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cg.stops.forEachIndexed { idx, stop ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(stop.colorLong))
                                .border(1.5.dp, PureWhite, CircleShape)
                                .clickable { editingStopIndex = idx }
                        )
                    }
                    if (cg.stops.size < 4) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(GlassSurface)
                                .border(0.75.dp, CherryAccent, CircleShape)
                                .clickable {
                                    val nextStops = cg.stops + CustomGradientStop(0xFFFFFFFFL, 0.5f)
                                    onCustomGradientChange(cg.copy(stops = nextStops.sortedBy { it.position }))
                                    onSliderChangeFinished?.invoke()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة لون", tint = CherryAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (cg.stops.size > 2) {
                        Text(
                            text = "حذف الأخير",
                            color = MutedWhite,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onCustomGradientChange(cg.copy(stops = cg.stops.dropLast(1)))
                                    onSliderChangeFinished?.invoke()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
                StudioSlider(
                    title = "زاوية التدرج الحر",
                    value = cg.angle,
                    onValueChange = { onCustomGradientChange(cg.copy(angle = it)) },
                    onValueChangeFinished = onSliderChangeFinished,
                    valueRange = 0f..360f,
                    step = 5f,
                    valueDisplay = "${cg.angle.toInt()}°",
                    icon = Icons.AutoMirrored.Filled.RotateRight
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تدرج دائري (Radial)", color = MutedWhite, fontSize = 11.sp)
                    Switch(
                        checked = cg.isRadial,
                        onCheckedChange = {
                            onCustomGradientChange(cg.copy(isRadial = it))
                            onSliderChangeFinished?.invoke()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AmoledWhite, checkedTrackColor = CherryAccent),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            if (editingStopIndex != null) {
                val idx = editingStopIndex!!
                val curStop = cg.stops.getOrNull(idx)
                if (curStop != null) {
                    CustomColorPickerDialog(
                        initialColorLong = curStop.colorLong,
                        title = "لون نقطة التدرج ${idx + 1}",
                        onColorSelected = { chosen ->
                            val updated = cg.stops.toMutableList().apply { set(idx, curStop.copy(colorLong = chosen)) }
                            onCustomGradientChange(cg.copy(stops = updated))
                            onSliderChangeFinished?.invoke()
                            editingStopIndex = null
                        },
                        onDismiss = { editingStopIndex = null }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Color Swatches Palette
        Text(text = "الألوان الجاهزة:", color = MutedWhite, fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Custom Free Color Picker Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showCustomColorDialog = true
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CherryAccent.copy(alpha = 0.2f))
                        .border(1.dp, CherryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "لون مخصص",
                        tint = CherryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "مخصص",
                    color = CherryAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

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
                            .background(if (swatch.argb == 0L) CherryAccent else Color(swatch.argb.toInt()))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) PureWhite else GlassBorder,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = swatch.name,
                        color = if (isSelected) CherryAccent else SubtleGray,
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
                                    color = if (isSelected) CherryAccent else GlassBorder,
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
                        selectedContainerColor = CherryAccent,
                        selectedLabelColor = app.xrtm8.athar.ui.theme.AmoledWhite,
                        containerColor = GlassCard,
                        labelColor = MutedWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        if (showCustomColorDialog) {
            CustomColorPickerDialog(
                initialColorLong = state.customTextColorLong ?: 0xFFFFFFFFL,
                title = "منتقي لون النص",
                onColorSelected = { onSelectCustomColor(it) },
                onDismiss = { showCustomColorDialog = false }
            )
        }
    }
}
