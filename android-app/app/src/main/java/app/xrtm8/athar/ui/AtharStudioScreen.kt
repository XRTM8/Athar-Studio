package app.xrtm8.athar.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.*
import androidx.activity.result.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.xrtm8.athar.ui.components.*
import app.xrtm8.athar.ui.dialogs.AtharStudioModals
import app.xrtm8.athar.ui.theme.*
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch

@Composable
fun AtharStudioScreen(
    updateManager: AtharUpdateManager,
    viewModel: AtharStudioViewModel = remember { AtharStudioViewModel() },
    onNavigateBackToHome: (() -> Unit)? = null,
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
    val exportMessage by viewModel.exportMessage.collectAsState()
    val customFonts by viewModel.customFonts.collectAsState()
    val hasCopiedStyle by viewModel.hasCopiedStyle.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isPanelExpanded by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showFontBrowser by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showProjectsSheet by remember { mutableStateOf(false) }
    var pendingCropImageUri by remember { mutableStateOf<Uri?>(null) }

    val handleBackNavigation: () -> Unit = {
        when {
            isPreviewMode -> isPreviewMode = false
            showTextDialog -> showTextDialog = false
            showFontBrowser -> showFontBrowser = false
            showExportSheet -> showExportSheet = false
            showProjectsSheet -> showProjectsSheet = false
            isPanelExpanded -> isPanelExpanded = false
            else -> {
                val hasContent = canvasState.layers.any { it.displayText.isNotBlank() } ||
                    canvasState.backgroundImageUri != null ||
                    canvasState.collageState.cells.any { it.imageUri != null }
                if (hasContent && viewModel.hasUnsavedChanges()) {
                    viewModel.saveCurrentProject(context)
                }
                onNavigateBackToHome?.invoke()
            }
        }
    }

    BackHandler(enabled = onNavigateBackToHome != null) { handleBackNavigation() }

    LaunchedEffect(exportMessage) {
        exportMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearExportMessage()
        }
    }
    LaunchedEffect(Unit) { viewModel.loadCustomFonts(context) }

    var pendingCollageCellId by remember { mutableStateOf<String?>(null) }
    var isPickingCustomMask by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val cellId = pendingCollageCellId
            if (isPickingCustomMask) {
                viewModel.setCustomMaskUri(uri)
            } else if (cellId != null) {
                viewModel.setCollageCellPhoto(cellId, uri)
            } else {
                pendingCropImageUri = uri
            }
        }
        pendingCollageCellId = null
        isPickingCustomMask = false
    }

    val launchImagePicker: (String?, Boolean) -> Unit = { cellId, customMask ->
        isPickingCustomMask = customMask; pendingCollageCellId = cellId
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val pickCollagePhoto: (String) -> Unit = { cellId -> launchImagePicker(cellId, false) }

    val projectImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.openProjectFromUri(context, uri)
            showProjectsSheet = false
        }
    }

    val isAmoledWhite = LocalAmoledWhite.current
    val updateInfo by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isAmoledWhite) AmoledWhite else AmoledBlack
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                GentleUpdateBanner(
                    updateInfo = updateInfo,
                    downloadProgress = downloadProgress,
                    onUpdateClick = { url -> coroutineScope.launch { updateManager.downloadAndInstallApk(url) } },
                    onDismissClick = { updateManager.dismissUpdate() }
                )

                // 1. Sleek Top Studio Bar
                AnimatedVisibility(visible = !isPreviewMode, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    TopStudioBar(
                        canUndo = canUndo,
                        canRedo = canRedo,
                        isLocked = canvasState.isLocked,
                        showGrid = canvasState.showGridGuides,
                        onOpenProjects = { viewModel.loadProjectsList(context); showProjectsSheet = true },
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onToggleGrid = { viewModel.toggleGridGuides() },
                        onToggleLock = { viewModel.toggleLock() },
                        onResetTransform = { viewModel.resetCanvasTransform() },
                        onExportClick = { showExportSheet = true },
                        onTogglePreview = { isPreviewMode = true },
                        onBackToHome = handleBackNavigation,
                        onToggleTheme = { viewModel.toggleAmoledWhite(context) }
                    )
                }

                // 2. Floating Multi-Text Layer Selector Strip
                AnimatedVisibility(visible = !isPreviewMode, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)) {
                        TextLayerStrip(
                            state = canvasState,
                            hasCopiedStyle = hasCopiedStyle,
                            onAddLayer = { viewModel.addTextLayer() },
                            onDuplicateLayer = { viewModel.duplicateActiveLayer() },
                            onCopyStyle = { viewModel.copyActiveLayerStyle() },
                            onPasteStyle = { viewModel.pasteStyleToActiveLayer() },
                            onDeleteLayer = { viewModel.removeActiveLayer() },
                            onSelectLayer = { viewModel.selectLayer(it) },
                            onMoveLayerUp = { viewModel.moveActiveLayerUp() },
                            onMoveLayerDown = { viewModel.moveActiveLayerDown() }
                        )
                    }
                }

                // 3. Interactive Typography Canvas Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = if (isPreviewMode) 4.dp else 14.dp, vertical = 2.dp)
                        .clipToBounds()
                ) {
                    CanvasView(
                        state = canvasState,
                        modifier = Modifier.fillMaxSize(),
                        onTransformFinished = { x, y, scale, rot -> viewModel.commitTransform(x, y, scale, rot) },
                        onPhotoFramingFinished = { px, py, pScale -> viewModel.commitPhotoFraming(px, py, pScale) },
                        onCollageCellFramingFinished = { id, x, y, s -> viewModel.commitCollageCellFraming(id, x, y, s) },
                        onSelectLayer = { layerId -> viewModel.selectLayer(layerId) },
                        onSelectCollageCell = { cellId -> viewModel.selectCollageCell(cellId) },
                        onDoubleTap = { showTextDialog = true },
                        onPickCollageCellPhoto = pickCollagePhoto,
                        onToggleFramingMode = { viewModel.toggleFramingMode(it) },
                        onResetCollageCellFraming = { viewModel.resetCollageCellFraming(it) }
                    )
                }

                // 4. Ergonomic Bottom Control Console
                AnimatedVisibility(visible = !isPreviewMode, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                        StudioConsoleDrawer(
                            selectedTab = selectedTab,
                            isPanelExpanded = isPanelExpanded,
                            canvasState = canvasState,
                            viewModel = viewModel,
                            onClosePanel = { isPanelExpanded = false },
                            onOpenTextEditor = { showTextDialog = true },
                            onOpenFontBrowser = { showFontBrowser = true },
                            onPickImage = { launchImagePicker(null, false) },
                            onRemoveImage = { viewModel.setBackgroundImageUri(null); Toast.makeText(context, "تمت إزالة صورة الخلفية", Toast.LENGTH_SHORT).show() },
                            onPickCollagePhoto = pickCollagePhoto,
                            onPickCustomMask = { launchImagePicker(null, true) }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        StudioDock(
                            selectedTab = selectedTab,
                            isPanelExpanded = isPanelExpanded,
                            onTabSelected = { index ->
                                if (selectedTab == index) isPanelExpanded = !isPanelExpanded else { selectedTab = index; isPanelExpanded = true }
                            }
                        )
                    }
                }
            }

            // Floating "إرجاع الأدوات" Button in Clean Preview Mode (Requirement 4)
            PreviewReturnButton(
                visible = isPreviewMode,
                onClick = { isPreviewMode = false }
            )
        }

        // Modals & BottomSheets Helper
        AtharStudioModals(
            context = context, canvasState = canvasState, viewModel = viewModel,
            customFonts = customFonts, projectsList = projectsList, isExporting = isExporting,
            showTextDialog = showTextDialog, onDismissTextDialog = { showTextDialog = false },
            showFontBrowser = showFontBrowser, onDismissFontBrowser = { showFontBrowser = false },
            showExportSheet = showExportSheet, onDismissExportSheet = { showExportSheet = false },
            showProjectsSheet = showProjectsSheet, onDismissProjectsSheet = { showProjectsSheet = false },
            onImportProjectRequest = { projectImportLauncher.launch("*/*") },
            pendingCropImageUri = pendingCropImageUri, onClearCrop = { pendingCropImageUri = null }
        )
    }
}
