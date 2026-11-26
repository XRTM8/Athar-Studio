package app.xrtm8.athar.ui.panels.typography

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CustomFont
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.ui.components.FontPickerCarousel
import app.xrtm8.athar.ui.theme.*

@Composable
fun TypographyFontSection(
    selectedFont: ArabicFontFamily,
    onSelectFont: (ArabicFontFamily) -> Unit,
    onOpenFontBrowser: () -> Unit,
    modifier: Modifier = Modifier,
    fontWeight: Int = 400,
    isBold: Boolean = false,
    customFontPath: String? = null,
    customFontName: String? = null,
    customFonts: List<CustomFont> = emptyList(),
    onSelectCustomFont: (CustomFont) -> Unit = {},
    onFontWeightChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current
    val surfaceBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val borderCol = if (isAmoledWhite) WhiteBorder else GlassBorder
    val subtleText = if (isAmoledWhite) DarkTextSubtle else MutedWhite

    val hasCustom = !customFontPath.isNullOrBlank()
    val effectiveWeight = if (isBold && fontWeight < 700) 700 else fontWeight
    val supportedWeights = remember(selectedFont, customFontPath) {
        AtharFontManager.getSupportedWeights(context, selectedFont, customFontPath)
    }
    val supportsTatweel = remember(selectedFont, customFontPath) {
        AtharFontManager.supportsTatweel(context, selectedFont, customFontPath)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الخط العربي:",
                color = subtleText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceBg)
                    .border(0.5.dp, borderCol, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenFontBrowser()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = null,
                    tint = CherryAccent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "كافة الخطوط والمفضلة",
                    color = CherryAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (hasCustom) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CherryAccent.copy(alpha = 0.12f))
                    .border(0.5.dp, CherryAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable { onOpenFontBrowser() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "الخط المخصص النشط: ${customFontName ?: "خط خارجي"}",
                    color = CherryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        FontPickerCarousel(
            selectedFont = selectedFont,
            onSelectFont = onSelectFont,
            hasCustomFont = hasCustom,
            customFonts = customFonts,
            selectedCustomFontPath = customFontPath,
            onSelectCustomFont = onSelectCustomFont
        )

        if (supportedWeights.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "وزن الخط:", color = subtleText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            val activeSnap = supportedWeights.minByOrNull { kotlin.math.abs(it - effectiveWeight) } ?: effectiveWeight
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                supportedWeights.forEach { weight ->
                    val isWeightSelected = weight == activeSnap
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isWeightSelected) CherryAccent else surfaceBg)
                            .border(0.5.dp, if (isWeightSelected) CherryAccent else borderCol, RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onFontWeightChange(weight)
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${ArabicFontFamily.weightLabel(weight)} $weight",
                            color = if (isWeightSelected) PureWhite else subtleText,
                            fontSize = 10.sp,
                            fontWeight = if (isWeightSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (!supportsTatweel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ملاحظة: هذا الخط لا يحتوي محرف المدّ (ـ)، لذا قد يُستبدل بمدّ افتراضي.",
                color = Color(0xFFF59E0B),
                fontSize = 10.sp
            )
        }
    }
}
