@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.photo

import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.photo.PhotoImportQueue
import com.maxeydev.picklejot.domain.photo.PhotoImportState
import com.maxeydev.picklejot.domain.photo.PhotoSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Full-size decodes are memory heavy, so only this many run at once; the rest wait in the queue. */
const val MAX_CONCURRENT_DECODES = 2

class QueuedPhotoImporter(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val compressor: ImageCompressor,
    private val photoStore: PhotoStore,
    private val matchRepository: MatchRepository,
    private val deleteTemporaryCapture: (String) -> Unit,
) : PhotoImportQueue {
    private class Import(
        val relativePath: String,
        val state: MutableStateFlow<PhotoImportState>,
        var job: Job? = null,
    )

    private val imports = mutableMapOf<String, Import>()
    private val lock = Any()
    private val decodePermits = Semaphore(MAX_CONCURRENT_DECODES)

    override fun ensureStarted(
        key: String,
        source: PhotoSource,
    ) {
        val started =
            synchronized(lock) {
                if (imports.containsKey(key)) {
                    null
                } else {
                    Import(photoStore.newPhotoPath(), MutableStateFlow(PhotoImportState.Importing))
                        .also { imports[key] = it }
                }
            } ?: return
        // ATOMIC: a discard that lands before the job first runs must still reach importFrom's cleanup.
        started.job =
            scope.launch(ioDispatcher, start = CoroutineStart.ATOMIC) {
                started.state.value = importFrom(source, started.relativePath)
            }
    }

    override fun observe(key: String): Flow<PhotoImportState> =
        synchronized(lock) { imports[key]?.state } ?: flowOf(PhotoImportState.Failed)

    override fun discard(key: String) {
        val removed = synchronized(lock) { imports.remove(key) } ?: return
        removed.job?.cancel()
        scope.launch(ioDispatcher) {
            removed.job?.join()
            photoStore.deletePhotoFiles(listOf(removed.relativePath))
        }
    }

    override fun release(key: String) {
        synchronized(lock) { imports.remove(key) }
    }

    override fun attachWhenReady(
        matchId: Uuid,
        keys: List<String>,
    ) {
        if (keys.isEmpty()) {
            return
        }
        val pending = synchronized(lock) { keys.mapNotNull { key -> imports[key]?.let { key to it } } }
        scope.launch(ioDispatcher) {
            pending.forEach { (key, import) ->
                val finished = import.state.first { it !is PhotoImportState.Importing }
                if (finished is PhotoImportState.Ready) {
                    val attached = matchRepository.appendPhoto(matchId, finished.photo)
                    if (!attached) {
                        photoStore.deletePhotoFiles(listOf(finished.photo.relativePath))
                    }
                }
                release(key)
            }
        }
    }

    private suspend fun importFrom(
        source: PhotoSource,
        relativePath: String,
    ): PhotoImportState {
        val target: File = photoStore.resolve(relativePath)
        val completed =
            try {
                decodePermits.withPermit { compressor.compress(source.uri, target) }
            } finally {
                if (source.isTemporaryCapture) {
                    deleteTemporaryCapture(source.uri)
                }
            }
        return when (completed) {
            is CompressionResult.Compressed ->
                PhotoImportState.Ready(
                    ImportedPhoto(
                        relativePath = relativePath,
                        width = completed.width,
                        height = completed.height,
                        byteSize = completed.byteSize,
                    ),
                )
            CompressionResult.Unreadable -> PhotoImportState.Failed
        }
    }
}
