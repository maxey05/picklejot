@file:OptIn(ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.onboarding

import com.maxeydev.picklejot.domain.onboarding.Onboarding
import com.maxeydev.picklejot.domain.profile.DisplayName
import com.maxeydev.picklejot.domain.settings.AppSettings
import com.maxeydev.picklejot.ui.fakes.FakeAppSettingsStore
import com.maxeydev.picklejot.ui.fakes.FakeProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingViewModelTest {
    private val settings = FakeAppSettingsStore(AppSettings())
    private val profile = FakeProfileRepository()
    private val viewModel = OnboardingViewModel(Onboarding(settings, profile) { 0 })

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `it starts empty and cannot continue because a name is required`() {
        assertEquals("", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.canContinue)
        assertFalse(viewModel.uiState.value.showNameRequired)
    }

    @Test
    fun `continuing without a name is blocked, flags the name as required and stores nothing`() =
        runTest {
            viewModel.continueToApp()

            assertFalse(viewModel.uiState.value.isFinished)
            assertTrue(viewModel.uiState.value.showNameRequired)
            assertEquals("", profile.observeProfile().first().displayName)
            assertFalse(settings.current.onboardingComplete)
        }

    @Test
    fun `every blocked attempt is counted so the screen can react again`() {
        viewModel.continueToApp()
        viewModel.continueToApp()

        assertEquals(2, viewModel.uiState.value.nameRequiredAttempts)
    }

    @Test
    fun `a whitespace only name is blocked like an empty one`() =
        runTest {
            viewModel.changeName("   ")
            viewModel.continueToApp()

            assertFalse(viewModel.uiState.value.isFinished)
            assertTrue(viewModel.uiState.value.showNameRequired)
            assertEquals("", profile.observeProfile().first().displayName)
        }

    @Test
    fun `a name continues, is stored trimmed and finishes onboarding`() =
        runTest {
            viewModel.changeName("  Matthew ")
            assertTrue(viewModel.uiState.value.canContinue)

            viewModel.continueToApp()

            assertTrue(viewModel.uiState.value.isFinished)
            assertEquals("Matthew", profile.observeProfile().first().displayName)
            assertTrue(settings.current.onboardingComplete)
        }

    @Test
    fun `the typed name is cut at the length limit`() {
        viewModel.changeName("x".repeat(DisplayName.MAX_LENGTH + 5))

        assertEquals(DisplayName.MAX_LENGTH, viewModel.uiState.value.name.length)
    }
}
