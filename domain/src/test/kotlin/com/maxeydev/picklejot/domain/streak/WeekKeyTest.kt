package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.datetime.atTimeIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.temporal.IsoFields

class WeekKeyTest {
    private val manila = AppTimeZone.of("Asia/Manila")

    private fun date(value: String): AppDate = AppDate.parse(value)

    private fun week(value: String): WeekKey = WeekKey.of(date(value))

    private fun localInstant(
        value: String,
        zone: AppTimeZone,
    ): AppInstant = AppDate.parse(value.substringBefore('T')).atTimeIn(AppTime.parse(value.substringAfter('T')), zone)

    private fun javaDate(date: AppDate): java.time.LocalDate = java.time.LocalDate.ofEpochDay(date.toEpochDays())

    @Test
    fun `every day from Monday to Sunday falls in the same week`() {
        val monday = week("2026-09-21")
        listOf("2026-09-22", "2026-09-23", "2026-09-24", "2026-09-25", "2026-09-26", "2026-09-27").forEach {
            assertEquals("$it should be in the week of Monday 2026-09-21", monday, week(it))
        }
    }

    @Test
    fun `a week reports its Monday and Sunday`() {
        val week = week("2026-09-24")
        assertEquals(date("2026-09-21"), week.monday)
        assertEquals(date("2026-09-27"), week.sunday)
    }

    @Test
    fun `the Monday after a Sunday starts the next week`() {
        assertEquals(week("2026-09-27").next(), week("2026-09-28"))
        assertEquals(week("2026-09-28").previous(), week("2026-09-27"))
    }

    @Test
    fun `a match dated Sunday falls in the week that is ending not the next one`() {
        assertEquals(week("2026-09-21"), week("2026-09-27"))
        assertNotEquals(week("2026-09-28"), week("2026-09-27"))
    }

    @Test
    fun `23 59 local on a Sunday is still inside that week`() {
        val instant = localInstant("2026-09-27T23:59:59", manila)
        assertEquals(week("2026-09-21"), WeekKey.containing(instant, manila))
    }

    @Test
    fun `00 00 local on a Monday is inside the new week`() {
        val instant = localInstant("2026-09-28T00:00:00", manila)
        assertEquals(week("2026-09-28"), WeekKey.containing(instant, manila))
        assertEquals(week("2026-09-27").next(), WeekKey.containing(instant, manila))
    }

    @Test
    fun `the week containing a moment depends on the zone it is read in`() {
        val instant = localInstant("2026-09-28T01:00:00", manila)
        val losAngeles = AppTimeZone.of("America/Los_Angeles")
        assertEquals(week("2026-09-28"), WeekKey.containing(instant, manila))
        assertEquals(week("2026-09-27"), WeekKey.containing(instant, losAngeles))
    }

    @Test
    fun `week 52 of one year is followed directly by week 1 of the next`() {
        val lastWeekOf2025 = week("2025-12-22")
        val firstWeekOf2026 = week("2025-12-29")
        assertEquals(2025, lastWeekOf2025.isoYear)
        assertEquals(52, lastWeekOf2025.isoWeek)
        assertEquals(2026, firstWeekOf2026.isoYear)
        assertEquals(1, firstWeekOf2026.isoWeek)
        assertEquals(lastWeekOf2025.next(), firstWeekOf2026)
    }

    @Test
    fun `January 1st can belong to the previous ISO year`() {
        val week = week("2027-01-01")
        assertEquals(2026, week.isoYear)
        assertEquals(53, week.isoWeek)
    }

    @Test
    fun `a year with 53 ISO weeks has no gap either side of week 53`() {
        val week52 = week("2026-12-21")
        val week53 = week("2026-12-28")
        val nextWeek1 = week("2027-01-04")
        assertEquals(52, week52.isoWeek)
        assertEquals(53, week53.isoWeek)
        assertEquals(2026, week53.isoYear)
        assertEquals(1, nextWeek1.isoWeek)
        assertEquals(2027, nextWeek1.isoYear)
        assertEquals(week52.next(), week53)
        assertEquals(week53.next(), nextWeek1)
    }

    @Test
    fun `2020 also had a week 53 which is continuous into 2021`() {
        val week53 = week("2020-12-31")
        assertEquals(53, week53.isoWeek)
        assertEquals(2020, week53.isoYear)
        assertEquals(week53.next(), week("2021-01-04"))
    }

    @Test
    fun `a year without week 53 goes from week 52 straight to week 1`() {
        assertEquals(1, week("2025-12-29").isoWeek)
        assertEquals(week("2025-12-28").next(), week("2025-12-29"))
    }

    @Test
    fun `dates before 1970 map to consecutive weeks too`() {
        assertEquals(week("1969-12-29"), week("1970-01-01"))
        assertEquals(week("1969-12-28").next(), week("1969-12-29"))
        assertEquals(DayOfWeek.MONDAY, javaDate(week("1969-06-15").monday).dayOfWeek)
    }

    @Test
    fun `every day for forty years agrees with java time ISO week fields`() {
        var day = java.time.LocalDate.of(1995, 1, 1)
        val end = java.time.LocalDate.of(2035, 12, 31)
        var previous: WeekKey? = null
        while (!day.isAfter(end)) {
            val key = WeekKey.of(AppDate.fromEpochDays(day.toEpochDay()))
            assertEquals("iso year of $day", day.get(IsoFields.WEEK_BASED_YEAR), key.isoYear)
            assertEquals("iso week of $day", day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), key.isoWeek)
            assertEquals(DayOfWeek.MONDAY, javaDate(key.monday).dayOfWeek)
            assertEquals(DayOfWeek.SUNDAY, javaDate(key.sunday).dayOfWeek)
            if (previous != null) {
                val step = key.ordinal - previous.ordinal
                val expectedStep = if (day.dayOfWeek == DayOfWeek.MONDAY) 1L else 0L
                assertEquals("ordinal step on $day", expectedStep, step)
            }
            previous = key
            day = day.plusDays(1)
        }
    }

    @Test
    fun `weeks order by time`() {
        assertTrue(week("2026-01-05") < week("2026-01-12"))
        assertTrue(week("2025-12-29") > week("2025-12-22"))
    }

    @Test
    fun `a week prints as its ISO year and week number`() {
        assertEquals("2026-W53", week("2026-12-30").toString())
        assertEquals("2026-W01", week("2026-01-01").toString())
    }
}
