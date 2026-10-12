package com.maxeydev.picklejot.domain.onboarding

import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.profile.DisplayName
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.ProfileRepository
import com.maxeydev.picklejot.domain.profile.UserProfile
import com.maxeydev.picklejot.domain.settings.AppSettings
import com.maxeydev.picklejot.domain.settings.AppSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemorySettings(
    initial: AppSettings = AppSettings(),
) : AppSettingsStore {
    val state = MutableStateFlow(initial)

    override fun observe(): Flow<AppSettings> = state

    override suspend fun setDarkTheme(dark: Boolean) {
        state.update { it.copy(darkTheme = dark) }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        state.update { it.copy(onboardingComplete = complete) }
    }

    override suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        state.update { it.copy(soundEffectsEnabled = enabled) }
    }
}

private class MemoryProfile(
    name: String = "",
) : ProfileRepository {
    val state =
        MutableStateFlow(
            UserProfile(
                displayName = name,
                createdAt = AppInstant.fromEpochMilliseconds(0),
                entitlement = Entitlement(isPro = false),
            ),
        )

    override fun observeProfile(): Flow<UserProfile> = state

    override suspend fun updateDisplayName(displayName: String) {
        state.update { it.copy(displayName = displayName) }
    }
}

class OnboardingTest {
    private val settings = MemorySettings()
    private val profile = MemoryProfile()
    private var matches = 0

    private val onboarding = Onboarding(settings, profile) { matches }

    @Test
    fun `a fresh install with no name and no matches requires onboarding`() =
        runTest {
            assertTrue(onboarding.isRequired())
            assertFalse(settings.state.value.onboardingComplete)
        }

    @Test
    fun `once completed onboarding is never required again`() =
        runTest {
            settings.state.value = AppSettings(onboardingComplete = true)

            assertFalse(onboarding.isRequired())
        }

    @Test
    fun `an existing install that already has matches skips onboarding and remembers it`() =
        runTest {
            matches = 3

            assertFalse(onboarding.isRequired())
            assertTrue(settings.state.value.onboardingComplete)
        }

    @Test
    fun `an existing install that already has a name skips onboarding and remembers it`() =
        runTest {
            profile.state.update { it.copy(displayName = "Matthew") }

            assertFalse(onboarding.isRequired())
            assertTrue(settings.state.value.onboardingComplete)
        }

    @Test
    fun `completing with a name trims it, stores it and marks onboarding done`() =
        runTest {
            assertTrue(onboarding.complete("  Matthew  "))

            assertEquals("Matthew", profile.state.value.displayName)
            assertTrue(settings.state.value.onboardingComplete)
        }

    @Test
    fun `completing without a name is refused and onboarding stays required`() =
        runTest {
            assertFalse(onboarding.complete(""))

            assertEquals("", profile.state.value.displayName)
            assertFalse(settings.state.value.onboardingComplete)
            assertTrue(onboarding.isRequired())
        }

    @Test
    fun `a whitespace only name is refused`() =
        runTest {
            assertFalse(onboarding.complete("   "))

            assertEquals("", profile.state.value.displayName)
            assertFalse(settings.state.value.onboardingComplete)
        }

    @Test
    fun `completing with a name over the limit is refused`() =
        runTest {
            assertFalse(onboarding.complete("x".repeat(DisplayName.MAX_LENGTH + 1)))

            assertFalse(settings.state.value.onboardingComplete)
        }

    @Test
    fun `after an erase the onboarding flag is cleared and onboarding is required again`() =
        runTest {
            onboarding.complete("Matthew")
            settings.setOnboardingComplete(false)
            profile.updateDisplayName("")
            matches = 0

            assertTrue(onboarding.isRequired())
        }
}
