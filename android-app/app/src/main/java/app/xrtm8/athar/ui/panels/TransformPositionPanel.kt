package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.components.TextPlacementMatrix
import app.xrtm8.athar.ui.theme.*
import java.util.Locale

/**
 * Dedicated Transform, Layout & Precision Positioning Tool.
 * Features:
 * - 9-Point Spatial Positioning Matrix
 * - 4-Way Micro-Nudge D-Pad (Pixel Precision)
 * - Angle Scrubber with Snap Presets (-90, 0, 90, 180)
 * - Scale & Zoom Slider with 1.0x Reset
 * - One-Touch Center Reset
 */
@Composable
fun TransformPositionPanel(
    state: CanvasState,
    onSnapPosition: (normX: Float, normY: Float, align: TextAlignOption) -> Unit,
    onNudgeOffset: (deltaX: Float, deltaY: Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onResetTransform: () -> Unit,
    modifier: Modifier = Modifier,
    onAutoSmartPlacement: (() -> Unit)? = null,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Quick Reset to Center Hero CTA
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onResetTransform()
            },
            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderActive),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إعادة ضبط الموضع والتكبير",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. 9-Point Spatial Positioning Matrix + Micro-Nudge D-Pad
        TextPlacementMatrix(
            normOffsetX = state.normOffsetX,
            normOffsetY = state.normOffsetY,
            onSnapPosition = onSnapPosition,
            onNudgeOffset = onNudgeOffset,
            onAutoSmartPlacement = onAutoSmartPlacement
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Precise Rotation with Fast Snap Chips
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تدوير النص:",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick Angle Snap Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    -90f to "-90°",
                    0f to "0°",
                    90f to "+90°",
                    180f to "180°"
                ).forEach { (angle, label) ->
                    val isSelected = ((state.rotationAngle.toInt() % 360 + 360) % 360) == ((angle.toInt() % 360 + 360) % 360)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) GoldAccent else GlassSurface)
                            .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRotationChange(angle)
                                onSliderChangeFinished?.invoke()
                            }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        StudioSlider(
            title = "زاوية الدوران",
            value = state.rotationAngle,
            onValueChange = onRotationChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -180f..180f,
            step = 5f,
            valueDisplay = "${state.rotationAngle.toInt()}°"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Scale / Zoom Factor Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مقياس الحجم والتكبير:",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(GlassSurface)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onScaleChange(1.0f)
                        onSliderChangeFinished?.invoke()
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1.0x",
                    color = GoldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        StudioSlider(
            title = "نسبة التكبير (Scale)",
            value = state.scaleFactor,
            onValueChange = onScaleChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0.4f..3.0f,
            step = 0.05f,
            valueDisplay = String.format(Locale.ENGLISH, "%.2fx", state.scaleFactor)
        )
    }
}
