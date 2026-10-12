@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.match.edit

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.entitlement.CanAddMatch
import com.maxeydev.picklejot.domain.entitlement.CanAddPhoto
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.entitlement.ProTier
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.photo.PhotoRef
import com.maxeydev.picklejot.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklejot.ui.fakes.FakeLastUsedFormatStore
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.fakes.FakePersonRepository
import com.maxeydev.picklejot.ui.fakes.FakePhotoImportQueue
import com.maxeydev.picklejot.ui.fakes.FixedClock
import com.maxeydev.picklejot.ui.navigation.MATCH_ID_ARGUMENT
import com.maxeydev.picklejot.ui.paywall.UpgradeReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

class MatchEditEntitlementTest {
    private val clock = FixedClock(Instant.parse("2026-09-24T12:30:45Z"))
    private val queue = FakePhotoImportQueue()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun photo(index: Int): PhotoRef = PhotoRef(Uuid.random(), "photos/p$index.jpg", 100, 100, 1_000, index)

    private fun match(photos: List<PhotoRef> = emptyList()): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = AppDate.parse("2026-09-01"),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(0),
            updatedAt = AppInstant.fromEpochMilliseconds(0),
            photos = photos,
        )

    private fun history(count: Int): List<Match> = List(count) { match() }

    private fun refund(entitlements: FakeEntitlementRepository) =
        runBlocking {
            entitlements.record(EntitlementSignal.RefundConfirmed(FakeEntitlementRepository.FAKE_PURCHASE_TOKEN))
        }

    private fun viewModel(
        matches: FakeMatchRepository,
        entitlements: FakeEntitlementRepository,
        handle: SavedStateHandle = SavedStateHandle(),
    ): MatchEditViewModel =
        MatchEditViewModel(
            savedStateHandle = handle,
            matchRepository = matches,
            personRepository = FakePersonRepository(),
            lastUsedFormatStore = FakeLastUsedFormatStore(),
            photoImportQueue = queue,
            canAddMatch = CanAddMatch(matches, entitlements),
            canAddPhoto = CanAddPhoto(entitlements),
            photoFile = { File("/files", it) },
            clock = clock,
            timeZone = { TimeZone.of("Asia/Manila") },
            defaultDispatcher = Dispatchers.Main,
        )

    @Test
    fun `a free user can save match fifty`() {
        val matches = FakeMatchRepository(history(49))
        val viewModel = viewModel(matches, FakeEntitlementRepository())

        viewModel.selectResult(MatchResult.WIN)
        viewModel.save()

        assertEquals(1, matches.saved.size)
        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `match fifty one on free opens the paywall and keeps the in progress match`() {
        val matches = FakeMatchRepository(history(50))
        val viewModel = viewModel(matches, FakeEntitlementRepository())

        viewModel.changeNotes("third game went long")
        viewModel.selectResult(MatchResult.LOSS)
        viewModel.save()

        val state = viewModel.uiState.value
        assertTrue(matches.saved.isEmpty())
        assertTrue(state.isPaywallRequested)
        assertNull(state.upgradePrompt)
        assertFalse(state.isFinished)
        assertFalse(state.isSaving)
        assertEquals("third game went long", state.notes)
        assertEquals(MatchResult.LOSS, state.result)
        assertTrue(state.canSave)
    }

    @Test
    fun `deleting a match frees a slot without restarting`() {
        val existing = history(50)
        val matches = FakeMatchRepository(existing)
        val viewModel = viewModel(matches, FakeEntitlementRepository())
        viewModel.selectResult(MatchResult.WIN)
        viewModel.save()
        viewModel.paywallOpened()

        runBlocking { matches.deleteMatch(existing.first().id) }
        viewModel.save()

        assertEquals(1, matches.saved.size)
        assertTrue(viewModel.uiState.value.isFinished)
        assertFalse(viewModel.uiState.value.isPaywallRequested)
    }

    @Test
    fun `pro saves past fifty`() {
        val matches = FakeMatchRepository(history(200))
        val viewModel = viewModel(matches, FakeEntitlementRepository(isPro = true))

        viewModel.selectResult(MatchResult.WIN)
        viewModel.save()

        assertEquals(1, matches.saved.size)
    }

    @Test
    fun `a revoked user with eighty matches can still edit and save any of them`() {
        val existing = history(80)
        val entitlements = FakeEntitlementRepository(isPro = true)
        refund(entitlements)
        val matches = FakeMatchRepository(existing)
        val target = existing[40]
        val viewModel =
            viewModel(matches, entitlements, SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to target.id.toString())))

        viewModel.changeNotes("edited after refund")
        viewModel.save()

        assertFalse(entitlements.current.isPro)
        assertEquals("edited after refund", matches.saved.single().notes)
        assertEquals(80, matches.current.size)
    }

    @Test
    fun `a free match takes one photo and a second shows the upgrade prompt instead of an error`() {
        val viewModel = viewModel(FakeMatchRepository(), FakeEntitlementRepository())

        viewModel.addPickedPhotos(listOf("content://a", "content://b", "content://c"))

        val state = viewModel.uiState.value
        assertEquals(1, state.photos.size)
        assertEquals(listOf("content://a"), queue.started.map { it.second.uri })
        assertEquals(UpgradeReason.PHOTO_LIMIT, state.upgradePrompt)
        assertFalse(state.hasPhotoError)
    }

    @Test
    fun `a free match that already has its photo refuses another with the prompt`() {
        val viewModel = viewModel(FakeMatchRepository(), FakeEntitlementRepository())
        viewModel.addCapturedPhoto("content://first")
        viewModel.dismissUpgradePrompt()

        viewModel.addCapturedPhoto("content://second")

        assertEquals(1, viewModel.uiState.value.photos.size)
        assertEquals(UpgradeReason.PHOTO_LIMIT, viewModel.uiState.value.upgradePrompt)
    }

    @Test
    fun `pro takes up to ten photos on a match without any prompt`() {
        val viewModel = viewModel(FakeMatchRepository(), FakeEntitlementRepository(isPro = true))

        viewModel.addPickedPhotos(List(ProTier.PHOTOS_PER_MATCH) { "content://$it" })

        assertEquals(ProTier.PHOTOS_PER_MATCH, viewModel.uiState.value.photos.size)
        assertNull(viewModel.uiState.value.upgradePrompt)
    }

    @Test
    fun `pro stops at ten photos and explains the limit without offering an upgrade`() {
        val viewModel = viewModel(FakeMatchRepository(), FakeEntitlementRepository(isPro = true))

        viewModel.addPickedPhotos(List(ProTier.PHOTOS_PER_MATCH + 3) { "content://$it" })

        assertEquals(ProTier.PHOTOS_PER_MATCH, viewModel.uiState.value.photos.size)
        assertEquals(UpgradeReason.PRO_PHOTO_LIMIT, viewModel.uiState.value.upgradePrompt)
    }

    @Test
    fun `a match that kept several photos after a refund shows and saves every one of them`() {
        val photos = List(3) { photo(it) }
        val existing = match(photos)
        val matches = FakeMatchRepository(listOf(existing))
        val entitlements = FakeEntitlementRepository(isPro = true)
        refund(entitlements)
        val viewModel =
            viewModel(matches, entitlements, SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to existing.id.toString())))

        assertEquals(3, viewModel.uiState.value.photos.size)
        viewModel.addPickedPhotos(listOf("content://extra"))
        viewModel.save()

        assertEquals(photos.map { it.id }, matches.saved.single().photos.map { it.id })
        assertEquals(UpgradeReason.PHOTO_LIMIT, viewModel.uiState.value.upgradePrompt)
    }

    @Test
    fun `unlocking pro from the paywall saves the held match immediately`() {
        val matches = FakeMatchRepository(history(50))
        val entitlements = FakeEntitlementRepository()
        val viewModel = viewModel(matches, entitlements)
        viewModel.changeNotes("the one that got away")
        viewModel.selectResult(MatchResult.WIN)
        viewModel.save()
        viewModel.paywallOpened()

        runBlocking {
            entitlements.record(
                EntitlementSignal.PurchaseVerified(FakeEntitlementRepository.FAKE_PURCHASE_TOKEN, clock.instant),
            )
        }

        assertEquals("the one that got away", matches.saved.single().notes)
        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `the held match survives process death and still saves on unlock`() {
        val matches = FakeMatchRepository(history(50))
        val entitlements = FakeEntitlementRepository()
        val handle = SavedStateHandle()
        val first = viewModel(matches, entitlements, handle)
        first.changeNotes("kept across death")
        first.selectResult(MatchResult.LOSS)
        first.save()

        val afterDeath = FakeEntitlementRepository()
        val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })
        val restored = viewModel(matches, afterDeath, restoredHandle)
        runBlocking {
            afterDeath.record(
                EntitlementSignal.PurchaseVerified(FakeEntitlementRepository.FAKE_PURCHASE_TOKEN, clock.instant),
            )
        }

        assertEquals("kept across death", matches.saved.single().notes)
        assertTrue(restored.uiState.value.isFinished)
    }
}
