package com.maxeydev.picklejot.ui.onboarding

import com.maxeydev.picklejot.domain.profile.DisplayName

data class OnboardingUiState(
    val name: String = "",
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val isFinished: Boolean = false,
    val nameRequiredAttempts: Int = 0,
) {
    val canContinue: Boolean
        get() = !isSaving && DisplayName.isValid(name)

    val showNameRequired: Boolean
        get() = nameRequiredAttempts > 0
}
