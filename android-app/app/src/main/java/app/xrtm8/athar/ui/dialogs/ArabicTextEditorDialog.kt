package app.xrtm8.athar.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.theme.*

/**
 * Strips Arabic diacritics (Harakat / Tashkeel) from string while keeping Tatweel and base letters intact.
 */
fun stripArabicDiacritics(text: String): String {
    return text.replace(Regex("[\u064B-\u065F\u0670]"), "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArabicTextEditorDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onApplyText: (String) -> Unit
) {
    var draftText by remember { mutableStateOf(initialText) }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AmoledBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(GlassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "محرر التايبوغرافي العربي",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Strip Tashkeel Button
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            draftText = stripArabicDiacritics(draftText)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = GoldAccent)
                    ) {
                        Icon(Icons.Default.FormatClear, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تجريد الحركات", fontSize = 11.5.sp)
                    }

                    // Clear Text Button
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            draftText = ""
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = SubtleGray)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح", fontSize = 11.5.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Typographic Hero Input
            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = GlassBorderActive,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = GlassSurface,
                    unfocusedContainerColor = GlassSurface,
                    cursorColor = GoldAccent
                ),
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                ),
                placeholder = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("اكتب النص أو العبارة هنا...", color = SubtleGray, fontSize = 16.sp)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Harakat & Islamic Glyphs Toolbar
            Text(
                text = "التشكيل والحركات والرموز الإسلامية:",
                color = MutedWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            val marks = listOf(
                "ـَ" to "َ",   // Fatha
                "ـُ" to "ُ",   // Damma
                "ـِ" to "ِ",   // Kasra
                "ـّ" to "ّ",   // Shadda
                "ـْ" to "ْ",   // Sukun
                "ـً" to "ً",   // Tanween Fath
                "ـٌ" to "ٌ",   // Tanween Damm
                "ـٍ" to "ٍ",   // Tanween Kasr
                "ـ" to "ـ",    // Tatweel / Kashida
                "ﷺ" to "ﷺ",
                "ﷻ" to "ﷻ",
                "﷽" to "﷽",
                "۞" to "۞",
                "۝" to "۝"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                marks.forEach { (displayGlyph, insertChar) ->
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurface)
                            .border(0.5.dp, GlassBorderActive, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                draftText += insertChar
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayGlyph,
                            color = GoldAccent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ready Calligraphy Presets
            Text(
                text = "اقتراحات وسياقات جاهزة:",
                color = MutedWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            val suggestions = listOf(
                "أَثَــر",
                "كُنْ ذَا أَثَـر",
                "وَيَبْقَى الأَثَـر",
                "أَثَـرٌ جَمِيل",
                "مَجْد",
                "سَلامٌ عَلَيْكُم",
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurface)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                draftText = suggestion
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons (Apply & Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (draftText.isNotBlank()) {
                            onApplyText(draftText)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("تطبيق وتحديث التصميم", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("إلغاء", color = PureWhite, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
