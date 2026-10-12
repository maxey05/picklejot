@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.photo

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface PhotoImportQueue {
    fun ensureStarted(
        key: String,
        source: PhotoSource,
    )

    fun observe(key: String): Flow<PhotoImportState>

    fun discard(key: String)

    fun release(key: String)

    fun attachWhenReady(
        matchId: Uuid,
        keys: List<String>,
    )
}
