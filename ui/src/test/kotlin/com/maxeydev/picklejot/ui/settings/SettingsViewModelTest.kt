@file:OptIn(ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.settings

import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.erase.EraseAllData
import com.maxeydev.picklejot.domain.profile.DisplayName
import com.maxeydev.picklejot.domain.reminder.StreakReminder
import com.maxeydev.picklejot.domain.streak.InsuredStreakEngine
import com.maxeydev.picklejot.ui.fakes.FakeAppCache
import com.maxeydev.picklejot.ui.fakes.FakeAppSettingsStore
import com.maxeydev.picklejot.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklejot.ui.fakes.FakeLocalDataEraser
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.fakes.FakeProfileRepository
import com.maxeydev.picklejot.ui.fakes.FakeReminderNotifier
import com.maxeydev.picklejot.ui.fakes.FakeReminderScheduling
import com.maxeydev.picklejot.ui.fakes.FakeReminderStore
import com.maxeydev.picklejot.ui.fakes.FixedClock
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

class SettingsViewModelTest {
    private val entitlements = FakeEntitlementRepository()
    private val matches = FakeMatchRepository()
    private val cache = FakeAppCache(bytes = 12_000L)
    private val reminderStore = FakeReminderStore()
    private val scheduling = FakeReminderScheduling()
    private val clock = FixedClock(AppInstant.parse("2026-09-30T09:00:00Z"))

    private val streakReminder =
        StreakReminder(
            store = reminderStore,
            matchDates = { emptyList() },
            insuranceStart = { null },
            engine = InsuredStreakEngine(clock) { TimeZone.UTC },
            scheduling = scheduling,
            notifier = FakeReminderNotifier(),
            clock = clock,
            timeZone = { TimeZone.UTC },
        )

    private val profile = FakeProfileRepository()
    private val appSettings = FakeAppSettingsStore()
    private val eraser = FakeLocalDataEraser()
    private val eraseAllData = EraseAllData(streakReminder, eraser)

    private fun viewModel(entitlementRepository: FakeEntitlementRepository = entitlements) =
        SettingsViewModel(
            entitlementRepository,
            matches,
            reminderStore,
            streakReminder,
            profile,
            appSettings,
            eraseAllData,
            cache,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `settings reports how big the cache is`() {
        assertEquals(12_000L, viewModel().uiState.value.cacheBytes)
    }

    @Test
    fun `clearing the cache empties it and shows the new size`() {
        val viewModel = viewModel()

        viewModel.clearCache()

        assertEquals(1, cache.clearCount)
        assertEquals(0L, viewModel.uiState.value.cacheBytes)
        assertFalse(viewModel.uiState.value.isClearingCache)
    }

    @Test
    fun `reopening settings picks up cache that grew since`() {
        val viewModel = viewModel()
        cache.bytes = 40_000L

        viewModel.refreshCacheSize()

        assertEquals(40_000L, viewModel.uiState.value.cacheBytes)
    }

    @Test
    fun `cancelling a name edit puts the saved name back`() {
        val viewModel = viewModel()
        viewModel.changeName("Matthew")
        viewModel.saveName()
        viewModel.changeName("Someone else")

        viewModel.discardNameDraft()

        assertEquals("Matthew", viewModel.uiState.value.nameDraft)
        assertFalse(viewModel.uiState.value.canSaveName)
    }

    @Test
    fun `a pro user sees pro as unlocked`() {
        val viewModel = viewModel(FakeEntitlementRepository(isPro = true))

        assertTrue(viewModel.uiState.value.hasPro)
    }

    @Test
    fun `the reminder starts switched off`() {
        assertFalse(viewModel().uiState.value.reminderEnabled)
    }

    @Test
    fun `enabling the reminder stores the choice and arms the next friday`() {
        val viewModel = viewModel()

        viewModel.enableReminder()

        assertTrue(viewModel.uiState.value.reminderEnabled)
        assertEquals(listOf(AppInstant.parse("2026-10-02T18:00:00Z")), scheduling.scheduled)
    }

    @Test
    fun `disabling the reminder cancels the scheduled work`() {
        val viewModel = viewModel()
        viewModel.enableReminder()

        viewModel.disableReminder()

        assertFalse(viewModel.uiState.value.reminderEnabled)
        assertEquals(1, scheduling.cancelCount)
    }

    @Test
    fun `a saved name is trimmed and stored on the profile`() {
        val viewModel = viewModel()

        viewModel.changeName("  Matthew  ")
        assertTrue(viewModel.uiState.value.canSaveName)
        viewModel.saveName()

        assertEquals("Matthew", viewModel.uiState.value.displayName)
        assertEquals("Matthew", viewModel.uiState.value.nameDraft)
        assertFalse(viewModel.uiState.value.canSaveName)
    }

    @Test
    fun `a blank name can never be saved`() {
        val viewModel = viewModel()
        viewModel.changeName("Matthew")
        viewModel.saveName()

        viewModel.changeName("   ")
        viewModel.saveName()

        assertFalse(viewModel.uiState.value.canSaveName)
        assertEquals("Matthew", viewModel.uiState.value.displayName)
    }

    @Test
    fun `an unchanged name offers nothing to save`() {
        val viewModel = viewModel()
        viewModel.changeName("Matthew")
        viewModel.saveName()

        viewModel.changeName("Matthew ")

        assertFalse(viewModel.uiState.value.canSaveName)
    }

    @Test
    fun `the name draft is cut at the length limit`() {
        val viewModel = viewModel()

        viewModel.changeName("x".repeat(DisplayName.MAX_LENGTH + 10))

        assertEquals(DisplayName.MAX_LENGTH, viewModel.uiState.value.nameDraft.length)
    }

    @Test
    fun `dark mode follows the system until the user chooses`() {
        val viewModel = viewModel()
        assertNull(viewModel.uiState.value.darkTheme)

        viewModel.changeDarkTheme(true)
        assertEquals(true, viewModel.uiState.value.darkTheme)

        viewModel.changeDarkTheme(false)
        assertEquals(false, viewModel.uiState.value.darkTheme)
    }

    @Test
    fun `sound effects start on and follow the switch`() {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.soundEffects)

        viewModel.changeSoundEffects(false)
        assertFalse(viewModel.uiState.value.soundEffects)
        assertFalse(appSettings.current.soundEffectsEnabled)

        viewModel.changeSoundEffects(true)
        assertTrue(viewModel.uiState.value.soundEffects)
    }

    @Test
    fun `changing the reminder time re-arms the next friday at that time`() {
        val viewModel = viewModel()
        viewModel.enableReminder()

        viewModel.changeReminderTime(AppTime(8, 30))

        assertEquals(AppTime(8, 30), viewModel.uiState.value.reminderTime)
        assertEquals(AppInstant.parse("2026-10-02T08:30:00Z"), scheduling.scheduled.last())
    }

    @Test
    fun `changing the time while the reminder is off arms nothing`() {
        val viewModel = viewModel()

        viewModel.changeReminderTime(AppTime(8, 30))

        assertEquals(AppTime(8, 30), viewModel.uiState.value.reminderTime)
        assertTrue(scheduling.scheduled.isEmpty())
    }

    @Test
    fun `erasing all data cancels the reminder, erases and flags completion`() {
        val viewModel = viewModel()
        viewModel.enableReminder()

        viewModel.eraseAll()

        assertEquals(1, eraser.eraseCount)
        assertEquals(1, scheduling.cancelCount)
        assertTrue(viewModel.uiState.value.isErased)
        assertFalse(viewModel.uiState.value.isErasing)
        assertFalse(viewModel.uiState.value.eraseFailed)
    }

    @Test
    fun `erasing all data never revokes pro`() {
        val proEntitlements = FakeEntitlementRepository(isPro = true)
        val viewModel = viewModel(proEntitlements)

        viewModel.eraseAll()

        assertTrue(proEntitlements.current.isPro)
        assertTrue(viewModel.uiState.value.hasPro)
    }

    @Test
    fun `a failed erase reports the failure and does not claim completion`() {
        eraser.failure = IllegalStateException("disk error")
        val viewModel = viewModel()

        viewModel.eraseAll()

        assertTrue(viewModel.uiState.value.eraseFailed)
        assertFalse(viewModel.uiState.value.isErased)
        assertFalse(viewModel.uiState.value.isErasing)
        viewModel.dismissEraseFailure()
        assertFalse(viewModel.uiState.value.eraseFailed)
    }
}
