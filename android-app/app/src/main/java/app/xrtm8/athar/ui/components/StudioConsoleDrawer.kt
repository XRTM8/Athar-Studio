package app.xrtm8.athar.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.AtharStudioViewModel
import app.xrtm8.athar.ui.panels.CanvasBackgroundPanel
import app.xrtm8.athar.ui.panels.EffectsDepthPanel
import app.xrtm8.athar.ui.panels.KashidaPanel
import app.xrtm8.athar.ui.panels.ShadersColorPanel
import app.xrtm8.athar.ui.panels.TypographyPanel
import app.xrtm8.athar.ui.theme.GlassBackground
import app.xrtm8.athar.ui.theme.GlassBorder
import app.xrtm8.athar.ui.theme.GoldAccent
import app.xrtm8.athar.ui.theme.PureWhite
import app.xrtm8.athar.ui.theme.SubtleGray

@Composable
fun StudioConsoleDrawer(
    selectedTab: Int,
    isPanelExpanded: Boolean,
    canvasState: CanvasState,
    viewModel: AtharStudioViewModel,
    onClosePanel: () -> Unit,
    onOpenTextEditor: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isPanelExpanded,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(GlassBackground)
                .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Panel Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentTabItem = STUDIO_DOCK_TABS.getOrNull(selectedTab)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentTabItem != null) {
                            Icon(
                                imageVector = currentTabItem.icon,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = when (selectedTab) {
                                0 -> "الخط والنص العربي"
                                1 -> "المدّ الذكي والمحاذاة"
                                2 -> "المظهر والألوان الفاخرة"
                                3 -> "العمق والظل والتحديد"
                                else -> "الكانفاس وصورة الخلفية"
                            },
                            color = PureWhite,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClosePanel()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "تصغير اللوحة",
                            tint = SubtleGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Animated Switcher between Panels
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut()
                        }
                    },
                    label = "StudioConsoleTabs"
                ) { currentTab ->
                    when (currentTab) {
                        0 -> TypographyPanel(
                            state = canvasState,
                            onOpenTextEditor = onOpenTextEditor,
                            onSelectFont = { viewModel.setFont(it) },
                            onFontSizeChange = { viewModel.setFontSize(it) },
                            onToggleBold = { viewModel.toggleBold() },
                            onToggleItalic = { viewModel.toggleItalic() },
                            onToggleUnderline = { viewModel.toggleUnderline() },
                            onOpacityChange = { viewModel.setTextOpacity(it) },
                            onLetterSpacingChange = { viewModel.setLetterSpacing(it) },
                            onLineSpacingChange = { viewModel.setLineSpacing(it) },
                            onTextAlignChange = { viewModel.setTextAlign(it) },
                            onSnapPosition = { x, y, a -> viewModel.snapToPosition(x, y, a) },
                            onNudgeOffset = { dx, dy -> viewModel.nudgeOffset(dx, dy) },
                            onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                        )

                        1 -> KashidaPanel(
                            state = canvasState,
                            onStretchLevelChange = { viewModel.setStretchLevel(it) },
                            onKashidaModeChange = { viewModel.setKashidaMode(it) },
                            onTargetIndexChange = { viewModel.setKashidaTargetIndex(it) },
                            onRotationChange = { viewModel.setRotationAngle(it) },
                            onRotateStep = { viewModel.rotateStep(it) },
                            onAlignTop = { viewModel.alignTop() },
                            onAlignCenter = { viewModel.alignCenter() },
                            onAlignBottom = { viewModel.alignBottom() },
                            onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                        )

                        2 -> ShadersColorPanel(
                            state = canvasState,
                            onSelectEffect = { viewModel.setEffect(it) },
                            onSelectCustomColor = { viewModel.setCustomColor(it) }
                        )

                        3 -> EffectsDepthPanel(
                            state = canvasState,
                            onStrokeWidthChange = { viewModel.setStrokeWidth(it) },
                            onStrokeColorChange = { viewModel.setStrokeColor(it) },
                            onShadowRadiusChange = { viewModel.setShadowRadius(it) },
                            onShadowDyChange = { viewModel.setShadowDy(it) },
                            onShadowDxChange = { viewModel.setShadowDx(it) },
                            onShadowColorChange = { viewModel.setShadowColor(it) },
                            onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                        )

                        else -> CanvasBackgroundPanel(
                            state = canvasState,
                            onToggleTransparent = { viewModel.setTransparentBg(it) },
                            onSelectSolidBgColor = { viewModel.setSolidBgColor(it) },
                            onPickImage = onPickImage,
                            onRemoveImage = onRemoveImage,
                            onDimChange = { viewModel.setBackgroundDim(it) },
                            onBlurChange = { viewModel.setBackgroundBlur(it) },
                            onBrightnessChange = { viewModel.setBackgroundBrightness(it) },
                            onContrastChange = { viewModel.setBackgroundContrast(it) },
                            onSaturationChange = { viewModel.setBackgroundSaturation(it) },
                            onVignetteChange = { viewModel.setBackgroundVignette(it) },
                            onResetPhotoAdjustments = {
                                viewModel.setBackgroundBlur(0f)
                                viewModel.setBackgroundBrightness(0f)
                                viewModel.setBackgroundContrast(1.0f)
                                viewModel.setBackgroundSaturation(1.0f)
                                viewModel.setBackgroundVignette(0f)
                                viewModel.setBackgroundDim(0.25f)
                            },
                            onToggleGlassBackdrop = { viewModel.toggleGlassBackdrop(it) },
                            onSelectGlassPreset = { viewModel.setGlassBackdropPreset(it) },
                            onGlassOpacityChange = { viewModel.setGlassBackdropOpacity(it) },
                            onGlassCornerRadiusChange = { viewModel.setGlassBackdropCornerRadius(it) },
                            onGlassPaddingChange = { viewModel.setGlassBackdropPadding(it) },
                            onAspectRatioChange = { viewModel.setAspectRatio(it) },
                            onToggleGridGuides = { viewModel.toggleGridGuides() },
                            onSliderChangeFinished = { viewModel.commitHistoryCheckpoint() }
                        )
                    }
                }
            }
        }
    }
}
