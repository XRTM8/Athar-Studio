package app.xrtm8.athar.ui.panels

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HistoryEdu
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
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.TashkeelItem
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.ui.theme.*

@Composable
fun ManuscriptAndTashkeelPanel(
    state: CanvasState,
    onAddManuscriptLayer: (TextLayer) -> Unit,
    onAddTashkeelMark: (String) -> Unit,
    onRemoveTashkeelMark: (String) -> Unit,
    onUpdateTashkeelMark: (TashkeelItem) -> Unit,
    onNudgeTashkeelMark: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Sub-segment switcher: المخطوطات vs التشكيل
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                0 to ("المخطوطات الإسلامية" to Icons.Default.HistoryEdu),
                1 to ("التشكيل الحر" to Icons.Default.FormatQuote)
            ).forEach { (idx, item) ->
                val (title, icon) = item
                val isSelected = (subTab == idx)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) GoldAccent else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            subTab = idx
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) AmoledBlack else PureWhite,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedContent(
            targetState = subTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut()
                }
            },
            label = "ManuscriptAndTashkeelSubTabs"
        ) { currentTab ->
            when (currentTab) {
                0 -> IslamicManuscriptsPanel(
                    onAddManuscriptLayer = onAddManuscriptLayer
                )
                else -> FreeTashkeelPanel(
                    state = state,
                    onAddMark = onAddTashkeelMark,
                    onRemoveMark = onRemoveTashkeelMark,
                    onUpdateMark = onUpdateTashkeelMark,
                    onNudgeMark = onNudgeTashkeelMark
                )
            }
        }
    }
}
