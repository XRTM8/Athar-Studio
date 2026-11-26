package app.xrtm8.athar.ui.panels.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.dialogs.CustomColorPickerDialog
import app.xrtm8.athar.ui.theme.*

@Composable
fun StrokeShadowSection(
    strokeWidth: Float,
    strokeColorLong: Long,
    isHollowText: Boolean,
    shadowRadius: Float,
    shadowDy: Float,
    shadowDx: Float,
    shadowColorLong: Long,
    outerGlowRadius: Float = 0f,
    outerGlowColorLong: Long = 0xFFE5C07BL,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeColorChange: (Long) -> Unit,
    onToggleHollowText: ((Boolean) -> Unit)? = null,
    onShadowRadiusChange: (Float) -> Unit,
    onShadowDyChange: (Float) -> Unit,
    onShadowDxChange: (Float) -> Unit,
    onShadowColorChange: (Long) -> Unit,
    onOuterGlowRadiusChange: ((Float) -> Unit)? = null,
    onOuterGlowColorChange: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var showCustomStrokeColorDialog by remember { mutableStateOf(false) }
    var showCustomShadowColorDialog by remember { mutableStateOf(false) }
    var showCustomGlowColorDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Hollow Outline Typography
        if (onToggleHollowText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CropFree,
                        contentDescription = null,
                        tint = if (isHollowText) CherryAccent else SubtleGray,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "تفريغ الحروف (تحديد خارجي فقط)",
                        color = if (isHollowText) PureWhite else MutedWhite,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = isHollowText,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleHollowText(it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = app.xrtm8.athar.ui.theme.AmoledWhite, checkedTrackColor = CherryAccent),
                    modifier = Modifier.height(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Stroke Width Slider
        StudioSlider(
            title = "سماكة التحديد",
            value = strokeWidth,
            onValueChange = onStrokeWidthChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..20f,
            step = 1f,
            valueDisplay = "${strokeWidth.toInt()} px",
            icon = Icons.Default.BorderColor
        )

        // Stroke Color Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CherryAccent.copy(alpha = 0.2f))
                    .border(1.dp, CherryAccent, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCustomStrokeColorDialog = true
                    }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ مخصص",
                    color = CherryAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            listOf(
                0xFF000000 to "أسود",
                0xFFFFFFFF to "أبيض",
                0xFFFFD700 to "ذهب",
                0xFFE0E0E0 to "فضي"
            ).forEach { (colorLong, name) ->
                val isSelected = strokeColorLong == colorLong
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CherryAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStrokeColorChange(colorLong)
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) app.xrtm8.athar.ui.theme.AmoledWhite else PureWhite,
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
            value = shadowRadius,
            onValueChange = onShadowRadiusChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..50f,
            step = 2f,
            valueDisplay = "${shadowRadius.toInt()} px",
            icon = Icons.Default.BlurOn
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Directional 2D Shadow: Dy and Dx
        StudioSlider(
            title = "عمق الظل (رأسي)",
            value = shadowDy,
            onValueChange = onShadowDyChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -15f..35f,
            step = 1f,
            valueDisplay = "${shadowDy.toInt()} px"
        )

        Spacer(modifier = Modifier.height(4.dp))

        StudioSlider(
            title = "إزاحة الظل (أفقي)",
            value = shadowDx,
            onValueChange = onShadowDxChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -25f..25f,
            step = 1f,
            valueDisplay = "${shadowDx.toInt()} px"
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(text = "لون الظل:", color = MutedWhite, fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CherryAccent.copy(alpha = 0.2f))
                    .border(1.dp, CherryAccent, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCustomShadowColorDialog = true
                    }
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ مخصص",
                    color = CherryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            listOf(
                0xCC000000L to "أسود",
                0xAAFFD700L to "ذهب",
                0xAA00FF9DL to "زمرد",
                0xAAFFFFFFL to "أبيض",
                0xAAFF3366L to "ياقوت"
            ).forEach { (colorLong, name) ->
                val isSelected = shadowColorLong == colorLong
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CherryAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShadowColorChange(colorLong)
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) app.xrtm8.athar.ui.theme.AmoledWhite else PureWhite,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (showCustomStrokeColorDialog) {
            CustomColorPickerDialog(
                initialColorLong = strokeColorLong,
                title = "منتقي لون التحديد (Stroke)",
                onColorSelected = { onStrokeColorChange(it) },
                onDismiss = { showCustomStrokeColorDialog = false }
            )
        }

        if (showCustomShadowColorDialog) {
            CustomColorPickerDialog(
                initialColorLong = shadowColorLong,
                title = "منتقي لون الظل (Shadow)",
                onColorSelected = { onShadowColorChange(it) },
                onDismiss = { showCustomShadowColorDialog = false }
            )
        }

        if (onOuterGlowRadiusChange != null) {
            Spacer(modifier = Modifier.height(10.dp))
            StudioSlider(
                title = "توهج خارجي (Outer Glow)",
                value = outerGlowRadius,
                onValueChange = onOuterGlowRadiusChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = 0f..50f,
                step = 1f,
                valueDisplay = "${outerGlowRadius.toInt()}",
                icon = Icons.Default.BlurOn
            )
            if (outerGlowRadius > 0.5f && onOuterGlowColorChange != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0xFFE5C07BL to "ذهبي", 0xFF9E1B32L to "عنابي", 0xFF38BDF8L to "سماوي", 0xFFFFFFFFL to "أبيض").forEach { (cLong, label) ->
                        val isSel = outerGlowColorLong == cLong
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CherryAccent else GlassSurface)
                                .border(0.5.dp, if (isSel) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                                .clickable { onOuterGlowColorChange(cLong) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(label, color = PureWhite, fontSize = 10.sp) }
                    }
                    IconButton(onClick = { showCustomGlowColorDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "لون مخصص", tint = CherryAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (showCustomGlowColorDialog && onOuterGlowColorChange != null) {
            CustomColorPickerDialog(
                initialColorLong = outerGlowColorLong,
                title = "منتقي لون التوهج الخارجي",
                onColorSelected = { onOuterGlowColorChange(it); showCustomGlowColorDialog = false },
                onDismiss = { showCustomGlowColorDialog = false }
            )
        }
    }
}
