package app.xrtm8.athar.ui.panels.background

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.CollageLayoutType
import app.xrtm8.athar.model.CollageState
import app.xrtm8.athar.ui.components.StudioSlider
import app.xrtm8.athar.ui.theme.*

/**
 * Editorial Photo Collage Panel (< 230 lines).
 */
@Composable
fun CollagePanel(
    state: CollageState,
    onLayoutChange: (CollageLayoutType) -> Unit,
    onSelectCell: (String) -> Unit,
    onPickCellPhoto: (String) -> Unit,
    onSpacingChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onFrameColorChange: (Long) -> Unit,
    onResetCollage: () -> Unit,
    onCellOffsetYChange: (String, Float) -> Unit = { _, _ -> },
    onCellOffsetXChange: (String, Float) -> Unit = { _, _ -> },
    onCellScaleChange: (String, Float) -> Unit = { _, _ -> },
    onResetCellFraming: (String) -> Unit = {},
    onRemoveCellPhoto: (String) -> Unit = {},
    isFramingMode: Boolean = false,
    onToggleFramingMode: ((Boolean) -> Unit)? = null,
    onRotateCell90: ((String) -> Unit)? = null,
    onToggleCellFlipH: ((String) -> Unit)? = null,
    onToggleCellFlipV: ((String) -> Unit)? = null,
    onSwapCellWithNext: ((String) -> Unit)? = null,
    onBatchPickPhotos: ((List<Uri>) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onSliderChangeFinished: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val activeCell = state.cells.find { it.id == state.activeCellId } ?: state.cells.firstOrNull()
    val activeIndex = state.cells.indexOfFirst { it.id == (activeCell?.id ?: "") }.coerceAtLeast(0)
    val multiPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onBatchPickPhotos?.invoke(uris)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Layout Templates Row
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قوالب الكولاج التحريرية",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (state.isEnabled) {
                    Text(
                        text = "إلغاء الكولاج",
                        color = CherryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onResetCollage()
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CollageLayoutType.values().forEach { layout ->
                    val isSelected = state.layout == layout
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BurgundyAccent else StudioSurface)
                            .border(0.5.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onLayoutChange(layout)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = layout.label,
                            color = if (isSelected) PureWhite else MutedWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (state.isEnabled) {
            // Cells management list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "الصور في الكولاج (اضغط للتأطير أو الاستبدال)", color = SubtleGray, fontSize = 11.sp)
                    if (onBatchPickPhotos != null) {
                        Text(
                            text = "تعبئة سريعة +",
                            color = CherryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CherryAccent.copy(alpha = 0.14f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    multiPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.cells.forEachIndexed { idx, cell ->
                        val isSelected = (activeCell?.id ?: state.activeCellId) == cell.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
                                .border(1.dp, if (isSelected) CherryAccent else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectCell(cell.id)
                                    if (cell.imageUri == null) {
                                        onPickCellPhoto(cell.id)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = if (cell.imageUri != null) CherryAccent else SubtleGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (cell.imageUri != null) "خلية ${idx + 1} ✓" else "خلية ${idx + 1} +",
                                    color = if (isSelected) PureWhite else MutedWhite,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Cell Framing Controls (صعود/نزول، يمين/يسار، زوم)
            if (activeCell != null) {
                CollageFramingSection(
                    cell = activeCell,
                    cellIndex = activeIndex,
                    onOffsetYChange = { onCellOffsetYChange(activeCell.id, it) },
                    onOffsetXChange = { onCellOffsetXChange(activeCell.id, it) },
                    onScaleChange = { onCellScaleChange(activeCell.id, it) },
                    onResetFraming = { onResetCellFraming(activeCell.id) },
                    onPickPhoto = { onPickCellPhoto(activeCell.id) },
                    onRemovePhoto = { onRemoveCellPhoto(activeCell.id) },
                    isFramingMode = isFramingMode,
                    onToggleFramingMode = onToggleFramingMode,
                    onRotate90 = onRotateCell90?.let { fn -> { fn(activeCell.id) } },
                    onToggleFlipH = onToggleCellFlipH?.let { fn -> { fn(activeCell.id) } },
                    onToggleFlipV = onToggleCellFlipV?.let { fn -> { fn(activeCell.id) } },
                    onSwapWithNext = onSwapCellWithNext?.let { fn -> { fn(activeCell.id) } },
                    onSliderChangeFinished = onSliderChangeFinished
                )
            }

            // Spacing & Corner Radius Sliders
            StudioSlider(
                title = "الفواصل بين الصور",
                value = state.spacingDp,
                valueRange = 0f..24f,
                valueDisplay = "${state.spacingDp.toInt()} dp",
                onValueChange = onSpacingChange,
                onValueChangeFinished = onSliderChangeFinished
            )

            StudioSlider(
                title = "استدارة الحواف (Corner Radius)",
                value = state.cornerRadiusDp,
                valueRange = 0f..32f,
                valueDisplay = "${state.cornerRadiusDp.toInt()} dp",
                onValueChange = onCornerRadiusChange,
                onValueChangeFinished = onSliderChangeFinished
            )

            // Frame background color palette
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "لون الإطار الخارجي", color = SubtleGray, fontSize = 11.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        0xFF0D0F14L to "أسود سينمائي",
                        0xFFF5F3EFL to "بيج كينفولك",
                        0xFF2A080FL to "عنابي",
                        0xFFFFFFFFL to "أبيض"
                    ).forEach { (colorLong, _) ->
                        val isSelected = state.frameColorLong == colorLong
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(colorLong))
                                .border(if (isSelected) 2.dp else 0.5.dp, if (isSelected) PureWhite else GlassBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onFrameColorChange(colorLong)
                                }
                        )
                    }
                }
            }
        }
    }
}
