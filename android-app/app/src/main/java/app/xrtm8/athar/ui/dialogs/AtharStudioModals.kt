package app.xrtm8.athar.ui.dialogs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import app.xrtm8.athar.model.ExportResolution
import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.CustomFont
import app.xrtm8.athar.project.AtharProjectMeta
import app.xrtm8.athar.ui.AtharStudioViewModel

@Composable
fun AtharStudioModals(
    context: Context,
    canvasState: CanvasState,
    viewModel: AtharStudioViewModel,
    customFonts: List<CustomFont>,
    projectsList: List<AtharProjectMeta>,
    isExporting: Boolean,
    showTextDialog: Boolean,
    onDismissTextDialog: () -> Unit,
    showFontBrowser: Boolean,
    onDismissFontBrowser: () -> Unit,
    showExportSheet: Boolean,
    onDismissExportSheet: () -> Unit,
    showProjectsSheet: Boolean,
    onDismissProjectsSheet: () -> Unit,
    onImportProjectRequest: () -> Unit,
    pendingCropImageUri: Uri?,
    onClearCrop: () -> Unit
) {
    if (showTextDialog) {
        ArabicTextEditorDialog(
            initialText = canvasState.rawText,
            onDismiss = onDismissTextDialog,
            onApplyText = { viewModel.setText(it) }
        )
    }

    if (showFontBrowser) {
        FontBrowserDialog(
            selectedFont = canvasState.selectedFont,
            customFontPath = canvasState.customFontPath,
            sampleText = canvasState.rawText,
            customFonts = customFonts,
            onSelectFont = { viewModel.setFont(it) },
            onSelectCustomFont = { viewModel.setCustomFont(it) },
            onImportFont = { uri -> viewModel.importCustomFont(context, uri) },
            onDeleteCustomFont = { fontId -> viewModel.deleteCustomFont(context, fontId) },
            onDismiss = onDismissFontBrowser
        )
    }

    var pendingResolution by rememberSaveable { mutableStateOf<ExportResolution?>(null) }
    var pendingSticker by rememberSaveable { mutableStateOf(false) }
    fun export(resolution: ExportResolution, sticker: Boolean) {
        viewModel.exportDesign(context, resolution, sticker || canvasState.isTransparentBg, sticker)
        onDismissExportSheet()
    }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val resolution = pendingResolution
        pendingResolution = null
        if (granted && resolution != null) export(resolution, pendingSticker)
        else if (!granted) Toast.makeText(context, "يلزم إذن التخزين لحفظ الصورة في المعرض", Toast.LENGTH_SHORT).show()
    }
    fun requestExport(resolution: ExportResolution, sticker: Boolean) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingResolution = resolution
            pendingSticker = sticker
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else export(resolution, sticker)
    }
    if (showExportSheet) {
        ExportBottomSheet(
            state = canvasState,
            isExporting = isExporting,
            onDismiss = onDismissExportSheet,
            onExportFullCanvas = { requestExport(it, false) },
            onExportSticker = { requestExport(it, true) },
            onExportPdf = { viewModel.exportPdf(context); onDismissExportSheet() },
            onExportSvg = { viewModel.exportSvg(context); onDismissExportSheet() },
            onSaveProject = { viewModel.saveCurrentProject(context); onDismissExportSheet() },
            onShareAtharProject = { viewModel.exportProjectAsAthar(context); onDismissExportSheet() }
        )
    }

    if (showProjectsSheet) {
        ProjectsBottomSheet(
            projects = projectsList,
            onDismiss = onDismissProjectsSheet,
            onImportProject = onImportProjectRequest,
            onSaveCurrent = { viewModel.saveCurrentProject(context) },
            onNewProject = { viewModel.createNewProject(); onDismissProjectsSheet() },
            onOpenProject = { id -> viewModel.openProjectById(context, id); onDismissProjectsSheet() },
            onShareProject = { id -> viewModel.shareProjectById(context, id) },
            onDeleteProject = { id -> viewModel.deleteProject(context, id) }
        )
    }

    if (pendingCropImageUri != null) {
        ImageCropDialog(
            sourceUri = pendingCropImageUri,
            onCropSuccess = { croppedUri ->
                viewModel.setBackgroundImageUri(croppedUri)
                viewModel.loadExtractedPalette(context)
                onClearCrop()
                Toast.makeText(context, "تم تعيين صورة الخلفية بعد القص", Toast.LENGTH_SHORT).show()
            },
            onSkipCrop = {
                viewModel.setBackgroundImageUri(pendingCropImageUri)
                viewModel.loadExtractedPalette(context)
                onClearCrop()
                Toast.makeText(context, "تم تعيين صورة الخلفية", Toast.LENGTH_SHORT).show()
            },
            onDismiss = onClearCrop
        )
    }
}
