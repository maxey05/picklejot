@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.match.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.entitlement.CanAddMatch
import com.maxeydev.picklejot.domain.entitlement.CanAddPhoto
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.photo.PhotoRef
import com.maxeydev.picklejot.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklejot.ui.fakes.FakeLastUsedFormatStore
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.fakes.FakePersonRepository
import com.maxeydev.picklejot.ui.fakes.FakePhotoImportQueue
import com.maxeydev.picklejot.ui.fakes.FixedClock
import com.maxeydev.picklejot.ui.navigation.MATCH_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MatchEditPhotosTest {
    private val proEntitlement = FakeEntitlementRepository(isPro = true)

    private val clock = FixedClock(Instant.parse("2026-09-24T12:30:45Z"))
    private val queue = FakePhotoImportQueue()
    private lateinit var matches: FakeMatchRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        matches = FakeMatchRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()): MatchEditViewModel =
        MatchEditViewModel(
            savedStateHandle = handle,
            matchRepository = matches,
            personRepository = FakePersonRepository(),
            lastUsedFormatStore = FakeLastUsedFormatStore(),
            photoImportQueue = queue,
            canAddMatch = CanAddMatch(matches, proEntitlement),
            canAddPhoto = CanAddPhoto(proEntitlement),
            photoFile = { File("/files", it) },
            clock = clock,
            timeZone = { TimeZone.of("Asia/Manila") },
            defaultDispatcher = Dispatchers.Main,
        )

    private fun imported(name: String): ImportedPhoto =
        ImportedPhoto(relativePath = "photos/$name.jpg", width = 2048, height = 1536, byteSize = 400_000)

    private fun copyOf(handle: SavedStateHandle): SavedStateHandle =
        SavedStateHandle(handle.keys().associateWith { key -> handle.get<Any?>(key) })

    private fun existing(vararg photoNames: String): Match {
        val match =
            Match(
                id = Uuid.random(),
                format = MatchFormat.SINGLES,
                date = AppDate.parse("2026-09-20"),
                result = MatchResult.WIN,
                createdAt = AppInstant.fromEpochMilliseconds(1_000),
                updatedAt = AppInstant.fromEpochMilliseconds(1_000),
                photos =
                    photoNames.mapIndexed { index, name ->
                        PhotoRef(Uuid.random(), "photos/$name.jpg", 10, 10, 100, index)
                    },
            )
        matches = FakeMatchRepository(listOf(match))
        return match
    }

    @Test
    fun `picked photos start importing and show as in progress without blocking save`() {
        val viewModel = viewModel()
        viewModel.selectResult(MatchResult.WIN)

        viewModel.addPickedPhotos(listOf("content://picker/1", "content://picker/2"))

        val state = viewModel.uiState.value
        assertEquals(2, queue.started.size)
        assertFalse(queue.started.any { it.second.isTemporaryCapture })
        assertTrue(state.photos.all { it.isImporting })
        assertTrue(state.canSave)
    }

    @Test
    fun `a finished import shows the compressed file from internal storage`() {
        val viewModel = viewModel()
        viewModel.addPickedPhotos(listOf("content://picker/1"))

        queue.finish(queue.keyFor("content://picker/1"), imported("one"))

        assertEquals(File("/files", "photos/one.jpg").path, viewModel.uiState.value.photos.single().filePath)
    }

    @Test
    fun `saving keeps finished photos in order and hands unfinished ones to the queue`() {
        val viewModel = viewModel()
        viewModel.selectResult(MatchResult.WIN)
        viewModel.addPickedPhotos(listOf("content://a", "content://b", "content://c"))
        queue.finish(queue.keyFor("content://a"), imported("a"))
        queue.finish(queue.keyFor("content://c"), imported("c"))

        viewModel.save()

        val saved = matches.saved.single()
        assertEquals(listOf("photos/a.jpg", "photos/c.jpg"), saved.photos.map { it.relativePath })
        assertEquals(listOf(0, 1), saved.photos.map { it.sortIndex })
        assertEquals(listOf(saved.id to listOf(queue.keyFor("content://b"))), queue.attached)
        assertEquals(setOf(queue.keyFor("content://a"), queue.keyFor("content://c")), queue.released.toSet())
        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `a captured photo is imported as a temporary capture`() {
        val viewModel = viewModel()

        viewModel.addCapturedPhoto("content://com.maxeydev.picklejot.fileprovider/camera/x.jpg")

        assertTrue(queue.started.single().second.isTemporaryCapture)
    }

    @Test
    fun `an unreadable photo is dropped with a message and the match still saves`() {
        val viewModel = viewModel()
        viewModel.selectResult(MatchResult.LOSS)
        viewModel.addPickedPhotos(listOf("content://broken"))

        queue.fail(queue.keyFor("content://broken"))

        val state = viewModel.uiState.value
        assertTrue(state.hasPhotoError)
        assertTrue(state.photos.isEmpty())
        assertTrue(state.canSave)
        viewModel.dismissPhotoError()
        assertFalse(viewModel.uiState.value.hasPhotoError)
        viewModel.save()
        assertTrue(matches.saved.single().photos.isEmpty())
    }

    @Test
    fun `reordering photos changes which one is primary and persists as an explicit index`() {
        val match = existing("first", "second", "third")
        val viewModel = viewModel(SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to match.id.toString())))
        val thirdKey = match.photos[2].id.toString()

        viewModel.movePhoto(thirdKey, offset = -1)
        viewModel.movePhoto(thirdKey, offset = -1)
        viewModel.save()

        val saved = matches.saved.single()
        assertEquals(
            listOf("photos/third.jpg", "photos/first.jpg", "photos/second.jpg"),
            saved.photos.map { it.relativePath },
        )
        assertEquals(listOf(0, 1, 2), saved.photos.map { it.sortIndex })
    }

    @Test
    fun `removing a saved photo is applied on save not on tap`() {
        val match = existing("keep", "drop")
        val viewModel = viewModel(SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to match.id.toString())))
        val dropped = match.photos[1].id

        viewModel.removePhoto(dropped.toString())

        assertTrue(matches.removedPhotos.isEmpty())
        assertTrue(queue.discarded.isEmpty())
        viewModel.save()
        assertEquals(setOf(dropped), matches.removedPhotos)
        assertEquals(listOf("photos/keep.jpg"), matches.current.single().photos.map { it.relativePath })
    }

    @Test
    fun `removing a new photo discards its import straight away`() {
        val viewModel = viewModel()
        viewModel.addPickedPhotos(listOf("content://a"))
        val key = queue.keyFor("content://a")

        viewModel.removePhoto(key)

        assertEquals(listOf(key), queue.discarded)
        assertTrue(viewModel.uiState.value.photos.isEmpty())
    }

    @Test
    fun `abandoning the screen without saving discards new photos but never saved ones`() {
        val match = existing("saved")
        val handle = SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to match.id.toString()))
        val owner = HoldingStore()
        val viewModel =
            ViewModelProvider(owner, viewModelFactory { initializer { viewModel(handle) } })[
                MatchEditViewModel::class.java,
            ]
        viewModel.addPickedPhotos(listOf("content://new"))
        val newKey = queue.keyFor("content://new")

        owner.viewModelStore.clear()

        assertEquals(listOf(newKey), queue.discarded)
    }

    @Test
    fun `a draft restored after process death resumes its unfinished imports`() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.addPickedPhotos(listOf("content://a"))
        val key = queue.keyFor("content://a")
        val restoredQueue = FakePhotoImportQueue()

        val restored =
            MatchEditViewModel(
                savedStateHandle = copyOf(handle),
                matchRepository = matches,
                personRepository = FakePersonRepository(),
                lastUsedFormatStore = FakeLastUsedFormatStore(),
                photoImportQueue = restoredQueue,
                canAddMatch = CanAddMatch(matches, proEntitlement),
                canAddPhoto = CanAddPhoto(proEntitlement),
                photoFile = { File("/files", it) },
                clock = clock,
                timeZone = { TimeZone.of("Asia/Manila") },
                defaultDispatcher = Dispatchers.Main,
            )

        assertEquals(listOf(key), restoredQueue.started.map { it.first })
        assertEquals("content://a", restoredQueue.started.single().second.uri)
        assertNull(restored.uiState.value.photos.single().filePath)
    }

    private class HoldingStore : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }
}
