package app.xrtm8.athar.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersManagerBottomSheet(
    state: CanvasState,
    onDismiss: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onAddLayer: () -> Unit,
    onAddShape: ((app.xrtm8.athar.model.ShapeType) -> Unit)? = null,
    onDuplicateLayer: () -> Unit,
    onDeleteLayer: () -> Unit,
    onMoveLayerUp: () -> Unit,
    onMoveLayerDown: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onRenameLayer: (String, String) -> Unit
) {
    val isAmoledWhite = LocalAmoledWhite.current
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val secondaryText = if (isAmoledWhite) DarkTextMuted else MutedWhite
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder

    var renamingLayerId by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isAmoledWhite) AmoledWhite else AmoledBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(cardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مدير الطبقات (${state.layers.size})", color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddLayer,
                    colors = ButtonDefaults.buttonColors(containerColor = CherryAccent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("طبقة جديدة", color = PureWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                app.xrtm8.athar.model.ShapeType.entries.filter { it != app.xrtm8.athar.model.ShapeType.NONE }.forEach { st ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardBg)
                            .border(0.5.dp, cardBorder, RoundedCornerShape(8.dp))
                            .clickable { onAddShape?.invoke(st) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("+ ${st.labelAr}", color = CherryAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.layers.reversed(), key = { _, l -> l.id }) { revIdx, layer ->
                    val realIdx = state.layers.lastIndex - revIdx
                    val isActive = layer.id == state.activeLayer.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) CherryAccent.copy(alpha = 0.16f) else cardBg)
                            .border(if (isActive) 1.dp else 0.5.dp, if (isActive) CherryAccent else cardBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = layer.name.ifBlank { "نص ${realIdx + 1}" },
                                color = if (isActive) CherryAccent else primaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = layer.rawText.ifBlank { "(طبقة فارغة)" },
                                color = secondaryText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = layer.selectedFont.fontName,
                                color = secondaryText.copy(alpha = 0.7f),
                                fontSize = 9.5.sp
                            )
                        }

                        IconButton(onClick = { onToggleVisibility(layer.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "رؤية",
                                tint = if (layer.isVisible) CherryAccent else secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                renamingLayerId = layer.id
                                renameInput = layer.name
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "تسمية", tint = secondaryText, modifier = Modifier.size(16.dp))
                        }
                        if (isActive && state.layers.size > 1) {
                            if (realIdx < state.layers.lastIndex) {
                                IconButton(onClick = onMoveLayerUp, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "أعلى", tint = primaryText, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (realIdx > 0) {
                                IconButton(onClick = onMoveLayerDown, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "أسفل", tint = primaryText, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = onDuplicateLayer, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "تكرار", tint = CherryAccent, modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = onDeleteLayer, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (renamingLayerId != null) {
        AlertDialog(
            onDismissRequest = { renamingLayerId = null },
            containerColor = if (isAmoledWhite) AmoledWhite else AmoledBlack,
            title = { Text("إعادة تسمية الطبقة", color = primaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedBorderColor = CherryAccent,
                        unfocusedBorderColor = cardBorder
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = renamingLayerId
                    if (id != null && renameInput.isNotBlank()) {
                        onRenameLayer(id, renameInput.trim())
                    }
                    renamingLayerId = null
                }) { Text("حفظ", color = CherryAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { renamingLayerId = null }) { Text("إلغاء", color = secondaryText) }
            }
        )
    }
}
