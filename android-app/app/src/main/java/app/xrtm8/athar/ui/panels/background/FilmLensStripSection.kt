package app.xrtm8.athar.ui.panels.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Vignette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.OpticalLensState
import app.xrtm8.athar.model.OpticalLensType
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

/**
 * Compact Dazz-inspired Optical Lens Attachment Strip & Contextual Controls (< 180 lines).
 */
@Composable
fun FilmLensStripSection(
    opticalLensState: OpticalLensState,
    onOpticalLensChange: (OpticalLensState) -> Unit,
    onSliderChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudioSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "العدسات البصرية (Optical Lens)",
                color = PureWhite,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
            if (opticalLensState.type != OpticalLensType.NONE) {
                Text(
                    text = "إعادة تعيين",
                    color = CherryAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpticalLensChange(OpticalLensState())
                        onSliderChangeFinished?.invoke()
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
            OpticalLensType.entries.forEach { lens ->
                val isSelected = opticalLensState.type == lens
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BurgundyAccent else StudioSurfaceElevated)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val updated = if (lens == OpticalLensType.NONE) {
                                OpticalLensState(type = OpticalLensType.NONE)
                            } else {
                                opticalLensState.copy(
                                    type = lens,
                                    intensity = if (opticalLensState.intensity <= 0.05f) 0.65f else opticalLensState.intensity
                                )
                            }
                            onOpticalLensChange(updated)
                            onSliderChangeFinished?.invoke()
                        }
                        .padding(horizontal = 6.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = lensIconFor(lens),
                        contentDescription = lens.labelAr,
                        tint = if (isSelected) PureWhite else lensAccentTint(lens),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = shortLensLabel(lens),
                        color = if (isSelected) PureWhite else MutedWhite,
                        fontSize = 9.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (opticalLensState.type != OpticalLensType.NONE) {
            Text(
                text = opticalLensState.type.descriptionAr,
                color = SubtleGray,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            StudioSlider(
                title = "شدة العدسة",
                value = opticalLensState.intensity * 100f,
                valueRange = 0f..100f,
                step = 5f,
                valueDisplay = "${(opticalLensState.intensity * 100f).toInt()}%",
                onValueChange = {
                    onOpticalLensChange(opticalLensState.copy(intensity = (it / 100f).coerceIn(0f, 1f)))
                },
                onValueChangeFinished = onSliderChangeFinished
            )

            StudioSlider(
                title = "الانتشار والمدى",
                value = opticalLensState.spread * 100f,
                valueRange = 10f..100f,
                step = 5f,
                valueDisplay = "${(opticalLensState.spread * 100f).toInt()}%",
                onValueChange = {
                    onOpticalLensChange(opticalLensState.copy(spread = (it / 100f).coerceIn(0.1f, 1f)))
                },
                onValueChangeFinished = onSliderChangeFinished
            )

            val needsAngle = opticalLensState.type == OpticalLensType.STARBURST_4PT ||
                opticalLensState.type == OpticalLensType.PRISM_DIFFRACTION ||
                opticalLensState.type == OpticalLensType.HALATION_LEAK
            if (needsAngle) {
                StudioSlider(
                    title = "زاوية التشتت",
                    value = opticalLensState.angleDegrees,
                    valueRange = 0f..180f,
                    step = 5f,
                    valueDisplay = "${opticalLensState.angleDegrees.toInt()}°",
                    onValueChange = {
                        onOpticalLensChange(opticalLensState.copy(angleDegrees = it.coerceIn(0f, 360f)))
                    },
                    onValueChangeFinished = onSliderChangeFinished
                )
            }
        }
    }
}

private fun lensIconFor(lens: OpticalLensType): ImageVector = when (lens) {
    OpticalLensType.NONE -> Icons.Default.Block
    OpticalLensType.SOFT_WIDE -> Icons.Default.BlurOn
    OpticalLensType.ORGANIC_VIGNETTE -> Icons.Default.Vignette
    OpticalLensType.HALATION_LEAK -> Icons.Default.Flare
    OpticalLensType.PRISM_DIFFRACTION -> Icons.Default.Gradient
    OpticalLensType.STARBURST_4PT -> Icons.Default.AutoAwesome
}

private fun lensAccentTint(lens: OpticalLensType): Color = when (lens) {
    OpticalLensType.NONE -> SubtleGray
    OpticalLensType.SOFT_WIDE -> Color(0xFF90CAF9)
    OpticalLensType.ORGANIC_VIGNETTE -> MutedWhite
    OpticalLensType.HALATION_LEAK -> Color(0xFFFF7043)
    OpticalLensType.PRISM_DIFFRACTION -> Color(0xFF4DD0E1)
    OpticalLensType.STARBURST_4PT -> Color(0xFFCE93D8)
}

private fun shortLensLabel(lens: OpticalLensType): String = when (lens) {
    OpticalLensType.NONE -> "بدون"
    OpticalLensType.SOFT_WIDE -> "عدسة واسعة"
    OpticalLensType.ORGANIC_VIGNETTE -> "تظليل طبيعي"
    OpticalLensType.HALATION_LEAK -> "هالة فيلمية"
    OpticalLensType.PRISM_DIFFRACTION -> "موشور"
    OpticalLensType.STARBURST_4PT -> "نجمة ٤"
}
