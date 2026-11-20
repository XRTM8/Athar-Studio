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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Tune
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
import app.xrtm8.athar.model.LUXURY_BG_PRESETS
import app.xrtm8.athar.ui.components.PhotoAdjustmentsPanel
import app.xrtm8.athar.ui.theme.*

/**
 * Streamlined Canvas & Background Panel.
 * Sub-segments:
 * 0 -> الكانفاس والأبعاد (Ratios, Transparent, Luxury Solid, Photo Picker)
 * 1 -> تعديل الصورة الفوتوغرافية (Blur, Dim, Light, Contrast, Saturation, Vignette)
 */
@Composable
fun CanvasBackgroundPanel(
    state: CanvasState,
    onToggleTransparent: (Boolean) -> Unit,
    onSelectSolidBgColor: (Long?) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDimChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onVignetteChange: (Float) -> Unit,
    onResetPhotoAdjustments: () -> Unit,
    onAspectRatioChange: (CanvasAspectRatio) -> Unit,
    onToggleGridGuides: () -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var subSection by remember { mutableIntStateOf(0) }

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
                0 to ("الكانفاس" to Icons.Default.AspectRatio),
                1 to ("تعديل الصورة" to Icons.Default.Tune)
            ).forEach { (idx, item) ->
                val (title, icon) = item
                val isSelected = subSection == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) GoldAccent else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            subSection = idx
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) AmoledBlack else MutedWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = title,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = subSection,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut()
                }
            },
            label = "CanvasSubSection"
        ) { currentSection ->
            when (currentSection) {
                0 -> Column(modifier = Modifier.fillMaxWidth()) {
                    // Aspect Ratio Selector
                    Text(text = "أبعاد التصميم:", color = MutedWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
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
                                    .padding(vertical = 8.dp),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transparent Background Switch (Sticker Mode)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "خلفية شفافة", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "تفريغ الخلفية لتصدير ملصق شفاف (PNG)", color = SubtleGray, fontSize = 10.sp)
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
                    if (!state.isTransparentBg) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "لون الخلفية:", color = MutedWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LUXURY_BG_PRESETS.forEach { bg ->
                                val isSelected = (state.solidBgColorLong == bg.argb || (bg.argb == 0xFF000000 && state.solidBgColorLong == null)) && state.backgroundImageUri == null
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) GoldAccent else GlassSurface)
                                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onRemoveImage()
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
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.backgroundImageUri != null) "تغيير صورة الخلفية" else "اختيار صورة من المعرض",
                                color = PureWhite,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (state.backgroundImageUri != null) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRemoveImage()
                                }
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إزالة الصورة", color = Color(0xFFFF6B6B), fontSize = 11.5.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Golden Grid Guides Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GridOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "شبكة المحاذاة (أثلاث)", color = PureWhite, fontSize = 12.sp)
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

                else -> Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.backgroundImageUri != null) {
                        PhotoAdjustmentsPanel(
                            state = state,
                            onDimChange = onDimChange,
                            onBlurChange = onBlurChange,
                            onBrightnessChange = onBrightnessChange,
                            onContrastChange = onContrastChange,
                            onSaturationChange = onSaturationChange,
                            onVignetteChange = onVignetteChange,
                            onResetAdjustments = onResetPhotoAdjustments,
                            onSliderChangeFinished = onSliderChangeFinished
                        )
                    } else {
                        // Empty State when no photo loaded
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(GlassSurface)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoFilter,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لم يتم اختيار صورة خلفية بعد",
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "اختر صورة لتفعيل مؤثرات الضبابية والسطوع والتباين وتدرج الحواف",
                                    color = SubtleGray,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPickImage()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("اختيار صورة من المعرض", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
