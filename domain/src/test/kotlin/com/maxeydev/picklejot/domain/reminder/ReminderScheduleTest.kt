package com.maxeydev.picklejot.domain.reminder

import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours

private const val HOURS_IN_EIGHT_DAYS = 8 * 24
private const val HOURS_BETWEEN_SAMPLES = 31

class ReminderScheduleTest {
    private val utc = AppTimeZone.UTC
    private val taipei = AppTimeZone.of("Asia/Taipei")
    private val newYork = AppTimeZone.of("America/New_York")

    private fun at(value: String): AppInstant = AppInstant.parse(value)

    private fun nextFire(
        now: String,
        zone: AppTimeZone = utc,
    ): AppInstant = ReminderSchedule.nextFire(at(now), zone)

    private fun late(
        now: String,
        zone: AppTimeZone = utc,
    ): Boolean = ReminderSchedule.isLateInWeek(at(now), zone)

    @Test
    fun `from the middle of the week the next fire is this friday at six in the evening`() {
        assertEquals(at("2026-10-02T18:00:00Z"), nextFire("2026-09-30T13:57:00Z"))
    }

    @Test
    fun `from monday morning the next fire is the same week's friday`() {
        assertEquals(at("2026-10-02T18:00:00Z"), nextFire("2026-09-28T00:00:00Z"))
    }

    @Test
    fun `just before friday six the next fire is that evening`() {
        assertEquals(at("2026-10-02T18:00:00Z"), nextFire("2026-10-02T17:59:59Z"))
    }

    @Test
    fun `exactly at friday six the next fire is a week later`() {
        assertEquals(at("2026-10-09T18:00:00Z"), nextFire("2026-10-02T18:00:00Z"))
    }

    @Test
    fun `on saturday and sunday the next fire is the following friday`() {
        assertEquals(at("2026-10-09T18:00:00Z"), nextFire("2026-10-03T09:00:00Z"))
        assertEquals(at("2026-10-09T18:00:00Z"), nextFire("2026-10-04T22:00:00Z"))
    }

    @Test
    fun `the fire time is local evening in the device time zone`() {
        assertEquals(at("2026-10-02T10:00:00Z"), nextFire("2026-09-30T13:57:00Z", taipei))
        assertEquals(at("2026-10-02T22:00:00Z"), nextFire("2026-09-30T13:57:00Z", newYork))
    }

    @Test
    fun `the week is decided by the local date not the utc date`() {
        assertEquals(at("2026-10-02T10:00:00Z"), nextFire("2026-09-27T17:30:00Z", taipei))
        assertEquals(at("2026-10-09T10:00:00Z"), nextFire("2026-10-04T17:30:00Z", taipei))
    }

    @Test
    fun `the fire stays at local six on either side of the autumn clock change`() {
        assertEquals(at("2026-11-06T23:00:00Z"), nextFire("2026-11-04T12:00:00Z", newYork))
        assertEquals(at("2026-10-30T22:00:00Z"), nextFire("2026-10-28T12:00:00Z", newYork))
    }

    @Test
    fun `the next fire is always a friday evening strictly after now`() {
        val zones = listOf(utc, taipei, newYork)
        var instant = at("2026-01-01T00:00:00Z")
        repeat(400) {
            zones.forEach { zone ->
                val fire = ReminderSchedule.nextFire(instant, zone)
                assertTrue("$fire is not after $instant", fire > instant)
                assertTrue("$fire is not inside the late window in $zone", ReminderSchedule.isLateInWeek(fire, zone))
                assertFalse("$fire is a week or more away from $instant", fire > instant.plusHours(HOURS_IN_EIGHT_DAYS))
            }
            instant = instant.plusHours(HOURS_BETWEEN_SAMPLES)
        }
    }

    @Test
    fun `the late window opens friday at six`() {
        assertFalse(late("2026-10-02T17:59:59Z"))
        assertTrue(late("2026-10-02T18:00:00Z"))
    }

    @Test
    fun `all of saturday is inside the late window`() {
        assertTrue(late("2026-10-03T00:00:00Z"))
        assertTrue(late("2026-10-03T23:59:59Z"))
    }

    @Test
    fun `the late window closes sunday at six in the evening`() {
        assertTrue(late("2026-10-04T17:59:59Z"))
        assertFalse(late("2026-10-04T18:00:00Z"))
    }

    @Test
    fun `monday to thursday is never inside the late window`() {
        listOf("2026-09-28T12:00:00Z", "2026-09-29T12:00:00Z", "2026-09-30T12:00:00Z", "2026-10-01T23:59:59Z")
            .forEach { assertFalse(it, late(it)) }
    }

    @Test
    fun `a late delivered worker on monday morning does not count as late in the week`() {
        assertFalse(late("2026-10-05T00:30:00Z"))
    }

    @Test
    fun `the window is judged in local time`() {
        assertTrue(late("2026-10-02T10:00:00Z", taipei))
        assertFalse(late("2026-10-02T09:59:00Z", taipei))
    }

    @Test
    fun `a custom time moves the friday fire to that time`() {
        val morning = AppTime(8, 30)

        assertEquals(
            at("2026-10-02T08:30:00Z"),
            ReminderSchedule.nextFire(at("2026-09-30T13:57:00Z"), utc, morning),
        )
    }

    @Test
    fun `a custom time that has already passed on friday rolls to next week`() {
        val morning = AppTime(8, 30)

        assertEquals(
            at("2026-10-09T08:30:00Z"),
            ReminderSchedule.nextFire(at("2026-10-02T12:00:00Z"), utc, morning),
        )
    }

    @Test
    fun `the late window opens and closes at the custom time`() {
        val morning = AppTime(8, 30)

        assertFalse(ReminderSchedule.isLateInWeek(at("2026-10-02T08:29:59Z"), utc, morning))
        assertTrue(ReminderSchedule.isLateInWeek(at("2026-10-02T08:30:00Z"), utc, morning))
        assertTrue(ReminderSchedule.isLateInWeek(at("2026-10-04T08:29:59Z"), utc, morning))
        assertFalse(ReminderSchedule.isLateInWeek(at("2026-10-04T08:30:00Z"), utc, morning))
    }

    @Test
    fun `a custom time is read in the local zone`() {
        val evening = AppTime(20, 15)

        assertEquals(
            at("2026-10-02T12:15:00Z"),
            ReminderSchedule.nextFire(at("2026-09-30T00:00:00Z"), taipei, evening),
        )
    }
}

private fun AppInstant.plusHours(hours: Int): AppInstant = this + hours.hours
