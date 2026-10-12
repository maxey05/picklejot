package com.maxeydev.picklejot.domain.streak

import kotlinx.coroutines.flow.Flow

interface StreakNoticeStore {
    fun observe(): Flow<StreakNoticeState>

    suspend fun acknowledgeSkip(weekOrdinal: Long)

    suspend fun acknowledgeMissed(weekOrdinal: Long)
}
