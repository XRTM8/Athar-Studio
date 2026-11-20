package app.xrtm8.athar.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.components.*
import app.xrtm8.athar.ui.dialogs.*
import app.xrtm8.athar.ui.panels.*
import app.xrtm8.athar.ui.theme.*
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch

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
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val projectsList by viewModel.projectsList.collectAsState()

    val updateInfo by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isPanelExpanded by remember { mutableStateOf(false) }

    var showTextDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showProjectsSheet by remember { mutableStateOf(false) }

    // Handle deep-linked project file (.athar)
    LaunchedEffect(initialProjectUri) {
        if (initialProjectUri != null) {
            viewModel.openProjectFromUri(context, initialProjectUri)
        }
    }

    // Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setBackgroundImageUri(uri)
            Toast.makeText(context, "تم تعيين صورة الخلفية بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    val projectImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.openProjectFromUri(context, uri)
            showProjectsSheet = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AmoledBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Gentle in-app update banner
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
                }
            )

            // 1. Sleek Top Studio Bar
            TopStudioBar(
                canUndo = canUndo,
                canRedo = canRedo,
                isLocked = canvasState.isLocked,
                showGrid = canvasState.showGridGuides,
                onOpenProjects = {
                    viewModel.loadProjectsList(context)
                    showProjectsSheet = true
                },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleGrid = { viewModel.toggleGridGuides() },
                onToggleLock = { viewModel.toggleLock() },
                onResetTransform = { viewModel.resetCanvasTransform() },
                onExportClick = { showExportSheet = true }
            )

            // 2. Interactive Typography Canvas Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                CanvasView(
                    state = canvasState,
                    modifier = Modifier.fillMaxSize(),
                    onTransformFinished = { x, y, scale, rot ->
                        viewModel.commitTransform(x, y, scale, rot)
                    },
                    onDoubleTap = {
                        showTextDialog = true
                    }
                )
            }

            // 3. Ergonomic Bottom Control Console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                // Collapsible Panel Drawer
                AnimatedVisibility(
                    visible = isPanelExpanded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(GlassBackground)
                            .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(22.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Panel Header with close button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentTabItem = STUDIO_DOCK_TABS.getOrNull(selectedTab)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentTabItem != null) {
                                        Icon(
                                            imageVector = currentTabItem.icon,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = when (selectedTab) {
                                            0 -> "الخط والنص العربي"
                                            1 -> "المدّ الذكي والمحاذاة"
                                            2 -> "المظهر والألوان الفاخرة"
                                            3 -> "العمق والظل والتحديد"
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

                            // Animated Switcher between Panels
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn() togetherWith
                                                slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut()
                                    } else {
                                        slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn() togetherWith
                                                slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut()
                                    }
                                },
                                label = "StudioConsoleTabs"
                            ) { currentTab ->
                                when (currentTab) {
                                    0 -> TypographyPanel(
                                        state = canvasState,
                                        onOpenTextEditor = { showTextDialog = true },
                                        onSelectFont = { viewModel.setFont(it) },
                                        onFontSizeChange = { viewModel.setFontSize(it) },
                                        onToggleBold = { viewModel.toggleBold() },
                                        onToggleItalic = { viewModel.toggleItalic() },
                                        onToggleUnderline = { viewModel.toggleUnderline() },
                                        onOpacityChange = { viewModel.setTextOpacity(it) },
                                        onLetterSpacingChange = { viewModel.setLetterSpacing(it) },
                                        onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                        onTextAlignChange = { viewModel.setTextAlign(it) }
                                    )

                                    1 -> KashidaPanel(
                                        state = canvasState,
                                        onStretchLevelChange = { viewModel.setStretchLevel(it) },
                                        onKashidaModeChange = { viewModel.setKashidaMode(it) },
                                        onTargetIndexChange = { viewModel.setKashidaTargetIndex(it) },
                                        onRotationChange = { viewModel.setRotationAngle(it) },
                                        onRotateStep = { viewModel.rotateStep(it) },
                                        onAlignTop = { viewModel.alignTop() },
                                        onAlignCenter = { viewModel.alignCenter() },
                                        onAlignBottom = { viewModel.alignBottom() }
                                    )

                                    2 -> ShadersColorPanel(
                                        state = canvasState,
                                        onSelectEffect = { viewModel.setEffect(it) },
                                        onSelectCustomColor = { viewModel.setCustomColor(it) }
                                    )

                                    3 -> EffectsDepthPanel(
                                        state = canvasState,
                                        onStrokeWidthChange = { viewModel.setStrokeWidth(it) },
                                        onStrokeColorChange = { viewModel.setStrokeColor(it) },
                                        onShadowRadiusChange = { viewModel.setShadowRadius(it) },
                                        onShadowDyChange = { viewModel.setShadowDy(it) },
                                        onShadowDxChange = { viewModel.setShadowDx(it) },
                                        onShadowColorChange = { viewModel.setShadowColor(it) }
                                    )

                                    else -> CanvasBackgroundPanel(
                                        state = canvasState,
                                        onToggleTransparent = { viewModel.setTransparentBg(it) },
                                        onSelectSolidBgColor = { viewModel.setSolidBgColor(it) },
                                        onPickImage = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        onRemoveImage = {
                                            viewModel.setBackgroundImageUri(null)
                                            Toast.makeText(context, "تمت إزالة صورة الخلفية", Toast.LENGTH_SHORT).show()
                                        },
                                        onDimChange = { viewModel.setBackgroundDim(it) },
                                        onToggleGrayscale = { viewModel.setBackgroundGrayscale(it) },
                                        onAspectRatioChange = { viewModel.setAspectRatio(it) },
                                        onToggleGridGuides = { viewModel.toggleGridGuides() }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. 5-Tool Ergonomic Studio Dock
                StudioDock(
                    selectedTab = selectedTab,
                    isPanelExpanded = isPanelExpanded,
                    onTabSelected = { index ->
                        if (selectedTab == index) {
                            isPanelExpanded = !isPanelExpanded
                        } else {
                            selectedTab = index
                            isPanelExpanded = true
                        }
                    }
                )
            }
        }

        // Modals & BottomSheets
        if (showTextDialog) {
            ArabicTextEditorDialog(
                initialText = canvasState.rawText,
                onDismiss = { showTextDialog = false },
                onApplyText = { viewModel.setText(it) }
            )
        }

        if (showExportSheet) {
            ExportBottomSheet(
                state = canvasState,
                isExporting = isExporting,
                onDismiss = { showExportSheet = false },
                onExportFullCanvas = { res ->
                    viewModel.exportDesign(
                        context,
                        resolution = res,
                        isTransparent = canvasState.isTransparentBg,
                        isStickerTrimmed = false
                    )
                    showExportSheet = false
                },
                onExportSticker = { res ->
                    viewModel.exportDesign(
                        context,
                        resolution = res,
                        isTransparent = true,
                        isStickerTrimmed = true
                    )
                    showExportSheet = false
                },
                onSaveProject = {
                    viewModel.saveCurrentProject(context)
                    showExportSheet = false
                },
                onShareAtharProject = {
                    viewModel.exportProjectAsAthar(context)
                    showExportSheet = false
                }
            )
        }

        if (showProjectsSheet) {
            ProjectsBottomSheet(
                projects = projectsList,
                onDismiss = { showProjectsSheet = false },
                onImportProject = { projectImportLauncher.launch("*/*") },
                onSaveCurrent = { viewModel.saveCurrentProject(context) },
                onNewProject = {
                    viewModel.setText("أَثَــر")
                    viewModel.resetCanvasTransform()
                    viewModel.setBackgroundImageUri(null)
                    showProjectsSheet = false
                },
                onOpenProject = { id ->
                    viewModel.openProjectById(context, id)
                    showProjectsSheet = false
                },
                onDeleteProject = { id ->
                    viewModel.deleteProject(context, id)
                }
            )
        }
    }
}
