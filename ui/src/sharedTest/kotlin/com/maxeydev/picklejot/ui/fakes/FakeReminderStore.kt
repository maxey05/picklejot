package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.reminder.ReminderState
import com.maxeydev.picklejot.domain.reminder.ReminderStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeReminderStore(
    initial: ReminderState = ReminderState(),
) : ReminderStore {
    private val state = MutableStateFlow(initial)

    val current: ReminderState
        get() = state.value

    override fun observe(): Flow<ReminderState> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.update { it.copy(enabled = enabled) }
    }

    override suspend fun setFireTime(time: AppTime) {
        state.update { it.copy(fireTime = time) }
    }

    override suspend fun markNotified(weekOrdinal: Long?) {
        state.update { it.copy(lastNotifiedWeek = weekOrdinal) }
    }
}
