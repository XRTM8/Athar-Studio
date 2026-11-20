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
    onSelectEffect: (VisualEffect) -> Unit,
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
                    // Shaders Carousel
                    Text(text = "الخامات المعدنية:", color = MutedWhite, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VisualEffect.entries.forEach { effect ->
                            val isSelected = state.selectedEffect == effect && state.customTextColorLong == null
                            val gradientBrush = when (effect) {
                                VisualEffect.ROYAL_GOLD -> Brush.horizontalGradient(listOf(Color(0xFFFFF7DB), Color(0xFFF5C369), Color(0xFFD49438)))
                                VisualEffect.LIQUID_CHROME -> Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFF9EA5B0), Color(0xFF3A3F47)))
                                VisualEffect.FROSTED_GLASS -> Brush.horizontalGradient(listOf(Color(0xCCFFFFFF), Color(0x33E2E8F0)))
                                VisualEffect.CYBER_NEON -> Brush.horizontalGradient(listOf(Color(0xFF00FF9D), Color(0xFF00B0FF)))
                                VisualEffect.CINEMATIC_SHADOW -> Brush.horizontalGradient(listOf(Color(0xFFF5F6F8), Color(0xFF4A4E5A)))
                                VisualEffect.MINIMAL_LUXURY -> Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD4D4D8)))
                            }

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
                                        onSelectEffect(effect)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(gradientBrush)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = effect.arabicName,
                                        color = if (isSelected) AmoledBlack else PureWhite,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                }

                1 -> Column(modifier = Modifier.fillMaxWidth()) {
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
