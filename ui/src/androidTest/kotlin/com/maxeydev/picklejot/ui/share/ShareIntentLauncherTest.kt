package com.maxeydev.picklejot.ui.share

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.ui.common.CARD_CACHE_DIRECTORY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShareIntentLauncherTest {
    private val launcher = ShareIntentLauncher(Dispatchers.IO)

    private fun card(): Bitmap =
        Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

    @Test
    fun the_card_is_shared_as_a_readable_content_uri_through_the_app_file_provider() {
        val intent = runBlocking { launcher.prepare(targetContext, card()) }

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals(CARD_MIME_TYPE, intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = requireNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals("content", uri.scheme)
        assertEquals("${targetContext.packageName}.fileprovider", uri.authority)
        val bytes = targetContext.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertTrue(bytes.size > 100)
        assertEquals(uri, intent.clipData?.getItemAt(0)?.uri)
    }

    @Test
    fun repeated_shares_leave_only_the_latest_card_file() {
        repeat(5) { runBlocking { launcher.prepare(targetContext, card()) } }

        val files = File(targetContext.cacheDir, CARD_CACHE_DIRECTORY).listFiles().orEmpty()
        assertEquals(1, files.size)
    }
}
