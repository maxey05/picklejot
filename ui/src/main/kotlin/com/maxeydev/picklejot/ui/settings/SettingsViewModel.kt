package com.maxeydev.picklejot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.erase.EraseAllData
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.profile.DisplayName
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import com.maxeydev.picklejot.domain.profile.ProfileRepository
import com.maxeydev.picklejot.domain.reminder.ReminderStore
import com.maxeydev.picklejot.domain.reminder.StreakReminder
import com.maxeydev.picklejot.domain.settings.AppCache
import com.maxeydev.picklejot.domain.settings.AppSettingsStore
import com.maxeydev.picklejot.ui.PicklejotDependencies
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    entitlementRepository: EntitlementRepository,
    matchRepository: MatchRepository,
    reminderStore: ReminderStore,
    private val streakReminder: StreakReminder,
    private val profileRepository: ProfileRepository,
    private val appSettingsStore: AppSettingsStore,
    private val eraseAllData: EraseAllData,
    private val appCache: AppCache,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            entitlementRepository.observeEntitlement().collect { entitlement ->
                mutableUiState.update { it.copy(hasPro = entitlement.isPro) }
            }
        }
        viewModelScope.launch {
            matchRepository.observeMatchCount().collect { count ->
                mutableUiState.update { it.copy(savedMatches = count) }
            }
        }
        viewModelScope.launch {
            reminderStore.observe().collect { state ->
                mutableUiState.update { it.copy(reminderEnabled = state.enabled, reminderTime = state.fireTime) }
            }
        }
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                mutableUiState.update { current ->
                    val draftFollowsSavedName = current.nameDraft == current.displayName
                    current.copy(
                        displayName = profile.displayName,
                        nameDraft = if (draftFollowsSavedName) profile.displayName else current.nameDraft,
                    )
                }
            }
        }
        viewModelScope.launch {
            appSettingsStore.observe().collect { settings ->
                mutableUiState.update {
                    it.copy(darkTheme = settings.darkTheme, soundEffects = settings.soundEffectsEnabled)
                }
            }
        }
        refreshCacheSize()
    }

    fun changeName(raw: String) {
        mutableUiState.update { it.copy(nameDraft = DisplayName.limit(raw)) }
    }

    fun saveName() {
        val draft = mutableUiState.value.nameDraft
        if (!mutableUiState.value.canSaveName) {
            return
        }
        val cleaned = DisplayName.clean(draft)
        mutableUiState.update { it.copy(nameDraft = cleaned) }
        viewModelScope.launch { profileRepository.updateDisplayName(cleaned) }
    }

    fun discardNameDraft() {
        mutableUiState.update { it.copy(nameDraft = it.displayName) }
    }

    fun changeDarkTheme(dark: Boolean) {
        viewModelScope.launch { appSettingsStore.setDarkTheme(dark) }
    }

    fun changeSoundEffects(enabled: Boolean) {
        viewModelScope.launch { appSettingsStore.setSoundEffectsEnabled(enabled) }
    }

    fun enableReminder() {
        viewModelScope.launch { streakReminder.enable() }
    }

    fun disableReminder() {
        viewModelScope.launch { streakReminder.disable() }
    }

    fun changeReminderTime(time: AppTime) {
        viewModelScope.launch { streakReminder.setFireTime(time) }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val size = appCache.sizeBytes()
            mutableUiState.update { it.copy(cacheBytes = size) }
        }
    }

    fun clearCache() {
        if (mutableUiState.value.isClearingCache) {
            return
        }
        mutableUiState.update { it.copy(isClearingCache = true) }
        viewModelScope.launch {
            appCache.clear()
            val size = appCache.sizeBytes()
            mutableUiState.update { it.copy(isClearingCache = false, cacheBytes = size) }
        }
    }

    fun eraseAll() {
        if (mutableUiState.value.isErasing) {
            return
        }
        mutableUiState.update { it.copy(isErasing = true, eraseFailed = false) }
        viewModelScope.launch {
            try {
                eraseAllData()
                mutableUiState.update { it.copy(isErasing = false, isErased = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableUiState.update { it.copy(isErasing = false, eraseFailed = true) }
            }
        }
    }

    fun dismissEraseFailure() {
        mutableUiState.update { it.copy(eraseFailed = false) }
    }

    fun erasedHandled() {
        mutableUiState.update { it.copy(isErased = false) }
    }

    companion object {
        fun factory(dependencies: PicklejotDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SettingsViewModel(
                        entitlementRepository = dependencies.entitlementRepository,
                        matchRepository = dependencies.matchRepository,
                        reminderStore = dependencies.reminderStore,
                        streakReminder = dependencies.streakReminder,
                        profileRepository = dependencies.profileRepository,
                        appSettingsStore = dependencies.appSettingsStore,
                        eraseAllData = dependencies.eraseAllData,
                        appCache = dependencies.appCache,
                    )
                }
            }
    }
}
