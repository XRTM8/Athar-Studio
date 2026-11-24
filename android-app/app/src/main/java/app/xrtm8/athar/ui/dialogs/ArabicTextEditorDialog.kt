package app.xrtm8.athar.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
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
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(initialText.length)))
    }
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    text = "تحرير النص",
                    color = primaryTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Strip Tashkeel Button
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val stripped = stripArabicDiacritics(textFieldValue.text)
                            textFieldValue = TextFieldValue(text = stripped, selection = TextRange(stripped.length))
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = subtleTextColor)
                    ) {
                        Icon(Icons.Default.FormatClear, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("تجريد", fontSize = 11.sp)
                    }

                    // Clear Text Button
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            textFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = subtleTextColor)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("مسح", fontSize = 11.sp)
                    }

                    // Direct One-Tap Apply Button (visible right here without closing keyboard!)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onApplyText(textFieldValue.text)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyAccent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تطبيق", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Typographic Hero Input
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onApplyText(textFieldValue.text)
                        onDismiss()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BurgundyAccent,
                    unfocusedBorderColor = if (isAmoledWhite) WhiteBorder else GlassBorderActive,
                    focusedTextColor = primaryTextColor,
                    unfocusedTextColor = primaryTextColor,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    cursorColor = BurgundyAccent
                ),
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    textDirection = TextDirection.Rtl,
                    lineHeight = 32.sp
                ),
                placeholder = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("اكتب النص أو العبارة هنا...", color = SubtleGray, fontSize = 16.sp)
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Harakat, Quranic Marks & Islamic Glyphs Palette
            Text(
                text = "التشكيل المتقدم وعلامات المصحف والزخارف:",
                color = secondaryTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            ArabicDiacriticsPalette(
                onInsertGlyph = { insertChar ->
                    val currentText = textFieldValue.text
                    val sel = textFieldValue.selection
                    val start = sel.min.coerceIn(0, currentText.length)
                    val end = sel.max.coerceIn(0, currentText.length)
                    val newText = currentText.replaceRange(start, end, insertChar)
                    val newCursor = start + insertChar.length
                    textFieldValue = TextFieldValue(
                        text = newText,
                        selection = TextRange(newCursor)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ready Calligraphy Presets
            Text(
                text = "نصوص سريعة:",
                color = secondaryTextColor,
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
                            .background(cardBg)
                            .border(0.5.dp, cardBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                textFieldValue = TextFieldValue(text = suggestion, selection = TextRange(suggestion.length))
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = primaryTextColor,
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
                        onApplyText(textFieldValue.text)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("تطبيق النص", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("إلغاء", color = primaryTextColor, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}
