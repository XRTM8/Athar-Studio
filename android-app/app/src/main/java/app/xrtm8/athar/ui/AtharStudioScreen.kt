package app.xrtm8.athar.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.xrtm8.athar.core.AtharNativeEngine
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.ui.components.CanvasView
import app.xrtm8.athar.ui.components.GentleUpdateBanner
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.GlassBackground
import app.xrtm8.athar.ui.theme.GlassBorder
import app.xrtm8.athar.ui.theme.GlassBorderActive
import app.xrtm8.athar.ui.theme.GlassSurface
import app.xrtm8.athar.ui.theme.GlassSurfaceHover
import app.xrtm8.athar.ui.theme.GoldAccent
import app.xrtm8.athar.ui.theme.MutedWhite
import app.xrtm8.athar.ui.theme.PureWhite
import app.xrtm8.athar.ui.theme.SubtleGray
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtharStudioScreen(
    updateManager: AtharUpdateManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var canvasState by remember { mutableStateOf(CanvasState()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    val updateInfo by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()

    // Smooth Kashida stretching powered by the native Rust core
    LaunchedEffect(canvasState.rawText, canvasState.stretchLevel) {
        val stretched = AtharNativeEngine.applyKashida(canvasState.rawText, canvasState.stretchLevel)
        canvasState = canvasState.copy(displayText = stretched)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Floating Frosted Glass Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBackground)
                    .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Branding
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "أَثَــر",
                            color = PureWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassSurface)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STUDIO",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Check Update Button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    val info = updateManager.checkForUpdates(showNotificationOnFound = false)
                                    if (info == null) {
                                        Toast.makeText(context, "أنت تستخدم أحدث إصدار من أَثَــر (v1.0.1)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "التحقق من التحديثات",
                                tint = MutedWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Reset Canvas Transform
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                canvasState = canvasState.copy(
                                    offsetX = 0f,
                                    offsetY = 0f,
                                    scaleFactor = 1.0f,
                                    rotationAngle = 0f
                                )
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "إعادة ضبط موضع النص",
                                tint = MutedWhite,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Export Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showExportSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "تصدير",
                                tint = AmoledBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تصدير",
                                color = AmoledBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
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

                // Quick Edit Text FAB (Glassmorphism circle)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(GlassBackground)
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showTextDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل النص",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Frosted Glass Bottom Studio Console
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(GlassBackground)
                    .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(26.dp))
                    .padding(18.dp)
            ) {
                Column {
                    // Segmented Pill Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurface)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .padding(4.dp)
                    ) {
                        val tabs = listOf("المَدّ الذكي", "التأثيرات", "الحجم والعمق")
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) GoldAccent else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTab = index
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) AmoledBlack else MutedWhite,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tab Content with Smooth Transitions
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn()) togetherWith
                                        (slideOutHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut())
                            } else {
                                (slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn()) togetherWith
                                        (slideOutHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut())
                            }
                        },
                        label = "tabContentAnimation"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> {
                                // Smart Kashida Tab
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "مستوى المد المتجهي (الكشيدة)",
                                            color = PureWhite,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GlassSurface)
                                                .border(0.5.dp, GlassBorderActive, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${canvasState.stretchLevel}x",
                                                color = GoldAccent,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Slider(
                                        value = canvasState.stretchLevel.toFloat(),
                                        onValueChange = {
                                            if (it.toInt() != canvasState.stretchLevel) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            canvasState = canvasState.copy(stretchLevel = it.toInt())
                                        },
                                        valueRange = 0f..8f,
                                        steps = 7,
                                        colors = SliderDefaults.colors(
                                            thumbColor = GoldAccent,
                                            activeTrackColor = GoldAccent,
                                            inactiveTrackColor = GlassSurface
                                        )
                                    )
                                }
                            }

                            1 -> {
                                // Visual Effects Shaders
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    VisualEffect.values().forEach { effect ->
                                        val isSelected = canvasState.selectedEffect == effect
                                        val scaleAnim by animateFloatAsState(
                                            targetValue = if (isSelected) 1.04f else 1.0f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                                            label = "chipScale"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .scale(scaleAnim)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) GoldAccent else GlassSurface)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                                    color = if (isSelected) GoldAccent else GlassBorder,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    canvasState = canvasState.copy(selectedEffect = effect)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 11.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = AmoledBlack,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(
                                                    text = effect.arabicName,
                                                    color = if (isSelected) AmoledBlack else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // Depth & Font Size Tab
                                Column {
                                    // Text Size Control
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.FormatSize,
                                                contentDescription = null,
                                                tint = MutedWhite,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "حجم الخط",
                                                color = PureWhite,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "${canvasState.fontSize.toInt()} sp",
                                            color = GoldAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = canvasState.fontSize,
                                        onValueChange = {
                                            canvasState = canvasState.copy(fontSize = it)
                                        },
                                        valueRange = 24f..96f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = GoldAccent,
                                            activeTrackColor = GoldAccent,
                                            inactiveTrackColor = GlassSurface
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Text Behind Subject Toggle
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(
                                                text = "وضع النص خلف العنصر (Text Behind Subject)",
                                                color = PureWhite,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "عزل أوتوماتيكي للعناصر ثلاثية الأبعاد",
                                                color = SubtleGray,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                        Switch(
                                            checked = canvasState.isTextBehindSubject,
                                            onCheckedChange = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                canvasState = canvasState.copy(isTextBehindSubject = it)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = AmoledBlack,
                                                checkedTrackColor = GoldAccent,
                                                uncheckedThumbColor = MutedWhite,
                                                uncheckedTrackColor = GlassSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gentle Floating In-App Update Banner
        GentleUpdateBanner(
            updateInfo = updateInfo,
            downloadProgress = downloadProgress,
            onUpdateClick = { url ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                coroutineScope.launch {
                    updateManager.downloadAndInstallApk(url)
                }
            },
            onDismissClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                updateManager.dismissUpdate()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Frosted Glass Text Edit Dialog
        if (showTextDialog) {
            var tempText by remember { mutableStateOf(canvasState.rawText) }
            AlertDialog(
                onDismissRequest = { showTextDialog = false },
                containerColor = GlassBackground,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "كتابة وتنسيق النص",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            cursorColor = GoldAccent
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            canvasState = canvasState.copy(rawText = tempText)
                            showTextDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تطبيق", color = AmoledBlack, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextDialog = false }) {
                        Text("إلغاء", color = MutedWhite)
                    }
                }
            )
        }

        // Frosted Glass Export BottomSheet
        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                containerColor = AmoledBlack,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(GlassBorder)
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "تصدير التصميم الفاخر",
                        color = PureWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اختر صيغة الحفظ المناسبة لقصص سناب شات وإنستغرام أو الطباعة",
                        color = SubtleGray,
                        fontSize = 12.5.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Export Transparent PNG
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            exportImage(context, canvasState, isTransparent = true)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "حفظ كملصق شفاف للستوري (PNG)",
                            color = AmoledBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Export Ultra HD 4K Image
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            exportImage(context, canvasState, isTransparent = false)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "حفظ الصورة بخلفية AMOLED كاملة (Ultra HD)",
                            color = PureWhite,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
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
            canvas.drawColor(android.graphics.Color.BLACK)
        }

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            textSize = state.fontSize * 3.5f
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val centerY = height / 2f

        val goldShader = LinearGradient(
            0f, -paint.textSize * 0.8f,
            0f, paint.textSize * 0.2f,
            intArrayOf(
                android.graphics.Color.parseColor("#FFF7DB"),
                android.graphics.Color.parseColor("#F5C369"),
                android.graphics.Color.parseColor("#D49438"),
                android.graphics.Color.parseColor("#FFE8A3")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = goldShader

        val lines = state.displayText.split("\n")
        val lineHeight = paint.textSize * 1.3f
        val totalHeight = (lines.size - 1) * lineHeight
        val startY = centerY - (totalHeight / 2f)

        canvas.save()
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, centerX, startY + (index * lineHeight), paint)
        }
        canvas.restore()

        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, "Athar_${System.currentTimeMillis()}.${if (isTransparent) "png" else "jpg"}")
        val out = FileOutputStream(file)
        bitmap.compress(if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()

        val uri: Uri = FileProvider.getUriForFile(context, "app.xrtm8.athar.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (isTransparent) "image/png" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة التصميم من أَثَــر"))
    } catch (e: Exception) {
        Toast.makeText(context, "حدث خطأ أثناء التصدير: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
