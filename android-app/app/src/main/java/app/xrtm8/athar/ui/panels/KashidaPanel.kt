package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

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
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Stretch Level Slider with StudioSlider
        StudioSlider(
            title = "مستوى المد المتجهي (الكشيدة)",
            value = state.stretchLevel.toFloat(),
            onValueChange = { onStretchLevelChange(it.toInt()) },
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "نمط وتوزيع الكشيدة الخطية:", color = MutedWhite, fontSize = 11.5.sp)
                Text(
                    text = state.kashidaMode.label,
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KashidaMode.entries.forEach { mode ->
                    val isSelected = state.kashidaMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GoldAccent else GlassSurface)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) GoldAccent else GlassBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKashidaModeChange(mode)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = mode.label,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            if (state.kashidaMode.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = state.kashidaMode.description,
                    color = SubtleGray,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }

        // Available Kashida Opportunities (Chips to select specific letter to stretch)
        val kashidaOpps = remember(state.rawText) {
            PureArabicKashidaEngine.findOpportunities(state.rawText)
        }
        if (kashidaOpps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(text = "مواضع المد:", color = MutedWhite, fontSize = 10.5.sp)
                kashidaOpps.forEach { opp ->
                    val isSelected = state.kashidaMode == KashidaMode.CUSTOM &&
                            state.kashidaTargetIndex == opp.globalIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GoldAccent else GlassSurface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) GoldAccent else if (opp.isRecommended) GoldAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTargetIndexChange(opp.globalIndex)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = opp.displayLabel,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected || opp.isRecommended) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

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
            valueRange = -180f..180f,
            step = 5f,
            valueDisplay = "${state.rotationAngle.toInt()}°"
        )
    }
}
