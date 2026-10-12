@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val CAMERA_CACHE_DIRECTORY = "camera"
const val CARD_CACHE_DIRECTORY = "cards"
private const val STALE_CAPTURE_MILLIS = 60L * 60 * 1000

fun fileProviderAuthority(context: Context): String = "${context.packageName}.fileprovider"

fun shareableUriFor(
    context: Context,
    file: File,
): Uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)

fun createCaptureUri(
    context: Context,
    nowMillis: Long,
): String {
    val directory = File(context.cacheDir, CAMERA_CACHE_DIRECTORY).apply { mkdirs() }
    directory
        .listFiles()
        .orEmpty()
        .filter { it.lastModified() < nowMillis - STALE_CAPTURE_MILLIS }
        .forEach { it.delete() }
    val capture = File(directory, "${Uuid.random()}.jpg")
    return shareableUriFor(context, capture).toString()
}
