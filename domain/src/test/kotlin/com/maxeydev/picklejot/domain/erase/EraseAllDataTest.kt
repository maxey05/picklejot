package com.maxeydev.picklejot.domain.erase

import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.reminder.ReminderNotifier
import com.maxeydev.picklejot.domain.reminder.ReminderScheduling
import com.maxeydev.picklejot.domain.reminder.ReminderState
import com.maxeydev.picklejot.domain.reminder.ReminderStore
import com.maxeydev.picklejot.domain.reminder.StreakReminder
import com.maxeydev.picklejot.domain.streak.InsuredStreakEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.time.Clock

private class Events {
    val log = mutableListOf<String>()
}

private class MemoryReminderStore : ReminderStore {
    val state = MutableStateFlow(ReminderState(enabled = true))

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

private class LoggingScheduling(
    private val events: Events,
) : ReminderScheduling {
    override fun scheduleNext(fireAt: AppInstant) {
        events.log += "schedule"
    }

    override fun cancel() {
        events.log += "cancel"
    }
}

private class LoggingEraser(
    private val events: Events,
    private val failure: Exception? = null,
) : LocalDataEraser {
    override suspend fun erase() {
        failure?.let { throw it }
        events.log += "erase"
    }
}

class EraseAllDataTest {
    private val events = Events()
    private val store = MemoryReminderStore()
    private val clock =
        object : Clock {
            override fun now(): AppInstant = AppInstant.parse("2026-09-30T09:00:00Z")
        }

    private val reminder =
        StreakReminder(
            store = store,
            matchDates = { emptyList() },
            insuranceStart = { null },
            engine = InsuredStreakEngine(clock) { AppTimeZone.UTC },
            scheduling = LoggingScheduling(events),
            notifier =
                object : ReminderNotifier {
                    override fun notifyStreakAtRisk(skipAvailable: Boolean): Boolean = false
                },
            clock = clock,
            timeZone = { AppTimeZone.UTC },
        )

    @Test
    fun `the scheduled reminder is cancelled before any data is erased`() =
        runTest {
            EraseAllData(reminder, LoggingEraser(events))()

            assertEquals(listOf("cancel", "erase"), events.log)
            assertFalse(store.state.value.enabled)
        }

    @Test
    fun `a failing eraser surfaces its failure to the caller`() =
        runTest {
            val failing = EraseAllData(reminder, LoggingEraser(events, IllegalStateException("disk")))

            var thrown: IllegalStateException? = null
            try {
                failing()
            } catch (failure: IllegalStateException) {
                thrown = failure
            }

            assertNotNull(thrown)
            assertEquals(listOf("cancel"), events.log)
        }
}
