package app.xrtm8.mada.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.BlurMaskFilter
import android.graphics.Shader
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.xrtm8.mada.core.MadaNativeEngine
import app.xrtm8.mada.model.CanvasState
import app.xrtm8.mada.model.VisualEffect
import app.xrtm8.mada.ui.components.CanvasView
import app.xrtm8.mada.ui.components.GentleUpdateBanner
import app.xrtm8.mada.ui.theme.DarkBackground
import app.xrtm8.mada.ui.theme.DarkCard
import app.xrtm8.mada.ui.theme.DarkSurface
import app.xrtm8.mada.ui.theme.GoldPrimary
import app.xrtm8.mada.ui.theme.TextLight
import app.xrtm8.mada.ui.theme.TextMuted
import app.xrtm8.mada.updater.MadaUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MadaStudioScreen(
    updateManager: MadaUpdateManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var canvasState by remember { mutableStateOf(CanvasState()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    val updateInfo by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()

    // Calculate stretched text via native Rust engine whenever text or stretchLevel changes
    LaunchedEffect(canvasState.rawText, canvasState.stretchLevel) {
        val stretched = MadaNativeEngine.applyKashida(canvasState.rawText, canvasState.stretchLevel)
        canvasState = canvasState.copy(displayText = stretched)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "مَــدَى",
                        color = GoldPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STUDIO",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Manual Update Check Icon
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val info = updateManager.checkForUpdates(showNotificationOnFound = false)
                                if (info == null) {
                                    Toast.makeText(context, "أنت تستخدم أحدث إصدار من مَــدَى (v1.0.0)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "التحقق من التحديثات",
                            tint = TextMuted
                        )
                    }

                    // Export Button
                    Button(
                        onClick = { showExportSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "تصدير",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تصدير",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Interactive Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CanvasView(
                    state = canvasState,
                    onTransformChange = { ox, oy, sc, rot ->
                        canvasState = canvasState.copy(
                            offsetX = ox,
                            offsetY = oy,
                            scaleFactor = sc,
                            rotationAngle = rot
                        )
                    }
                )

                // Quick Edit Text FAB
                IconButton(
                    onClick = { showTextDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(DarkCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل النص",
                        tint = GoldPrimary
                    )
                }
            }

            // Bottom Controls Sheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp)
            ) {
                // Control Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = GoldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("المَدّ الذكي", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("التأثيرات", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("العمق والطبقات", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // Smart Kashida Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "مستوى استطالة الحرف (الكشيدة)",
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${canvasState.stretchLevel}x",
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Slider(
                            value = canvasState.stretchLevel.toFloat(),
                            onValueChange = { canvasState = canvasState.copy(stretchLevel = it.toInt()) },
                            valueRange = 0f..8f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = DarkCard
                            )
                        )
                    }

                    1 -> {
                        // Visual Effects Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VisualEffect.values().forEach { effect ->
                                val isSelected = canvasState.selectedEffect == effect
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GoldPrimary else DarkCard)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) GoldPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { canvasState = canvasState.copy(selectedEffect = effect) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = effect.arabicName,
                                        color = if (isSelected) Color.Black else TextLight,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // Depth & Layering
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "وضع النص خلف العنصر (Text Behind Subject)",
                                    color = TextLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "تفعيل طبقة العزل ثلاثية الأبعاد لعزل العناصر التلقائي",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = canvasState.isTextBehindSubject,
                                onCheckedChange = { canvasState = canvasState.copy(isTextBehindSubject = it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GoldPrimary,
                                    checkedTrackColor = GoldPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Gentle In-App Update Banner (Floats at top if an update is found)
        GentleUpdateBanner(
            updateInfo = updateInfo,
            downloadProgress = downloadProgress,
            onUpdateClick = { url ->
                coroutineScope.launch {
                    updateManager.downloadAndInstallApk(url)
                }
            },
            onDismissClick = {
                updateManager.dismissUpdate()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Text Edit Dialog
        if (showTextDialog) {
            var tempText by remember { mutableStateOf(canvasState.rawText) }
            AlertDialog(
                onDismissRequest = { showTextDialog = false },
                containerColor = DarkCard,
                title = { Text("اكتب العبارة أو النص", color = TextLight, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            canvasState = canvasState.copy(rawText = tempText)
                            showTextDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("تم", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextDialog = false }) {
                        Text("إلغاء", color = TextMuted)
                    }
                }
            )
        }

        // Export BottomSheet
        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                containerColor = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "خيارات التصدير والمشاركة",
                        color = TextLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Export Transparent PNG Sticker
                    Button(
                        onClick = {
                            exportImage(context, canvasState, isTransparent = true)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "حفظ كملصق شفاف لقصص سناب وإنستغرام (PNG)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Export 4K Canvas Image
                    Button(
                        onClick = {
                            exportImage(context, canvasState, isTransparent = false)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "حفظ الصورة كاملة بخلفية (Ultra HD)",
                            color = TextLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun exportImage(context: Context, state: CanvasState, isTransparent: Boolean) {
    try {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!isTransparent) {
            canvas.drawColor(android.graphics.Color.parseColor("#0C0D12"))
        }

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            textSize = state.fontSize * 3.5f
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val centerY = height / 2f

        // Apply Gold shader for export
        val goldShader = LinearGradient(
            0f, -paint.textSize * 0.8f,
            0f, paint.textSize * 0.2f,
            intArrayOf(
                android.graphics.Color.parseColor("#FFF4D0"),
                android.graphics.Color.parseColor("#F5C369"),
                android.graphics.Color.parseColor("#D49438"),
                android.graphics.Color.parseColor("#FFE8A3")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = goldShader

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.drawText(state.displayText, 0f, 0f, paint)
        canvas.restore()

        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, "Mada_${System.currentTimeMillis()}.${if (isTransparent) "png" else "jpg"}")
        val out = FileOutputStream(file)
        bitmap.compress(if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()

        // Share Intent
        val uri: Uri = FileProvider.getUriForFile(context, "app.xrtm8.mada.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (isTransparent) "image/png" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة التصميم من مَــدَى"))
    } catch (e: Exception) {
        Toast.makeText(context, "حدث خطأ أثناء التصدير: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
