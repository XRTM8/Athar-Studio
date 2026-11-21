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
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Tune
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
import app.xrtm8.athar.model.CanvasGradientDirection
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.FilmPreset
import app.xrtm8.athar.model.LUXURY_BG_PRESETS
import app.xrtm8.athar.ui.components.PhotoAdjustmentsPanel
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*
import java.util.Locale

/**
 * Streamlined Canvas & Background Panel.
 * Sub-segments:
 * 0 -> الكانفاس والتأطير (Ratios, Transparent, Solid, Photo Picker, Photo Framing & Crop Pan/Zoom)
 * 1 -> تعديل الصورة (Blur, Dim, Light, Contrast, Saturation, Vignette)
 * 2 -> الظل المتدرج (Multi-Directional Scrim, Custom Colors, Opacity, Midpoint)
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
    onToggleFramingMode: (Boolean) -> Unit,
    onBgPhotoOffsetXChange: (Float) -> Unit,
    onBgPhotoOffsetYChange: (Float) -> Unit,
    onBgPhotoScaleChange: (Float) -> Unit,
    onToggleGradientScrim: (Boolean) -> Unit,
    onGradientScrimDirectionChange: (CanvasGradientDirection) -> Unit,
    onGradientScrimColorChange: (Long) -> Unit,
    onGradientScrimOpacityChange: (Float) -> Unit,
    onGradientScrimMidpointChange: (Float) -> Unit,
    onToggleTextBehindSubject: (Boolean) -> Unit = {},
    onSubjectDepthSensitivityChange: (Float) -> Unit = {},
    onSelectFilmPreset: (FilmPreset) -> Unit = {},
    onToggleColorGrading: ((Boolean) -> Unit)? = null,
    onLiftChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onGammaChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onGainChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onResetColorGrading: (() -> Unit)? = null,
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
                1 to ("تعديل الصورة" to Icons.Default.Tune),
                2 to ("الظل المتدرج" to Icons.Default.Gradient),
                3 to ("الفلاتر والألوان" to Icons.Default.PhotoFilter)
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
                                    .background(if (isSelected) GoldAccent else GlassSurface)
                                    .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAspectRatioChange(ratio)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "إظهار شبكة المحاذاة الذهبية", color = MutedWhite, fontSize = 11.5.sp)
                        Switch(
                            checked = state.showGridGuides,
                            onCheckedChange = { onToggleGridGuides() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldAccent,
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
                                tint = GoldAccent,
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

                    // Text Behind Subject (AI Depth Cutout)
                    if (state.backgroundImageUri != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurface)
                                .border(0.5.dp, if (state.isTextBehindSubject) GoldAccent else GlassBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "النص خلف موضوع الصورة (Text Behind Subject)",
                                    color = if (state.isTextBehindSubject) GoldAccent else PureWhite,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "عزل ذكي ثلاثي الأبعاد يضع النصوص المحددة خلف الشخصيات والمباني",
                                    color = SubtleGray,
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = state.isTextBehindSubject,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleTextBehindSubject(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmoledBlack,
                                    checkedTrackColor = GoldAccent,
                                    uncheckedThumbColor = SubtleGray,
                                    uncheckedTrackColor = GlassCard
                                )
                            )
                        }

                        if (state.isTextBehindSubject) {
                            Spacer(modifier = Modifier.height(6.dp))
                            StudioSlider(
                                title = "حساسية العزل",
                                value = state.subjectDepthSensitivity,
                                onValueChange = onSubjectDepthSensitivityChange,
                                onValueChangeFinished = onSliderChangeFinished,
                                valueRange = 0.2f..0.8f,
                                step = 0.05f,
                                valueDisplay = String.format(Locale.US, "%.2f", state.subjectDepthSensitivity),
                                icon = Icons.Default.Tune
                            )
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
                                .border(0.5.dp, if (state.isFramingMode) GoldAccent else GlassBorder, RoundedCornerShape(14.dp))
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
                                            tint = if (state.isFramingMode) GoldAccent else PureWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "تأطير واختيار جزء الصورة للقص",
                                                color = if (state.isFramingMode) GoldAccent else PureWhite,
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
                                        colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
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
                                        value = state.bgPhotoScale,
                                        onValueChange = onBgPhotoScaleChange,
                                        onValueChangeFinished = onSliderChangeFinished,
                                        valueRange = 0.5f..3.0f,
                                        step = 0.05f,
                                        valueDisplay = String.format(Locale.US, "%.1fx", state.bgPhotoScale),
                                        icon = Icons.Default.ZoomIn
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                }

                1 -> PhotoAdjustmentsPanel(
                    state = state,
                    onBlurChange = onBlurChange,
                    onDimChange = onDimChange,
                    onBrightnessChange = onBrightnessChange,
                    onContrastChange = onContrastChange,
                    onSaturationChange = onSaturationChange,
                    onVignetteChange = onVignetteChange,
                    onResetAdjustments = onResetPhotoAdjustments,
                    onSliderChangeFinished = onSliderChangeFinished
                )

                2 -> Column(modifier = Modifier.fillMaxWidth()) {
                    // Gradient Scrim Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تفعيل الظل المتدرج",
                                color = PureWhite,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إسدال تدرج سينمائي يبرز النصوص فوق الصورة أو الخلفية",
                                color = SubtleGray,
                                fontSize = 10.sp
                            )
                        }

                        Switch(
                            checked = state.gradientScrim.isEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleGradientScrim(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
                        )
                    }

                    if (state.gradientScrim.isEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Gradient Direction Selector
                        Text(text = "اتجاه التدرج الظلي:", color = MutedWhite, fontSize = 11.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CanvasGradientDirection.entries.forEach { dir ->
                                val isSelected = state.gradientScrim.direction == dir
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isSelected) GoldAccent else GlassSurface)
                                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(9.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onGradientScrimDirectionChange(dir)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = dir.label,
                                        color = if (isSelected) AmoledBlack else PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Gradient Scrim Colors
                        Text(text = "لون الظل:", color = MutedWhite, fontSize = 11.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0xFF000000L to "أسود داكن",
                                0xFF0A1128L to "كحلي ليلي",
                                0xFF1C1917L to "بني دافئ",
                                0xFF1A080EL to "عنابي فاخر",
                                0xFF081C15L to "زمردي داكن",
                                0xFF121418L to "رمادي فحمي"
                            ).forEach { (colorLong, name) ->
                                val isSelected = state.gradientScrim.colorLong == colorLong
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) GoldAccent else GlassSurface)
                                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onGradientScrimColorChange(colorLong)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorLong.toInt()))
                                            .border(0.5.dp, if (isSelected) AmoledBlack else PureWhite, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        color = if (isSelected) AmoledBlack else PureWhite,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Opacity Slider
                        StudioSlider(
                            title = "كثافة الظل",
                            value = state.gradientScrim.opacity,
                            onValueChange = onGradientScrimOpacityChange,
                            onValueChangeFinished = onSliderChangeFinished,
                            valueRange = 0.05f..1.0f,
                            step = 0.05f,
                            valueDisplay = "${(state.gradientScrim.opacity * 100).toInt()}%"
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Midpoint Falloff Slider
                        StudioSlider(
                            title = "نطاق امتداد الظل (المنتصف)",
                            value = state.gradientScrim.midpoint,
                            onValueChange = onGradientScrimMidpointChange,
                            onValueChangeFinished = onSliderChangeFinished,
                            valueRange = 0.1f..0.9f,
                            step = 0.05f,
                            valueDisplay = "${(state.gradientScrim.midpoint * 100).toInt()}%"
                        )
                    }
                }

                else -> FilmFilterPanel(
                    state = state,
                    onSelectFilmPreset = onSelectFilmPreset,
                    onToggleColorGrading = onToggleColorGrading,
                    onLiftChange = onLiftChange,
                    onGammaChange = onGammaChange,
                    onGainChange = onGainChange,
                    onResetColorGrading = onResetColorGrading,
                    onSliderChangeFinished = onSliderChangeFinished
                )
            }
        }
    }
}
