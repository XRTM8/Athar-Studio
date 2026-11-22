package app.xrtm8.athar.ui.panels.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.AtharMaskState
import app.xrtm8.athar.model.BlackGradientPlacement
import app.xrtm8.athar.model.MaskBlendMode
import app.xrtm8.athar.ui.theme.*

/**
 * PicsArt-Grade Mask Studio & Atmospheric Black Gradient Coordinator Panel.
 * Assembles MaskCarouselSection, MaskAdjustmentsSection, and BlackGradientSection (< 90 lines).
 */
@Composable
fun MaskPanel(
    state: AtharMaskState,
    onSelectMask: (String?) -> Unit,
    onPickCustomMask: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (MaskBlendMode) -> Unit,
    onHueShiftChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onToggleFlipH: () -> Unit,
    onToggleFlipV: () -> Unit,
    onRotate90: () -> Unit,
    onToggleBlackGradient: (Boolean) -> Unit,
    onBlackGradientOpacityChange: (Float) -> Unit,
    onBlackGradientPlacementChange: (BlackGradientPlacement) -> Unit,
    onBlackGradientCoverageChange: (Float) -> Unit,
    onResetMask: () -> Unit,
    onSliderChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header with Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نظام الأقنعة والتدرج الأسود (خلف النص)",
                color = PureWhite,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
            if (state.isEffectActive) {
                Text(
                    text = "إعادة تعيين",
                    color = CherryAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResetMask()
                    }
                )
            }
        }

        // 2. Mask Thumbnails Carousel (Category Filter + Thumbnails + Custom Import)
        MaskCarouselSection(
            state = state,
            onSelectMask = onSelectMask,
            onPickCustomMask = onPickCustomMask
        )

        // 3. Fine-tuning adjustments (Opacity, Scale, Offsets, Hue, Flip, Rotate, Blend Mode)
        MaskAdjustmentsSection(
            state = state,
            onOpacityChange = onOpacityChange,
            onBlendModeChange = onBlendModeChange,
            onHueShiftChange = onHueShiftChange,
            onOffsetXChange = onOffsetXChange,
            onOffsetYChange = onOffsetYChange,
            onScaleChange = onScaleChange,
            onToggleFlipH = onToggleFlipH,
            onToggleFlipV = onToggleFlipV,
            onRotate90 = onRotate90,
            onSliderChangeFinished = onSliderChangeFinished
        )

        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

        // 4. Atmospheric Black Gradient
        BlackGradientSection(
            gradient = state.blackGradient,
            onToggle = onToggleBlackGradient,
            onPlacementChange = onBlackGradientPlacementChange,
            onOpacityChange = onBlackGradientOpacityChange,
            onCoverageChange = onBlackGradientCoverageChange,
            onSliderChangeFinished = onSliderChangeFinished
        )
    }
}
