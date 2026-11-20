package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VerticalAlignTop
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
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.ui.components.KashidaModeSelector
import app.xrtm8.athar.ui.components.KashidaOpportunityChips
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.GlassSurface
import app.xrtm8.athar.ui.theme.GoldAccent
import app.xrtm8.athar.ui.theme.MutedWhite
import app.xrtm8.athar.ui.theme.PureWhite

@Composable
fun KashidaPanel(
    state: CanvasState,
    onStretchLevelChange: (Int) -> Unit,
    onKashidaModeChange: (KashidaMode) -> Unit,
    onTargetIndexChange: (Int) -> Unit,
    onRotationChange: (Float) -> Unit,
    onRotateStep: (Float) -> Unit,
    onAlignTop: () -> Unit,
    onAlignCenter: () -> Unit,
    onAlignBottom: () -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Stretch Level Slider with StudioSlider
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

        Spacer(modifier = Modifier.height(4.dp))

        // Quick Stretch Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0 to "بدون مد", 2 to "2x", 4 to "4x", 6 to "6x", 8 to "أقصى 8x").forEach { (lvl, title) ->
                val isSelected = state.stretchLevel == lvl
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStretchLevelChange(lvl)
                        }
                        .padding(vertical = 5.dp),
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

        Spacer(modifier = Modifier.height(10.dp))

        // Kashida Distribution Mode (7 Classical Calligraphic Varieties)
        KashidaModeSelector(
            selectedMode = state.kashidaMode,
            onSelectMode = onKashidaModeChange
        )

        // Available Kashida Opportunities (Chips to select specific letter to stretch)
        Spacer(modifier = Modifier.height(6.dp))
        KashidaOpportunityChips(
            rawText = state.rawText,
            currentMode = state.kashidaMode,
            targetIndex = state.kashidaTargetIndex,
            onSelectOpportunity = onTargetIndexChange
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Vertical Alignment & Snaps
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "المحاذاة الرأسية للكانفاس:", color = MutedWhite, fontSize = 11.5.sp)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassSurface)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAlignTop()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.VerticalAlignTop, contentDescription = "أعلى", tint = PureWhite, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAlignCenter()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.VerticalAlignCenter, contentDescription = "توسيط", tint = GoldAccent, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAlignBottom()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = "أسفل", tint = PureWhite, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Rotation & Precise Angle Scrubber
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "التدوير الدقيق", color = PureWhite, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GlassSurface)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRotateStep(-90f)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("-90°", color = PureWhite, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GlassSurface)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRotationChange(0f)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("0°", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GlassSurface)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRotateStep(90f)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("+90°", color = PureWhite, fontSize = 10.sp)
                }
                Text(text = "${state.rotationAngle.toInt()}°", color = GoldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        StudioSlider(
            title = "زاوية الدوران",
            value = state.rotationAngle,
            onValueChange = onRotationChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -180f..180f,
            step = 5f,
            valueDisplay = "${state.rotationAngle.toInt()}°"
        )
    }
}
