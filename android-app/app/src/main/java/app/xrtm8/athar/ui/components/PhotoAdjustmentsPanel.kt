package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Vignette
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.theme.GlassBorder
import app.xrtm8.athar.ui.theme.GlassSurface
import app.xrtm8.athar.ui.theme.GoldAccent
import app.xrtm8.athar.ui.theme.MutedWhite
import app.xrtm8.athar.ui.theme.PureWhite

@Composable
fun PhotoAdjustmentsPanel(
    state: CanvasState,
    onBlurChange: (Float) -> Unit,
    onDimChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onVignetteChange: (Float) -> Unit,
    onResetAdjustments: () -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Blur Slider
        StudioSlider(
            title = "نسبة ضبابية الصورة (Blur)",
            value = state.backgroundBlur,
            onValueChange = onBlurChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..30f,
            step = 1f,
            valueDisplay = "${((state.backgroundBlur / 30f) * 100).toInt()}%",
            icon = Icons.Default.BlurOn
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Dim Overlay Slider
        StudioSlider(
            title = "تعتيم الصورة لبروز الخط (Dim)",
            value = state.backgroundDim,
            onValueChange = onDimChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..0.90f,
            step = 0.05f,
            valueDisplay = "${(state.backgroundDim * 100).toInt()}%",
            icon = Icons.Default.DarkMode
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Brightness Slider
        StudioSlider(
            title = "السطوع (Brightness)",
            value = state.backgroundBrightness,
            onValueChange = onBrightnessChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -0.5f..0.5f,
            step = 0.05f,
            valueDisplay = if (state.backgroundBrightness > 0f) "+${(state.backgroundBrightness * 100).toInt()}%" else "${(state.backgroundBrightness * 100).toInt()}%",
            icon = Icons.Default.Brightness6
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Contrast Slider
        StudioSlider(
            title = "التباين (Contrast)",
            value = state.backgroundContrast,
            onValueChange = onContrastChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0.5f..1.8f,
            step = 0.05f,
            valueDisplay = "${(state.backgroundContrast * 100).toInt()}%",
            icon = Icons.Default.Contrast
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Saturation Slider
        StudioSlider(
            title = "التشبع اللوني (Saturation)",
            value = state.backgroundSaturation,
            onValueChange = onSaturationChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..2.0f,
            step = 0.05f,
            valueDisplay = "${(state.backgroundSaturation * 100).toInt()}%",
            icon = Icons.Default.ColorLens
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Vignette Slider
        StudioSlider(
            title = "الظل السينمائي المحيطي (Vignette)",
            value = state.backgroundVignette,
            onValueChange = onVignetteChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..1.0f,
            step = 0.05f,
            valueDisplay = "${(state.backgroundVignette * 100).toInt()}%",
            icon = Icons.Default.Vignette
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onResetAdjustments()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "إعادة ضبط التعديلات", color = PureWhite, fontSize = 11.sp)
            }
        }
    }
}
