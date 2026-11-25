package app.xrtm8.athar.ui.panels.typography

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

/**
 * OpenType Stylistic Sets, Swashes, Discretionary Ligatures & Micro-Spacing (< 200 lines).
 */
@Composable
fun TypographyOpenTypeSection(
    stylisticSet: Int,
    enableSwash: Boolean,
    enableDiscretionaryLigatures: Boolean,
    wordSpacing: Float,
    onStylisticSetChange: (Int) -> Unit,
    onToggleSwash: (Boolean) -> Unit,
    onToggleDiscretionaryLigatures: (Boolean) -> Unit,
    onWordSpacingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null,
    supportedFeatures: Set<String>? = null
) {
    val haptic = LocalHapticFeedback.current
    fun isSupported(tag: String): Boolean = supportedFeatures == null || tag in supportedFeatures

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudioSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "خصائص الخط المتقدمة (OpenType Engine)",
            color = PureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        if (supportedFeatures != null && supportedFeatures.isEmpty()) {
            Text(
                text = "هذا الخط لا يتضمن تشكيلات OpenType بديلة إضافية.",
                color = SubtleGray,
                fontSize = 10.sp
            )
        }

        // 1. Stylistic Sets Selector (ss01 .. ss06)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "المحارف والتشكيلات البديلة (Stylistic Sets)",
                color = MutedWhite,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    0 to "تلقائي",
                    1 to "نمط 1 (ss01)",
                    2 to "نمط 2 (ss02)",
                    3 to "نمط 3 (ss03)",
                    4 to "نمط 4 (ss04)",
                    5 to "نمط 5 (ss05)",
                    6 to "نمط 6 (ss06)"
                ).forEach { (setIdx, label) ->
                    val tag = String.format(java.util.Locale.US, "ss%02d", setIdx)
                    val available = setIdx == 0 || isSupported(tag)
                    val isSelected = stylisticSet == setIdx
                    Box(
                        modifier = Modifier
                            .alpha(if (available) 1f else 0.45f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) BurgundyAccent else StudioSurfaceElevated)
                            .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                            .clickable(enabled = available) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onStylisticSetChange(setIdx)
                                onSliderChangeFinished?.invoke()
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) PureWhite else MutedWhite,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // 2. Swashes & Ligatures Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val swashAvailable = isSupported("swsh")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (swashAvailable) 1f else 0.45f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (enableSwash) BurgundyAccent else StudioSurfaceElevated)
                    .border(0.5.dp, if (enableSwash) CherryAccent else GlassBorder, RoundedCornerShape(9.dp))
                    .clickable(enabled = swashAvailable) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleSwash(!enableSwash)
                        onSliderChangeFinished?.invoke()
                    }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (enableSwash) PureWhite else MutedWhite,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "الذيول الممتدة (Swash)",
                        color = if (enableSwash) PureWhite else MutedWhite,
                        fontSize = 10.sp,
                        fontWeight = if (enableSwash) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            val dligAvailable = isSupported("dlig")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (dligAvailable) 1f else 0.45f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (enableDiscretionaryLigatures) BurgundyAccent else StudioSurfaceElevated)
                    .border(0.5.dp, if (enableDiscretionaryLigatures) CherryAccent else GlassBorder, RoundedCornerShape(9.dp))
                    .clickable(enabled = dligAvailable) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleDiscretionaryLigatures(!enableDiscretionaryLigatures)
                        onSliderChangeFinished?.invoke()
                    }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = null,
                        tint = if (enableDiscretionaryLigatures) PureWhite else MutedWhite,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "الوصلات الخاصة (dlig)",
                        color = if (enableDiscretionaryLigatures) PureWhite else MutedWhite,
                        fontSize = 10.sp,
                        fontWeight = if (enableDiscretionaryLigatures) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // 3. Word Spacing Slider
        StudioSlider(
            title = "تباعد الكلمات (Word Spacing)",
            value = wordSpacing,
            range = -10f..40f,
            onValueChange = onWordSpacingChange,
            onValueChangeFinished = onSliderChangeFinished
        )
    }
}
