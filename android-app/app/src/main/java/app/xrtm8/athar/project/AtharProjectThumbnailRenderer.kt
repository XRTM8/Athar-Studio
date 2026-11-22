package app.xrtm8.athar.project

import android.content.Context
import android.graphics.Bitmap
import app.xrtm8.athar.export.AtharExportManager
import app.xrtm8.athar.model.CanvasState

object AtharProjectThumbnailRenderer {
    fun renderThumbnail(context: Context, state: CanvasState): Bitmap =
        AtharExportManager.renderDesign(context, state, 480, state.isTransparentBg)
}
