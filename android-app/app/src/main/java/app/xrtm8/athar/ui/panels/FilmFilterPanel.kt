package app.xrtm8.athar.ui.panels

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.xrtm8.athar.model.FilmPreset
import app.xrtm8.athar.model.OpticalLensState
import app.xrtm8.athar.ui.components.ColorGradingWheelsPanel
import app.xrtm8.athar.ui.panels.background.FilmLensStripSection
import app.xrtm8.athar.ui.panels.background.FilmPresetSwatchStrip
import app.xrtm8.athar.ui.theme.*

/**
 * Film Camera Presets, Optical Lens Attachments & 3-Way Color Grading Coordinator Panel (< 120 lines).
 */
@Composable
fun FilmFilterPanel(
    state: CanvasState,
    onSelectFilmPreset: (FilmPreset) -> Unit,
    onToggleColorGrading: ((Boolean) -> Unit)? = null,
    onLiftChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onGammaChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onGainChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onResetColorGrading: (() -> Unit)? = null,
    onSliderChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    filmPresetIntensity: Float = state.filmPresetIntensity,
    onFilmPresetIntensityChange: (Float) -> Unit = {},
    opticalLensState: OpticalLensState = state.opticalLensState,
    onOpticalLensChange: (OpticalLensState) -> Unit = {},
    previewThumbnailBitmap: Bitmap? = null
) {
    val haptic = LocalHapticFeedback.current
    var mainFilterSubTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Sub-segment switcher: Film Cameras & Lenses vs 3-Way Color Wheels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                0 to "الكاميرات والعدسات",
                1 to "عجلات التدريج (3-Way)"
            ).forEach { (idx, title) ->
                val isSelected = mainFilterSubTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) BurgundyAccent else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            mainFilterSubTab = idx
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) PureWhite else MutedWhite,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (mainFilterSubTab == 1 &&
            onToggleColorGrading != null &&
            onLiftChange != null &&
            onGammaChange != null &&
            onGainChange != null &&
            onResetColorGrading != null
        ) {
            ColorGradingWheelsPanel(
                state = state,
                onToggleColorGrading = onToggleColorGrading,
                onLiftChange = onLiftChange,
                onGammaChange = onGammaChange,
                onGainChange = onGainChange,
                onResetColorGrading = onResetColorGrading,
                onSliderChangeFinished = onSliderChangeFinished
            )
        } else {
            FilmPresetSwatchStrip(
                selectedPreset = state.filmPreset,
                filmPresetIntensity = filmPresetIntensity,
                previewThumbnailBitmap = previewThumbnailBitmap,
                onSelectFilmPreset = onSelectFilmPreset,
                onFilmPresetIntensityChange = onFilmPresetIntensityChange,
                onSliderChangeFinished = onSliderChangeFinished
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilmLensStripSection(
                opticalLensState = opticalLensState,
                onOpticalLensChange = onOpticalLensChange,
                onSliderChangeFinished = onSliderChangeFinished
            )
        }
    }
}
