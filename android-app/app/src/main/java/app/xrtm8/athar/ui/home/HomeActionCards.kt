package app.xrtm8.athar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.theme.CherryAccent

@Composable
fun HomeActionCards(
    cardBackground: Color,
    cardBorder: Color,
    primaryTextColor: Color,
    subtleTextColor: Color,
    onDesignOnPhotoClick: () -> Unit,
    onImportProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Design on Photo Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground)
                .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                .clickable(onClick = onDesignOnPhotoClick)
                .padding(14.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = CherryAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تصميم على صورة",
                    color = primaryTextColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "اختر صورة من المعرض وتنسيق ألوانها",
                    color = subtleTextColor,
                    fontSize = 10.5.sp,
                    maxLines = 2
                )
            }
        }

        // Import .athar Project Card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground)
                .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                .clickable(onClick = onImportProjectClick)
                .padding(14.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = CherryAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "استيراد مشروع",
                    color = primaryTextColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "فتح ملف .athar بكافة طبقاته السابقة",
                    color = subtleTextColor,
                    fontSize = 10.5.sp,
                    maxLines = 2
                )
            }
        }
    }
}
