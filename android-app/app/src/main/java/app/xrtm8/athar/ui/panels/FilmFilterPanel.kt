package app.xrtm8.athar.ui.panels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.AtharFavoritesManager
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.FilmPreset
import app.xrtm8.athar.ui.theme.*

/**
 * Professional Film Camera Presets & QR Code Sharing Panel.
 * Includes:
 * - Emulation of Fujifilm, Kodak, Leica, and Hollywood Cinematic color stocks
 * - Favorites management
 * - Polarr & Lightroom style QR Code card export and QR scanner import
 */
@Composable
fun FilmFilterPanel(
    state: CanvasState,
    onSelectFilmPreset: (FilmPreset) -> Unit,
    onExportQrCard: () -> Unit,
    onImportQrRecipe: (Bitmap) -> Unit,
    onToggleColorGrading: ((Boolean) -> Unit)? = null,
    onLiftChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onGammaChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onGainChange: ((r: Float, g: Float, b: Float) -> Unit)? = null,
    onResetColorGrading: (() -> Unit)? = null,
    onSliderChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var mainFilterSubTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var favoriteFilters by remember { mutableStateOf(AtharFavoritesManager.getFavoriteFilters(context)) }

    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        onImportQrRecipe(bmp)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val categories = listOf("الكل", "المفضلة", "كاميرات كلاسيكية", "أبيض وأسود", "سينمائي")

    val filteredPresets = FilmPreset.entries.filter { preset ->
        when (selectedCategory) {
            "الكل" -> true
            "المفضلة" -> favoriteFilters.contains(preset.id)
            "كاميرات كلاسيكية" -> preset.brand in listOf("Fujifilm", "Kodak")
            "أبيض وأسود" -> preset.id in listOf("kodak_tri_x", "leica_mono")
            "سينمائي" -> preset.id in listOf("teal_orange", "vintage_warmth")
            else -> true
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Sub-segment switcher: Film Presets vs 3-Way Color Wheels
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
                0 to "فلاتر الأفلام والوصفات",
                1 to "عجلات التدريج اللوني (3-Way)"
            ).forEach { (idx, title) ->
                val isSelected = mainFilterSubTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) GoldAccent else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            mainFilterSubTab = idx
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) AmoledBlack else MutedWhite,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (mainFilterSubTab == 1 && onToggleColorGrading != null && onLiftChange != null && onGammaChange != null && onGainChange != null && onResetColorGrading != null) {
            app.xrtm8.athar.ui.components.ColorGradingWheelsPanel(
                state = state,
                onToggleColorGrading = onToggleColorGrading,
                onLiftChange = onLiftChange,
                onGammaChange = onGammaChange,
                onGainChange = onGainChange,
                onResetColorGrading = onResetColorGrading,
                onSliderChangeFinished = onSliderChangeFinished
            )
        } else {
            // QR Code Sharing & Scanning Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExportQrCard()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderActive),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مشاركة كود QR",
                    color = PureWhite,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    qrPickerLauncher.launch("image/*")
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.weight(1f).height(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "مسح باركود فلتر",
                    color = PureWhite,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(9.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = cat
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) AmoledBlack else PureWhite,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Presets Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredPresets.forEach { preset ->
                val isSelected = state.filmPreset == preset
                val isFav = favoriteFilters.contains(preset.id)

                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) GoldAccent else GlassSurface)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) GoldAccent else GlassBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectFilmPreset(preset)
                        }
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.brand,
                                color = if (isSelected) AmoledBlack else SubtleGray,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (preset != FilmPreset.NONE) {
                                Box(
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        AtharFavoritesManager.toggleFilterFavorite(context, preset.id)
                                        favoriteFilters = AtharFavoritesManager.getFavoriteFilters(context)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "مفضلة",
                                        tint = if (isFav) Color(0xFFFF4081) else if (isSelected) AmoledBlack else SubtleGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = preset.titleAr,
                            color = if (isSelected) AmoledBlack else PureWhite,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = preset.description,
                            color = if (isSelected) AmoledBlack.copy(alpha = 0.8f) else SubtleGray,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
}
