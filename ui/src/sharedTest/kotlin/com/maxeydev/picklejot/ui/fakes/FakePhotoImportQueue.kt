@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.photo.PhotoImportQueue
import com.maxeydev.picklejot.domain.photo.PhotoImportState
import com.maxeydev.picklejot.domain.photo.PhotoSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FakePhotoImportQueue : PhotoImportQueue {
    private val states = mutableMapOf<String, MutableStateFlow<PhotoImportState>>()

    val started: MutableList<Pair<String, PhotoSource>> = CopyOnWriteArrayList()
    val discarded: MutableList<String> = CopyOnWriteArrayList()
    val released: MutableList<String> = CopyOnWriteArrayList()
    val attached: MutableList<Pair<Uuid, List<String>>> = CopyOnWriteArrayList()

    override fun ensureStarted(
        key: String,
        source: PhotoSource,
    ) {
        if (states.containsKey(key)) {
            return
        }
        started += key to source
        states[key] = MutableStateFlow(PhotoImportState.Importing)
    }

    override fun observe(key: String): Flow<PhotoImportState> = states[key] ?: flowOf(PhotoImportState.Failed)

    override fun discard(key: String) {
        discarded += key
        states.remove(key)
    }

    override fun release(key: String) {
        released += key
        states.remove(key)
    }

    override fun attachWhenReady(
        matchId: Uuid,
        keys: List<String>,
    ) {
        if (keys.isNotEmpty()) {
            attached += matchId to keys
        }
    }

    fun finish(
        key: String,
        photo: ImportedPhoto,
    ) {
        requireNotNull(states[key]) { "No import was started for $key." }.value = PhotoImportState.Ready(photo)
    }

    fun fail(key: String) {
        requireNotNull(states[key]) { "No import was started for $key." }.value = PhotoImportState.Failed
    }

    fun keyFor(uri: String): String = started.first { it.second.uri == uri }.first
}
