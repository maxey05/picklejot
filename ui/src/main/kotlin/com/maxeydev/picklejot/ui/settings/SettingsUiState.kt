package com.maxeydev.picklejot.ui.settings

import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.profile.DisplayName
import com.maxeydev.picklejot.domain.reminder.ReminderSchedule

data class SettingsUiState(
    val hasPro: Boolean = false,
    val savedMatches: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderTime: AppTime = ReminderSchedule.DEFAULT_FIRE_TIME,
    val displayName: String = "",
    val nameDraft: String = "",
    val darkTheme: Boolean? = null,
    val soundEffects: Boolean = true,
    val cacheBytes: Long? = null,
    val isClearingCache: Boolean = false,
    val isErasing: Boolean = false,
    val eraseFailed: Boolean = false,
    val isErased: Boolean = false,
) {
    val canSaveName: Boolean
        get() = DisplayName.isValid(nameDraft) && DisplayName.clean(nameDraft) != displayName
}
