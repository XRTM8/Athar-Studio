package app.xrtm8.athar.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.theme.*

enum class DiacriticCategory(val title: String) {
    TASHKEEL("الحركات"),
    QURANIC("رسم المصحف"),
    WAQF("علامات الوقف"),
    ORNAMENTS("الزخارف والتراكيب")
}

data class DiacriticItem(val displayGlyph: String, val insertChar: String)

/**
 * Advanced diacritics and Islamic calligraphy glyphs palette (< 160 lines).
 */
@Composable
fun ArabicDiacriticsPalette(
    onInsertGlyph: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(DiacriticCategory.TASHKEEL) }
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current
    val secondaryText = if (isAmoledWhite) DarkTextMuted else MutedWhite
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder

    val items = remember(selectedCategory) {
        when (selectedCategory) {
            DiacriticCategory.TASHKEEL -> listOf(
                DiacriticItem("ـَ", "َ"),
                DiacriticItem("ـُ", "ُ"),
                DiacriticItem("ـِ", "ِ"),
                DiacriticItem("ـّ", "ّ"),
                DiacriticItem("ـْ", "ْ"),
                DiacriticItem("ـً", "ً"),
                DiacriticItem("ـٌ", "ٌ"),
                DiacriticItem("ـٍ", "ٍ"),
                DiacriticItem("ـ", "ـ")
            )
            DiacriticCategory.QURANIC -> listOf(
                DiacriticItem("ـٰ", "\u0670"),
                DiacriticItem("ـٖ", "\u0656"),
                DiacriticItem("ـٗ", "\u0657"),
                DiacriticItem("ـۡ", "\u06E1"),
                DiacriticItem("ـٓ", "\u0653"),
                DiacriticItem("ـ۫", "\u06EB"),
                DiacriticItem("ـ۬", "\u06EC")
            )
            DiacriticCategory.WAQF -> listOf(
                DiacriticItem("ـۗ", "\u06D7"),
                DiacriticItem("ـۖ", "\u06D6"),
                DiacriticItem("ـۚ", "\u06DA"),
                DiacriticItem("ـۘ", "\u06D8"),
                DiacriticItem("ـۜ", "\u06DC"),
                DiacriticItem("ـۛ", "\u06DB"),
                DiacriticItem("ـ۩", "\u06E9"),
                DiacriticItem("۝", "۝"),
                DiacriticItem("۞", "۞")
            )
            DiacriticCategory.ORNAMENTS -> listOf(
                DiacriticItem("ﷺ", "ﷺ"),
                DiacriticItem("ﷻ", "ﷻ"),
                DiacriticItem("﷽", "﷽"),
                DiacriticItem("ـؓ", "\u0613"),
                DiacriticItem("ـؑ", "\u0611"),
                DiacriticItem("ـؒ", "\u0612"),
                DiacriticItem("✦", "✦"),
                DiacriticItem("❖", "❖"),
                DiacriticItem("❈", "❈"),
                DiacriticItem("✤", "✤")
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Category Switcher Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DiacriticCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CherryAccent else cardBg)
                        .border(0.5.dp, if (isSelected) CherryAccent else cardBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = cat
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.title,
                        color = if (isSelected) PureWhite else secondaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Glyphs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .widthIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(0.5.dp, cardBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onInsertGlyph(item.insertChar)
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.displayGlyph,
                        color = GoldAccent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
