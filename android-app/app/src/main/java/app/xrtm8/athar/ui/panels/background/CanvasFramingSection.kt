package app.xrtm8.athar.ui.panels.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ZoomIn
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
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*
import java.util.Locale

@Composable
fun CanvasFramingSection(
    state: CanvasState,
    onAspectRatioChange: (CanvasAspectRatio) -> Unit,
    onToggleGridGuides: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onToggleFramingMode: (Boolean) -> Unit,
    onBgPhotoOffsetXChange: (Float) -> Unit,
    onBgPhotoOffsetYChange: (Float) -> Unit,
    onBgPhotoScaleChange: (Float) -> Unit,
    onToggleTransparent: (Boolean) -> Unit,
    onSelectSolidBgColor: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Aspect Ratio Selector
        Text(text = "أبعاد التصميم:", color = MutedWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CanvasAspectRatio.entries.forEach { ratio ->
                val isSelected = state.aspectRatio == ratio
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CherryAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAspectRatioChange(ratio)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ratio.label,
                        color = if (isSelected) PureWhite else MutedWhite,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "إظهار شبكة المحاذاة", color = MutedWhite, fontSize = 11.5.sp)
            Switch(
                checked = state.showGridGuides,
                onCheckedChange = { onToggleGridGuides() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CherryAccent,
                    checkedTrackColor = GlassSurface,
                    uncheckedThumbColor = SubtleGray,
                    uncheckedTrackColor = AmoledBlack
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Photo from Gallery Action Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPickImage()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderActive)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = CherryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.backgroundImageUri != null) "تغيير صورة الخلفية" else "اختيار صورة من المعرض",
                    color = PureWhite,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.backgroundImageUri != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRemoveImage()
                    }
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "حذف الصورة", tint = Color(0xFFFF5252))
                }
            }
        }

        // Background Framing & Crop Part (Active if Image is present)
        if (state.backgroundImageUri != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSurface)
                    .border(0.5.dp, if (state.isFramingMode) CherryAccent else GlassBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Crop,
                                contentDescription = null,
                                tint = if (state.isFramingMode) CherryAccent else PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "تأطير واختيار جزء الصورة للقص",
                                    color = if (state.isFramingMode) CherryAccent else PureWhite,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تحريك وتكبير الصورة بالأصابع داخل الإطار",
                                    color = SubtleGray,
                                    fontSize = 9.5.sp
                                )
                            }
                        }

                        Switch(
                            checked = state.isFramingMode,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleFramingMode(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = app.xrtm8.athar.ui.theme.AmoledWhite, checkedTrackColor = CherryAccent)
                        )
                    }

                    if (state.isFramingMode) {
                        Spacer(modifier = Modifier.height(8.dp))

                        StudioSlider(
                            title = "إزاحة أفقية للصورة",
                            value = state.bgPhotoOffsetX,
                            onValueChange = onBgPhotoOffsetXChange,
                            onValueChangeFinished = onSliderChangeFinished,
                            valueRange = -1.0f..1.0f,
                            step = 0.02f,
                            valueDisplay = String.format(Locale.US, "%.0f%%", state.bgPhotoOffsetX * 100)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        StudioSlider(
                            title = "إزاحة رأسية للصورة",
                            value = state.bgPhotoOffsetY,
                            onValueChange = onBgPhotoOffsetYChange,
                            onValueChangeFinished = onSliderChangeFinished,
                            valueRange = -1.0f..1.0f,
                            step = 0.02f,
                            valueDisplay = String.format(Locale.US, "%.0f%%", state.bgPhotoOffsetY * 100)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        StudioSlider(
                            title = "تقريب / زووم الصورة",
                            value = state.bgPhotoScale.coerceIn(1.0f, 4.0f),
                            onValueChange = onBgPhotoScaleChange,
                            onValueChangeFinished = onSliderChangeFinished,
                            valueRange = 1.0f..4.0f,
                            step = 0.05f,
                            valueDisplay = String.format(Locale.US, "%.1fx", state.bgPhotoScale),
                            icon = Icons.Default.ZoomIn
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        CanvasSolidBgSection(
            state = state,
            onRemoveImage = onRemoveImage,
            onToggleTransparent = onToggleTransparent,
            onSelectSolidBgColor = onSelectSolidBgColor
        )
    }
}
