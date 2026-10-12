package com.maxeydev.picklejot.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.SingletonImageLoader
import coil3.request.SuccessResult
import coil3.size.Size
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val THUMBNAIL_TAG = "thumbnail_under_test"
private const val SOURCE_WIDTH = 2048
private const val SOURCE_HEIGHT = 1536
private const val JPEG_QUALITY = 80

@RunWith(AndroidJUnit4::class)
class MatchThumbnailTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun writeFullSizePhoto(): File {
        val file = File(context.cacheDir, "thumbnail-test-source.jpg")
        val bitmap = Bitmap.createBitmap(SOURCE_WIDTH, SOURCE_HEIGHT, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(40, 160, 90))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        return file
    }

    @Test
    fun `the_request_asks_coil_for_the_thumbnail_size_not_the_source_size`() {
        val request = thumbnailRequest(context, File("photos/any.jpg"), 147)

        assertEquals(Size(147, 147), runBlocking { request.sizeResolver.size() })
    }

    @Test
    fun `the_thumbnail_occupies_exactly_its_56dp_box`() {
        var requestedPx = 0
        compose.setContent {
            requestedPx = with(LocalDensity.current) { MATCH_THUMBNAIL_SIZE.roundToPx() }
            MatchThumbnail(path = "/nonexistent/photo.jpg", modifier = Modifier.testTag(THUMBNAIL_TAG))
        }

        compose
            .onNodeWithTag(THUMBNAIL_TAG)
            .assertWidthIsEqualTo(MATCH_THUMBNAIL_SIZE)
            .assertHeightIsEqualTo(MATCH_THUMBNAIL_SIZE)
        assertTrue(requestedPx in 1 until SOURCE_WIDTH)
    }

    @Test
    fun `a_full_size_2048px_photo_is_never_decoded_at_full_size_for_the_list`() {
        val source = writeFullSizePhoto()
        val sizePx = (MATCH_THUMBNAIL_SIZE.value * context.resources.displayMetrics.density).toInt()

        val result =
            runBlocking {
                SingletonImageLoader.get(context).execute(thumbnailRequest(context, source, sizePx))
            }

        assertTrue("Coil failed to decode the photo: $result", result is SuccessResult)
        val image = (result as SuccessResult).image
        assertTrue(
            "decoded ${image.width}x${image.height} for a ${sizePx}px thumbnail",
            maxOf(image.width, image.height) <= 2 * sizePx,
        )
        source.delete()
    }
}
