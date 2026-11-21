package app.xrtm8.athar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.content.ContentUris
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import app.xrtm8.athar.export.AtharExportManager
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.xrtm8.athar.core.PureArabicKashidaEngine
import app.xrtm8.athar.model.*
import app.xrtm8.athar.project.AtharProjectManager
import app.xrtm8.athar.typography.AtharFontManager
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidReleaseRegressionTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun eachBundledFontVisiblyExtendsMarkedAndUnmarkedWords() {
        for (font in ArabicFontFamily.entries) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.createFromAsset(context.assets, font.assetPath)
                textSize = 100f
            }
            for (word in listOf("أثر", "مَجْد")) {
                var previous = paint.measureText(word)
                for (level in listOf(2, 4, 8)) {
                    val stretched = PureArabicKashidaEngine.stretchText(word, level)
                    val width = paint.measureText(stretched)
                    assertTrue("${font.name} $word at $level measured $width after $previous", width > previous)
                    previous = width
                }
            }
        }
    }

    @Test
    fun renderedStickerPreservesOpacityAndGrowsWithStretch() {
        val layer = TextLayer(rawText = "أثر", displayText = "أثر", textOpacity = 0.5f,
            selectedFont = ArabicFontFamily.AREF_RUQAA, shadowRadius = 0f, customTextColorLong = 0xFFFFFFFF)
        fun inkWidth(textLayer: TextLayer): Int {
            val state = CanvasState(layers = listOf(textLayer), isTransparentBg = true)
            val bitmap = AtharExportManager.renderDesign(context, state, 380, true)
            try {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                assertEquals(127, pixels.maxOf { Color.alpha(it) })
                val ink = pixels.indices.filter { Color.alpha(pixels[it]) > 16 }
                return ink.maxOf { it % bitmap.width } - ink.minOf { it % bitmap.width }
            } finally {
                bitmap.recycle()
            }
        }
        val originalWidth = inkWidth(layer)
        assertTrue(inkWidth(layer.copy(displayText = "أثــــر")) > originalWidth)
        assertTrue(inkWidth(layer.copy(displayText = "أثــــر", isCurved = true, curveAngle = 12f)) > originalWidth)
    }

    @Test
    fun galleryContainsDecodableFullCanvasAndCroppedSticker() = runBlocking {
        if (Build.VERSION.SDK_INT < 29) {
            // The test queries the legacy gallery to inspect the exported file.
            for (permission in listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                val grant = "pm grant ${context.packageName} $permission"
                val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(grant)
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            }
        }
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val resolver = context.contentResolver
        val layer = TextLayer(rawText = "أثر", displayText = "أثــــر")
        val state = CanvasState(layers = listOf(layer), aspectRatio = CanvasAspectRatio.SQUARE_1_1)
        for (sticker in listOf(false, true)) {
            val started = System.currentTimeMillis()
            AtharExportManager.exportDesign(context, state, isTransparent = sticker, isStickerTrimmed = sticker).getOrThrow()
            val mime = if (sticker) "image/png" else "image/jpeg"
            val cursor = requireNotNull(resolver.query(collection, arrayOf("_id", "_display_name"),
                "mime_type = ?", arrayOf(mime), "_id DESC"))
            val uri = cursor.use {
                assertTrue(it.moveToFirst())
                val timestamp = it.getString(1).substringAfter("Athar_").substringBefore('.').toLong()
                assertTrue(timestamp >= started)
                ContentUris.withAppendedId(collection, it.getLong(0))
            }
            try {
                val bitmap = resolver.openInputStream(uri).use { requireNotNull(BitmapFactory.decodeStream(it)) }
                if (sticker) {
                    assertTrue(bitmap.width in 1 until 1080)
                    assertTrue(bitmap.height in 1 until 1080)
                    assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
                } else {
                    assertEquals(1080, bitmap.width)
                    assertEquals(1080, bitmap.height)
                }
                bitmap.recycle()
            } finally {
                resolver.delete(uri, null, null)
            }
        }
    }

    @Test
    fun invalidFontReplacementLeavesTheImportedFontIntact() {
        val dir = File(context.cacheDir, "font-test").apply { mkdirs() }
        val source = File(dir, "sample.ttf")
        context.assets.open(ArabicFontFamily.AMIRI.assetPath).use { input ->
            source.outputStream().use { input.copyTo(it) }
        }
        val font = requireNotNull(AtharFontManager.importFontFromUri(context, Uri.fromFile(source)))
        try {
            val original = File(font.filePath).readBytes()
            source.writeText("invalid font")
            assertNull(AtharFontManager.importFontFromUri(context, Uri.fromFile(source)))
            assertArrayEquals(original, File(font.filePath).readBytes())
        } finally {
            AtharFontManager.deleteCustomFont(context, font.id)
            source.delete()
        }
    }

    @Test
    fun renamedProjectRestoresBackgroundAndCollageAfterSourceIsGone() = runBlocking {
        val photo = File.createTempFile("source-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val original = photo.readBytes()
        val uri = Uri.fromFile(photo)
        val cell = CollageCell("left", uri, 0f, 0f, 0.5f, 1f)
        val state = CanvasState(backgroundImageUri = uri, collageState = CollageState(CollageLayoutType.DIPTYCH_V, listOf(cell)))
        val project = AtharProjectManager.saveProject(context, state, "before")
        try {
            assertTrue(photo.delete())
            assertTrue(AtharProjectManager.renameProject(context, project.id, "after"))
            assertEquals("after", AtharProjectManager.listProjects(context).single { it.id == project.id }.title)
            val restored = AtharProjectManager.loadProjectById(context, project.id)
            for (image in listOf(restored.backgroundImageUri, restored.collageState.cells.single().imageUri)) {
                val file = File(requireNotNull(image?.path))
                assertArrayEquals(original, file.readBytes())
                file.delete()
            }
        } finally {
            AtharProjectManager.deleteProject(context, project.id)
            photo.delete()
        }
        Unit
    }
}
