package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import app.xrtm8.athar.model.GLASS_BACKDROP_PRESETS
import app.xrtm8.athar.model.GlassBackdropPreset
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.GlassBorder
import app.xrtm8.athar.ui.theme.GlassSurface
import app.xrtm8.athar.ui.theme.GoldAccent
import app.xrtm8.athar.ui.theme.MutedWhite
import app.xrtm8.athar.ui.theme.PureWhite
import app.xrtm8.athar.ui.theme.SubtleGray

@Composable
fun TextGlassBackdropPanel(
    state: CanvasState,
    onToggleGlassBackdrop: (Boolean) -> Unit,
    onSelectPreset: (GlassBackdropPreset) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onPaddingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle Switch for Frosted Glass Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "لوح زجاجي خلف النص",
                    color = PureWhite,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "طبقة مصنفرة شبه شفافة تبرز النص فوق الصور",
                    color = SubtleGray,
                    fontSize = 10.5.sp
                )
            }
            Switch(
                checked = state.enableGlassBackdrop,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleGlassBackdrop(it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
            )
        }

        if (state.enableGlassBackdrop) {
            Spacer(modifier = Modifier.height(10.dp))

            // Presets Horizontal Carousel
            Text(text = "أنماط الزجاج:", color = MutedWhite, fontSize = 11.5.sp)
            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GLASS_BACKDROP_PRESETS.forEach { preset ->
                    val isSelected = state.glassBackdropColorLong == preset.bgArgb
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GoldAccent else GlassSurface)
                            .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectPreset(preset)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(preset.bgArgb.toInt()))
                                .border(1.dp, Color(preset.strokeArgb.toInt()), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = preset.name,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Opacity Slider
            StudioSlider(
                title = "عتامة وشفافية الزجاج",
                value = state.glassBackdropOpacity,
                onValueChange = onOpacityChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = 0.15f..0.95f,
                step = 0.05f,
                valueDisplay = "${(state.glassBackdropOpacity * 100).toInt()}%",
                icon = Icons.Default.Opacity
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Corner Radius Slider
            StudioSlider(
                title = "استدارة حواف البطاقة",
                value = state.glassBackdropCornerRadius,
                onValueChange = onCornerRadiusChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = 6f..44f,
                step = 2f,
                valueDisplay = "${state.glassBackdropCornerRadius.toInt()}dp",
                icon = Icons.Default.RoundedCorner
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Padding Slider
            StudioSlider(
                title = "اتساع وهوامش البطاقة",
                value = state.glassBackdropPadding,
                onValueChange = onPaddingChange,
                onValueChangeFinished = onSliderChangeFinished,
                valueRange = 10f..48f,
                step = 2f,
                valueDisplay = "${state.glassBackdropPadding.toInt()}dp",
                icon = Icons.Default.CropSquare
            )
        }
    }
}
