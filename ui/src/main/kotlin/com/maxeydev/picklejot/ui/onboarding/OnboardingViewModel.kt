package com.maxeydev.picklejot.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.onboarding.Onboarding
import com.maxeydev.picklejot.domain.profile.DisplayName
import com.maxeydev.picklejot.ui.PicklejotDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class OnboardingViewModel(
    private val onboarding: Onboarding,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    fun changeName(raw: String) {
        mutableUiState.update { it.copy(name = DisplayName.limit(raw), saveFailed = false) }
    }

    fun continueToApp() {
        val current = mutableUiState.value
        if (current.isSaving) {
            return
        }
        if (!DisplayName.isValid(current.name)) {
            mutableUiState.update { it.copy(nameRequiredAttempts = it.nameRequiredAttempts + 1) }
            return
        }
        mutableUiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val completed = onboarding.complete(current.name)
                mutableUiState.update { it.copy(isSaving = false, isFinished = completed) }
            } catch (unwritable: IOException) {
                mutableUiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    companion object {
        fun factory(dependencies: PicklejotDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { OnboardingViewModel(dependencies.onboarding) }
            }
    }
}
