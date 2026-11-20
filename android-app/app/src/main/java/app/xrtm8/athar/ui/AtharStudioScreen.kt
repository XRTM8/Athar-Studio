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
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.model.ArabicFontFamily
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.KashidaMode
import app.xrtm8.athar.model.LUXURY_BG_PRESETS
import app.xrtm8.athar.model.LUXURY_SWATCHES
import app.xrtm8.athar.model.TextAlignOption
import app.xrtm8.athar.model.VisualEffect
import app.xrtm8.athar.core.PureArabicKashidaEngine
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
    var isPanelExpanded by remember { mutableStateOf(true) }
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

            // Modern Sleek Studio Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBackground)
                    .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Right: Logo & Project Library Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "أَثَــر",
                            color = PureWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.loadProjectsList(context)
                                showProjectsSheet = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "مشاريعي ومسوداتي",
                                tint = GoldAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Center: Studio Quick Controls Pill (Undo, Redo, Grid, Lock, Reset)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurface)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.undo()
                            },
                            enabled = canUndo,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "تراجع",
                                tint = if (canUndo) PureWhite else SubtleGray.copy(alpha = 0.35f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.redo()
                            },
                            enabled = canRedo,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "إعادة",
                                tint = if (canRedo) PureWhite else SubtleGray.copy(alpha = 0.35f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(GlassBorder)
                        )

                        // Grid Toggle
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleGridGuides()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "شبكة المحاذاة الذهبية",
                                tint = if (canvasState.showGridGuides) GoldAccent else SubtleGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Lock Toggle
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleLock()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (canvasState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "قفل موضع النص",
                                tint = if (canvasState.isLocked) GoldAccent else SubtleGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Reset Canvas Transform
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.resetCanvasTransform()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "توسيط وإعادة ضبط النص",
                                tint = SubtleGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Left: Prominent Export Button
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
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "تصدير",
                                tint = AmoledBlack,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تصدير",
                                color = AmoledBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Interactive Canvas Area (Clean, Unobstructed Workspace)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CanvasView(
                    state = canvasState,
                    onTransformFinished = { normX, normY, scale, rot ->
                        viewModel.commitTransform(normX, normY, scale, rot)
                    },
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showTextDialog = true
                    }
                )
            }

            // Professional Un-Cluttered Studio Tool Panel & Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (isPanelExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(GlassBackground)
                            .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(24.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            // Header of Active Tool Panel with Title & Collapse Chevron
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (selectedTab) {
                                            0 -> Icons.Default.Edit
                                            1 -> Icons.Default.FormatLineSpacing
                                            2 -> Icons.Default.AutoAwesome
                                            3 -> Icons.Default.CenterFocusStrong
                                            else -> Icons.Default.PhotoLibrary
                                        },
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = when (selectedTab) {
                                            0 -> "الخط والنص العربي"
                                            1 -> "المدّ الذكي والمحاذاة"
                                            2 -> "المظهر والألوان الفاخرة"
                                            3 -> "الظل والتحديد الخارجي"
                                            else -> "الكانفاس والخلفية"
                                        },
                                        color = PureWhite,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isPanelExpanded = false
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "تصغير اللوحة",
                                        tint = SubtleGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

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
                                            // Primary Edit Text Trigger
                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    showTextDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderActive),
                                                modifier = Modifier.fillMaxWidth().height(40.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("تعديل النص والتشكيل", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    Text(
                                                        text = canvasState.rawText.take(14),
                                                        color = GoldAccent,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(text = "اختر الخط العربي الأصيل:", color = MutedWhite, fontSize = 11.5.sp)
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ArabicFontFamily.entries.forEach { font ->
                                                    val isSelected = canvasState.selectedFont == font
                                                    Box(
                                                        modifier = Modifier
                                                            .width(118.dp)
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
                                                            .padding(horizontal = 8.dp, vertical = 7.dp)
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                                            Text(
                                                                text = font.specimen,
                                                                color = if (isSelected) AmoledBlack else GoldAccent,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.5.sp,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = font.fontName,
                                                                color = if (isSelected) AmoledBlack else PureWhite,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 10.5.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Size Slider
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "حجم الخط", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Text(text = "${canvasState.fontSize.toInt()} sp", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Slider(
                                                value = canvasState.fontSize,
                                                onValueChange = { viewModel.setFontSize(it) },
                                                valueRange = 24f..100f,
                                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Styling Chips: Bold, Italic, Underline & Opacity
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (canvasState.isBold) GoldAccent else GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.toggleBold()
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.FormatBold, contentDescription = "عريض", tint = if (canvasState.isBold) AmoledBlack else PureWhite, modifier = Modifier.size(16.dp))
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (canvasState.isItalic) GoldAccent else GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.toggleItalic()
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.FormatItalic, contentDescription = "مائل", tint = if (canvasState.isItalic) AmoledBlack else PureWhite, modifier = Modifier.size(16.dp))
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (canvasState.isUnderline) GoldAccent else GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.toggleUnderline()
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.FormatUnderlined, contentDescription = "تسطير", tint = if (canvasState.isUnderline) AmoledBlack else PureWhite, modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                Text(text = "الشفافية: ${(canvasState.textOpacity * 100).toInt()}%", color = GoldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            Slider(
                                                value = canvasState.textOpacity,
                                                onValueChange = { viewModel.setTextOpacity(it) },
                                                valueRange = 0.15f..1.0f,
                                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "تباعد الحروف (Letter Spacing)", color = PureWhite, fontSize = 11.5.sp)
                                                Text(
                                                    text = String.format(Locale.ENGLISH, "%.2f", canvasState.letterSpacing),
                                                    color = GoldAccent,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Slider(
                                                value = canvasState.letterSpacing,
                                                onValueChange = { viewModel.setLetterSpacing(it) },
                                                valueRange = -0.05f..0.25f,
                                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                            )
                                        }
                                    }

                                    1 -> {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "مستوى المد المتجهي (الكشيدة)", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

                                            // Quick Stretch Presets
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(0 to "بدون مد", 2 to "2x", 4 to "4x", 6 to "6x", 8 to "أقصى 8x").forEach { (lvl, title) ->
                                                    val isSelected = canvasState.stretchLevel == lvl
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) GoldAccent else GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.setStretchLevel(lvl)
                                                            }
                                                            .padding(vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = title,
                                                            color = if (isSelected) AmoledBlack else PureWhite,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Kashida Distribution Mode (خطي ذكي / شامل متوازن / موضع محدد)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "توزيع ونمط المد:", color = MutedWhite, fontSize = 11.5.sp)

                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(GlassSurface)
                                                        .padding(2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    listOf(
                                                        KashidaMode.SMART to "خطي ذكي",
                                                        KashidaMode.ALL to "شامل (الكل)",
                                                        KashidaMode.CUSTOM to "موضع محدد"
                                                    ).forEach { (mode, label) ->
                                                        val isSelected = canvasState.kashidaMode == mode
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isSelected) GoldAccent else Color.Transparent)
                                                                .clickable {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    viewModel.setKashidaMode(mode)
                                                                }
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                color = if (isSelected) AmoledBlack else PureWhite,
                                                                fontSize = 10.5.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Available Kashida Opportunities (Chips to select specific letter to stretch)
                                            val kashidaOpps = remember(canvasState.rawText) {
                                                PureArabicKashidaEngine.findOpportunities(canvasState.rawText)
                                            }
                                            if (kashidaOpps.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState())
                                                ) {
                                                    Text(text = "مواضع المد:", color = MutedWhite, fontSize = 10.5.sp)
                                                    kashidaOpps.forEach { opp ->
                                                        val isSelected = canvasState.kashidaMode == KashidaMode.CUSTOM &&
                                                                canvasState.kashidaTargetIndex == opp.globalIndex
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isSelected) GoldAccent else GlassSurface)
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = if (isSelected) GoldAccent else if (opp.isRecommended) GoldAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .clickable {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    viewModel.setKashidaTargetIndex(opp.globalIndex)
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = opp.displayLabel,
                                                                color = if (isSelected) AmoledBlack else PureWhite,
                                                                fontSize = 10.5.sp,
                                                                fontWeight = if (isSelected || opp.isRecommended) FontWeight.Bold else FontWeight.Normal
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
                                                // RTL Alignment options
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(GlassSurface)
                                                        .padding(2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    TextAlignOption.entries.forEach { align ->
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
                                                                fontSize = 11.5.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }

                                                // Vertical alignment quick snaps
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(GlassSurface)
                                                        .padding(2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.alignTop()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.VerticalAlignTop, contentDescription = "أعلى", tint = PureWhite, modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.alignCenter()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.VerticalAlignCenter, contentDescription = "توسيط", tint = GoldAccent, modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.alignBottom()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.VerticalAlignBottom, contentDescription = "أسفل", tint = PureWhite, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Rotation slider + step buttons
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = MutedWhite, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "التدوير الدقيق", color = PureWhite, fontSize = 12.sp)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.rotateStep(-90f)
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("-90°", color = PureWhite, fontSize = 10.sp)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.setRotationAngle(0f)
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("0°", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.rotateStep(90f)
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("+90°", color = PureWhite, fontSize = 10.sp)
                                                    }
                                                    Text(text = "${canvasState.rotationAngle.toInt()}°", color = GoldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Slider(
                                                value = canvasState.rotationAngle,
                                                onValueChange = { viewModel.setRotationAngle(it) },
                                                valueRange = -180f..180f,
                                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "تباعد الأسطر (Line Spacing)", color = PureWhite, fontSize = 11.5.sp)
                                                Text(
                                                    text = "${String.format(Locale.ENGLISH, "%.2f", canvasState.lineSpacingMultiplier)}x",
                                                    color = GoldAccent,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Slider(
                                                value = canvasState.lineSpacingMultiplier,
                                                onValueChange = { viewModel.setLineSpacing(it) },
                                                valueRange = 1.0f..2.2f,
                                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                            )
                                        }
                                    }

                                    2 -> {
                                        Column {
                                            Text(text = "التظليلات المعدنية الفاخرة (6 Shaders):", color = MutedWhite, fontSize = 11.5.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                VisualEffect.entries.forEach { effect ->
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
                                                            fontSize = 11.5.sp
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
                                                                .background(if (swatch.argb == 0L) GoldAccent else Color(swatch.argb.toInt()))
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
                                        }
                                    }

                                    3 -> {
                                        Column {
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
                                                valueRange = 0f..16f,
                                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                            )

                                            // Stroke color chips
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(
                                                    0xFF000000 to "أسود داكن",
                                                    0xFFFFFFFF to "أبيض ناصع",
                                                    0xFFFFD700 to "ذهب ملكي",
                                                    0xFFE0E0E0 to "فضي بلاتيني"
                                                ).forEach { (colorLong, name) ->
                                                    val isSelected = canvasState.strokeColorLong == colorLong
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) GoldAccent else GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.setStrokeColor(colorLong)
                                                            }
                                                            .padding(vertical = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = name,
                                                            color = if (isSelected) AmoledBlack else PureWhite,
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

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

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Directional 2D Shadow: Dy and Dx
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "عمق الظل الرأسي (Dy)", color = PureWhite, fontSize = 11.5.sp)
                                                Text(text = "${canvasState.shadowDy.toInt()} px", color = GoldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Slider(
                                                value = canvasState.shadowDy,
                                                onValueChange = { viewModel.setShadowDy(it) },
                                                valueRange = -15f..30f,
                                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = "انحراف الظل الأفقي (Dx)", color = PureWhite, fontSize = 11.5.sp)
                                                Text(text = "${canvasState.shadowDx.toInt()} px", color = GoldAccent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Slider(
                                                value = canvasState.shadowDx,
                                                onValueChange = { viewModel.setShadowDx(it) },
                                                valueRange = -20f..20f,
                                                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent, inactiveTrackColor = GlassSurface)
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(text = "لون الظل والتوهج:", color = MutedWhite, fontSize = 11.5.sp)
                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(
                                                    0xCC000000L to "ظل أسود",
                                                    0xAAFFD700L to "توهج ذهبي",
                                                    0xAA00FF9DL to "توهج زمردي",
                                                    0xAAFFFFFFL to "وهج أبيض",
                                                    0xAAFF3366L to "وهج ياقوتي"
                                                ).forEach { (colorLong, name) ->
                                                    val isSelected = canvasState.shadowColorLong == colorLong
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) GoldAccent else GlassSurface)
                                                            .clickable {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                viewModel.setShadowColor(colorLong)
                                                            }
                                                            .padding(vertical = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = name,
                                                            color = if (isSelected) AmoledBlack else PureWhite,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    4 -> {
                                        Column {
                                            // Transparent Background Switch
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = "خلفية شفافة (ملصق ستوري)", color = PureWhite, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = "تفريغ الخلفية لتصدير ملصق شفاف نقي بدون هوامش", color = SubtleGray, fontSize = 10.5.sp)
                                                }
                                                Switch(
                                                    checked = canvasState.isTransparentBg,
                                                    onCheckedChange = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.setTransparentBg(it)
                                                    },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = AmoledBlack, checkedTrackColor = GoldAccent)
                                                )
                                            }

                                            // Solid Luxury Presets (when not transparent and no image)
                                            if (!canvasState.isTransparentBg && canvasState.backgroundImageUri == null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(text = "ألوان الكانفاس الفاخرة:", color = MutedWhite, fontSize = 11.5.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    LUXURY_BG_PRESETS.forEach { bg ->
                                                        val isSelected = canvasState.solidBgColorLong == bg.argb || (bg.argb == 0xFF000000 && canvasState.solidBgColorLong == null)
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(if (isSelected) GoldAccent else GlassSurface)
                                                                .border(0.5.dp, if (isSelected) GoldAccent else GlassBorder, RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    viewModel.setSolidBgColor(if (bg.argb == 0xFF000000) null else bg.argb)
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(10.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(bg.argb.toInt()))
                                                                    .border(0.5.dp, if (isSelected) AmoledBlack else PureWhite, CircleShape)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = bg.name,
                                                                color = if (isSelected) AmoledBlack else PureWhite,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

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
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
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
                                                CanvasAspectRatio.entries.forEach { ratio ->
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

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Golden Grid Guides Switch
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.GridOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(text = "شبكة المحاذاة الذهبية (Rule of Thirds)", color = PureWhite, fontSize = 12.sp)
                                                }
                                                Switch(
                                                    checked = canvasState.showGridGuides,
                                                    onCheckedChange = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.toggleGridGuides()
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

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 5-Tool Ergonomic Dock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassBackground)
                        .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dockTools = listOf(
                        Triple(0, "الخط", Icons.Default.Edit),
                        Triple(1, "المدّ", Icons.Default.FormatLineSpacing),
                        Triple(2, "الألوان", Icons.Default.AutoAwesome),
                        Triple(3, "الظلال", Icons.Default.CenterFocusStrong),
                        Triple(4, "الكانفاس", Icons.Default.PhotoLibrary)
                    )

                    dockTools.forEach { (index, title, icon) ->
                        val isSelected = selectedTab == index && isPanelExpanded
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GoldAccent.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (selectedTab == index) {
                                        isPanelExpanded = !isPanelExpanded
                                    } else {
                                        selectedTab = index
                                        isPanelExpanded = true
                                    }
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected) GoldAccent else SubtleGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = title,
                                color = if (isSelected) GoldAccent else MutedWhite,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
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
                        .padding(horizontal = 22.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "تحرير النص والتشكيل العربي",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "انقر مرتين على الكانفاس للفتح السريع",
                                color = SubtleGray,
                                fontSize = 11.sp
                            )
                        }
                        TextButton(onClick = { tempText = "" }) {
                            Text("مسح", color = SubtleGray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hero Text Input Area (Single, Large, Gorgeous Typographic Input)
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 150.dp),
                        singleLine = false,
                        maxLines = 4,
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            textAlign = TextAlign.Center
                        ),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = {
                            Text(
                                text = "اكتب النص أو بيت الشعر هنا...",
                                color = SubtleGray,
                                fontSize = 16.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = GlassSurface,
                            unfocusedContainerColor = GlassSurface,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            cursorColor = GoldAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Authentic Diacritics and Islamic Calligraphic Glyphs (100% visibly rendered with carrier)
                    Text(text = "التشكيل والحركات والرموز الإسلامية:", color = MutedWhite, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val arabicMarks = listOf(
                        "ـَ" to "َ",
                        "ـُ" to "ُ",
                        "ـِ" to "ِ",
                        "ـّ" to "ّ",
                        "ـْ" to "ْ",
                        "ـً" to "ً",
                        "ـٌ" to "ٌ",
                        "ـٍ" to "ٍ",
                        "ـ" to "ـ",
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
                        arabicMarks.forEach { (displayGlyph, insertChar) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassSurface)
                                    .border(0.5.dp, GlassBorderActive, RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        tempText += insertChar
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayGlyph,
                                    color = GoldAccent,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "اقتراحات وسياقات جاهزة:", color = MutedWhite, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
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

                    // Action Buttons (Apply & Cancel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setText(tempText.ifBlank { "أَثَــر" })
                                showTextDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("تطبيق وتحديث التصميم", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                showTextDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("إلغاء", color = PureWhite, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(34.dp))
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
                        ExportResolution.entries.forEach { res ->
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

                    // 1. Export Full Canvas Frame (Matching current canvas settings)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportDesign(
                                context,
                                resolution = selectedResolution,
                                isTransparent = canvasState.isTransparentBg,
                                isStickerTrimmed = false
                            )
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isExporting
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = AmoledBlack, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (canvasState.isTransparentBg) "تصدير الكانفاس الشفاف الكامل (PNG)" else "تصدير التصميم الكامل (${selectedResolution.label})",
                                        color = AmoledBlack,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "بالأبعاد الكاملة مع ضبط الخلفية والظلال الحالي بدقة الشاشة",
                                        color = AmoledBlack.copy(alpha = 0.75f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Export Transparent PNG Sticker (Trimmed tightly to text)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportDesign(
                                context,
                                resolution = selectedResolution,
                                isTransparent = true,
                                isStickerTrimmed = true
                            )
                            showExportSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorderActive),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isExporting
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "ملصق شفاف مقصوص للستوري (Sticker PNG)", color = PureWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "مفرغ بدون خلفية ومقصوص بدقة حول الخط مباشرة", color = SubtleGray, fontSize = 10.sp)
                                }
                            }
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
