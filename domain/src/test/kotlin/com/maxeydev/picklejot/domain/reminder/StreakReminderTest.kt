package com.maxeydev.picklejot.domain.reminder

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.streak.InsuredStreakEngine
import com.maxeydev.picklejot.domain.streak.WeekKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

private class FakeReminderStore(
    initial: ReminderState = ReminderState(),
) : ReminderStore {
    val state = MutableStateFlow(initial)

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

private class RecordingScheduling : ReminderScheduling {
    val scheduled = mutableListOf<AppInstant>()
    var cancelled = 0

    override fun scheduleNext(fireAt: AppInstant) {
        scheduled += fireAt
    }

    override fun cancel() {
        cancelled += 1
    }
}

private class RecordingNotifier(
    var posts: Boolean = true,
) : ReminderNotifier {
    val skipFlags = mutableListOf<Boolean>()

    override fun notifyStreakAtRisk(skipAvailable: Boolean): Boolean {
        skipFlags += skipAvailable
        return posts
    }
}

class StreakReminderTest {
    private var now = AppInstant.parse("2026-10-02T19:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): AppInstant = now
        }
    private val store = FakeReminderStore()
    private val scheduling = RecordingScheduling()
    private val notifier = RecordingNotifier()
    private var dates = weeklyThroughLastWeek()
    private var insuranceStart: AppInstant? = null

    private val reminder =
        StreakReminder(
            store = store,
            matchDates = { dates },
            insuranceStart = { insuranceStart },
            engine = InsuredStreakEngine(clock) { AppTimeZone.UTC },
            scheduling = scheduling,
            notifier = notifier,
            clock = clock,
            timeZone = { AppTimeZone.UTC },
        )

    private fun weeklyThroughLastWeek(): List<AppDate> =
        listOf("2026-09-08", "2026-09-15", "2026-09-22").map { AppDate.parse(it) }

    private fun enabled() {
        store.state.value = ReminderState(enabled = true)
    }

    private val thisWeek: Long
        get() = WeekKey.of(AppDate.parse("2026-10-02")).ordinal

    @Test
    fun `it is off by default and a fire while off does nothing`() =
        runTest {
            assertEquals(false, store.state.value.enabled)

            assertEquals(ReminderOutcome.DISABLED, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
            assertTrue(scheduling.scheduled.isEmpty())
        }

    @Test
    fun `an at risk streak on a friday evening is notified once and the next fire is armed`() =
        runTest {
            enabled()

            assertEquals(ReminderOutcome.NOTIFIED, reminder.onFire())

            assertEquals(listOf(false), notifier.skipFlags)
            assertEquals(thisWeek, store.state.value.lastNotifiedWeek)
            assertEquals(listOf(AppInstant.parse("2026-10-09T18:00:00Z")), scheduling.scheduled)
        }

    @Test
    fun `having played this week never notifies`() =
        runTest {
            enabled()
            dates = dates + AppDate.parse("2026-09-30")

            assertEquals(ReminderOutcome.NOT_AT_RISK, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
            assertNull(store.state.value.lastNotifiedWeek)
        }

    @Test
    fun `playing earlier on the same friday never notifies`() =
        runTest {
            enabled()
            dates = dates + AppDate.parse("2026-10-02")

            assertEquals(ReminderOutcome.NOT_AT_RISK, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
        }

    @Test
    fun `at most one notification is sent in a week`() =
        runTest {
            enabled()

            reminder.onFire()
            now = AppInstant.parse("2026-10-03T12:00:00Z")
            val second = reminder.onFire()
            now = AppInstant.parse("2026-10-04T09:00:00Z")
            val third = reminder.onFire()

            assertEquals(ReminderOutcome.ALREADY_NOTIFIED, second)
            assertEquals(ReminderOutcome.ALREADY_NOTIFIED, third)
            assertEquals(1, notifier.skipFlags.size)
        }

    @Test
    fun `a new week is allowed its own notification`() =
        runTest {
            store.state.value = ReminderState(enabled = true, lastNotifiedWeek = thisWeek - 1)
            now = AppInstant.parse("2026-10-09T18:05:00Z")
            dates = dates + AppDate.parse("2026-09-29")

            assertEquals(ReminderOutcome.NOTIFIED, reminder.onFire())

            assertEquals(thisWeek + 1, store.state.value.lastNotifiedWeek)
        }

    @Test
    fun `a fire outside the late in the week window is skipped but still arms the next one`() =
        runTest {
            enabled()
            now = AppInstant.parse("2026-09-30T12:00:00Z")

            assertEquals(ReminderOutcome.OUTSIDE_WINDOW, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
            assertEquals(listOf(AppInstant.parse("2026-10-02T18:00:00Z")), scheduling.scheduled)
        }

    @Test
    fun `a worker delivered late on monday morning does not tell anyone their new week is at risk`() =
        runTest {
            enabled()
            now = AppInstant.parse("2026-10-05T00:30:00Z")
            dates = dates + AppDate.parse("2026-09-28")

            assertEquals(ReminderOutcome.OUTSIDE_WINDOW, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
        }

    @Test
    fun `a pro user with a banked skip is told a skip is available`() =
        runTest {
            enabled()
            insuranceStart = AppInstant.parse("2026-09-01T00:00:00Z")

            assertEquals(ReminderOutcome.NOTIFIED, reminder.onFire())

            assertEquals(listOf(true), notifier.skipFlags)
        }

    @Test
    fun `an insurance start that has not begun yet grants no skip to mention`() =
        runTest {
            enabled()
            insuranceStart = AppInstant.parse("2026-11-01T00:00:00Z")

            assertEquals(ReminderOutcome.NOTIFIED, reminder.onFire())

            assertEquals(listOf(false), notifier.skipFlags)
        }

    @Test
    fun `no matches at all is never at risk`() =
        runTest {
            enabled()
            dates = emptyList()

            assertEquals(ReminderOutcome.NOT_AT_RISK, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
        }

    @Test
    fun `a streak that already broke is not at risk`() =
        runTest {
            enabled()
            dates = listOf(AppDate.parse("2026-08-25"), AppDate.parse("2026-09-01"))

            assertEquals(ReminderOutcome.NOT_AT_RISK, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
        }

    @Test
    fun `a notification the system refused does not use up the week`() =
        runTest {
            enabled()
            notifier.posts = false

            assertEquals(ReminderOutcome.NOT_POSTED, reminder.onFire())

            assertNull(store.state.value.lastNotifiedWeek)

            notifier.posts = true
            now = AppInstant.parse("2026-10-03T10:00:00Z")

            assertEquals(ReminderOutcome.NOTIFIED, reminder.onFire())
        }

    @Test
    fun `enabling turns the store on and arms the next friday`() =
        runTest {
            now = AppInstant.parse("2026-09-30T13:57:00Z")

            reminder.enable()

            assertTrue(store.state.value.enabled)
            assertEquals(listOf(AppInstant.parse("2026-10-02T18:00:00Z")), scheduling.scheduled)
        }

    @Test
    fun `disabling cancels the scheduled work and turns the store off`() =
        runTest {
            enabled()

            reminder.disable()

            assertEquals(1, scheduling.cancelled)
            assertEquals(false, store.state.value.enabled)
            assertTrue(scheduling.scheduled.isEmpty())
        }

    @Test
    fun `a fire that arrives after disabling does nothing`() =
        runTest {
            enabled()
            reminder.disable()

            assertEquals(ReminderOutcome.DISABLED, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
            assertTrue(scheduling.scheduled.isEmpty())
        }

    @Test
    fun `starting the app re-arms only when the reminder is on`() =
        runTest {
            reminder.ensureArmed()
            assertTrue(scheduling.scheduled.isEmpty())

            enabled()
            reminder.ensureArmed()

            assertEquals(1, scheduling.scheduled.size)
        }

    @Test
    fun `choosing a time while enabled re-arms the next friday at that time`() =
        runTest {
            enabled()

            reminder.setFireTime(AppTime(8, 30))

            assertEquals(AppTime(8, 30), store.state.value.fireTime)
            assertEquals(listOf(AppInstant.parse("2026-10-09T08:30:00Z")), scheduling.scheduled)
        }

    @Test
    fun `choosing a time while disabled stores it and arms nothing`() =
        runTest {
            reminder.setFireTime(AppTime(8, 30))

            assertEquals(AppTime(8, 30), store.state.value.fireTime)
            assertTrue(scheduling.scheduled.isEmpty())
        }

    @Test
    fun `a fire before the chosen time on friday is outside the window`() =
        runTest {
            now = AppInstant.parse("2026-10-02T19:00:00Z")
            store.state.value = ReminderState(enabled = true, fireTime = AppTime(21, 0))

            assertEquals(ReminderOutcome.OUTSIDE_WINDOW, reminder.onFire())

            assertTrue(notifier.skipFlags.isEmpty())
        }
}
