package app.xrtm8.athar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.project.AtharProjectMeta
import app.xrtm8.athar.ui.theme.CherryAccent

fun LazyListScope.homeRecentProjectsSection(
    projectsList: List<AtharProjectMeta>,
    cardBackground: Color,
    cardBorder: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    subtleTextColor: Color,
    onProjectClick: (AtharProjectMeta) -> Unit,
    onProjectShare: (AtharProjectMeta) -> Unit,
    onProjectRename: (AtharProjectMeta) -> Unit,
    onProjectDelete: (AtharProjectMeta) -> Unit
) {
    // Header
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المشاريع والمسودات الأخيرة",
                color = primaryTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            if (projectsList.isNotEmpty()) {
                Text(
                    text = "${projectsList.size} مشروع",
                    color = CherryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Empty State or Grid
    if (projectsList.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBackground)
                    .border(0.5.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = subtleTextColor,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لا توجد مشاريع سابقة محفوظة",
                        color = secondaryTextColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أي تصميم تقوم بحفظه كمسودة سيظهر هنا لتعديله في أي وقت",
                        color = subtleTextColor,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        val chunks = projectsList.chunked(2)
        items(chunks.size) { chunkIdx ->
            val pair = chunks[chunkIdx]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { proj ->
                    HomeProjectCard(
                        project = proj,
                        cardBackground = cardBackground,
                        cardBorder = cardBorder,
                        primaryTextColor = primaryTextColor,
                        subtleTextColor = subtleTextColor,
                        onClick = { onProjectClick(proj) },
                        onShare = { onProjectShare(proj) },
                        onRename = { onProjectRename(proj) },
                        onDelete = { onProjectDelete(proj) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
