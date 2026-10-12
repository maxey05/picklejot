package com.maxeydev.picklejot

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxeydev.picklejot.data.photo.CompressionResult
import com.maxeydev.picklejot.data.photo.ImageCompressor
import com.maxeydev.picklejot.data.photo.MAX_LONG_EDGE_PX
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import kotlin.math.max

private const val SAMPLE_DIRECTORY = "compression-samples"
private const val MINIMUM_SAMPLES = 10
private const val BYTES_PER_KB = 1024

@RunWith(AndroidJUnit4::class)
class CompressionSizeTest {
    @Test
    fun average_compressed_size_of_real_phone_photos_is_measured() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assets = instrumentation.context.assets
        val samples = assets.list(SAMPLE_DIRECTORY).orEmpty().filter { it.endsWith(".jpg", ignoreCase = true) }
        assumeTrue(
            "Put at least $MINIMUM_SAMPLES real phone photos in app/src/androidTest/assets/$SAMPLE_DIRECTORY/",
            samples.size >= MINIMUM_SAMPLES,
        )
        val work = File(instrumentation.targetContext.cacheDir, "compression-size").apply { mkdirs() }
        val compressor = ImageCompressor(openSource = { FileInputStream(it) })
        try {
            val results =
                samples.mapIndexed { index, name ->
                    val source = File(work, "source-$index.jpg")
                    assets.open("$SAMPLE_DIRECTORY/$name").use { input -> source.outputStream().use(input::copyTo) }
                    val sourceBytes = source.length()
                    val result = compressor.compress(source.path, File(work, "out-$index.jpg"))
                    val compressed = result as CompressionResult.Compressed
                    assertTrue(max(compressed.width, compressed.height) <= MAX_LONG_EDGE_PX)
                    sourceBytes to compressed.byteSize
                }
            val averageSourceKb = results.map { it.first }.average() / BYTES_PER_KB
            val averageCompressedKb = results.map { it.second }.average() / BYTES_PER_KB
            val report =
                Bundle().apply {
                    putString(
                        "compression-size",
                        "samples=${results.size} averageSourceKb=${averageSourceKb.toInt()} " +
                            "averageCompressedKb=${averageCompressedKb.toInt()} " +
                            "largestCompressedKb=${results.maxOf { it.second } / BYTES_PER_KB}",
                    )
                }
            instrumentation.sendStatus(0, report)
            println("AC-9.12 ${report.getString("compression-size")}")
        } finally {
            work.deleteRecursively()
        }
    }
}
