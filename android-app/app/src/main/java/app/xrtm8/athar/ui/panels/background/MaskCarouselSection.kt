package app.xrtm8.athar.ui.panels.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.*
import app.xrtm8.athar.ui.theme.*

@Composable
fun MaskCarouselSection(
    state: AtharMaskState,
    onSelectMask: (String?) -> Unit,
    onPickCustomMask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf(MaskCategory.ALL) }

    val filteredMasks = remember(selectedCategory) {
        if (selectedCategory == MaskCategory.ALL) {
            AtharMaskCatalog.ALL_MASKS
        } else {
            AtharMaskCatalog.ALL_MASKS.filter { it.category == selectedCategory }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MaskCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BurgundyAccent else StudioSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = cat
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.label,
                        color = if (isSelected) PureWhite else MutedWhite,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Thumbnails Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. None Option
            val isNoneSelected = !state.isEnabled || (state.activeMaskId == null && state.customMaskUri == null)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isNoneSelected) BurgundyAccent.copy(alpha = 0.35f) else StudioSurface)
                    .border(
                        width = if (isNoneSelected) 1.5.dp else 0.5.dp,
                        color = if (isNoneSelected) CherryAccent else GlassBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMask(null)
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = if (isNoneSelected) CherryAccent else SubtleGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "بدون",
                    color = if (isNoneSelected) PureWhite else MutedWhite,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 2. Custom Mask Import Option (+ مخصص)
            val isCustomActive = state.isEnabled && !state.customMaskUri.isNullOrEmpty()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCustomActive) BurgundyAccent.copy(alpha = 0.45f) else StudioSurface)
                    .border(
                        width = if (isCustomActive) 1.5.dp else 0.5.dp,
                        color = if (isCustomActive) CherryAccent else GlassBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPickCustomMask()
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCustomActive) CherryAccent.copy(alpha = 0.25f) else GlassSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = if (isCustomActive) CherryAccent else MutedWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCustomActive) "خامة مخصصة" else "+ استيراد",
                    color = if (isCustomActive) PureWhite else MutedWhite,
                    fontSize = 10.sp,
                    fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            // 3. Real Visual Thumbnails for Built-in Masks
            filteredMasks.forEach { maskDef ->
                val isSelected = state.isEnabled && state.activeMaskId == maskDef.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) BurgundyAccent.copy(alpha = 0.45f) else StudioSurface)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) CherryAccent else GlassBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectMask(maskDef.id)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    MaskThumbnailView(
                        maskDef = maskDef,
                        modifier = Modifier
                            .size(44.dp)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) CherryAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = maskDef.title,
                        color = if (isSelected) PureWhite else MutedWhite,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
