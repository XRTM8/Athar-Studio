package app.xrtm8.athar.ui.panels.background

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CollageCell
import app.xrtm8.athar.model.CollageState
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*
import java.util.Locale

@Composable
private fun CollageActionChip(label: String, isAccent: Boolean = false, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (isAccent) CherryAccent else MutedWhite,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(StudioSurface)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun CollageFramingSection(
    cell: CollageCell?,
    cellIndex: Int,
    onOffsetYChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onResetFraming: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    isFramingMode: Boolean = false,
    onToggleFramingMode: ((Boolean) -> Unit)? = null,
    onRotate90: (() -> Unit)? = null,
    onToggleFlipH: (() -> Unit)? = null,
    onToggleFlipV: (() -> Unit)? = null,
    onSwapWithNext: (() -> Unit)? = null,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val hasPhoto = cell?.imageUri != null

    AnimatedVisibility(visible = cell != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CropFree, null, tint = CherryAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأطير صورة الخلية ${cellIndex + 1}", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (hasPhoto) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (onSwapWithNext != null) {
                            CollageActionChip("تبديل") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSwapWithNext() }
                        }
                        if (onRotate90 != null) {
                            CollageActionChip("تدوير") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onRotate90() }
                        }
                        if (onToggleFlipH != null) {
                            CollageActionChip("عكس أفقي") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleFlipH() }
                        }
                        if (onToggleFlipV != null) {
                            CollageActionChip("عكس رأسي") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleFlipV() }
                        }
                        CollageActionChip("استبدال") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPickPhoto() }
                        CollageActionChip("توسيط") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onResetFraming() }
                        CollageActionChip("حذف", isAccent = true) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRemovePhoto() }
                    }
                }
            }

            if (hasPhoto && cell != null) {
                if (onToggleFramingMode != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFramingMode) "وضع اللمس المباشر (سحب وتكبير) ✓" else "تفعيل السحب المباشر باللمس",
                            color = if (isFramingMode) CherryAccent else MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isFramingMode) FontWeight.Bold else FontWeight.Normal
                        )
                        Switch(
                            checked = isFramingMode,
                            onCheckedChange = onToggleFramingMode,
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = BurgundyAccent)
                        )
                    }
                }

                StudioSlider(
                    title = "تحريك رأسي (صعود ونزول الصورة)",
                    value = cell.photoOffsetY,
                    onValueChange = onOffsetYChange,
                    valueRange = -1.0f..1.0f,
                    step = 0.02f,
                    valueDisplay = String.format(Locale.US, "%+.0f%%", cell.photoOffsetY * 100),
                    icon = Icons.Default.SwapVert,
                    onValueChangeFinished = onSliderChangeFinished
                )
                StudioSlider(
                    title = "تحريك أفقي (يمين ويسار)",
                    value = cell.photoOffsetX,
                    onValueChange = onOffsetXChange,
                    valueRange = -1.0f..1.0f,
                    step = 0.02f,
                    valueDisplay = String.format(Locale.US, "%+.0f%%", cell.photoOffsetX * 100),
                    icon = Icons.Default.SwapHoriz,
                    onValueChangeFinished = onSliderChangeFinished
                )
                StudioSlider(
                    title = "تكبير وتأطير الصورة (Zoom)",
                    value = cell.photoScale.coerceIn(CollageState.MIN_CELL_SCALE, CollageState.MAX_CELL_SCALE),
                    onValueChange = onScaleChange,
                    valueRange = CollageState.MIN_CELL_SCALE..CollageState.MAX_CELL_SCALE,
                    step = 0.05f,
                    valueDisplay = String.format(Locale.US, "%.2fx", cell.photoScale),
                    icon = Icons.Default.ZoomIn,
                    onValueChangeFinished = onSliderChangeFinished
                )
            } else {
                Button(
                    onClick = onPickPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختر صورة لهذه الخلية من المعرض", fontSize = 11.5.sp, color = PureWhite)
                }
            }
        }
    }
}

@Composable
fun CollageCanvasActionPills(
    isComparingBefore: Boolean,
    isFramingMode: Boolean,
    tappedCellId: String?,
    hasCollageEnabled: Boolean,
    onEnterFraming: () -> Unit,
    onExitFraming: () -> Unit,
    onPickPhoto: ((String) -> Unit)?,
    onDismissPill: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AnimatedVisibility(visible = isComparingBefore, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Surface(shape = CircleShape, color = AmoledBlack.copy(alpha = 0.88f), border = BorderStroke(1.dp, BurgundyAccent.copy(alpha = 0.8f))) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BurgundyAccent))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الصورة الأصلية (قبل)", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        AnimatedVisibility(visible = !isComparingBefore && isFramingMode, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Surface(shape = CircleShape, color = AmoledBlack.copy(alpha = 0.88f), border = BorderStroke(1.dp, CherryAccent.copy(alpha = 0.8f))) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text("اسحب لتحريك الصورة • نقرتان للتوسيط", color = MutedWhite, fontSize = 10.5.sp)
                    Text("تم", color = CherryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onExitFraming))
                }
            }
        }
        AnimatedVisibility(visible = !isComparingBefore && !isFramingMode && hasCollageEnabled && tappedCellId != null, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Surface(shape = CircleShape, color = AmoledBlack.copy(alpha = 0.88f), border = BorderStroke(1.dp, GlassBorder)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text("تأطير", color = CherryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onEnterFraming))
                    if (onPickPhoto != null && tappedCellId != null) {
                        Text("تغيير الصورة", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onPickPhoto(tappedCellId) })
                    }
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = SubtleGray, modifier = Modifier.size(14.dp).clickable(onClick = onDismissPill))
                }
            }
        }
    }
}
