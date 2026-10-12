@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.edit

import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.photo.PhotoRef
import com.maxeydev.picklejot.domain.photo.PhotoSource
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class PhotoDraft(
    val key: String,
    val isPersisted: Boolean,
    val sourceUri: String? = null,
    val isTemporaryCapture: Boolean = false,
    val relativePath: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val byteSize: Long = 0,
) {
    val isReady: Boolean
        get() = relativePath != null

    val source: PhotoSource?
        get() = sourceUri?.let { PhotoSource(uri = it, isTemporaryCapture = isTemporaryCapture) }

    fun withImported(photo: ImportedPhoto): PhotoDraft =
        copy(
            relativePath = photo.relativePath,
            width = photo.width,
            height = photo.height,
            byteSize = photo.byteSize,
        )

    fun toPhotoRef(sortIndex: Int): PhotoRef =
        PhotoRef(
            id = Uuid.parse(key),
            relativePath = requireNotNull(relativePath) { "Only a finished photo can be saved with its match." },
            width = width,
            height = height,
            byteSize = byteSize,
            sortIndex = sortIndex,
        )

    companion object {
        fun importing(source: PhotoSource): PhotoDraft =
            PhotoDraft(
                key = Uuid.random().toString(),
                isPersisted = false,
                sourceUri = source.uri,
                isTemporaryCapture = source.isTemporaryCapture,
            )

        fun persisted(photo: PhotoRef): PhotoDraft =
            PhotoDraft(
                key = photo.id.toString(),
                isPersisted = true,
                relativePath = photo.relativePath,
                width = photo.width,
                height = photo.height,
                byteSize = photo.byteSize,
            )
    }
}
