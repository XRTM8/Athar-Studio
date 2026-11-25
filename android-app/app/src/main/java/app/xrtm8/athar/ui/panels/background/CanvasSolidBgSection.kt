package app.xrtm8.athar.ui.panels.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import app.xrtm8.athar.model.LUXURY_BG_PRESETS
import app.xrtm8.athar.ui.dialogs.CustomColorPickerDialog
import app.xrtm8.athar.ui.theme.*

@Composable
internal fun CanvasSolidBgSection(
    state: CanvasState,
    onRemoveImage: () -> Unit,
    onToggleTransparent: (Boolean) -> Unit,
    onSelectSolidBgColor: (Long?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showCustomBgColorDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "خلفية شفافة", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "تفريغ الخلفية لتصدير ملصق شفاف (PNG)", color = SubtleGray, fontSize = 10.sp)
        }
        Switch(
            checked = state.isTransparentBg,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleTransparent(it)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = CherryAccent)
        )
    }

    if (!state.isTransparentBg) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "لون الخلفية:", color = MutedWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CherryAccent.copy(alpha = 0.2f))
                    .border(1.dp, CherryAccent, RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRemoveImage()
                        showCustomBgColorDialog = true
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "لون خلفية مخصص",
                    tint = CherryAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "مخصص",
                    color = CherryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LUXURY_BG_PRESETS.forEach { bg ->
                val isSelected = (state.solidBgColorLong == bg.argb || (bg.argb == 0xFF000000 && state.solidBgColorLong == null)) && state.backgroundImageUri == null
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CherryAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRemoveImage()
                            onSelectSolidBgColor(if (bg.argb == 0xFF000000) null else bg.argb)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(bg.argb.toInt()))
                            .border(0.5.dp, if (isSelected) AmoledBlack else PureWhite, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bg.name,
                        color = if (isSelected) PureWhite else MutedWhite,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    if (showCustomBgColorDialog) {
        CustomColorPickerDialog(
            initialColorLong = state.solidBgColorLong ?: 0xFF000000L,
            title = "منتقي لون خلفية الكانفاس",
            onColorSelected = { onSelectSolidBgColor(it) },
            onDismiss = { showCustomBgColorDialog = false }
        )
    }
}
