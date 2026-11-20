package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LinearScale
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
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.ui.components.KashidaModeSelector
import app.xrtm8.athar.ui.components.KashidaOpportunityChips
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

/**
 * Pure Authentic Arabic Calligraphy & Kashida Panel.
 * Focused exclusively on calligraphic elongation, opportunity points, and classical modes.
 */
@Composable
fun KashidaPanel(
    state: CanvasState,
    onStretchLevelChange: (Int) -> Unit,
    onKashidaModeChange: (KashidaMode) -> Unit,
    onTargetIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Hero Header Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LinearScale,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "المدّ والكشيدة العربية الأصيلة",
                    color = PureWhite,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(GlassSurface)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStretchLevelChange(0)
                        onTargetIndexChange(-1)
                        onSliderChangeFinished?.invoke()
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "إلغاء المد (0x)",
                    color = SubtleGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Stretch Level Slider
        StudioSlider(
            title = "مستوى المد المتجهي (الكشيدة)",
            value = state.stretchLevel.toFloat(),
            onValueChange = { onStretchLevelChange(it.toInt()) },
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..8f,
            step = 1f,
            steps = 7,
            valueDisplay = "${state.stretchLevel}x",
            icon = Icons.Default.LinearScale
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Quick Stretch Presets Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0 to "بدون مد",
                2 to "خفيف 2x",
                4 to "متوازن 4x",
                6 to "ممدود 6x",
                8 to "ملكي 8x"
            ).forEach { (lvl, title) ->
                val isSelected = state.stretchLevel == lvl
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStretchLevelChange(lvl)
                            onSliderChangeFinished?.invoke()
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) AmoledBlack else PureWhite,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Kashida Distribution Mode Selector (7 Classical Styles)
        Text(
            text = "أسلوب توزيع المد الخطّي:",
            color = MutedWhite,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        KashidaModeSelector(
            selectedMode = state.kashidaMode,
            onSelectMode = onKashidaModeChange
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Interactive Letter Chips for Individual Letter Elongation
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "اختر حرفاً محدداً لتمديده بشكل مستقل:",
                color = MutedWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        KashidaOpportunityChips(
            rawText = state.rawText,
            currentMode = state.kashidaMode,
            targetIndex = state.kashidaTargetIndex,
            onSelectOpportunity = onTargetIndexChange
        )
    }
}
