package app.xrtm8.athar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.GlassSurface
import app.xrtm8.athar.ui.theme.CherryAccent
import app.xrtm8.athar.ui.theme.MutedWhite
import app.xrtm8.athar.ui.theme.PureWhite

@Composable
fun KashidaOpportunityChips(
    rawText: String,
    currentMode: KashidaMode,
    targetIndex: Int,
    onSelectOpportunity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val kashidaOpps = remember(rawText) {
        PureArabicKashidaEngine.findOpportunities(rawText)
    }

    if (kashidaOpps.isEmpty()) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Text(text = "مواضع المد:", color = MutedWhite, fontSize = 10.5.sp)
        kashidaOpps.forEach { opp ->
            val isSelected = currentMode == KashidaMode.CUSTOM && targetIndex == opp.globalIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CherryAccent else GlassSurface)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CherryAccent else if (opp.isRecommended) CherryAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectOpportunity(opp.globalIndex)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = opp.displayLabel,
                    color = if (isSelected) AmoledBlack else PureWhite,
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected || opp.isRecommended) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
