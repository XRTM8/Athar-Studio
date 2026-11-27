package app.xrtm8.athar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.ui.theme.*

@Composable
fun HomeHeroCard(
    isAmoledWhite: Boolean,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    subtleTextColor: Color,
    cardBorder: Color,
    selectedAspectRatio: CanvasAspectRatio,
    onSelectAspectRatio: (CanvasAspectRatio) -> Unit,
    onStartDesign: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val heroGrad = if (isAmoledWhite) {
        Brush.verticalGradient(listOf(WhiteSurface, WhiteSurfaceElevated))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF220810), Color(0xFF14151B)))
    }
    val heroBorder = if (isAmoledWhite) {
        Brush.horizontalGradient(listOf(CherryAccent.copy(alpha = 0.5f), WhiteBorder))
    } else {
        Brush.horizontalGradient(listOf(CherryAccent.copy(alpha = 0.7f), Color(0x33E02447)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(heroGrad)
            .border(
                width = 1.dp,
                brush = heroBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CherryAccent)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "مشروع جديد",
                            color = primaryTextColor,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ابدأ لوحة فارغة بالمقاس المناسب",
                            color = subtleTextColor,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "اختر قياس التصميم:",
                color = secondaryTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CanvasAspectRatio.entries.forEach { ratio ->
                    val isSelected = selectedAspectRatio == ratio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CherryAccent else if (isAmoledWhite) WhiteSurfaceElevated else GlassSurface)
                            .border(
                                0.5.dp,
                                if (isSelected) CherryAccent else cardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectAspectRatio(ratio)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = ratio.label,
                            color = if (isSelected) PureWhite else primaryTextColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStartDesign,
                colors = ButtonDefaults.buttonColors(containerColor = CherryAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "بدء التصميم",
                        color = PureWhite,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
