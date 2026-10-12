@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.photo

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ImportedPhoto(
    val relativePath: String,
    val width: Int,
    val height: Int,
    val byteSize: Long,
) {
    init {
        require(relativePath.isNotBlank()) { "An imported photo needs a path inside app storage." }
        require(width > 0 && height > 0) { "An imported photo needs positive dimensions, but was ${width}x$height." }
        require(byteSize > 0) { "An imported photo cannot be empty." }
    }

    fun toPhotoRef(
        id: Uuid,
        sortIndex: Int,
    ): PhotoRef =
        PhotoRef(
            id = id,
            relativePath = relativePath,
            width = width,
            height = height,
            byteSize = byteSize,
            sortIndex = sortIndex,
        )
}
