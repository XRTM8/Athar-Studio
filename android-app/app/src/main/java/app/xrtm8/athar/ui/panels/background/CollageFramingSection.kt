package app.xrtm8.athar.ui.panels.background

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*
import java.util.Locale

/**
 * Precision framing and pan controls for active collage cell (< 140 lines).
 * Allows sliding the photo up/down, panning left/right, and zooming to frame the subject.
 */
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
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val hasPhoto = cell?.imageUri != null

    AnimatedVisibility(
        visible = cell != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Cell title + quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = null,
                        tint = CherryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تأطير صورة الخلية ${cellIndex + 1}",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasPhoto) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "استبدال",
                            color = MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioSurface)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPickPhoto()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "توسيط",
                            color = MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioSurface)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onResetFraming()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "حذف",
                            color = CherryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioSurface)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRemovePhoto()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (hasPhoto && cell != null) {
                // Vertical Pan Slider (صعود ونزول الصورة)
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

                // Horizontal Pan Slider (يمين ويسار)
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

                // Scale / Zoom Slider (تكبير الصورة داخل الخلية)
                StudioSlider(
                    title = "تكبير وتأطير الصورة (Zoom)",
                    value = cell.photoScale,
                    onValueChange = onScaleChange,
                    valueRange = 1.0f..3.0f,
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
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "اختر صورة لهذه الخلية من المعرض", fontSize = 11.5.sp, color = PureWhite)
                }
            }
        }
    }
}
