package com.maxeydev.picklejot.data.photo

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import kotlin.math.max

@RunWith(AndroidJUnit4::class)
class ImageCompressorTest {
    private lateinit var root: File
    private val compressor = ImageCompressor(openSource = { path -> FileInputStream(path) })

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "compressor-test-${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun compress(source: File): Pair<CompressionResult, File> {
        val target = File(root, "out/${source.name}")
        return compressor.compress(source.path, target) to target
    }

    @Test
    fun a_large_photo_is_reduced_to_a_2048_longest_edge_jpeg() {
        val (result, target) = compress(writeTestJpeg(File(root, "big.jpg"), width = 4032, height = 3024))

        val compressed = result as CompressionResult.Compressed
        assertEquals(2048, max(compressed.width, compressed.height))
        assertEquals(1536, compressed.height)
        val decoded = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.path, decoded)
        assertEquals("image/jpeg", decoded.outMimeType)
        assertEquals(compressed.width, decoded.outWidth)
        assertEquals(compressed.height, decoded.outHeight)
    }

    @Test
    fun recorded_size_and_dimensions_come_from_the_compressed_file_not_the_source() {
        val source = writeTestJpeg(File(root, "source.jpg"), width = 3000, height = 2000)

        val (result, target) = compress(source)

        val compressed = result as CompressionResult.Compressed
        assertEquals(target.length(), compressed.byteSize)
        assertTrue(compressed.byteSize != source.length())
        assertEquals(2048 to 1365, compressed.width to compressed.height)
    }

    @Test
    fun a_small_photo_keeps_its_size() {
        val (result, _) = compress(writeTestJpeg(File(root, "small.jpg"), width = 800, height = 600))

        assertEquals(800 to 600, (result as CompressionResult.Compressed).let { it.width to it.height })
    }

    @Test
    fun a_rotated_camera_photo_is_stored_upright() {
        val source =
            writeTestJpeg(
                File(root, "rotated.jpg"),
                width = 4000,
                height = 3000,
                orientation = ExifInterface.ORIENTATION_ROTATE_90,
            )

        val compressed = compress(source).first as CompressionResult.Compressed

        assertEquals(1536 to 2048, compressed.width to compressed.height)
    }

    @Test
    fun a_corrupt_file_is_reported_as_unreadable_and_leaves_nothing_behind() {
        val corrupt = File(root, "corrupt.jpg").apply { writeBytes(ByteArray(512) { it.toByte() }) }

        val (result, target) = compress(corrupt)

        assertEquals(CompressionResult.Unreadable, result)
        assertFalse(target.exists())
    }

    @Test
    fun a_missing_source_is_reported_as_unreadable() {
        val (result, _) = compress(File(root, "never-existed.jpg"))

        assertEquals(CompressionResult.Unreadable, result)
    }

    @Test
    fun deleting_the_original_leaves_the_stored_copy_intact() {
        val original = writeTestJpeg(File(root, "gallery/original.jpg"), width = 2500, height = 2500)
        val (result, stored) = compress(original)

        assertTrue(original.delete())

        assertTrue(result is CompressionResult.Compressed)
        assertTrue(stored.exists())
        val decoded = BitmapFactory.decodeFile(stored.path)
        assertEquals(2048, decoded.width)
    }
}
