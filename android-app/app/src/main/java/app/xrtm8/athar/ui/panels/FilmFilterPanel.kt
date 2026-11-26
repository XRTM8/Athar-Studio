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
import app.xrtm8.athar.model.ToneCurveState
import app.xrtm8.athar.ui.components.ColorGradingWheelsPanel
import app.xrtm8.athar.ui.components.StudioSlider
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
    toneCurveState: ToneCurveState = state.toneCurveState,
    onToneCurveChange: ((ToneCurveState) -> Unit)? = null,
    onResetToneCurve: (() -> Unit)? = null,
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
                1 to "عجلات التدريج",
                2 to "المنحنى والتقسيم"
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

        if (mainFilterSubTab == 2 && onToneCurveChange != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("منحنى الإضاءة (4-Point Tone Curve)", color = PureWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    if (onResetToneCurve != null && toneCurveState.isEnabled) {
                        Text(
                            "إعادة ضبط", color = CherryAccent, fontSize = 10.5.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onResetToneCurve() }.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                StudioSlider(title = "الأسود (Blacks)", value = toneCurveState.blacks, onValueChange = { onToneCurveChange(toneCurveState.copy(blacks = it)) }, onValueChangeFinished = onSliderChangeFinished, valueRange = -0.5f..0.5f, step = 0.02f, valueDisplay = "${(toneCurveState.blacks * 100).toInt()}%")
                StudioSlider(title = "الظلال (Shadows)", value = toneCurveState.shadows, onValueChange = { onToneCurveChange(toneCurveState.copy(shadows = it)) }, onValueChangeFinished = onSliderChangeFinished, valueRange = -0.5f..0.5f, step = 0.02f, valueDisplay = "${(toneCurveState.shadows * 100).toInt()}%")
                StudioSlider(title = "الإضاءات (Highlights)", value = toneCurveState.highlights, onValueChange = { onToneCurveChange(toneCurveState.copy(highlights = it)) }, onValueChangeFinished = onSliderChangeFinished, valueRange = -0.5f..0.5f, step = 0.02f, valueDisplay = "${(toneCurveState.highlights * 100).toInt()}%")
                StudioSlider(title = "الأبيض (Whites)", value = toneCurveState.whites, onValueChange = { onToneCurveChange(toneCurveState.copy(whites = it)) }, onValueChangeFinished = onSliderChangeFinished, valueRange = -0.5f..0.5f, step = 0.02f, valueDisplay = "${(toneCurveState.whites * 100).toInt()}%")

                Spacer(modifier = Modifier.height(8.dp))
                Text("تقسيم الدرجات (Split Toning)", color = PureWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                StudioSlider(title = "صبغة الظلال (Hue)", value = toneCurveState.splitShadowHue, onValueChange = { onToneCurveChange(toneCurveState.copy(splitShadowHue = it)) }, onValueChangeFinished = onSliderChangeFinished, valueRange = 0f..360f, step = 5f, valueDisplay = "${toneCurveState.splitShadowHue.toInt()}°")
                StudioSlider(title = "تشبع الظلال", value = toneCurveState.splitShadowSat, onValueChange = { onToneCurveChange(toneCurveState.copy(splitShadowSat = it)) }, onValueChangeFinished = onSliderChangeFinished, valueRange = 0f..0.6f, step = 0.02f, valueDisplay = "${(toneCurveState.splitShadowSat * 100).toInt()}%")
                StudioSlider(title = "صبغة الإضاءات (Hue)", value = toneCurveState.splitHighlightHue, onValueChange = { onToneCurveChange(toneCurveState.copy(splitHighlightHue = it)) }, onValueChangeFinished = onSliderChangeFinished, valueRange = 0f..360f, step = 5f, valueDisplay = "${toneCurveState.splitHighlightHue.toInt()}°")
                StudioSlider(title = "تشبع الإضاءات", value = toneCurveState.splitHighlightSat, onValueChange = { onToneCurveChange(toneCurveState.copy(splitHighlightSat = it)) }, onValueChangeFinished = onSliderChangeFinished, valueRange = 0f..0.6f, step = 0.02f, valueDisplay = "${(toneCurveState.splitHighlightSat * 100).toInt()}%")
            }
        } else if (mainFilterSubTab == 1 &&
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
