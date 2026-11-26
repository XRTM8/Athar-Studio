package app.xrtm8.athar.ui.viewmodel

import android.net.Uri
import app.xrtm8.athar.core.CollageLayoutCalculator
import app.xrtm8.athar.model.*

/**
 * Pure state transformations for background photos, collage framing, masks,
 * color grading, color mix, film presets, and optical/glitch effects.
 */
internal object StudioEffectsCollageOps {

    fun updateCollageCell(state: CanvasState, cellId: String, transform: (CollageCell) -> CollageCell): CanvasState {
        val cur = state.collageState
        return state.copy(
            collageState = cur.copy(
                cells = cur.cells.map { if (it.id == cellId) transform(it) else it }
            )
        )
    }

    fun setCollageLayout(state: CanvasState, layout: CollageLayoutType): CanvasState {
        val cur = state.collageState
        val newCells = CollageLayoutCalculator.generateCellsForLayout(layout, cur.cells)
        return state.copy(collageState = cur.copy(layout = layout, cells = newCells, activeCellId = newCells.firstOrNull()?.id))
    }

    fun resetCellFraming(cell: CollageCell, clearPhoto: Boolean = false): CollageCell = cell.copy(
        imageUri = if (clearPhoto) null else cell.imageUri,
        photoOffsetX = 0f,
        photoOffsetY = 0f,
        photoScale = 1.0f,
        photoRotationDeg = 0,
        isFlippedH = false,
        isFlippedV = false
    )

    fun swapCollageCells(state: CanvasState, cellIdA: String, cellIdB: String): CanvasState {
        if (cellIdA == cellIdB) return state
        val cells = state.collageState.cells
        val idxA = cells.indexOfFirst { it.id == cellIdA }
        val idxB = cells.indexOfFirst { it.id == cellIdB }
        if (idxA < 0 || idxB < 0) return state
        val a = cells[idxA]
        val b = cells[idxB]
        val updated = cells.toMutableList()
        updated[idxA] = a.copy(
            imageUri = b.imageUri,
            photoOffsetX = b.photoOffsetX,
            photoOffsetY = b.photoOffsetY,
            photoScale = b.photoScale,
            photoRotationDeg = b.photoRotationDeg,
            isFlippedH = b.isFlippedH,
            isFlippedV = b.isFlippedV
        )
        updated[idxB] = b.copy(
            imageUri = a.imageUri,
            photoOffsetX = a.photoOffsetX,
            photoOffsetY = a.photoOffsetY,
            photoScale = a.photoScale,
            photoRotationDeg = a.photoRotationDeg,
            isFlippedH = a.isFlippedH,
            isFlippedV = a.isFlippedV
        )
        return state.copy(collageState = state.collageState.copy(cells = updated, activeCellId = cellIdB))
    }

    fun fillCollageCellsWithUris(state: CanvasState, uris: List<Uri>): CanvasState {
        if (uris.isEmpty()) return state
        val cur = state.collageState
        if (cur.cells.isEmpty()) return state
        var uriIdx = 0
        val updated = cur.cells.map { cell ->
            if (cell.imageUri == null && uriIdx < uris.size) {
                resetCellFraming(cell).copy(imageUri = uris[uriIdx++])
            } else {
                cell
            }
        }.let { afterEmpty ->
            if (uriIdx < uris.size) {
                afterEmpty.mapIndexed { idx, cell ->
                    if (idx < uris.size) resetCellFraming(cell).copy(imageUri = uris[idx]) else cell
                }
            } else {
                afterEmpty
            }
        }
        return state.copy(collageState = cur.copy(cells = updated))
    }

    fun selectMask(state: CanvasState, maskId: String?): CanvasState {
        val current = state.maskState
        if (maskId == null) {
            return state.copy(maskState = current.copy(isEnabled = false, activeMaskId = null))
        }
        val def = AtharMaskCatalog.getById(maskId)
        return state.copy(
            maskState = current.copy(
                isEnabled = true,
                activeMaskId = maskId,
                customMaskUri = null,
                blendMode = def?.defaultBlendMode ?: current.blendMode,
                opacity = def?.defaultOpacity ?: current.opacity,
                softness = def?.defaultSoftness ?: current.softness,
                warmth = def?.defaultWarmth ?: current.warmth
            )
        )
    }

    fun setCustomMaskUri(state: CanvasState, uri: Uri?): CanvasState {
        val current = state.maskState
        return if (uri == null) {
            state.copy(maskState = current.copy(customMaskUri = null))
        } else {
            state.copy(maskState = current.copy(isEnabled = true, customMaskUri = uri.toString(), activeMaskId = null))
        }
    }

    fun setColorMixChannel(
        state: CanvasState,
        channel: ColorMixChannel,
        hue: Float,
        sat: Float,
        lum: Float
    ): CanvasState {
        val cur = state.colorMixState
        val map = cur.channelAdjustments.toMutableMap()
        map[channel] = ChannelAdjustment(
            hueShift = hue.coerceIn(-50f, 50f),
            satShift = sat.coerceIn(-100f, 100f),
            lumShift = lum.coerceIn(-100f, 100f)
        )
        return state.copy(colorMixState = cur.copy(isEnabled = true, channelAdjustments = map))
    }

    fun resetColorMixChannel(state: CanvasState, channel: ColorMixChannel): CanvasState {
        val cur = state.colorMixState
        val map = cur.channelAdjustments.toMutableMap().apply { remove(channel) }
        return state.copy(colorMixState = cur.copy(channelAdjustments = map))
    }
}
