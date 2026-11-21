package app.xrtm8.athar.ui.panels.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

@Composable
fun StrokeShadowSection(
    strokeWidth: Float,
    strokeColorLong: Long,
    isHollowText: Boolean,
    shadowRadius: Float,
    shadowDy: Float,
    shadowDx: Float,
    shadowColorLong: Long,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeColorChange: (Long) -> Unit,
    onToggleHollowText: ((Boolean) -> Unit)? = null,
    onShadowRadiusChange: (Float) -> Unit,
    onShadowDyChange: (Float) -> Unit,
    onShadowDxChange: (Float) -> Unit,
    onShadowColorChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Hollow Outline Typography
        if (onToggleHollowText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CropFree,
                        contentDescription = null,
                        tint = if (isHollowText) GoldAccent else SubtleGray,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "تفريغ الحروف (تحديد خارجي فقط)",
                        color = if (isHollowText) PureWhite else MutedWhite,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = isHollowText,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleHollowText(it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent),
                    modifier = Modifier.height(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Stroke Width Slider
        StudioSlider(
            title = "سماكة التحديد",
            value = strokeWidth,
            onValueChange = onStrokeWidthChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..20f,
            step = 1f,
            valueDisplay = "${strokeWidth.toInt()} px",
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
                0xFFFFD700 to "ذهب كلاسيكي",
                0xFFE0E0E0 to "فضي بلاتيني"
            ).forEach { (colorLong, name) ->
                val isSelected = strokeColorLong == colorLong
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
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
            title = "تشتت الظل",
            value = shadowRadius,
            onValueChange = onShadowRadiusChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 0f..50f,
            step = 2f,
            valueDisplay = "${shadowRadius.toInt()} px",
            icon = Icons.Default.BlurOn
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Directional 2D Shadow: Dy and Dx
        StudioSlider(
            title = "عمق الظل (رأسي)",
            value = shadowDy,
            onValueChange = onShadowDyChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -15f..35f,
            step = 1f,
            valueDisplay = "${shadowDy.toInt()} px"
        )

        Spacer(modifier = Modifier.height(4.dp))

        StudioSlider(
            title = "إزاحة الظل (أفقي)",
            value = shadowDx,
            onValueChange = onShadowDxChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = -25f..25f,
            step = 1f,
            valueDisplay = "${shadowDx.toInt()} px"
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(text = "لون الظل:", color = MutedWhite, fontSize = 11.5.sp)
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
                val isSelected = shadowColorLong == colorLong
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(8.dp))
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
