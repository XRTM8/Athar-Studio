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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import app.xrtm8.athar.ui.components.PhotoAdjustmentsPanel
import app.xrtm8.athar.ui.panels.background.CanvasFramingSection
import app.xrtm8.athar.ui.panels.background.GradientScrimSection
import app.xrtm8.athar.ui.theme.*

/**
 * Clean Canvas & Background Panel Coordinator.
 * Sub-units:
 * 0 -> CanvasFramingSection (Ratios, Solid BG, Framing, Stickers)
 * 1 -> PhotoAdjustmentsPanel (Blur, Dim, Contrast, Brightness, Saturation)
 * 2 -> GradientScrimSection (Directional Scrim, Custom Colors, Midpoint)
 * 3 -> FilmFilterPanel (Film Emulations & 3-way color grading)
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
                        .background(if (isSelected) CherryAccent else Color.Transparent)
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
                0 -> CanvasFramingSection(
                    state = state,
                    onAspectRatioChange = onAspectRatioChange,
                    onToggleGridGuides = onToggleGridGuides,
                    onPickImage = onPickImage,
                    onRemoveImage = onRemoveImage,
                    onToggleFramingMode = onToggleFramingMode,
                    onBgPhotoOffsetXChange = onBgPhotoOffsetXChange,
                    onBgPhotoOffsetYChange = onBgPhotoOffsetYChange,
                    onBgPhotoScaleChange = onBgPhotoScaleChange,
                    onToggleTransparent = onToggleTransparent,
                    onSelectSolidBgColor = onSelectSolidBgColor,
                    onSliderChangeFinished = onSliderChangeFinished
                )

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

                2 -> GradientScrimSection(
                    state = state,
                    onToggleGradientScrim = onToggleGradientScrim,
                    onGradientScrimDirectionChange = onGradientScrimDirectionChange,
                    onGradientScrimColorChange = onGradientScrimColorChange,
                    onGradientScrimOpacityChange = onGradientScrimOpacityChange,
                    onGradientScrimMidpointChange = onGradientScrimMidpointChange,
                    onSliderChangeFinished = onSliderChangeFinished
                )

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
