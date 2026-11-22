package app.xrtm8.athar.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.xrtm8.athar.ui.theme.*

/**
 * Confirmation dialog for project deletion to avoid accidental data loss.
 */
@Composable
fun DeleteProjectConfirmDialog(
    projectTitle: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current

    val dialogBg = if (isAmoledWhite) AmoledWhite else GlassBackground
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val subtleText = if (isAmoledWhite) DarkTextSubtle else SubtleGray

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background((if (isAmoledWhite) AmoledWhite else AmoledBlack).copy(alpha = 0.85f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(dialogBg)
                    .border(1.dp, if (isAmoledWhite) WhiteBorder else GlassBorderActive, RoundedCornerShape(22.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CherryAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = CherryAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "تأكيد حذف المشروع",
                    color = primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "هل أنت متأكد من حذف \"$projectTitle\"؟ لا يمكن التراجع عن هذا الإجراء.",
                    color = subtleText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", color = primaryText)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirmDelete()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CherryAccent)
                    ) {
                        Text("حذف نهائي", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog to rename an existing draft/project.
 */
@Composable
fun RenameProjectDialog(
    initialTitle: String,
    onConfirmRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current

    var newTitle by remember { mutableStateOf(initialTitle) }

    val dialogBg = if (isAmoledWhite) AmoledWhite else GlassBackground
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val subtleText = if (isAmoledWhite) DarkTextSubtle else SubtleGray

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background((if (isAmoledWhite) AmoledWhite else AmoledBlack).copy(alpha = 0.85f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(dialogBg)
                    .border(1.dp, if (isAmoledWhite) WhiteBorder else GlassBorderActive, RoundedCornerShape(22.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CherryAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = CherryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "إعادة تسمية المشروع",
                    color = primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.take(40) },
                    placeholder = { Text("اسم المشروع الجديد...", color = subtleText, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = CherryAccent,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", color = primaryText)
                    }

                    Button(
                        onClick = {
                            val trimmed = newTitle.trim()
                            if (trimmed.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onConfirmRename(trimmed)
                                onDismiss()
                            }
                        },
                        enabled = newTitle.trim().isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CherryAccent)
                    ) {
                        Text("حفظ التسمية", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
