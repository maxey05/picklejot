@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.CARD_CACHE_DIRECTORY
import com.maxeydev.picklejot.ui.common.shareableUriFor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val CARD_MIME_TYPE = "image/jpeg"
private const val CARD_JPEG_QUALITY = 95

class ShareIntentLauncher(
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun prepare(
        context: Context,
        card: Bitmap,
    ): Intent {
        val file = withContext(ioDispatcher) { writeCard(context, card) }
        val uri = shareableUriFor(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = CARD_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun launch(
        context: Context,
        send: Intent,
    ) {
        val chooser = Intent.createChooser(send, context.getString(R.string.share_chooser_title))
        context.startActivity(chooser)
    }

    private fun writeCard(
        context: Context,
        card: Bitmap,
    ): File {
        val directory = File(context.cacheDir, CARD_CACHE_DIRECTORY).apply { mkdirs() }
        directory.listFiles().orEmpty().forEach { it.delete() }
        val file = File(directory, "picklejot-card-${Uuid.random()}.jpg")
        file.outputStream().use { output ->
            if (!card.compress(Bitmap.CompressFormat.JPEG, CARD_JPEG_QUALITY, output)) {
                throw IOException("The card image could not be written.")
            }
        }
        return file
    }
}
