package app.xrtm8.athar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.BuildConfig
import app.xrtm8.athar.ui.theme.*

@Composable
fun HomeHeader(
    isAmoledWhite: Boolean,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    cardBorder: Color,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "أَثَــر",
                    color = primaryTextColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CherryAccent.copy(alpha = 0.15f))
                        .border(0.5.dp, CherryAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = CherryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "محرر ومصمم النصوص العربية",
                color = secondaryTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isAmoledWhite) WhiteSurfaceElevated else StudioSurface)
                .border(0.5.dp, cardBorder, CircleShape)
        ) {
            Icon(
                imageVector = if (isAmoledWhite) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = "تبديل الثيم",
                tint = if (isAmoledWhite) DarkText else CherryAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
