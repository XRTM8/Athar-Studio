package app.xrtm8.athar.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.ui.components.CanvasView
import app.xrtm8.athar.ui.components.GentleUpdateBanner
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.ui.theme.GlassBackground
import app.xrtm8.athar.ui.theme.GlassBorder
import app.xrtm8.athar.ui.theme.GlassBorderActive
import app.xrtm8.athar.ui.theme.GlassSurface
import app.xrtm8.athar.ui.theme.GoldAccent
import app.xrtm8.athar.ui.theme.MutedWhite
import app.xrtm8.athar.ui.theme.PureWhite
import app.xrtm8.athar.ui.theme.SubtleGray
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtharStudioScreen(
    updateManager: AtharUpdateManager,
    viewModel: AtharStudioViewModel = remember { AtharStudioViewModel() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val canvasState by viewModel.canvasState.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    val updateInfo by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()

    // Photo picker contract for selecting background image from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setBackgroundImageUri(uri)
                Toast.makeText(context, "تم تعيين الصورة كخلفية للتصميم", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearExportMessage()
        }
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
                                    val currentVersion = try {
                                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1.0"
                                    } catch (e: Exception) {
                                        "1.1.0"
                                    }
                                    val info = updateManager.checkForUpdates(currentVersionName = currentVersion, showNotificationOnFound = false)
                                    if (info == null) {
                                        Toast.makeText(context, "أنت تستخدم أحدث إصدار من أَثَــر (v$currentVersion)", Toast.LENGTH_SHORT).show()
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

                        Spacer(modifier = Modifier.width(4.dp))

                        // Reset Canvas Transform
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.resetCanvasTransform()
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

                        Spacer(modifier = Modifier.width(8.dp))

                        // Export Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showExportSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp),
                            enabled = !isExporting
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    color = AmoledBlack,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
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
            }

            // Interactive Canvas Area (Draw-Phase Isolated)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CanvasView(state = canvasState)

                // Quick Edit Text FAB
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

            // Expanded Frosted Glass Bottom Studio Console
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(GlassBackground)
                    .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(26.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // 5-Tab Segmented Glass Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurface)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf("المَدّ والمحاذاة", "التأثيرات", "الخطوط", "الخلفية والصورة", "الحجم والعمق")
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) GoldAccent else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTab = index
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) AmoledBlack else MutedWhite,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab Content
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
                                // Kashida & Alignment Tab
                                Column {
                                    // Kashida Slider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "مستوى المد المتجهي (الكشيدة)",
                                            color = PureWhite,
                                            fontSize = 13.sp,
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
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Slider(
                                        value = canvasState.stretchLevel.toFloat(),
                                        onValueChange = {
                                            if (it.toInt() != canvasState.stretchLevel) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            viewModel.setStretchLevel(it.toInt())
                                        },
                                        valueRange = 0f..8f,
                                        steps = 7,
                                        colors = SliderDefaults.colors(
                                            thumbColor = GoldAccent,
                                            activeTrackColor = GoldAccent,
                                            inactiveTrackColor = GlassSurface
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Quick Alignment Snapping Tools
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(GlassSurface)
                                            .padding(6.dp)
                                    ) {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.alignTop()
                                        }) {
                                            Icon(Icons.Default.VerticalAlignTop, contentDescription = "محاذاة لأعلى", tint = PureWhite)
                                        }
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.alignCenter()
                                        }) {
                                            Icon(Icons.Default.VerticalAlignCenter, contentDescription = "محاذاة للوسط", tint = GoldAccent)
                                        }
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.alignBottom()
                                        }) {
                                            Icon(Icons.Default.VerticalAlignBottom, contentDescription = "محاذاة لأسفل", tint = PureWhite)
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // Shaders Tab
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
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) GoldAccent else GlassSurface)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                                    color = if (isSelected) GoldAccent else GlassBorder,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.setEffect(effect)
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
                                // Arabic Fonts Tab
                                Column {
                                    Text(
                                        text = "اختر نمط ونوع الخط العربي",
                                        color = MutedWhite,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ArabicFontFamily.values().forEach { font ->
                                            val isSelected = canvasState.selectedFont == font
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isSelected) GoldAccent else GlassSurface)
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) GoldAccent else GlassBorder,
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.setFont(font)
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = font.fontName,
                                                        color = if (isSelected) AmoledBlack else PureWhite,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = font.category,
                                                        color = if (isSelected) AmoledBlack.copy(alpha = 0.7f) else SubtleGray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // Photo & Background Canvas Tab
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Pick photo button
                                        Button(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderActive)
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (canvasState.backgroundImageUri != null) "تغيير الصورة" else "إضافة صورة من المعرض",
                                                color = PureWhite,
                                                fontSize = 12.5.sp
                                            )
                                        }

                                        if (canvasState.backgroundImageUri != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.setBackgroundImageUri(null)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
                                            ) {
                                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إزالة", color = Color(0xFFFF5252), fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Photo Dimming Slider (if image is present)
                                    if (canvasState.backgroundImageUri != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = "تعتيم الصورة لبروز النص", color = PureWhite, fontSize = 12.5.sp)
                                            Text(text = "${(canvasState.backgroundDim * 100).toInt()}%", color = GoldAccent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = canvasState.backgroundDim,
                                            onValueChange = { viewModel.setBackgroundDim(it) },
                                            valueRange = 0f..0.85f,
                                            colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // Aspect Ratio Selector
                                    Text(text = "أبعاد التصميم والستوري", color = MutedWhite, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CanvasAspectRatio.values().forEach { ratio ->
                                            val isSelected = canvasState.aspectRatio == ratio
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) GoldAccent else GlassSurface)
                                                    .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.setAspectRatio(ratio)
                                                    }
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = ratio.label,
                                                    color = if (isSelected) AmoledBlack else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                // Size & Depth Tab
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FormatSize, contentDescription = null, tint = MutedWhite, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "حجم الخط", color = PureWhite, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Text(text = "${canvasState.fontSize.toInt()} sp", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = canvasState.fontSize,
                                        onValueChange = { viewModel.setFontSize(it) },
                                        valueRange = 24f..96f,
                                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(text = "وضع النص خلف العنصر (Text Behind Subject)", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text(text = "عزل أوتوماتيكي للعناصر ثلاثية الأبعاد", color = SubtleGray, fontSize = 11.5.sp)
                                        }
                                        Switch(
                                            checked = canvasState.isTextBehindSubject,
                                            onCheckedChange = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setTextBehindSubject(it)
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
                title = { Text(text = "كتابة وتنسيق النص", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                            viewModel.setText(tempText)
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
                    Text(text = "تصدير التصميم الفاخر", color = PureWhite, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "اختر صيغة الحفظ المناسبة للستوري أو المنشورات", color = SubtleGray, fontSize = 12.5.sp)

                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Export Transparent PNG Sticker
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportDesign(context, isTransparent = true)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isExporting
                    ) {
                        Text(text = "حفظ كملصق شفاف للستوري (PNG)", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Export Ultra HD Image with Photo / Canvas Background
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportDesign(context, isTransparent = false)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isExporting
                    ) {
                        Text(text = "حفظ الصورة كاملة بخلفية (Ultra HD)", color = PureWhite, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}
