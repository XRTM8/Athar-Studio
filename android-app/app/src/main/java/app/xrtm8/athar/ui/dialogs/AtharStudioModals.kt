package app.xrtm8.athar.ui.dialogs

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
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

    if (showExportSheet) {
        ExportBottomSheet(
            state = canvasState,
            isExporting = isExporting,
            onDismiss = onDismissExportSheet,
            onExportFullCanvas = { res ->
                viewModel.exportDesign(context, resolution = res, isTransparent = canvasState.isTransparentBg, isStickerTrimmed = false)
                onDismissExportSheet()
            },
            onExportSticker = { res ->
                viewModel.exportDesign(context, resolution = res, isTransparent = true, isStickerTrimmed = true)
                onDismissExportSheet()
            },
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
