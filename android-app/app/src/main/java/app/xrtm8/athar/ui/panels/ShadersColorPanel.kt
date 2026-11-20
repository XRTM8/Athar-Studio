package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.LUXURY_SWATCHES
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.ui.theme.*

@Composable
fun ShadersColorPanel(
    state: CanvasState,
    onSelectEffect: (VisualEffect) -> Unit,
    onSelectCustomColor: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "التظليلات المعدنية الفاخرة (Metallic Shaders):", color = MutedWhite, fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VisualEffect.entries.forEach { effect ->
                val isSelected = state.selectedEffect == effect && state.customTextColorLong == null
                val gradientBrush = when (effect) {
                    VisualEffect.ROYAL_GOLD -> Brush.horizontalGradient(listOf(Color(0xFFFFF7DB), Color(0xFFF5C369), Color(0xFFD49438)))
                    VisualEffect.LIQUID_CHROME -> Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFF9EA5B0), Color(0xFF3A3F47)))
                    VisualEffect.FROSTED_GLASS -> Brush.horizontalGradient(listOf(Color(0xCCFFFFFF), Color(0x33E2E8F0)))
                    VisualEffect.CYBER_NEON -> Brush.horizontalGradient(listOf(Color(0xFF00FF9D), Color(0xFF00B0FF)))
                    VisualEffect.CINEMATIC_SHADOW -> Brush.horizontalGradient(listOf(Color(0xFFF5F6F8), Color(0xFF4A4E5A)))
                    VisualEffect.MINIMAL_LUXURY -> Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD4D4D8)))
                }

                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) GoldAccent else GlassBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectEffect(effect)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(gradientBrush)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = effect.arabicName,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "لوحة الألوان الفاخرة المخصصة:", color = MutedWhite, fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LUXURY_SWATCHES.forEach { swatch ->
                val isSelected = state.customTextColorLong == swatch.argb
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCustomColor(if (swatch.argb == 0L) null else swatch.argb)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (swatch.argb == 0L) GoldAccent else Color(swatch.argb.toInt()))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) PureWhite else GlassBorder,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = swatch.name,
                        color = if (isSelected) GoldAccent else SubtleGray,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
