package app.xrtm8.athar.ui.dialogs

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.xrtm8.athar.ui.theme.*

/**
 * Professional Custom Color Picker Dialog with Hex Input & HSV Sliders.
 * Allows picking any arbitrary color with instant live preview.
 */
@Composable
fun CustomColorPickerDialog(
    initialColorLong: Long,
    title: String = "منتقي الألوان المخصص",
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current

    // Convert initial Long (ARGB) to HSV components
    val initialInt = initialColorLong.toInt()
    val initialHsv = remember(initialColorLong) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialInt, hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    fun computeCurrentColor(): Color {
        val rgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        return Color(rgb)
    }

    fun computeCurrentColorLong(): Long {
        val rgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        return (rgb.toLong() and 0xFFFFFFFFL)
    }

    var hexText by remember {
        val hex = String.format("%06X", (initialInt and 0x00FFFFFF))
        mutableStateOf(hex)
    }

    val currentColor = computeCurrentColor()

    val quickSwatches = listOf(
        0xFFE02447L, // Cherry
        0xFFE5C07BL, // Gold
        0xFF10B981L, // Emerald
        0xFF2563EBL, // Sapphire
        0xFF8B5CF6L, // Violet
        0xFFF59E0BL, // Amber
        0xFF06B6D4L, // Cyan
        0xFFEC4899L, // Pink
        0xFFFFFFFFL, // White
        0xFF94A3B8L, // Platinum Slate
        0xFF1E293BL, // Dark Charcoal
        0xFF000000L  // Pure Black
    )

    val dialogBg = if (isAmoledWhite) AmoledWhite else GlassBackground
    val cardBg = if (isAmoledWhite) WhiteSurface else GlassSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else GlassBorder
    val primaryText = if (isAmoledWhite) DarkText else PureWhite
    val subtleText = if (isAmoledWhite) DarkTextSubtle else SubtleGray

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background((if (isAmoledWhite) AmoledWhite else AmoledBlack).copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(dialogBg)
                    .border(1.dp, if (isAmoledWhite) WhiteBorder else GlassBorderActive, RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = CherryAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = primaryText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Color Preview & Hex Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(currentColor)
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "كود اللون (HEX)", color = subtleText, fontSize = 11.sp)
                        OutlinedTextField(
                            value = hexText,
                            onValueChange = { input ->
                                val clean = input.removePrefix("#").take(6).uppercase()
                                hexText = clean
                                if (clean.length == 6) {
                                    try {
                                        val parsed = AndroidColor.parseColor("#$clean")
                                        val hsv = FloatArray(3)
                                        AndroidColor.colorToHSV(parsed, hsv)
                                        hue = hsv[0]
                                        saturation = hsv[1]
                                        value = hsv[2]
                                    } catch (_: Exception) {}
                                }
                            },
                            prefix = { Text("#", color = CherryAccent, fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = primaryText
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickSwatches.forEach { swatchLong ->
                        val swatchColor = Color(swatchLong)
                        val isSelected = (computeCurrentColorLong() and 0x00FFFFFF) == (swatchLong and 0x00FFFFFF)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) CherryAccent else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(swatchLong.toInt(), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                    hexText = String.format("%06X", (swatchLong.toInt() and 0x00FFFFFF))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (value > 0.6f && saturation < 0.4f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hue Slider (درجة اللون)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "درجة اللون (Hue)", color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${hue.toInt()}°", color = subtleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            hexText = String.format("%06X", (computeCurrentColorLong().toInt() and 0x00FFFFFF))
                        },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Saturation Slider (التشبع)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "التشبع (Saturation)", color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${(saturation * 100).toInt()}%", color = subtleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            hexText = String.format("%06X", (computeCurrentColorLong().toInt() and 0x00FFFFFF))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Brightness / Value Slider (السطوع)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "السطوع (Brightness)", color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${(value * 100).toInt()}%", color = subtleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                            hexText = String.format("%06X", (computeCurrentColorLong().toInt() and 0x00FFFFFF))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", color = primaryText)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onColorSelected(computeCurrentColorLong())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CherryAccent)
                    ) {
                        Text("تطبيق اللون", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
