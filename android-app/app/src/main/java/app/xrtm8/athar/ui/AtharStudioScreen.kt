package app.xrtm8.athar.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.xrtm8.athar.ui.components.CanvasView
import app.xrtm8.athar.ui.components.GentleUpdateBanner
import app.xrtm8.athar.ui.components.StudioConsoleDrawer
import app.xrtm8.athar.ui.components.StudioDock
import app.xrtm8.athar.ui.components.TextLayerStrip
import app.xrtm8.athar.ui.components.TopStudioBar
import app.xrtm8.athar.ui.dialogs.ArabicTextEditorDialog
import app.xrtm8.athar.ui.dialogs.ExportBottomSheet
import app.xrtm8.athar.ui.dialogs.FontBrowserDialog
import app.xrtm8.athar.ui.dialogs.ProjectsBottomSheet
import app.xrtm8.athar.ui.theme.AmoledBlack
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun AtharStudioScreen(
    updateManager: AtharUpdateManager,
    initialProjectUri: Uri? = null,
    viewModel: AtharStudioViewModel = remember { AtharStudioViewModel() },
    onNavigateBackToHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = onNavigateBackToHome != null) {
        onNavigateBackToHome?.invoke()
    }

    val canvasState by viewModel.canvasState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val projectsList by viewModel.projectsList.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    LaunchedEffect(exportMessage) {
        exportMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearExportMessage()
        }
    }

    val updateInfo by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isPanelExpanded by remember { mutableStateOf(false) }

    var showTextDialog by remember { mutableStateOf(false) }
    var showFontBrowser by remember { mutableStateOf(false) }
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
            viewModel.loadExtractedPalette(context)
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
            // In-app update notification
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
                onExportClick = { showExportSheet = true },
                onBackToHome = onNavigateBackToHome
            )

            // 2. Floating Multi-Text Layer Selector Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                TextLayerStrip(
                    state = canvasState,
                    onAddLayer = { viewModel.addTextLayer() },
                    onDuplicateLayer = { viewModel.duplicateActiveLayer() },
                    onDeleteLayer = { viewModel.removeActiveLayer() },
                    onSelectLayer = { viewModel.selectLayer(it) },
                    onMoveLayerUp = { viewModel.moveActiveLayerUp() },
                    onMoveLayerDown = { viewModel.moveActiveLayerDown() }
                )
            }

            // 3. Interactive Typography Canvas Viewport
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
                    onPhotoFramingFinished = { px, py, pScale ->
                        viewModel.commitPhotoFraming(px, py, pScale)
                    },
                    onSelectLayer = { layerId ->
                        viewModel.selectLayer(layerId)
                    },
                    onDoubleTap = {
                        showTextDialog = true
                    }
                )
            }

            // 4. Ergonomic Bottom Control Console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                // Collapsible Panel Drawer
                StudioConsoleDrawer(
                    selectedTab = selectedTab,
                    isPanelExpanded = isPanelExpanded,
                    canvasState = canvasState,
                    viewModel = viewModel,
                    onClosePanel = { isPanelExpanded = false },
                    onOpenTextEditor = { showTextDialog = true },
                    onOpenFontBrowser = { showFontBrowser = true },
                    onPickImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = {
                        viewModel.setBackgroundImageUri(null)
                        Toast.makeText(context, "تمت إزالة صورة الخلفية", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 5. 6-Tool Ergonomic Studio Dock
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

        if (showFontBrowser) {
            FontBrowserDialog(
                selectedFont = canvasState.selectedFont,
                sampleText = canvasState.rawText,
                onSelectFont = { viewModel.setFont(it) },
                onDismiss = { showFontBrowser = false }
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
                    viewModel.createNewProject()
                    showProjectsSheet = false
                },
                onOpenProject = { id ->
                    viewModel.openProjectById(context, id)
                    showProjectsSheet = false
                },
                onShareProject = { id -> viewModel.shareProjectById(context, id) },
                onDeleteProject = { id -> viewModel.deleteProject(context, id) }
            )
        }
    }
}
