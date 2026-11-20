package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.theme.*

data class DockTabItem(
    val index: Int,
    val title: String,
    val icon: ImageVector
)

val STUDIO_DOCK_TABS = listOf(
    DockTabItem(0, "الخط", Icons.Default.TextFields),
    DockTabItem(1, "المدّ", Icons.Default.LinearScale),
    DockTabItem(2, "المظهر", Icons.Default.AutoAwesome),
    DockTabItem(3, "العمق", Icons.Default.Layers),
    DockTabItem(4, "الكانفاس", Icons.Default.AspectRatio)
)

/**
 * 5-Tool Ergonomic Studio Dock with haptic micro-interactions and gold highlight indicators.
 */
@Composable
fun StudioDock(
    selectedTab: Int,
    isPanelExpanded: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBackground)
            .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        STUDIO_DOCK_TABS.forEach { tab ->
            val isSelected = selectedTab == tab.index && isPanelExpanded
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) GoldAccent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(tab.index)
                    }
                    .padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = if (isSelected) GoldAccent else SubtleGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = tab.title,
                    color = if (isSelected) GoldAccent else MutedWhite,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
