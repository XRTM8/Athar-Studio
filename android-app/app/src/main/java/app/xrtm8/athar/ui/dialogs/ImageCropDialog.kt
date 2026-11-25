package app.xrtm8.athar.ui.dialogs

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import app.xrtm8.athar.core.AtharImageProcessor
import app.xrtm8.athar.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("حر", null),
    SQUARE("1:1", 1.0f),
    STORY("9:16", 9f / 16f),
    PORTRAIT("4:5", 4f / 5f),
    LANDSCAPE("16:9", 16f / 9f)
}

@Composable
fun ImageCropDialog(
    sourceUri: Uri,
    onCropSuccess: (Uri) -> Unit,
    onSkipCrop: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isAmoledWhite = LocalAmoledWhite.current

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationAngle by remember { mutableIntStateOf(0) }
    var selectedAspect by remember { mutableStateOf(CropAspect.FREE) }

    var cropLeft by remember { mutableFloatStateOf(0.05f) }
    var cropTop by remember { mutableFloatStateOf(0.05f) }
    var cropRight by remember { mutableFloatStateOf(0.95f) }
    var cropBottom by remember { mutableFloatStateOf(0.95f) }

    LaunchedEffect(sourceUri, rotationAngle) {
        withContext(Dispatchers.IO) {
            try {
                val raw = AtharImageProcessor.decodeSampledBitmap(context, sourceUri, 2048, 2048)
                if (raw != null) {
                    if (rotationAngle != 0) {
                        val matrix = Matrix().apply { postRotate(rotationAngle.toFloat()) }
                        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                        if (rotated !== raw && !raw.isRecycled) raw.recycle()
                        loadedBitmap = rotated
                    } else {
                        loadedBitmap = raw
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun applyAspectConstraint(aspect: CropAspect) {
        val targetRatio = aspect.ratio ?: return
        val bm = loadedBitmap ?: return
        val imgW = bm.width.toFloat()
        val imgH = bm.height.toFloat()
        val normRatio = targetRatio * (imgH / imgW)

        if (normRatio <= 1.0f) {
            val targetH = 0.82f
            val targetW = (targetH * normRatio).coerceIn(0.15f, 0.98f)
            cropLeft = (0.5f - targetW / 2f).coerceIn(0f, 1f - targetW)
            cropRight = cropLeft + targetW
            cropTop = (0.5f - targetH / 2f).coerceIn(0f, 1f - targetH)
            cropBottom = cropTop + targetH
        } else {
            val targetW = 0.85f
            val targetH = (targetW / normRatio).coerceIn(0.15f, 0.98f)
            cropLeft = (0.5f - targetW / 2f).coerceIn(0f, 1f - targetW)
            cropRight = cropLeft + targetW
            cropTop = (0.5f - targetH / 2f).coerceIn(0f, 1f - targetH)
            cropBottom = cropTop + targetH
        }
    }

    val dialogBg = if (isAmoledWhite) AmoledWhite else AmoledBlack
    val primaryText = if (isAmoledWhite) DarkText else PureWhite

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(dialogBg).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = primaryText) }
                    Text(text = "اقتصاص وتحديد الصورة", color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        rotationAngle = (rotationAngle + 90) % 360
                    }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "تدوير", tint = BurgundyAccent)
                    }
                }

                // Aspect Ratio Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CropAspect.values().forEach { aspect ->
                        val isSelected = (selectedAspect == aspect)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BurgundyAccent else if (isAmoledWhite) WhiteSurfaceElevated else GlassSurface)
                                .border(0.5.dp, if (isSelected) BurgundyAccent else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedAspect = aspect
                                    applyAspectConstraint(aspect)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(text = aspect.label, color = if (isSelected) PureWhite else primaryText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Crop Canvas Area
                ImageCropCanvas(
                    bitmap = loadedBitmap,
                    isAmoledWhite = isAmoledWhite,
                    cropLeft = cropLeft,
                    cropTop = cropTop,
                    cropRight = cropRight,
                    cropBottom = cropBottom,
                    targetRatio = selectedAspect.ratio,
                    onCropChange = { l, t, r, b ->
                        cropLeft = l
                        cropTop = t
                        cropRight = r
                        cropBottom = b
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onSkipCrop, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("استخدام الصورة كاملة", color = primaryText, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val bm = loadedBitmap
                            if (bm != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                try {
                                    val bx = (cropLeft * bm.width).toInt().coerceIn(0, bm.width - 2)
                                    val by = (cropTop * bm.height).toInt().coerceIn(0, bm.height - 2)
                                    val bw = ((cropRight - cropLeft) * bm.width).toInt().coerceIn(2, bm.width - bx)
                                    val bh = ((cropBottom - cropTop) * bm.height).toInt().coerceIn(2, bm.height - by)
                                    val cropped = Bitmap.createBitmap(bm, bx, by, bw, bh)
                                    val cropFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.png")
                                    FileOutputStream(cropFile).use { out -> cropped.compress(Bitmap.CompressFormat.PNG, 95, out) }
                                    onCropSuccess(Uri.fromFile(cropFile))
                                } catch (_: Exception) {
                                    onSkipCrop()
                                }
                            } else {
                                onSkipCrop()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyAccent),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تأكيد القص", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
