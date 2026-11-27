package app.xrtm8.athar.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.core.CollageLayoutCalculator
import app.xrtm8.athar.model.CollageLayoutType
import app.xrtm8.athar.ui.theme.CherryAccent
import app.xrtm8.athar.ui.theme.PureWhite

@Composable
fun HomeCollageCard(
    cardBackground: Color,
    cardBorder: Color,
    primaryTextColor: Color,
    subtleTextColor: Color,
    onStartCollage: (CollageLayoutType) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickLayouts = remember {
        listOf(
            CollageLayoutType.DIPTYCH_V to "ثنائي 50/50",
            CollageLayoutType.TRIPTYCH_HERO to "ثلاثي بطل",
            CollageLayoutType.QUAD_GRID to "شبكة 2×2",
            CollageLayoutType.MAGAZINE_MOSAIC to "فسيفساء",
            CollageLayoutType.BENTO_SIX to "بنتو 3×2"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBackground)
            .border(0.75.dp, cardBorder, RoundedCornerShape(18.dp))
            .clickable { onStartCollage(CollageLayoutType.QUAD_GRID) }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CherryAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = CherryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "استوديو الكولاج",
                            color = primaryTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CherryAccent)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "2.0",
                                color = PureWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "اختر تخطيطاً للبدء مباشرة وتعبئة الصور",
                        color = subtleTextColor,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = CherryAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickLayouts.forEach { (layout, title) ->
                val cells = remember(layout) { CollageLayoutCalculator.generateCellsForLayout(layout) }
                Column(
                    modifier = Modifier
                        .width(74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CherryAccent.copy(alpha = 0.06f))
                        .border(0.75.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable { onStartCollage(layout) }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(width = 30.dp, height = 36.dp)) {
                        val gap = 1.5.dp.toPx()
                        val cr = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        cells.forEach { cell ->
                            val l = cell.leftNorm * size.width + gap * 0.5f
                            val t = cell.topNorm * size.height + gap * 0.5f
                            val r = cell.rightNorm * size.width - gap * 0.5f
                            val b = cell.bottomNorm * size.height - gap * 0.5f
                            drawRoundRect(
                                color = CherryAccent.copy(alpha = 0.75f),
                                topLeft = Offset(l, t),
                                size = Size((r - l).coerceAtLeast(1f), (b - t).coerceAtLeast(1f)),
                                cornerRadius = cr
                            )
                        }
                    }
                    Text(
                        text = title,
                        color = primaryTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
