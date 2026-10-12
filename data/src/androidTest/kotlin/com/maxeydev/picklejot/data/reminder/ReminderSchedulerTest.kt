package com.maxeydev.picklejot.data.reminder

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
class ReminderSchedulerTest {
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: ReminderScheduler

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        workManager = WorkManager.getInstance(context)
        scheduler = ReminderScheduler(workManager, Clock.System)
        workManager.cancelAllWork().result.get()
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork().result.get()
    }

    private fun states(name: String): List<WorkInfo.State> =
        workManager.getWorkInfosForUniqueWork(name).get().map { it.state }

    private fun enqueuedCount(name: String): Int = states(name).count { it == WorkInfo.State.ENQUEUED }

    @Test
    fun arming_enqueues_one_pending_unique_work_for_the_fire() {
        val fireAt = Clock.System.now() + 3.days

        scheduler.scheduleNext(fireAt)

        assertEquals(1, enqueuedCount(ReminderScheduler.workNameFor(fireAt)))
    }

    @Test
    fun arming_the_same_fire_again_replaces_the_pending_work_instead_of_stacking_it() {
        val fireAt = Clock.System.now() + 3.days

        scheduler.scheduleNext(fireAt)
        scheduler.scheduleNext(fireAt)

        assertEquals(1, enqueuedCount(ReminderScheduler.workNameFor(fireAt)))
    }

    @Test
    fun consecutive_weeks_use_different_work_names_so_a_running_fire_is_never_replaced() {
        val thisFire = Clock.System.now() + 3.days
        val nextFire = thisFire + 7.days

        assertNotEquals(ReminderScheduler.workNameFor(thisFire), ReminderScheduler.workNameFor(nextFire))
        assertEquals(
            ReminderScheduler.workNameFor(thisFire),
            ReminderScheduler.workNameFor(thisFire + 14.days),
        )
    }

    @Test
    fun arming_two_consecutive_weeks_keeps_both_pending() {
        val thisFire = Clock.System.now() + 3.days
        val nextFire = thisFire + 7.days

        scheduler.scheduleNext(thisFire)
        scheduler.scheduleNext(nextFire)

        assertEquals(1, enqueuedCount(ReminderScheduler.workNameFor(thisFire)))
        assertEquals(1, enqueuedCount(ReminderScheduler.workNameFor(nextFire)))
    }

    @Test
    fun cancelling_removes_the_pending_work_in_every_slot_immediately() {
        val thisFire = Clock.System.now() + 3.days
        val nextFire = thisFire + 7.days
        scheduler.scheduleNext(thisFire)
        scheduler.scheduleNext(nextFire)

        scheduler.cancel()

        ReminderScheduler.allWorkNames().forEach { name ->
            assertEquals(name, 0, enqueuedCount(name))
        }
    }

    @Test
    fun cancelling_with_nothing_scheduled_is_harmless() {
        scheduler.cancel()

        ReminderScheduler.allWorkNames().forEach { name ->
            assertEquals(name, 0, enqueuedCount(name))
        }
    }
}
