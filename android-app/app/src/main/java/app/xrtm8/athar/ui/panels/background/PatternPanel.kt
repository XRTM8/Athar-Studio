package app.xrtm8.athar.ui.panels.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import app.xrtm8.athar.model.PatternBlend
import app.xrtm8.athar.model.PatternState
import app.xrtm8.athar.model.PatternType
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

/**
 * Micro-Texture & Pattern Layer Control Panel (< 200 lines).
 */
@Composable
fun PatternPanel(
    state: PatternState,
    onPatternTypeChange: (PatternType) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onBlendModeChange: (PatternBlend) -> Unit,
    onToggleLightTint: (Boolean) -> Unit,
    onResetPattern: () -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Pattern Type Horizontal Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نمط الباترن والشبكة المجهرية",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (state.isEnabled) {
                    Text(
                        text = "إعادة تعيين",
                        color = CherryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onResetPattern()
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PatternType.values().forEach { type ->
                    val isSelected = state.type == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BurgundyAccent else StudioSurface)
                            .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPatternTypeChange(type)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.label,
                            color = if (isSelected) PureWhite else MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (state.isEnabled) {
            // Opacity & Scale Sliders
            StudioSlider(
                title = "شفافية الباترن",
                value = state.opacity,
                valueRange = 0.05f..1.0f,
                valueDisplay = "${(state.opacity * 100).toInt()}%",
                onValueChange = onOpacityChange,
                onValueChangeFinished = onSliderChangeFinished
            )

            StudioSlider(
                title = "كثافة وحجم الخطوط",
                value = state.scale,
                valueRange = 0.5f..3.0f,
                valueDisplay = String.format("%.1fx", state.scale),
                onValueChange = onScaleChange,
                onValueChangeFinished = onSliderChangeFinished
            )

            // Tint Mode & Blend Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "لون الباترن", color = SubtleGray, fontSize = 11.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(true to "فاتح / أبيض", false to "داكن / أسود").forEach { (isLight, label) ->
                        val isSelected = state.isLightTint == isLight
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
                                .border(0.5.dp, if (isSelected) BurgundyAccent else GlassBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleLightTint(isLight)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) PureWhite else MutedWhite,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Blend Mode Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "نمط الدمج (Blend Mode)", color = SubtleGray, fontSize = 11.5.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PatternBlend.values().forEach { blend ->
                        val isSelected = state.blendMode == blend
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BurgundyAccent else StudioSurface)
                                .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBlendModeChange(blend)
                            }
                            .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = blend.label,
                                color = if (isSelected) PureWhite else MutedWhite,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
