package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipNoticesTest {
    private val today = date("2026-09-30")

    private fun date(value: String): AppDate = AppDate.parse(value)

    private fun dates(vararg values: String): List<AppDate> = values.map { date(it) }

    private fun week(day: String): WeekKey = WeekKey.of(date(day))

    @Test
    fun `one missed week after a streak of two or more is an opportunity naming the streak it would have saved`() {
        val found = SkipNotices.findMissedOpportunity(dates("2026-09-08", "2026-09-15"), today)

        assertEquals(MissedSkipOpportunity(missedWeek = week("2026-09-21"), brokenStreakWeeks = 2), found)
    }

    @Test
    fun `a streak of a single week is not worth an upgrade prompt`() {
        assertNull(SkipNotices.findMissedOpportunity(dates("2026-09-15"), today))
    }

    @Test
    fun `a played last week leaves nothing missed`() {
        assertNull(SkipNotices.findMissedOpportunity(dates("2026-09-08", "2026-09-22"), today))
    }

    @Test
    fun `two missed weeks could not have been saved by one skip`() {
        assertNull(SkipNotices.findMissedOpportunity(dates("2026-09-01", "2026-09-08"), today))
    }

    @Test
    fun `playing this week does not hide the opportunity`() {
        val found = SkipNotices.findMissedOpportunity(dates("2026-09-08", "2026-09-15", "2026-09-29"), today)

        assertEquals(2, found?.brokenStreakWeeks)
    }

    @Test
    fun `an acknowledged opportunity is recognised`() {
        val opportunity = MissedSkipOpportunity(missedWeek = week("2026-09-21"), brokenStreakWeeks = 2)

        assertFalse(SkipNotices.isMissedOpportunityAcknowledged(opportunity, null))
        assertTrue(SkipNotices.isMissedOpportunityAcknowledged(opportunity, opportunity.missedWeek.ordinal))
    }

    @Test
    fun `the newest unacknowledged recent skip is the one announced`() {
        val insured =
            StreakInsurance.compute(
                dates("2026-08-11", "2026-08-25", "2026-09-01", "2026-09-08", "2026-09-22", "2026-09-29"),
                today,
                date("2026-01-01"),
            )

        val announced = SkipNotices.unacknowledgedSkip(insured, today, acknowledgedThrough = null)

        assertEquals(listOf(week("2026-08-17"), week("2026-09-14")), insured.skippedWeeks)
        assertEquals(week("2026-09-14"), announced)
    }

    @Test
    fun `an acknowledged skip is not announced again`() {
        val insured = StreakInsurance.compute(dates("2026-09-08", "2026-09-29"), today, date("2026-01-01"))
        val skipped = insured.skippedWeeks.last()

        assertNull(SkipNotices.unacknowledgedSkip(insured, today, acknowledgedThrough = skipped.ordinal))
    }

    @Test
    fun `a skip from long ago is not announced after a restore`() {
        val insured =
            StreakInsurance.compute(dates("2026-05-05", "2026-05-19", "2026-09-29"), today, date("2026-01-01"))

        assertNull(SkipNotices.unacknowledgedSkip(insured, today, acknowledgedThrough = null))
    }
}
