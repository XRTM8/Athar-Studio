package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

val FLARE_COLOR_SWATCHES = listOf(
    Pair("شمس ذهبية", 0xFFFFE082),
    Pair("أزرق سينمائي", 0xFF80D8FF),
    Pair("دفء الغروب", 0xFFFFAB91),
    Pair("نيون زمردي", 0xFF69F0AE),
    Pair("أبيض ناصع", 0xFFFFFFFF)
)

@Composable
fun OpticalAndGrainPanel(
    state: CanvasState,
    onFilmGrainIntensityChange: (Float) -> Unit,
    onToggleLensFlare: (Boolean) -> Unit,
    onLensFlareIntensityChange: (Float) -> Unit,
    onLensFlareXChange: (Float) -> Unit,
    onLensFlareYChange: (Float) -> Unit,
    onLensFlareColorChange: (Long) -> Unit,
    onSliderChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Film Grain Section
        Text(
            text = "حبيبات الفيلم التماثلية 35mm (Film Grain)",
            color = PureWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "توليد خوارزمي واقعي يمنح التصميم عمقاً سينمائياً كلاسيكياً",
            color = SubtleGray,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        StudioSlider(
            title = "كثافة الحبيبات",
            value = state.filmGrainIntensity,
            valueRange = 0f..1.0f,
            step = 0.05f,
            valueDisplay = "${(state.filmGrainIntensity * 100).toInt()}%",
            onValueChange = onFilmGrainIntensityChange,
            onValueChangeFinished = onSliderChangeFinished
        )

        Spacer(modifier = Modifier.height(14.dp))
        Divider(color = GlassBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // 2. Cinematic Lens Flare Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "التوهج السينمائي (Anamorphic Lens Flare)",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "شريط ضوئي وتوهج شعاعي واقعي في الفضاء",
                    color = SubtleGray,
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = state.lensFlareEnabled,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleLensFlare(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmoledBlack,
                    checkedTrackColor = GoldAccent,
                    uncheckedThumbColor = SubtleGray,
                    uncheckedTrackColor = GlassCard
                )
            )
        }

        if (state.lensFlareEnabled) {
            Spacer(modifier = Modifier.height(8.dp))

            StudioSlider(
                title = "قوة التوهج",
                value = state.lensFlareIntensity,
                valueRange = 0.1f..1.0f,
                step = 0.05f,
                valueDisplay = "${(state.lensFlareIntensity * 100).toInt()}%",
                onValueChange = onLensFlareIntensityChange,
                onValueChangeFinished = onSliderChangeFinished
            )

            Spacer(modifier = Modifier.height(4.dp))

            StudioSlider(
                title = "الموضع الأفقي (X)",
                value = state.lensFlareX,
                valueRange = 0.0f..1.0f,
                step = 0.02f,
                valueDisplay = String.format(java.util.Locale.US, "%.2f", state.lensFlareX),
                onValueChange = onLensFlareXChange,
                onValueChangeFinished = onSliderChangeFinished
            )

            Spacer(modifier = Modifier.height(4.dp))

            StudioSlider(
                title = "الموضع الرأسي (Y)",
                value = state.lensFlareY,
                valueRange = 0.0f..1.0f,
                step = 0.02f,
                valueDisplay = String.format(java.util.Locale.US, "%.2f", state.lensFlareY),
                onValueChange = onLensFlareYChange,
                onValueChangeFinished = onSliderChangeFinished
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "لون التوهج:", color = SubtleGray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FLARE_COLOR_SWATCHES.forEach { (name, argb) ->
                    val isSelected = (state.lensFlareColorLong == argb)
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(argb.toInt()))
                            .border(if (isSelected) 2.dp else 0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLensFlareColorChange(argb)
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (argb == 0xFFFFFFFF) AmoledBlack else PureWhite,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
