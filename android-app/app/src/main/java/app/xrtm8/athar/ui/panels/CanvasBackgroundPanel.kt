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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Texture
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
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.AtharStudioViewModel
import app.xrtm8.athar.ui.components.PhotoAdjustmentsPanel
import app.xrtm8.athar.ui.panels.background.CanvasFramingSection
import app.xrtm8.athar.ui.panels.background.CinematicEffectsPanel
import app.xrtm8.athar.ui.panels.background.CollagePanel
import app.xrtm8.athar.ui.panels.background.ColorMixPanel
import app.xrtm8.athar.ui.panels.background.MaskPanel
import app.xrtm8.athar.ui.panels.background.PatternPanel
import app.xrtm8.athar.ui.theme.*

/**
 * Clean Canvas & Background Panel Coordinator (< 240 lines).
 */
@Composable
fun CanvasBackgroundPanel(
    state: CanvasState,
    viewModel: AtharStudioViewModel,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onPickCollagePhoto: (String) -> Unit = {},
    onPickCustomMask: () -> Unit = {},
    modifier: Modifier = Modifier
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
                .padding(3.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf(
                0 to ("الكانفاس" to Icons.Default.AspectRatio),
                1 to ("أقنعة" to Icons.Default.AutoAwesomeMotion),
                2 to ("كولاج" to Icons.Default.GridView),
                3 to ("سينمائي" to Icons.Default.Flare),
                4 to ("باترن" to Icons.Default.Texture),
                5 to ("الضبط" to Icons.Default.Tune),
                6 to ("خلط الألوان" to Icons.Default.Palette),
                7 to ("الفلاتر" to Icons.Default.PhotoFilter)
            ).forEach { (idx, item) ->
                val (title, icon) = item
                val isSelected = subSection == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) BurgundyAccent else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            subSection = idx
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) PureWhite else MutedWhite,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = title,
                            color = if (isSelected) PureWhite else MutedWhite,
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
                    onAspectRatioChange = { viewModel.setAspectRatio(it) },
                    onToggleGridGuides = { viewModel.toggleGridGuides() },
                    onPickImage = onPickImage,
                    onRemoveImage = onRemoveImage,
                    onToggleFramingMode = { viewModel.toggleFramingMode(it) },
                    onBgPhotoOffsetXChange = { viewModel.setBgPhotoOffset(it, state.bgPhotoOffsetY) },
                    onBgPhotoOffsetYChange = { viewModel.setBgPhotoOffset(state.bgPhotoOffsetX, it) },
                    onBgPhotoScaleChange = { viewModel.setBgPhotoScale(it) },
                    onToggleTransparent = { viewModel.setTransparentBg(it) },
                    onSelectSolidBgColor = { viewModel.setSolidBgColor(it) },
                    onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                )

                1 -> MaskPanel(
                    state = state.maskState,
                    onSelectMask = { viewModel.selectMask(it) },
                    onPickCustomMask = onPickCustomMask,
                    onOpacityChange = { viewModel.setMaskOpacity(it) },
                    onBlendModeChange = { viewModel.setMaskBlendMode(it) },
                    onHueShiftChange = { viewModel.setMaskHueShift(it) },
                    onOffsetXChange = { viewModel.setMaskOffsetX(it) },
                    onOffsetYChange = { viewModel.setMaskOffsetY(it) },
                    onScaleChange = { viewModel.setMaskScale(it) },
                    onToggleFlipH = { viewModel.toggleMaskFlipH() },
                    onToggleFlipV = { viewModel.toggleMaskFlipV() },
                    onRotate90 = { viewModel.rotateMask90() },
                    onToggleBlackGradient = { viewModel.setBlackGradientEnabled(it) },
                    onBlackGradientOpacityChange = { viewModel.setBlackGradientOpacity(it) },
                    onBlackGradientPlacementChange = { viewModel.setBlackGradientPlacement(it) },
                    onBlackGradientCoverageChange = { viewModel.setBlackGradientCoverage(it) },
                    onResetMask = { viewModel.resetMask() },
                    onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                )

                2 -> CollagePanel(
                    state = state.collageState,
                    onLayoutChange = { viewModel.setCollageLayout(it) },
                    onSelectCell = { viewModel.selectCollageCell(it) },
                    onPickCellPhoto = onPickCollagePhoto,
                    onSpacingChange = { viewModel.setCollageSpacing(it) },
                    onCornerRadiusChange = { viewModel.setCollageCornerRadius(it) },
                    onFrameColorChange = { viewModel.setCollageFrameColor(it) },
                    onResetCollage = { viewModel.resetCollage() },
                    onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                )

                3 -> CinematicEffectsPanel(
                    glowState = state.cinematicGlowState,
                    grainState = state.filmGrainState,
                    onGlowChange = { viewModel.setCinematicGlow(it) },
                    onRangeChange = { viewModel.setCinematicRange(it) },
                    onSpreadChange = { viewModel.setCinematicSpread(it) },
                    onWarmthChange = { viewModel.setCinematicWarmth(it) },
                    onStyleChange = { viewModel.setCinematicStyle(it) },
                    onResetGlow = { viewModel.resetCinematicGlow() },
                    onGrainChange = { viewModel.setFilmGrain(it) },
                    onGrainSizeChange = { viewModel.setFilmGrainSize(it) },
                    onGrainRoughnessChange = { viewModel.setFilmGrainRoughness(it) },
                    onResetGrain = { viewModel.resetFilmGrain() },
                    onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                )

                4 -> PatternPanel(
                    state = state.patternState,
                    onPatternTypeChange = { viewModel.setPatternType(it) },
                    onOpacityChange = { viewModel.setPatternOpacity(it) },
                    onScaleChange = { viewModel.setPatternScale(it) },
                    onBlendModeChange = { viewModel.setPatternBlendMode(it) },
                    onToggleLightTint = { viewModel.setPatternLightTint(it) },
                    onResetPattern = { viewModel.resetPattern() },
                    onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                )

                5 -> PhotoAdjustmentsPanel(
                    state = state,
                    onBlurChange = { viewModel.setBackgroundBlur(it) },
                    onDimChange = { viewModel.setBackgroundDim(it) },
                    onBrightnessChange = { viewModel.setBackgroundBrightness(it) },
                    onContrastChange = { viewModel.setBackgroundContrast(it) },
                    onSaturationChange = { viewModel.setBackgroundSaturation(it) },
                    onVignetteChange = { viewModel.setBackgroundVignette(it) },
                    onResetAdjustments = { viewModel.resetPhotoAdjustments() },
                    onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                )

                6 -> ColorMixPanel(
                    state = state.colorMixState,
                    onChannelAdjust = { ch, h, s, l -> viewModel.setColorMixChannel(ch, h, s, l) },
                    onIsolateChannel = { viewModel.setIsolatedColorChannel(it) },
                    onMonochromeChannel = { viewModel.setMonochromeColorChannel(it) },
                    onResetChannel = { viewModel.resetColorMixChannel(it) },
                    onResetAll = { viewModel.resetAllColorMix() },
                    onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                )

                else -> FilmFilterPanel(
                    state = state,
                    onSelectFilmPreset = { viewModel.setFilmPreset(it) },
                    onToggleColorGrading = { viewModel.setColorGradingEnabled(it) },
                    onLiftChange = { r, g, b -> viewModel.setColorGradingLift(r, g, b) },
                    onGammaChange = { r, g, b -> viewModel.setColorGradingGamma(r, g, b) },
                    onGainChange = { r, g, b -> viewModel.setColorGradingGain(r, g, b) },
                    onResetColorGrading = { viewModel.resetColorGrading() },
                    onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                )
            }
        }
    }
}
