@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.photo

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class PhotoRef(
    val id: Uuid,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val byteSize: Long,
    val sortIndex: Int,
)
