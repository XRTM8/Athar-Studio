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
            initialSpans = canvasState.activeLayer.textSpans,
            onDismiss = onDismissTextDialog,
            onApplyText = { viewModel.setText(it) },
            onApplyTextWithSpans = { text, spans -> viewModel.setTextWithSpans(text, spans) }
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

    var pendingExportType by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingResolution by rememberSaveable { mutableStateOf<ExportResolution?>(null) }
    var pendingSticker by rememberSaveable { mutableStateOf(false) }
    var pendingShare by rememberSaveable { mutableStateOf(false) }

    fun executePendingExport(type: String, resolution: ExportResolution?, sticker: Boolean, share: Boolean) {
        when (type) {
            "RASTER" -> {
                val res = resolution ?: ExportResolution.HD_1080P
                viewModel.exportDesign(context, res, sticker || canvasState.isTransparentBg, sticker, share)
            }
            "PDF" -> viewModel.exportPdf(context, share)
            "SVG" -> viewModel.exportSvg(context, share)
        }
        onDismissExportSheet()
    }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val type = pendingExportType
        val resolution = pendingResolution
        val share = pendingShare
        pendingExportType = null
        pendingResolution = null
        pendingShare = false
        if (granted && type != null) executePendingExport(type, resolution, pendingSticker, share)
        else if (!granted) Toast.makeText(context, "يلزم إذن التخزين لحفظ الملف", Toast.LENGTH_SHORT).show()
    }

    fun requestStorageExport(type: String, resolution: ExportResolution? = null, sticker: Boolean = false, share: Boolean = false) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingExportType = type
            pendingResolution = resolution
            pendingSticker = sticker
            pendingShare = share
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            executePendingExport(type, resolution, sticker, share)
        }
    }

    if (showExportSheet) {
        ExportBottomSheet(
            state = canvasState,
            isExporting = isExporting,
            onDismiss = onDismissExportSheet,
            onExportFullCanvas = { res, share -> requestStorageExport("RASTER", res, false, share) },
            onExportSticker = { res, share -> requestStorageExport("RASTER", res, true, share) },
            onExportPdf = { share -> requestStorageExport("PDF", share = share) },
            onExportSvg = { share -> requestStorageExport("SVG", share = share) },
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
            onNewProject = {
                val hasContent = canvasState.layers.any { it.displayText.isNotBlank() } ||
                    canvasState.backgroundImageUri != null ||
                    canvasState.collageState.cells.any { it.imageUri != null }
                if (hasContent && viewModel.hasUnsavedChanges()) {
                    viewModel.saveCurrentProject(context)
                }
                viewModel.createNewProject()
                onDismissProjectsSheet()
            },
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
