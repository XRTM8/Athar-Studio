package app.xrtm8.athar.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import app.xrtm8.athar.model.CustomFont
import app.xrtm8.athar.ui.theme.*

/**
 * Specimen card row for an imported custom font (.ttf / .otf).
 */
@Composable
fun CustomFontItemRow(
    customFont: CustomFont,
    isSelected: Boolean,
    sampleText: String,
    isAmoledWhite: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val primaryText = if (isAmoledWhite) DarkText else PureWhite

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) CherryAccent.copy(alpha = if (isAmoledWhite) 0.15f else 0.16f) else cardBg)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) CherryAccent else cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customFont.name,
                        color = if (isSelected) CherryAccent else primaryText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAmoledWhite) WhiteSurfaceElevated else AmoledBlack)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ملف خارجي",
                            color = CherryAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sampleText.take(24).ifBlank { "أثر للتايبوغرافي العربي" },
                    color = if (isSelected) PureWhite else MutedWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف الخط المخصص",
                    tint = SubtleGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
