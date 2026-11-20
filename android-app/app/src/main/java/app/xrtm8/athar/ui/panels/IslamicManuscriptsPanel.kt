package app.xrtm8.athar.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.core.IslamicManuscriptEngine
import app.xrtm8.athar.core.IslamicManuscriptEngine.ManuscriptCategory
import app.xrtm8.athar.core.IslamicManuscriptEngine.ManuscriptEntry
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.ui.theme.*

@Composable
fun IslamicManuscriptsPanel(
    onAddManuscriptLayer: (TextLayer) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf<ManuscriptCategory?>(null) }

    val filteredList = remember(selectedCategory) {
        val cat = selectedCategory
        if (cat == null) IslamicManuscriptEngine.MANUSCRIPTS
        else IslamicManuscriptEngine.MANUSCRIPTS.filter { it.category == cat }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المخطوطات والعبارات الإسلامية",
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${filteredList.size} مخطوطة",
                color = SubtleGray,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Categories Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedCategory = null
                },
                label = { Text("الكل", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldAccent,
                    selectedLabelColor = AmoledBlack,
                    containerColor = GlassCard,
                    labelColor = MutedWhite
                ),
                shape = RoundedCornerShape(12.dp)
            )

            ManuscriptCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedCategory = cat
                    },
                    label = { Text(cat.labelAr, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldAccent,
                        selectedLabelColor = AmoledBlack,
                        containerColor = GlassCard,
                        labelColor = MutedWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manuscripts Cards Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredList.forEach { entry ->
                ManuscriptCard(
                    entry = entry,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val layer = IslamicManuscriptEngine.createLayerFromManuscript(entry)
                        onAddManuscriptLayer(layer)
                    }
                )
            }
        }
    }
}

@Composable
private fun ManuscriptCard(
    entry: ManuscriptEntry,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassCard)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entry.titleAr,
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = entry.fullText,
                    color = PureWhite,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Right
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(GoldAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة",
                        tint = GoldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "إدراج",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
