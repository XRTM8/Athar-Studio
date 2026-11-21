package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.ui.theme.*

/**
 * Authentic Arabic Typefaces horizontal selection carousel with specimen text preview.
 */
@Composable
fun FontPickerCarousel(
    selectedFont: ArabicFontFamily,
    onSelectFont: (ArabicFontFamily) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "اختر الخط العربي الأصيل:", color = MutedWhite, fontSize = 11.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArabicFontFamily.entries.forEach { font ->
                val isSelected = selectedFont == font
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) CherryAccent else GlassSurface)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) CherryAccent else GlassBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectFont(font)
                        }
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = font.specimen,
                            color = if (isSelected) AmoledBlack else CherryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = font.fontName,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }
    }
}
