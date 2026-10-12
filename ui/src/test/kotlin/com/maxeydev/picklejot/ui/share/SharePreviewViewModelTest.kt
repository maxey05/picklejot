@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.share

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.photo.PhotoRef
import com.maxeydev.picklejot.domain.share.CardDetail
import com.maxeydev.picklejot.domain.share.CardFormat
import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme
import com.maxeydev.picklejot.ui.fakes.FakeCardFormatStore
import com.maxeydev.picklejot.ui.fakes.FakeCardRenderer
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.fakes.FakeProfileRepository
import com.maxeydev.picklejot.ui.navigation.MATCH_ID_ARGUMENT
import com.maxeydev.picklejot.ui.paywall.UpgradeReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SharePreviewViewModelTest {
    private val renderer = FakeCardRenderer()
    private val formats = FakeCardFormatStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun match(
        date: String,
        photos: List<PhotoRef> = emptyList(),
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
        games: List<GameScore> = emptyList(),
        location: String? = null,
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = AppDate.parse(date),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(0),
            updatedAt = AppInstant.fromEpochMilliseconds(0),
            photos = photos,
            opponents = opponents,
            partner = partner,
            games = games,
            location = location,
        )

    private fun person(name: String): Person = Person(Uuid.random(), name, AppInstant.fromEpochMilliseconds(0))

    private fun fullMatch(): Match =
        match(
            "2026-03-02",
            opponents = listOf(person("Ana"), person("Ben")),
            partner = person("Cy"),
            games = listOf(GameScore(1, 11, 9)),
            location = "Ayala Triangle",
        )

    private fun viewModel(
        id: Uuid,
        matches: FakeMatchRepository,
        isPro: Boolean = false,
        savedState: Map<String, Any?> = emptyMap(),
    ): SharePreviewViewModel =
        SharePreviewViewModel(
            savedStateHandle = SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to id.toString()) + savedState),
            matchRepository = matches,
            profileRepository = FakeProfileRepository(displayName = "Matty", isPro = isPro),
            formatStore = formats,
            photoFile = { File("/nonexistent", it) },
            renderer = renderer,
            labels = FakeCardLabels(),
            ioDispatcher = Dispatchers.Main,
        )

    @Test
    fun `the card carries the match details and the display name`() {
        val shared = fullMatch()

        viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        val data = renderer.rendered.single()
        assertEquals("Matty", data.displayName)
        assertEquals(listOf("Ana", "Ben"), data.opponents?.values)
        assertEquals(listOf("Cy"), data.partner?.values)
        assertEquals(listOf("11–9"), data.games?.values)
        assertEquals("Ayala Triangle", data.location)
    }

    @Test
    fun `a failed render offers a retry instead of a broken card`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        val failed = viewModel.uiState.value
        assertTrue(failed.hasFailed)
        assertFalse(failed.isRendering)

        viewModel.retry()

        assertEquals(2, renderer.rendered.size)
    }

    @Test
    fun `a match that no longer exists closes the share screen`() {
        val state = viewModel(Uuid.random(), FakeMatchRepository()).uiState.value

        assertTrue(state.isGone)
        assertTrue(renderer.rendered.isEmpty())
    }

    @Test
    fun `a sharing failure is reported without losing the card state`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.reportShareFailed()

        assertTrue(viewModel.uiState.value.hasShareFailed)
        assertTrue(viewModel.uiState.value.hasFailed)
    }

    @Test
    fun `the remembered ratio and variant are used for the first render`() {
        val shared = match("2026-03-02")
        formats.store(CardFormat(CardRatio.SQUARE, CardTheme.LIGHT))

        val state = viewModel(shared.id, FakeMatchRepository(listOf(shared))).uiState.value

        val data = renderer.rendered.single()
        assertEquals(CardRatio.SQUARE, data.ratio)
        assertEquals(CardTheme.LIGHT, data.theme)
        assertEquals(CardRatio.SQUARE, state.format.ratio)
    }

    @Test
    fun `picking a ratio re-renders the real match and remembers the choice`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.selectRatio(CardRatio.SQUARE)
        viewModel.selectTheme(CardTheme.LIGHT)

        val last = renderer.rendered.last()
        assertEquals(3, renderer.rendered.size)
        assertEquals(CardRatio.SQUARE, last.ratio)
        assertEquals(CardTheme.LIGHT, last.theme)
        assertEquals("Matty", last.displayName)
        assertEquals(CardFormat(CardRatio.SQUARE, CardTheme.LIGHT), formats.saved.last())
    }

    @Test
    fun `picking the same option again does not re-render`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.selectRatio(CardRatio.TALL)

        assertEquals(1, renderer.rendered.size)
        assertTrue(formats.saved.isEmpty())
    }

    @Test
    fun `a match with a photo is offered the photo layout and can override it off`() {
        val photo = PhotoRef(Uuid.random(), "photos/missing.jpg", 10, 10, 10, 0)
        val shared = match("2026-03-02", photos = listOf(photo))
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        assertTrue(viewModel.uiState.value.hasPhoto)
        assertEquals(CardLayout.PHOTO, viewModel.uiState.value.layout)

        viewModel.selectLayout(CardLayout.NO_PHOTO)

        assertEquals(CardLayout.NO_PHOTO, viewModel.uiState.value.layout)
        assertEquals(CardLayout.NO_PHOTO, formats.saved.last().layoutOverride)
        assertEquals(null, renderer.rendered.last().photo)
    }

    @Test
    fun `a match without a photo gets the no-photo layout automatically`() {
        val shared = match("2026-03-02")

        val state = viewModel(shared.id, FakeMatchRepository(listOf(shared))).uiState.value

        assertFalse(state.hasPhoto)
        assertEquals(CardLayout.NO_PHOTO, state.layout)
    }

    @Test
    fun `a free user picking a pro theme is offered the upgrade and nothing changes`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.selectTheme(CardTheme.COURT)

        assertEquals(UpgradeReason.PRO_THEME, viewModel.uiState.value.upgradeReason)
        assertEquals(CardTheme.DARK, viewModel.uiState.value.format.theme)
        assertEquals(1, renderer.rendered.size)
        assertTrue(formats.saved.isEmpty())

        viewModel.dismissUpgrade()

        assertNull(viewModel.uiState.value.upgradeReason)
    }

    @Test
    fun `a pro user can pick a pro theme and it is remembered`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)), isPro = true)

        viewModel.selectTheme(CardTheme.SUNSET)

        assertNull(viewModel.uiState.value.upgradeReason)
        assertEquals(CardTheme.SUNSET, renderer.rendered.last().theme)
        assertEquals(CardTheme.SUNSET, formats.saved.last().theme)
        assertEquals(CardTheme.SUNSET, viewModel.uiState.value.format.theme)
    }

    @Test
    fun `a free user with a stored pro theme sees and renders dark and the choice is kept`() {
        val shared = match("2026-03-02")
        formats.store(CardFormat(theme = CardTheme.COURT))

        val state = viewModel(shared.id, FakeMatchRepository(listOf(shared))).uiState.value

        assertEquals(CardTheme.DARK, renderer.rendered.single().theme)
        assertEquals(CardTheme.DARK, state.format.theme)
        assertTrue(formats.saved.isEmpty())
    }

    @Test
    fun `a pro user with a stored pro theme gets it on the first render`() {
        val shared = match("2026-03-02")
        formats.store(CardFormat(theme = CardTheme.COURT))

        viewModel(shared.id, FakeMatchRepository(listOf(shared)), isPro = true)

        assertEquals(CardTheme.COURT, renderer.rendered.single().theme)
    }

    @Test
    fun `pro removes the wordmark and free keeps it`() {
        val shared = match("2026-03-02")

        viewModel(shared.id, FakeMatchRepository(listOf(shared)), isPro = false)
        viewModel(shared.id, FakeMatchRepository(listOf(shared)), isPro = true)

        assertEquals(listOf("Picklejot", ""), renderer.rendered.map { it.brand })
    }

    @Test
    fun `the details a match has are offered to hide and none start hidden`() {
        val shared = fullMatch()

        val state = viewModel(shared.id, FakeMatchRepository(listOf(shared))).uiState.value

        assertEquals(CardDetail.entries.toSet(), state.availableDetails)
        assertTrue(state.hiddenDetails.isEmpty())
    }

    @Test
    fun `a match with no extras offers nothing to hide`() {
        val shared = match("2026-03-02")

        val state = viewModel(shared.id, FakeMatchRepository(listOf(shared))).uiState.value

        assertTrue(state.availableDetails.isEmpty())
    }

    @Test
    fun `hiding a detail re-renders without it and showing it brings it back`() {
        val shared = fullMatch()
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.setDetailShown(CardDetail.OPPONENTS, isShown = false)

        assertEquals(2, renderer.rendered.size)
        assertNull(renderer.rendered.last().opponents)
        assertEquals(listOf("Cy"), renderer.rendered.last().partner?.values)
        assertEquals(setOf(CardDetail.OPPONENTS), viewModel.uiState.value.hiddenDetails)

        viewModel.setDetailShown(CardDetail.OPPONENTS, isShown = true)

        assertEquals(3, renderer.rendered.size)
        assertEquals(listOf("Ana", "Ben"), renderer.rendered.last().opponents?.values)
        assertTrue(viewModel.uiState.value.hiddenDetails.isEmpty())
    }

    @Test
    fun `setting a detail to what it already is does not re-render`() {
        val shared = fullMatch()
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.setDetailShown(CardDetail.LOCATION, isShown = true)

        assertEquals(1, renderer.rendered.size)
    }

    @Test
    fun `hidden details survive a rebuilt view model through saved state`() {
        val shared = fullMatch()
        val matches = FakeMatchRepository(listOf(shared))
        val handle = SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to shared.id.toString()))
        val first =
            SharePreviewViewModel(
                savedStateHandle = handle,
                matchRepository = matches,
                profileRepository = FakeProfileRepository(displayName = "Matty", isPro = false),
                formatStore = formats,
                photoFile = { File("/nonexistent", it) },
                renderer = renderer,
                labels = FakeCardLabels(),
                ioDispatcher = Dispatchers.Main,
            )
        first.setDetailShown(CardDetail.LOCATION, isShown = false)

        val restored =
            SharePreviewViewModel(
                savedStateHandle = handle,
                matchRepository = matches,
                profileRepository = FakeProfileRepository(displayName = "Matty", isPro = false),
                formatStore = formats,
                photoFile = { File("/nonexistent", it) },
                renderer = renderer,
                labels = FakeCardLabels(),
                ioDispatcher = Dispatchers.Main,
            )

        assertEquals(setOf(CardDetail.LOCATION), restored.uiState.value.hiddenDetails)
        assertNull(renderer.rendered.last().location)
    }

    @Test
    fun `a retry keeps the hidden details`() {
        val shared = fullMatch()
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))
        viewModel.setDetailShown(CardDetail.GAME_SCORES, isShown = false)

        viewModel.retry()

        assertNull(renderer.rendered.last().games)
    }
}
