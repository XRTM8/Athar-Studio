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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Colorize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CustomFont
import app.xrtm8.athar.model.TextSpanStyle
import app.xrtm8.athar.typography.AtharFontManager
import app.xrtm8.athar.typography.RichTextSpanMapper
import app.xrtm8.athar.ui.components.StudioSlider
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
    initialSpans: List<TextSpanStyle> = emptyList(),
    customFonts: List<CustomFont> = emptyList(),
    onDismiss: () -> Unit,
    onApplyText: (String) -> Unit,
    onApplyTextWithSpans: ((String, List<TextSpanStyle>) -> Unit)? = null
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(initialText.length)))
    }
    var spans by remember { mutableStateOf(initialSpans) }
    var activeSpanTab by remember { mutableStateOf(0) } // 0 = Font, 1 = Highlight, 2 = Text Color
    var colorPickerTarget by remember { mutableStateOf<String?>(null) } // "highlight" | "border" | "text"
    val context = LocalContext.current
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
                    .verticalScroll(rememberScrollState())
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
                            spans = RichTextSpanMapper.adjustSpansOnTextEdit(textFieldValue.text, stripped, spans)
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
                            spans = emptyList()
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
                            if (onApplyTextWithSpans != null) {
                                onApplyTextWithSpans(textFieldValue.text, spans)
                            } else {
                                onApplyText(textFieldValue.text)
                            }
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
                onValueChange = { next ->
                    if (next.text != textFieldValue.text) {
                        spans = RichTextSpanMapper.adjustSpansOnTextEdit(textFieldValue.text, next.text, spans)
                    }
                    textFieldValue = next
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (onApplyTextWithSpans != null) {
                            onApplyTextWithSpans(textFieldValue.text, spans)
                        } else {
                            onApplyText(textFieldValue.text)
                        }
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

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Word Selection & Rich Text Span Formatting Section
            val curText = textFieldValue.text
            val selMin = textFieldValue.selection.min.coerceIn(0, curText.length)
            val selMax = textFieldValue.selection.max.coerceIn(selMin, curText.length)
            val hasSelection = selMax > selMin

            if (curText.isNotBlank()) {
                // Live multi-word Rich Text Preview
                if (spans.any { it.hasAnyStyle }) {
                    val previewAnnotated = remember(curText, spans) {
                        buildAnnotatedString {
                            append(curText)
                            spans.forEach { sp ->
                                val s = sp.start.coerceIn(0, curText.length)
                                val e = sp.end.coerceIn(s, curText.length)
                                if (e > s) {
                                    addStyle(
                                        SpanStyle(
                                            color = sp.textColorLong?.let { Color(it) } ?: primaryTextColor,
                                            background = sp.highlightColorLong?.let { Color(it).copy(alpha = sp.highlightAlpha) } ?: Color.Unspecified,
                                            fontFamily = when {
                                                !sp.customFontPath.isNullOrBlank() -> customFonts.find { it.filePath == sp.customFontPath }?.let {
                                                    AtharFontManager.getComposeFontFamily(context, it, it.weightClass)
                                                }
                                                sp.fontFamily != null -> AtharFontManager.getComposeFontFamily(context, sp.fontFamily)
                                                else -> null
                                            },
                                            fontWeight = if (sp.isBold == true) FontWeight.Bold else null,
                                            fontStyle = if (sp.isItalic == true) FontStyle.Italic else null
                                        ),
                                        s, e
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .border(0.5.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = previewAnnotated,
                            color = primaryTextColor,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasSelection) "تخصيص التحديد (${curText.substring(selMin, selMax).take(16)})" else "اضغط كلمة أو ظلّل جزءاً لتخصيصه:",
                        color = if (hasSelection) CherryAccent else secondaryTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (hasSelection && selMax < curText.length) {
                            Text(
                                text = "+ التالية",
                                color = CherryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CherryAccent.copy(alpha = 0.14f))
                                    .clickable {
                                        var nextEnd = selMax
                                        while (nextEnd < curText.length && curText[nextEnd].isWhitespace()) nextEnd++
                                        while (nextEnd < curText.length && !curText[nextEnd].isWhitespace()) nextEnd++
                                        textFieldValue = textFieldValue.copy(selection = TextRange(selMin, nextEnd))
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (spans.isNotEmpty()) {
                            Text(
                                text = "مسح التنسيقات",
                                color = subtleTextColor,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { spans = emptyList() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Quick word selector chips with visual highlight indicator dot
                val wordRanges = remember(curText) {
                    Regex("\\S+").findAll(curText).map { it.range.first..(it.range.last + 1) }.toList()
                }
                if (wordRanges.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        wordRanges.forEach { range ->
                            val isSelected = selMin <= range.first && selMax >= range.last
                            val wordText = curText.substring(range.first, range.last)
                            val matchingSpan = spans.find { it.start < range.last && it.end > range.first }
                            val hasSpan = matchingSpan != null
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CherryAccent.copy(alpha = 0.25f) else cardBg)
                                    .border(
                                        0.75.dp,
                                        when {
                                            isSelected -> CherryAccent
                                            hasSpan -> BurgundyAccent
                                            else -> cardBorder
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        textFieldValue = textFieldValue.copy(selection = TextRange(range.first, range.last))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                if (matchingSpan?.highlightColorLong != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(matchingSpan.highlightColorLong))
                                    )
                                }
                                Text(
                                    text = wordText,
                                    color = primaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected || hasSpan) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                if (hasSelection) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val targetRange = RichTextSpanMapper.expandToWordBounds(curText, selMin, selMax)
                    val activeSpan = spans.find { it.start == targetRange.first && it.end == targetRange.last }
                        ?: TextSpanStyle(start = targetRange.first, end = targetRange.last)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(0.5.dp, cardBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        // Sub-tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("الخط والحجم", "خلفية الكلمة", "لون النص").forEachIndexed { idx, title ->
                                val selected = activeSpanTab == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) BurgundyAccent else Color.Transparent)
                                        .border(0.5.dp, if (selected) BurgundyAccent else cardBorder, RoundedCornerShape(8.dp))
                                        .clickable { activeSpanTab = idx }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        color = if (selected) PureWhite else secondaryTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (activeSpanTab) {
                            0 -> {
                                // Per-word Font Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isDefaultFont = activeSpan.fontFamily == null && activeSpan.customFontPath.isNullOrBlank()
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDefaultFont) CherryAccent.copy(alpha = 0.2f) else Color.Transparent)
                                            .border(0.5.dp, if (isDefaultFont) CherryAccent else cardBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                spans = RichTextSpanMapper.upsertSpan(
                                                    spans,
                                                    activeSpan.copy(fontFamily = null, customFontPath = null, customFontName = null)
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("افتراضي", color = primaryTextColor, fontSize = 11.sp)
                                    }
                                    customFonts.forEach { cf ->
                                        val isSel = activeSpan.customFontPath == cf.filePath
                                        val cfFamily = remember(cf.filePath) {
                                            AtharFontManager.getComposeFontFamily(context, cf, cf.weightClass)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) CherryAccent.copy(alpha = 0.25f) else Color.Transparent)
                                                .border(0.5.dp, if (isSel) CherryAccent else cardBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    spans = RichTextSpanMapper.upsertSpan(
                                                        spans,
                                                        activeSpan.copy(fontFamily = null, customFontPath = cf.filePath, customFontName = cf.name)
                                                    )
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(cf.name, fontFamily = cfFamily, color = primaryTextColor, fontSize = 11.sp)
                                        }
                                    }
                                    ArabicFontFamily.values().forEach { font ->
                                        val isSel = activeSpan.fontFamily == font && activeSpan.customFontPath.isNullOrBlank()
                                        val composeFont = remember(font) {
                                            AtharFontManager.getComposeFontFamily(context, font, font.defaultWeight)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) CherryAccent.copy(alpha = 0.25f) else Color.Transparent)
                                                .border(0.5.dp, if (isSel) CherryAccent else cardBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    spans = RichTextSpanMapper.upsertSpan(
                                                        spans,
                                                        activeSpan.copy(fontFamily = font, customFontPath = null, customFontName = null)
                                                    )
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(font.fontName, fontFamily = composeFont, color = primaryTextColor, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val boldOn = activeSpan.isBold == true
                                    val italicOn = activeSpan.isItalic == true
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (boldOn) CherryAccent.copy(alpha = 0.25f) else Color.Transparent)
                                            .border(0.5.dp, if (boldOn) CherryAccent else cardBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                spans = RichTextSpanMapper.upsertSpan(
                                                    spans,
                                                    activeSpan.copy(isBold = if (boldOn) null else true)
                                                )
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) { Text("عريض B", color = primaryTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (italicOn) CherryAccent.copy(alpha = 0.25f) else Color.Transparent)
                                            .border(0.5.dp, if (italicOn) CherryAccent else cardBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                spans = RichTextSpanMapper.upsertSpan(
                                                    spans,
                                                    activeSpan.copy(isItalic = if (italicOn) null else true)
                                                )
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) { Text("مائل I", color = primaryTextColor, fontSize = 11.sp, fontStyle = FontStyle.Italic) }
                                }
                                StudioSlider(
                                    title = "الحجم النسبي",
                                    value = activeSpan.scaleMultiplier,
                                    onValueChange = {
                                        spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(scaleMultiplier = it))
                                    },
                                    valueRange = 0.6f..1.8f,
                                    step = 0.05f,
                                    valueDisplay = "${(activeSpan.scaleMultiplier * 100).toInt()}%"
                                )
                                StudioSlider(
                                    title = "إزاحة السطر",
                                    value = activeSpan.baselineShift,
                                    onValueChange = {
                                        spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(baselineShift = it))
                                    },
                                    valueRange = -0.25f..0.25f,
                                    step = 0.02f,
                                    valueDisplay = "${(activeSpan.baselineShift * 100).toInt()}%"
                                )
                            }

                            1 -> {
                                // Highlight Background Controls
                                val palette = listOf(
                                    0xFFE5C07BL, 0xFF9E1B32L, 0xFF1E3A5FL, 0xFF1F6F54L,
                                    0xFFF5EFE6L, 0xFF141414L, 0xFFD97706L, 0xFF7C3AEDL
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(0.5.dp, cardBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(highlightColorLong = null))
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) { Text("بدون", color = secondaryTextColor, fontSize = 10.sp) }

                                    palette.forEach { cLong ->
                                        val isSel = activeSpan.highlightColorLong == cLong
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color(cLong))
                                                .border(if (isSel) 2.dp else 0.75.dp, if (isSel) PureWhite else cardBorder, CircleShape)
                                                .clickable {
                                                    spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(highlightColorLong = cLong))
                                                }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(cardBg)
                                            .border(0.75.dp, CherryAccent, CircleShape)
                                            .clickable { colorPickerTarget = "highlight" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Colorize, contentDescription = "لون مخصص", tint = CherryAccent, modifier = Modifier.size(14.dp))
                                    }
                                }
                                if (activeSpan.highlightColorLong != null) {
                                    StudioSlider(
                                        title = "شفافية الخلفية",
                                        value = activeSpan.highlightAlpha,
                                        onValueChange = { spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(highlightAlpha = it)) },
                                        valueRange = 0.1f..1.0f,
                                        step = 0.05f,
                                        valueDisplay = "${(activeSpan.highlightAlpha * 100).toInt()}%"
                                    )
                                    StudioSlider(
                                        title = "استدارة الزوايا",
                                        value = activeSpan.highlightCornerRadius,
                                        onValueChange = { spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(highlightCornerRadius = it)) },
                                        valueRange = 0f..28f,
                                        step = 1f,
                                        valueDisplay = "${activeSpan.highlightCornerRadius.toInt()}"
                                    )
                                    StudioSlider(
                                        title = "الهامش الأفقي",
                                        value = activeSpan.highlightPaddingH,
                                        onValueChange = { spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(highlightPaddingH = it)) },
                                        valueRange = 0f..20f,
                                        step = 1f,
                                        valueDisplay = "${activeSpan.highlightPaddingH.toInt()}"
                                    )
                                    StudioSlider(
                                        title = "الهامش العمودي",
                                        value = activeSpan.highlightPaddingV,
                                        onValueChange = { spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(highlightPaddingV = it)) },
                                        valueRange = 0f..14f,
                                        step = 1f,
                                        valueDisplay = "${activeSpan.highlightPaddingV.toInt()}"
                                    )
                                    StudioSlider(
                                        title = "سمك الإطار",
                                        value = activeSpan.highlightBorderWidth,
                                        onValueChange = {
                                            val borderColor = activeSpan.highlightBorderColorLong ?: 0xFFFFFFFFL
                                            spans = RichTextSpanMapper.upsertSpan(
                                                spans,
                                                activeSpan.copy(highlightBorderWidth = it, highlightBorderColorLong = borderColor)
                                            )
                                        },
                                        valueRange = 0f..4f,
                                        step = 0.5f,
                                        valueDisplay = "${activeSpan.highlightBorderWidth}"
                                    )
                                    if (activeSpan.highlightBorderWidth > 0f) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("لون الإطار:", color = secondaryTextColor, fontSize = 10.sp)
                                            listOf(0xFFFFFFFFL, 0xFFE5C07BL, 0xFF9E1B32L, 0xFF0A0A0AL).forEach { bLong ->
                                                val isSel = activeSpan.highlightBorderColorLong == bLong
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(bLong))
                                                        .border(if (isSel) 2.dp else 0.75.dp, if (isSel) CherryAccent else cardBorder, CircleShape)
                                                        .clickable {
                                                            spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(highlightBorderColorLong = bLong))
                                                        }
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(cardBg)
                                                    .border(0.75.dp, CherryAccent, CircleShape)
                                                    .clickable { colorPickerTarget = "border" },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Colorize, contentDescription = null, tint = CherryAccent, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                // Per-word Text Color Controls
                                val textColors = listOf(
                                    0xFFFFFFFFL, 0xFF0A0A0AL, 0xFFE5C07BL, 0xFF9E1B32L,
                                    0xFFFDF6E3L, 0xFF38BDF8L, 0xFF34D399L, 0xFFF472B6L
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(0.5.dp, cardBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(textColorLong = null))
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) { Text("افتراضي", color = secondaryTextColor, fontSize = 10.sp) }

                                    textColors.forEach { cLong ->
                                        val isSel = activeSpan.textColorLong == cLong
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color(cLong))
                                                .border(if (isSel) 2.dp else 0.75.dp, if (isSel) CherryAccent else cardBorder, CircleShape)
                                                .clickable {
                                                    spans = RichTextSpanMapper.upsertSpan(spans, activeSpan.copy(textColorLong = cLong))
                                                }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(cardBg)
                                            .border(0.75.dp, CherryAccent, CircleShape)
                                            .clickable { colorPickerTarget = "text" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Colorize, contentDescription = "لون مخصص", tint = CherryAccent, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (colorPickerTarget != null) {
                        val initColor = when (colorPickerTarget) {
                            "highlight" -> activeSpan.highlightColorLong ?: 0xFFE5C07BL
                            "border" -> activeSpan.highlightBorderColorLong ?: 0xFFFFFFFFL
                            else -> activeSpan.textColorLong ?: 0xFFFFFFFFL
                        }
                        val title = when (colorPickerTarget) {
                            "highlight" -> "لون خلفية الكلمة"
                            "border" -> "لون إطار الخلفية"
                            else -> "لون نص التحديد"
                        }
                        CustomColorPickerDialog(
                            initialColorLong = initColor,
                            title = title,
                            onColorSelected = { chosen ->
                                val updated = when (colorPickerTarget) {
                                    "highlight" -> activeSpan.copy(highlightColorLong = chosen)
                                    "border" -> activeSpan.copy(highlightBorderColorLong = chosen)
                                    else -> activeSpan.copy(textColorLong = chosen)
                                }
                                spans = RichTextSpanMapper.upsertSpan(spans, updated)
                                colorPickerTarget = null
                            },
                            onDismiss = { colorPickerTarget = null }
                        )
                    }
                }
            }

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
                    spans = RichTextSpanMapper.adjustSpansOnTextEdit(currentText, newText, spans)
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
                                spans = emptyList()
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
                        if (onApplyTextWithSpans != null) {
                            onApplyTextWithSpans(textFieldValue.text, spans)
                        } else {
                            onApplyText(textFieldValue.text)
                        }
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
