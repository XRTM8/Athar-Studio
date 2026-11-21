package app.xrtm8.athar.ui

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.project.AtharProjectMeta
import app.xrtm8.athar.ui.components.GentleUpdateBanner
import app.xrtm8.athar.ui.home.*
import app.xrtm8.athar.ui.theme.*
import app.xrtm8.athar.updater.AtharUpdateManager
import kotlinx.coroutines.launch

/**
 * Athar Home Screen (Clean Orchestrator).
 * Decomposed into small, focused UI units in app.xrtm8.athar.ui.home.*:
 * - HomeHeader
 * - HomeHeroCard
 * - HomeActionCards
 * - homeRecentProjectsSection
 */
@Composable
fun AtharHomeScreen(
    viewModel: AtharStudioViewModel,
    updateManager: AtharUpdateManager,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val projectsList by viewModel.projectsList.collectAsState()
    val updateInfo by updateManager.updateState.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "اضغط مرة أخرى للخروج من أثر", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedAspectRatio by remember { mutableStateOf(CanvasAspectRatio.STORY_9_16) }
    var projectToDelete by remember { mutableStateOf<AtharProjectMeta?>(null) }
    var projectToRename by remember { mutableStateOf<AtharProjectMeta?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var pendingCropImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProjectsList(context)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCropImageUri = uri
        }
    }

    val projectImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.openProjectFromUri(context, uri)
            onNavigateToStudio()
        }
    }

    val isAmoledWhite = LocalAmoledWhite.current
    val primaryTextColor = if (isAmoledWhite) DarkText else PureWhite
    val secondaryTextColor = if (isAmoledWhite) DarkTextMuted else MutedWhite
    val subtleTextColor = if (isAmoledWhite) DarkTextSubtle else SubtleGray
    val cardBackground = if (isAmoledWhite) WhiteSurface else StudioSurface
    val cardBorder = if (isAmoledWhite) WhiteBorder else StudioBorder

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isAmoledWhite) AmoledWhite else AmoledBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. In-App Update Banner
            item {
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
            }

            // 2. Header & Theme Switcher
            item {
                HomeHeader(
                    isAmoledWhite = isAmoledWhite,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    cardBorder = cardBorder,
                    onToggleTheme = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleAmoledWhite(context)
                    },
                    onOpenSettings = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showSettingsDialog = true
                    }
                )
            }

            // 3. Hero Card: Create Blank New Project
            item {
                HomeHeroCard(
                    isAmoledWhite = isAmoledWhite,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    subtleTextColor = subtleTextColor,
                    cardBorder = cardBorder,
                    selectedAspectRatio = selectedAspectRatio,
                    onSelectAspectRatio = { selectedAspectRatio = it },
                    onStartDesign = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.createNewProject(selectedAspectRatio, initialText = "")
                        onNavigateToStudio()
                    }
                )
            }

            // 4. Action Cards: Design on Photo & Import Project
            item {
                HomeActionCards(
                    cardBackground = cardBackground,
                    cardBorder = cardBorder,
                    primaryTextColor = primaryTextColor,
                    subtleTextColor = subtleTextColor,
                    onDesignOnPhotoClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onImportProjectClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        projectImportLauncher.launch("*/*")
                    }
                )
            }

            // 5. Recent Projects Section
            homeRecentProjectsSection(
                projectsList = projectsList,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                subtleTextColor = subtleTextColor,
                onProjectClick = { proj ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.openProjectById(context, proj.id)
                    onNavigateToStudio()
                },
                onProjectShare = { proj ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.shareProjectById(context, proj.id)
                },
                onProjectRename = { proj ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    projectToRename = proj
                },
                onProjectDelete = { proj ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    projectToDelete = proj
                }
            )

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        HomeModals(
            context = context, viewModel = viewModel, haptic = haptic,
            selectedAspectRatio = selectedAspectRatio,
            projectToDelete = projectToDelete, onDismissDelete = { projectToDelete = null },
            projectToRename = projectToRename, onDismissRename = { projectToRename = null },
            showSettingsDialog = showSettingsDialog, onDismissSettings = { showSettingsDialog = false },
            pendingCropImageUri = pendingCropImageUri, onClearCrop = { pendingCropImageUri = null },
            onNavigateToStudio = onNavigateToStudio
        )
    }
}
