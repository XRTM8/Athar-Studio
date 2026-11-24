package app.xrtm8.athar.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    state: CanvasState,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExportFullCanvas: (ExportResolution) -> Unit,
    onExportSticker: (ExportResolution) -> Unit,
    onExportPdf: () -> Unit,
    onExportSvg: () -> Unit,
    onSaveProject: () -> Unit,
    onShareAtharProject: () -> Unit
) {
    var selectedResolution by remember { mutableStateOf(ExportResolution.HD_1080P) }
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current
    val primaryTextColor = if (isAmoledWhite) DarkText else PureWhite
    val secondaryTextColor = if (isAmoledWhite) DarkTextMuted else MutedWhite
    val subtleTextColor = if (isAmoledWhite) DarkTextSubtle else SubtleGray
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder

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
                .padding(horizontal = 22.dp, vertical = 6.dp)
                .navigationBarsPadding()
        ) {
            Text(text = "تصدير التصميم", color = primaryTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = "اختر دقة التصدير أو احفظ كمسودة قابلة للتعديل", color = subtleTextColor, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(14.dp))

            // Resolution Selector
            Text(text = "دقة التصدير:", color = secondaryTextColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExportResolution.entries.forEach { res ->
                    val isSelected = selectedResolution == res
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CherryAccent else cardBg)
                            .border(1.dp, if (isSelected) CherryAccent else cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedResolution = res
                            }
                            .padding(vertical = 9.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = res.label,
                                color = if (isSelected) PureWhite else primaryTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "${res.width}px",
                                color = if (isSelected) PureWhite.copy(alpha = 0.85f) else subtleTextColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Export Full Canvas Frame
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExportFullCanvas(selectedResolution)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CherryAccent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isExporting
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (state.isTransparentBg) "تصدير الكانفاس الشفاف (PNG)" else "تصدير التصميم بالكامل (${selectedResolution.label})",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "بالأبعاد الكاملة مع ضبط الخلفية والظلال الحالي",
                                color = PureWhite.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Export Transparent PNG Sticker (Trimmed tightly to text)
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExportSticker(selectedResolution)
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isExporting
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "ملصق شفاف مقصوص (Sticker PNG)", color = primaryTextColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "مفرغ بدون خلفية ومقصوص بدقة حول الخط", color = subtleTextColor, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Export Vector PDF (Print-Ready 300 DPI)
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExportPdf()
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isExporting
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "مستند فيكتور للطباعة (Vector PDF)", color = primaryTextColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "دقة متجهة صافية 100% جاهزة للمطابع دون أي بكسلة", color = subtleTextColor, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Export Vector SVG (Figma & Illustrator Compatible)
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExportSvg()
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isExporting
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Polyline, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "ملف فيكتور مفتوح (Vector SVG)", color = primaryTextColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "متوافق مع Figma و Illustrator مع نصوص قابلة للتعديل", color = subtleTextColor, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Save to Local Drafts
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSaveProject()
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "حفظ كمسودة في التطبيق", color = primaryTextColor, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Share Editable .athar Project
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShareAtharProject()
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "مشاركة ملف المشروع (.athar)", color = CherryAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(26.dp))
        }
    }
}
