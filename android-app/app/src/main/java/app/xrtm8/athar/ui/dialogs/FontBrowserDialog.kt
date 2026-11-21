package app.xrtm8.athar.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.AtharFavoritesManager
import app.xrtm8.athar.model.CustomFont
import app.xrtm8.athar.ui.theme.*

/**
 * Deluxe Arabic Font Browser Modal inspired by Al-Musamim Al-Arabi.
 * Features:
 * - Live font search
 * - External font import (.ttf / .otf)
 * - Custom fonts section & management
 * - Category filters (All, Custom, Favorites, Classical, Kufic, Nastaliq, etc.)
 * - Heart toggle for favorites with atomic persistence
 * - Specimen cards with instant selection
 */
@Composable
fun FontBrowserDialog(
    selectedFont: ArabicFontFamily,
    customFontPath: String? = null,
    sampleText: String,
    customFonts: List<CustomFont> = emptyList(),
    onSelectFont: (ArabicFontFamily) -> Unit,
    onSelectCustomFont: (CustomFont) -> Unit = {},
    onImportFont: (Uri) -> Unit = {},
    onDeleteCustomFont: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportFont(uri)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var favoriteFonts by remember { mutableStateOf(AtharFavoritesManager.getFavoriteFonts(context)) }

    val categories = listOf("الكل", "خطوط مخصصة", "المفضلة", "كلاسيكي", "كوفي", "نستعليق", "عناوين", "حديث", "تعبيري", "قرآني")

    val filteredCustomFonts = customFonts.filter { cf ->
        cf.name.contains(searchQuery, ignoreCase = true)
    }

    val filteredFonts = ArabicFontFamily.entries.filter { font ->
        val matchesSearch = font.fontName.contains(searchQuery, ignoreCase = true) ||
                font.category.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (selectedCategory) {
            "الكل" -> true
            "المفضلة" -> favoriteFonts.contains(font.name)
            "خطوط مخصصة" -> false
            else -> font.category == selectedCategory
        }

        matchesSearch && matchesCategory
    }

    val isAmoledWhite = LocalAmoledWhite.current
    val primaryTextColor = if (isAmoledWhite) DarkText else PureWhite
    val subtleTextColor = if (isAmoledWhite) DarkTextSubtle else SubtleGray
    val dialogBg = if (isAmoledWhite) AmoledWhite else GlassBackground
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background((if (isAmoledWhite) AmoledWhite else AmoledBlack).copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(dialogBg)
                    .border(1.dp, if (isAmoledWhite) WhiteBorder else GlassBorderActive, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مكتبة الخطوط العربية",
                            color = CherryAccent,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اختر الخط المناسب لتصميمك",
                            color = subtleTextColor,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = primaryTextColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن اسم الخط أو النمط...", color = subtleTextColor, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CherryAccent) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = CherryAccent,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = primaryTextColor,
                        unfocusedTextColor = primaryTextColor
                    ),
                    singleLine = true
                )

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
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CherryAccent else cardBg)
                                .border(0.5.dp, if (isSelected) CherryAccent else cardBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = cat
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) PureWhite else primaryTextColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Import Font Button
                OutlinedButton(
                    onClick = {
                        fontPickerLauncher.launch(
                            arrayOf(
                                "font/*",
                                "application/x-font-ttf",
                                "application/x-font-opentype",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CherryAccent.copy(alpha = if (isAmoledWhite) 0.08f else 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CherryAccent)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "استيراد خط خارجي (.ttf / .otf)",
                        color = CherryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fonts List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Custom fonts section
                    if (selectedCategory == "خطوط مخصصة" || (selectedCategory == "الكل" && filteredCustomFonts.isNotEmpty())) {
                        items(filteredCustomFonts, key = { "custom_${it.id}" }) { cf ->
                            val isSelected = customFontPath == cf.filePath
                            CustomFontItemRow(
                                customFont = cf,
                                isSelected = isSelected,
                                sampleText = sampleText,
                                isAmoledWhite = isAmoledWhite,
                                onSelect = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectCustomFont(cf)
                                    onDismiss()
                                },
                                onDelete = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDeleteCustomFont(cf.id)
                                }
                            )
                        }
                    }

                    // Built-in fonts
                    if (selectedCategory != "خطوط مخصصة") {
                        items(filteredFonts, key = { it.name }) { font ->
                            val isSelected = selectedFont == font && customFontPath == null
                            val isFav = favoriteFonts.contains(font.name)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) CherryAccent.copy(alpha = if (isAmoledWhite) 0.15f else 0.16f) else cardBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) CherryAccent else cardBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectFont(font)
                                    onDismiss()
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = font.fontName,
                                            color = if (isSelected) CherryAccent else primaryTextColor,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isAmoledWhite) WhiteSurfaceElevated else AmoledBlack)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = font.category,
                                                color = subtleTextColor,
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = sampleText.take(24).ifBlank { font.specimen },
                                        color = if (isSelected) PureWhite else MutedWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }

                                // Favorite Heart Toggle Button
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        AtharFavoritesManager.toggleFontFavorite(context, font.name)
                                        favoriteFonts = AtharFavoritesManager.getFavoriteFonts(context)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "مفضلة",
                                        tint = if (isFav) Color(0xFFFF4081) else SubtleGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
