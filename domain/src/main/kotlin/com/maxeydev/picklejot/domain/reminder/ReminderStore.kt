package com.maxeydev.picklejot.domain.reminder

import com.maxeydev.picklejot.domain.datetime.AppTime
import kotlinx.coroutines.flow.Flow

interface ReminderStore {
    fun observe(): Flow<ReminderState>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setFireTime(time: AppTime)

    suspend fun markNotified(weekOrdinal: Long?)
}
