package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderColor
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
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

@Composable
fun EffectsDepthPanel(
    state: CanvasState,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeColorChange: (Long) -> Unit,
    onShadowRadiusChange: (Float) -> Unit,
    onShadowDyChange: (Float) -> Unit,
    onShadowDxChange: (Float) -> Unit,
    onShadowColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Stroke Width Slider
        StudioSlider(
            title = "سماكة التحديد الخارجي (Stroke)",
            value = state.strokeWidth,
            onValueChange = onStrokeWidthChange,
            valueRange = 0f..20f,
            step = 1f,
            valueDisplay = "${state.strokeWidth.toInt()} px",
            icon = Icons.Default.BorderColor
        )

        // Stroke Color Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                0xFF000000 to "أسود داكن",
                0xFFFFFFFF to "أبيض ناصع",
                0xFFFFD700 to "ذهب ملكي",
                0xFFE0E0E0 to "فضي بلاتيني"
            ).forEach { (colorLong, name) ->
                val isSelected = state.strokeColorLong == colorLong
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStrokeColorChange(colorLong)
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) AmoledBlack else PureWhite,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Shadow Blur Radius Slider
        StudioSlider(
            title = "تشتت الظل والوهج (Blur Radius)",
            value = state.shadowRadius,
            onValueChange = onShadowRadiusChange,
            valueRange = 0f..50f,
            step = 2f,
            valueDisplay = "${state.shadowRadius.toInt()} px",
            icon = Icons.Default.BlurOn
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Directional 2D Shadow: Dy and Dx
        StudioSlider(
            title = "عمق الظل الرأسي (Dy)",
            value = state.shadowDy,
            onValueChange = onShadowDyChange,
            valueRange = -15f..35f,
            step = 1f,
            valueDisplay = "${state.shadowDy.toInt()} px"
        )

        Spacer(modifier = Modifier.height(4.dp))

        StudioSlider(
            title = "انحراف الظل الأفقي (Dx)",
            value = state.shadowDx,
            onValueChange = onShadowDxChange,
            valueRange = -25f..25f,
            step = 1f,
            valueDisplay = "${state.shadowDx.toInt()} px"
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(text = "لون الظل والتوهج:", color = MutedWhite, fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0xCC000000L to "ظل أسود",
                0xAAFFD700L to "توهج ذهبي",
                0xAA00FF9DL to "توهج زمردي",
                0xAAFFFFFFL to "وهج أبيض",
                0xAAFF3366L to "وهج ياقوتي"
            ).forEach { (colorLong, name) ->
                val isSelected = state.shadowColorLong == colorLong
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShadowColorChange(colorLong)
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) AmoledBlack else PureWhite,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
