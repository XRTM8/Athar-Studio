package app.xrtm8.athar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CollageLayoutType
import app.xrtm8.athar.ui.theme.BurgundyAccent
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
    val quickLayouts = listOf(
        CollageLayoutType.DIPTYCH_V to "ثنائي 50/50",
        CollageLayoutType.TRIPTYCH_HERO to "ثلاثي بطل",
        CollageLayoutType.QUAD_GRID to "شبكة 2×2",
        CollageLayoutType.MAGAZINE_MOSAIC to "فسيفساء مجلة",
        CollageLayoutType.BENTO_SIX to "بنتو 3×2"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        cardBackground,
                        BurgundyAccent.copy(alpha = 0.14f)
                    )
                )
            )
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CherryAccent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = CherryAccent,
                        modifier = Modifier.size(20.dp)
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
                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "2.0",
                                color = PureWhite,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "دمج احترافي للصور بتخطيطات مجلات وتبديل فوري للخلايا",
                        color = subtleTextColor,
                        fontSize = 10.5.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickLayouts.forEach { (layout, title) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBackground)
                        .border(0.5.dp, cardBorder, RoundedCornerShape(10.dp))
                        .clickable { onStartCollage(layout) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = title,
                        color = primaryTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
