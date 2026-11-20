package app.xrtm8.athar.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.LUXURY_SWATCHES
import app.xrtm8.athar.model.TextAlignOption
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtharStudioScreen(
    updateManager: AtharUpdateManager,
    initialProjectUri: Uri? = null,
    viewModel: AtharStudioViewModel = remember { AtharStudioViewModel() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val canvasState by viewModel.canvasState.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val projectsList by viewModel.projectsList.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showProjectsSheet by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf(ExportResolution.HD_1080P) }

    val updateInfo by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()

    // Handle deep-linked project file (.athar)
    LaunchedEffect(initialProjectUri) {
        if (initialProjectUri != null) {
            viewModel.openProjectFromUri(context, initialProjectUri)
        }
    }

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

    val projectImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.openProjectFromUri(context, uri)
                showProjectsSheet = false
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
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBackground)
                    .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Branding & Projects Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "أَثَــر",
                            color = PureWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Open Projects Library Button
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.loadProjectsList(context)
                                showProjectsSheet = true
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "مشاريعي ومسوداتي",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Undo / Redo & Studio Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.undo()
                            },
                            enabled = canUndo,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "تراجع",
                                tint = if (canUndo) PureWhite else SubtleGray.copy(alpha = 0.35f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.redo()
                            },
                            enabled = canRedo,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "إعادة",
                                tint = if (canRedo) PureWhite else SubtleGray.copy(alpha = 0.35f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    val currentVersion = try {
                                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.4.0"
                                    } catch (e: Exception) {
                                        "1.4.0"
                                    }
                                    val info = updateManager.checkForUpdates(currentVersionName = currentVersion, showNotificationOnFound = false)
                                    if (info == null) {
                                        Toast.makeText(context, "أنت تستخدم أحدث إصدار من أَثَــر (v$currentVersion)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "التحقق من التحديثات",
                                tint = MutedWhite,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.resetCanvasTransform()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "إعادة ضبط موضع النص",
                                tint = MutedWhite,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showExportSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(34.dp),
                            enabled = !isExporting
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    color = AmoledBlack,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "تصدير",
                                    tint = AmoledBlack,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "تصدير",
                                    color = AmoledBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Canvas Area (Draw-Phase Isolated 120Hz)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CanvasView(
                    state = canvasState,
                    onTransformChanged = { normX, normY, scale, rot ->
                        viewModel.updateTransform(normX, normY, scale, rot)
                    },
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showTextDialog = true
                    }
                )

                // Quick Edit Text Floating Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GlassBackground)
                        .border(1.dp, GlassBorderActive, CircleShape)
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
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Overhauled Frosted Glass Bottom Studio Console
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(GlassBackground)
                    .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(26.dp))
                    .padding(14.dp)
            ) {
                Column {
                    val tabs = listOf("الخط والمدّ", "الألوان", "الظل والوهج", "الخلفية", "الحجم والتحويل")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurface)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (isSelected) GoldAccent else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTab = index
                                    }
                                    .padding(vertical = 7.dp),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut()
                            }
                        },
                        label = "StudioConsoleTabs"
                    ) { currentTab ->
                        when (currentTab) {
                            0 -> {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ArabicFontFamily.values().forEach { font ->
                                            val isSelected = canvasState.selectedFont == font
                                            Box(
                                                modifier = Modifier
                                                    .width(120.dp)
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
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        text = font.specimen,
                                                        color = if (isSelected) AmoledBlack else GoldAccent,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = font.fontName,
                                                        color = if (isSelected) AmoledBlack else PureWhite,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "مستوى المد المتجهي (الكشيدة)", color = PureWhite, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                        Text(text = "${canvasState.stretchLevel}x", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)
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
                                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(GlassSurface)
                                                .padding(2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            TextAlignOption.values().forEach { align ->
                                                val isSelected = canvasState.textAlign == align
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) GoldAccent else Color.Transparent)
                                                        .clickable {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.setTextAlign(align)
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = align.label,
                                                        color = if (isSelected) AmoledBlack else PureWhite,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FormatLineSpacing, contentDescription = null, tint = MutedWhite, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "تباعد: ${(canvasState.lineSpacingMultiplier * 10).toInt() / 10f}x", color = GoldAccent, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }

                            1 -> {
                                Column {
                                    Text(text = "التظليلات المعدنية الفاخرة:", color = MutedWhite, fontSize = 11.5.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        VisualEffect.values().forEach { effect ->
                                            val isSelected = canvasState.selectedEffect == effect && canvasState.customTextColorLong == null
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) GoldAccent else GlassSurface)
                                                    .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.setEffect(effect)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = effect.arabicName,
                                                    color = if (isSelected) AmoledBlack else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(text = "ألوان مخصصة راقية:", color = MutedWhite, fontSize = 11.5.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        LUXURY_SWATCHES.forEach { swatch ->
                                            val isSelected = canvasState.customTextColorLong == swatch.argb
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.setCustomColor(if (swatch.argb == 0L) null else swatch.argb)
                                                }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(if (swatch.argb == 0L) GoldAccent else Color(swatch.argb))
                                                        .border(
                                                            width = if (isSelected) 2.5.dp else 1.dp,
                                                            color = if (isSelected) PureWhite else GlassBorder,
                                                            shape = CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(text = swatch.name, color = SubtleGray, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "سماكة التحديد الخارجي (Stroke)", color = PureWhite, fontSize = 12.sp)
                                        Text(text = "${canvasState.strokeWidth.toInt()} px", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = canvasState.strokeWidth,
                                        onValueChange = { viewModel.setStrokeWidth(it) },
                                        valueRange = 0f..14f,
                                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                    )
                                }
                            }

                            2 -> {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "تشتت الظل والوهج (Blur Radius)", color = PureWhite, fontSize = 12.sp)
                                        Text(text = "${canvasState.shadowRadius.toInt()} px", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = canvasState.shadowRadius,
                                        onValueChange = { viewModel.setShadowRadius(it) },
                                        valueRange = 0f..45f,
                                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "عمق الظل العمودي (3D Depth)", color = PureWhite, fontSize = 12.sp)
                                        Text(text = "${canvasState.shadowDy.toInt()} px", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = canvasState.shadowDy,
                                        onValueChange = { viewModel.setShadowDy(it) },
                                        valueRange = -10f..25f,
                                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                    )
                                }
                            }

                            3 -> {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
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
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (canvasState.backgroundImageUri != null) "تغيير الصورة" else "إضافة صورة من المعرض",
                                                color = PureWhite,
                                                fontSize = 12.sp
                                            )
                                        }

                                        if (canvasState.backgroundImageUri != null) {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.setBackgroundImageUri(null)
                                                    Toast.makeText(context, "تمت إزالة صورة الخلفية", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.DeleteSweep, contentDescription = "إزالة الصورة", tint = Color(0xFFFF6B6B))
                                            }
                                        }
                                    }

                                    if (canvasState.backgroundImageUri != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = "تعتيم الصورة لبروز النص", color = PureWhite, fontSize = 12.sp)
                                            Text(text = "${(canvasState.backgroundDim * 100).toInt()}%", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = canvasState.backgroundDim,
                                            onValueChange = { viewModel.setBackgroundDim(it) },
                                            valueRange = 0f..0.90f,
                                            colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = "فلتر أبيض وأسود كلاسيكي (B&W)", color = PureWhite, fontSize = 12.sp)
                                            Switch(
                                                checked = canvasState.backgroundGrayscale,
                                                onCheckedChange = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.setBackgroundGrayscale(it)
                                                },
                                                colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(text = "نسبة أبعاد الكانفاس:", color = MutedWhite, fontSize = 11.5.sp)
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CanvasAspectRatio.values().forEach { ratio ->
                                            val isSelected = canvasState.aspectRatio == ratio
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) GoldAccent else GlassSurface)
                                                    .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.setAspectRatio(ratio)
                                                    }
                                                    .padding(vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = ratio.label,
                                                    color = if (isSelected) AmoledBlack else PureWhite,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FormatSize, contentDescription = null, tint = MutedWhite, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "حجم الخط", color = PureWhite, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Text(text = "${canvasState.fontSize.toInt()} sp", color = GoldAccent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = canvasState.fontSize,
                                        onValueChange = { viewModel.setFontSize(it) },
                                        valueRange = 24f..96f,
                                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(GlassSurface)
                                            .padding(4.dp)
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
                                            Icon(Icons.Default.VerticalAlignCenter, contentDescription = "توسيط", tint = GoldAccent)
                                        }
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.alignBottom()
                                        }) {
                                            Icon(Icons.Default.VerticalAlignBottom, contentDescription = "محاذاة لأسفل", tint = PureWhite)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(text = "وضع النص خلف العنصر (3D Subject)", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text(text = "عزل أوتوماتيكي للعناصر في الصورة", color = SubtleGray, fontSize = 10.5.sp)
                                        }
                                        Switch(
                                            checked = canvasState.isTextBehindSubject,
                                            onCheckedChange = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setTextBehindSubject(it)
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
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

        // Modern In-Place Text Editor Modal BottomSheet
        if (showTextDialog) {
            var tempText by remember { mutableStateOf(canvasState.rawText) }
            ModalBottomSheet(
                onDismissRequest = { showTextDialog = false },
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
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
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
                        TextButton(onClick = { tempText = "" }) {
                            Text("مسح الكل", color = SubtleGray, fontSize = 12.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSurface)
                            .border(1.dp, GlassBorderActive, RoundedCornerShape(16.dp))
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tempText.isBlank()) "أَثَــر" else tempText,
                            color = GoldAccent,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("اكتب نصك أو بيت الشعر هنا...", color = SubtleGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            cursorColor = GoldAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "التشكيل والحركات والرموز الإسلامية:", color = SubtleGray, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(5.dp))
                    val arabicMarks = listOf("َ", "ُ", "ِ", "ّ", "ْ", "ً", "ٍ", "ٌ", "ـ", "ﷻ", "ﷺ", "﷽", "۞", "۝")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        arabicMarks.forEach { mark ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassSurface)
                                    .border(0.5.dp, GlassBorderActive, RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        tempText += mark
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = mark, color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "اقتراحات وسياقات جاهزة:", color = SubtleGray, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("أَثَــر", "كُنْ ذَا أَثَــر", "وَيَبْقَى الأَثَــر", "أَثَــرٌ جَمِيل", "مُخْتَلِف", "إِبْدَاعٌ عَرَبِيّ").forEach { phrase ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassSurface)
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        tempText = phrase
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = phrase, color = PureWhite, fontSize = 12.5.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setText(tempText.ifBlank { "أَثَــر" })
                            showTextDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("تطبيق وتحديث التصميم", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    }

                    Spacer(modifier = Modifier.height(26.dp))
                }
            }
        }

        // Frosted Glass Export BottomSheet with .athar Project Export
        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
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
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(text = "تصدير وحفظ التصميم", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = "دقة فائقة تصل إلى 4K أو حفظ كمشروع قابل للتعديل", color = SubtleGray, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "دقة التصدير:", color = MutedWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExportResolution.values().forEach { res ->
                            val isSelected = selectedResolution == res
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) GoldAccent else GlassSurface)
                                    .border(1.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedResolution = res
                                    }
                                    .padding(vertical = 9.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = res.label,
                                        color = if (isSelected) AmoledBlack else PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = "${res.width}px",
                                        color = if (isSelected) AmoledBlack.copy(alpha = 0.7f) else SubtleGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Export Transparent PNG Sticker
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportDesign(context, resolution = selectedResolution, isTransparent = true)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isExporting
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = AmoledBlack, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "حفظ كملصق شفاف للستوري (PNG)", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Export Image with Photo Background
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportDesign(context, resolution = selectedResolution, isTransparent = false)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorderActive),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isExporting
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "حفظ ومشاركة الصورة (${selectedResolution.label})", color = PureWhite, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Save to Local Drafts
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveCurrentProject(context)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorderActive),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "حفظ في مسوداتي (مشروع محلي)", color = PureWhite, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Share Editable .athar Project
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportProjectAsAthar(context)
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "مشاركة كمشروع قابل للتعديل (.athar)", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Projects & Drafts Drawer (.athar)
        if (showProjectsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showProjectsSheet = false },
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
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مشاريعي ومسوداتي (.athar)",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.5.sp
                        )
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                projectImportLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorderActive),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استيراد مشروع", color = PureWhite, fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.saveCurrentProject(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = AmoledBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ العمل الحالي", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setText("أَثَــر")
                                viewModel.resetCanvasTransform()
                                viewModel.setBackgroundImageUri(null)
                                showProjectsSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشروع جديد", color = PureWhite, fontWeight = FontWeight.Medium, fontSize = 12.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (projectsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = SubtleGray,
                                    modifier = Modifier.size(46.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد مشاريع محفوظة بعد",
                                    color = MutedWhite,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "احفظ تصميمك كملف .athar لتعديله في أي وقت أو مشاركته مع الآخرين",
                                    color = SubtleGray,
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                        ) {
                            items(projectsList) { proj ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GlassSurface)
                                        .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.openProjectById(context, proj.id)
                                            showProjectsSheet = false
                                        }
                                ) {
                                    Column {
                                        val thumbFile = File(proj.thumbnailPath)
                                        val thumbBitmap = remember(proj.thumbnailPath) {
                                            if (thumbFile.exists()) BitmapFactory.decodeFile(thumbFile.absolutePath) else null
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .background(AmoledBlack),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (thumbBitmap != null) {
                                                Image(
                                                    bitmap = thumbBitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text("أَثَــر", color = GoldAccent, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = proj.title,
                                                    color = PureWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(proj.lastModified)),
                                                    color = SubtleGray,
                                                    fontSize = 9.5.sp
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.deleteProject(context, proj.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = Color(0xFFFF6B6B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}
