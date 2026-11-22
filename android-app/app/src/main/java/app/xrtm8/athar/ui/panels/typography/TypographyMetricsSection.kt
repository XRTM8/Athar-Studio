package app.xrtm8.athar.ui.panels.typography

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
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
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

@Composable
fun TypographyMetricsSection(
    fontSize: Float,
    boxWidthRatio: Float,
    onFontSizeChange: (Float) -> Unit,
    onBoxWidthRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Font Size Slider (Range from 6sp to 100sp)
        StudioSlider(
            title = "حجم الخط",
            value = fontSize,
            onValueChange = onFontSizeChange,
            onValueChangeFinished = onSliderChangeFinished,
            valueRange = 6f..100f,
            step = 1f,
            valueDisplay = "${fontSize.toInt()} sp",
            icon = Icons.Default.FormatSize
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Text Box Area & Multi-Line Word Wrapping Control
        StudioSlider(
            title = "عرض مساحة النص (التفاف الأسطر)",
            value = (boxWidthRatio * 100f).coerceIn(30f, 100f),
            onValueChange = { onBoxWidthRatioChange(it / 100f) },
            valueRange = 30f..100f,
            valueDisplay = "${((boxWidthRatio * 100f).toInt())}%",
            onValueChangeFinished = onSliderChangeFinished
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0.50f to "عمودي 50%",
                0.70f to "موجز 70%",
                0.85f to "متوازن 85%",
                1.00f to "كامل 100%"
            ).forEach { (ratio, label) ->
                val isSelected = kotlin.math.abs(boxWidthRatio - ratio) < 0.05f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CherryAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBoxWidthRatioChange(ratio)
                            onSliderChangeFinished?.invoke()
                        }
                        .padding(vertical = 5.dp),
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
}
