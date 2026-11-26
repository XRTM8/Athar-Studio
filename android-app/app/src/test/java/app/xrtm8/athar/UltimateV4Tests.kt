package app.xrtm8.athar

import app.xrtm8.athar.model.CanvasState
import app.xrtm8.athar.model.ShapeLayerConfig
import app.xrtm8.athar.model.ShapeType
import app.xrtm8.athar.model.TextLayer
import app.xrtm8.athar.project.AtharProjectSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UltimateV4Tests {

    @Test
    fun shapeLayerRoundTripsThroughSerializer() {
        val shape = TextLayer(
            id = "shape_1",
            name = "إطار",
            shapeConfig = ShapeLayerConfig(
                type = ShapeType.FRAME,
                widthRatio = 0.8f,
                heightRatio = 0.7f,
                cornerRadius = 16f,
                strokeWidth = 3f,
                fillColorLong = 0xFFE5C07BL,
                strokeColorLong = 0xFF9E1B32L,
                isFilled = false,
                isStroked = true
            )
        )
        val state = CanvasState(layers = listOf(shape), activeLayerId = shape.id)
        val json = AtharProjectSerializer.serializeCanvasState(state, hasBackgroundPhoto = false)
        val restored = AtharProjectSerializer.deserializeCanvasState(json)
        val rs = restored.layers.first().shapeConfig
        assertTrue(rs.isShape)
        assertEquals(ShapeType.FRAME, rs.type)
        assertEquals(0.8f, rs.widthRatio, 0.001f)
        assertEquals(16f, rs.cornerRadius, 0.001f)
        assertEquals(0xFF9E1B32L, rs.strokeColorLong)
    }

    @Test
    fun collage2LayoutsAndSvgShapeExportWorkCorrectly() {
        val mosaicCells = app.xrtm8.athar.core.CollageLayoutCalculator.generateCellsForLayout(
            app.xrtm8.athar.model.CollageLayoutType.MAGAZINE_MOSAIC
        )
        assertEquals(5, mosaicCells.size)
        val bentoCells = app.xrtm8.athar.core.CollageLayoutCalculator.generateCellsForLayout(
            app.xrtm8.athar.model.CollageLayoutType.BENTO_SIX
        )
        assertEquals(6, bentoCells.size)

        val shape = TextLayer(
            id = "svg_rect_1",
            shapeConfig = ShapeLayerConfig(
                type = ShapeType.RECTANGLE,
                widthRatio = 0.5f,
                heightRatio = 0.2f,
                fillColorLong = 0xFFE5C07BL,
                isFilled = true
            )
        )
        val svg = app.xrtm8.athar.export.AtharSvgExporter.generateSvgString(
            CanvasState(layers = listOf(shape), activeLayerId = shape.id)
        )
        assertTrue(svg.contains("<rect"))
    }
}
