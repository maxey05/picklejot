package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.streak.StreakNoticeState
import com.maxeydev.picklejot.domain.streak.StreakNoticeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeStreakNoticeStore(
    initial: StreakNoticeState = StreakNoticeState(),
) : StreakNoticeStore {
    private val state = MutableStateFlow(initial)

    val current: StreakNoticeState
        get() = state.value

    override fun observe(): Flow<StreakNoticeState> = state

    override suspend fun acknowledgeSkip(weekOrdinal: Long) {
        state.update { it.copy(acknowledgedSkipWeek = maxOf(weekOrdinal, it.acknowledgedSkipWeek ?: weekOrdinal)) }
    }

    override suspend fun acknowledgeMissed(weekOrdinal: Long) {
        state.update { it.copy(acknowledgedMissedWeek = maxOf(weekOrdinal, it.acknowledgedMissedWeek ?: weekOrdinal)) }
    }
}
