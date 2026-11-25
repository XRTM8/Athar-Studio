package app.xrtm8.athar.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CustomFont
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.ui.theme.*

/**
 * Specimen card row for an imported custom font (.ttf / .otf),
 * rendering live typeface specimen, format/weight metadata, and grouped family weight pills.
 */
@Composable
fun CustomFontItemRow(
    customFont: CustomFont,
    isSelected: Boolean,
    sampleText: String,
    isAmoledWhite: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    familyVariants: List<CustomFont> = emptyList(),
    selectedCustomPath: String? = null,
    onSelectVariant: (CustomFont) -> Unit = { onSelect() },
    onDeleteVariant: (String) -> Unit = { onDelete() }
) {
    val context = LocalContext.current
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val badgeBg = if (isAmoledWhite) WhiteSurfaceElevated else AmoledBlack
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val subtleText = if (isAmoledWhite) DarkTextSubtle else MutedWhite

    val variants = remember(customFont, familyVariants) {
        if (familyVariants.size > 1) familyVariants.sortedBy { it.weightClass }
        else listOf(customFont)
    }
    val activeVariant = remember(variants, selectedCustomPath, customFont) {
        variants.find { it.filePath == selectedCustomPath } ?: customFont
    }
    val composeFont = remember(activeVariant.filePath, activeVariant.weightClass) {
        AtharFontManager.getComposeFontFamily(context, activeVariant, activeVariant.weightClass)
    }
    val hasActiveMatch = isSelected || variants.any { it.filePath == selectedCustomPath }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (hasActiveMatch) CherryAccent.copy(alpha = 0.15f) else cardBg)
            .border(
                width = if (hasActiveMatch) 1.5.dp else 0.5.dp,
                color = if (hasActiveMatch) CherryAccent else cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelectVariant(activeVariant) }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = activeVariant.familyName.ifBlank { activeVariant.name },
                        color = if (hasActiveMatch) CherryAccent else primaryText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    MetaChip(text = activeVariant.format, bg = badgeBg, color = CherryAccent)
                    val weightBadge = if (activeVariant.isVariable) "متغير"
                    else "${ArabicFontFamily.weightLabel(activeVariant.weightClass)} ${activeVariant.weightClass}"
                    MetaChip(text = weightBadge, bg = badgeBg, color = subtleText)
                    if (!activeVariant.supportsArabic) {
                        MetaChip(text = "أحرف عربية ناقصة", bg = badgeBg, color = Color(0xFFF59E0B))
                    } else if (!activeVariant.supportsTatweel) {
                        MetaChip(text = "بدون مدّ", bg = badgeBg, color = Color(0xFFF59E0B))
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = sampleText.take(28).ifBlank { "أثر للتايبوغرافي العربي" },
                    fontFamily = composeFont,
                    color = if (hasActiveMatch) primaryText else subtleText,
                    fontSize = 15.5.sp,
                    maxLines = 1
                )
            }
            IconButton(onClick = { onDeleteVariant(activeVariant.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف الخط المخصص",
                    tint = SubtleGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (variants.size > 1) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                variants.forEach { variant ->
                    val isVarSelected = variant.filePath == selectedCustomPath
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isVarSelected) CherryAccent else badgeBg)
                            .border(0.5.dp, if (isVarSelected) CherryAccent else cardBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectVariant(variant) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${ArabicFontFamily.weightLabel(variant.weightClass)} ${variant.weightClass}",
                            color = if (isVarSelected) PureWhite else subtleText,
                            fontSize = 10.sp,
                            fontWeight = if (isVarSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, bg: Color, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
