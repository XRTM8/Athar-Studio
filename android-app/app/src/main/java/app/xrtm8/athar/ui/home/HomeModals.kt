package app.xrtm8.athar.ui.home

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import app.xrtm8.athar.model.CanvasAspectRatio
import app.xrtm8.athar.project.AtharProjectMeta
import app.xrtm8.athar.ui.AtharStudioViewModel
import app.xrtm8.athar.ui.dialogs.AtharSettingsDialog
import app.xrtm8.athar.ui.dialogs.DeleteProjectConfirmDialog
import app.xrtm8.athar.ui.dialogs.ImageCropDialog
import app.xrtm8.athar.ui.dialogs.RenameProjectDialog

@Composable
fun HomeModals(
    context: Context,
    viewModel: AtharStudioViewModel,
    haptic: HapticFeedback,
    selectedAspectRatio: CanvasAspectRatio,
    projectToDelete: AtharProjectMeta?,
    onDismissDelete: () -> Unit,
    projectToRename: AtharProjectMeta?,
    onDismissRename: () -> Unit,
    showSettingsDialog: Boolean,
    onDismissSettings: () -> Unit,
    pendingCropImageUri: Uri?,
    onClearCrop: () -> Unit,
    onNavigateToStudio: () -> Unit
) {
    if (projectToDelete != null) {
        DeleteProjectConfirmDialog(
            projectTitle = projectToDelete.title,
            onConfirmDelete = {
                viewModel.deleteProject(context, projectToDelete.id)
                onDismissDelete()
            },
            onDismiss = onDismissDelete
        )
    }

    if (projectToRename != null) {
        RenameProjectDialog(
            initialTitle = projectToRename.title,
            onConfirmRename = { newTitle ->
                viewModel.renameProject(context, projectToRename.id, newTitle)
                onDismissRename()
            },
            onDismiss = onDismissRename
        )
    }

    if (showSettingsDialog) {
        AtharSettingsDialog(onDismiss = onDismissSettings)
    }

    if (pendingCropImageUri != null) {
        ImageCropDialog(
            sourceUri = pendingCropImageUri,
            onCropSuccess = { croppedUri ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.createNewProjectWithImage(context, croppedUri, selectedAspectRatio)
                onClearCrop()
                onNavigateToStudio()
            },
            onSkipCrop = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.createNewProjectWithImage(context, pendingCropImageUri, selectedAspectRatio)
                onClearCrop()
                onNavigateToStudio()
            },
            onDismiss = onClearCrop
        )
    }
}
