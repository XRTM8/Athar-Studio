package app.xrtm8.athar.ui.panels

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
import androidx.compose.runtime.Composable
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
import app.xrtm8.athar.model.LUXURY_BG_PRESETS
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

@Composable
fun CanvasBackgroundPanel(
    state: CanvasState,
    onToggleTransparent: (Boolean) -> Unit,
    onSelectSolidBgColor: (Long?) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDimChange: (Float) -> Unit,
    onToggleGrayscale: (Boolean) -> Unit,
    onAspectRatioChange: (CanvasAspectRatio) -> Unit,
    onToggleGridGuides: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Transparent Background Switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "خلفية شفافة (ملصق ستوري)", color = PureWhite, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Text(text = "تفريغ الخلفية لتصدير ملصق شفاف نقي بدون هوامش", color = SubtleGray, fontSize = 10.5.sp)
            }
            Switch(
                checked = state.isTransparentBg,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleTransparent(it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
            )
        }

        // Solid Luxury Presets (when not transparent and no image)
        if (!state.isTransparentBg && state.backgroundImageUri == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "ألوان الكانفاس الفاخرة:", color = MutedWhite, fontSize = 11.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LUXURY_BG_PRESETS.forEach { bg ->
                    val isSelected = state.solidBgColorLong == bg.argb || (bg.argb == 0xFF000000 && state.solidBgColorLong == null)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GoldAccent else GlassSurface)
                            .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectSolidBgColor(if (bg.argb == 0xFF000000) null else bg.argb)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(bg.argb.toInt()))
                                .border(0.5.dp, if (isSelected) AmoledBlack else PureWhite, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bg.name,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Background Image Picker & Remove Action
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
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.backgroundImageUri != null) "تغيير الصورة" else "إضافة صورة من المعرض",
                    color = PureWhite,
                    fontSize = 12.sp
                )
            }

            if (state.backgroundImageUri != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRemoveImage()
                    }
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "إزالة الصورة", tint = Color(0xFFFF6B6B))
                }
            }
        }

        if (state.backgroundImageUri != null) {
            Spacer(modifier = Modifier.height(8.dp))
            StudioSlider(
                title = "تعتيم الصورة لبروز النص",
                value = state.backgroundDim,
                onValueChange = onDimChange,
                valueRange = 0f..0.90f,
                step = 0.05f,
                valueDisplay = "${(state.backgroundDim * 100).toInt()}%"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "فلتر أبيض وأسود كلاسيكي (B&W)", color = PureWhite, fontSize = 12.sp)
                Switch(
                    checked = state.backgroundGrayscale,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleGrayscale(it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Aspect Ratio Selector
        Text(text = "نسبة أبعاد الكانفاس:", color = MutedWhite, fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CanvasAspectRatio.entries.forEach { ratio ->
                val isSelected = state.aspectRatio == ratio
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAspectRatioChange(ratio)
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ratio.label,
                        color = if (isSelected) AmoledBlack else PureWhite,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Golden Grid Guides Switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GridOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "شبكة المحاذاة الذهبية (Rule of Thirds)", color = PureWhite, fontSize = 12.sp)
            }
            Switch(
                checked = state.showGridGuides,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleGridGuides()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
            )
        }
    }
}
