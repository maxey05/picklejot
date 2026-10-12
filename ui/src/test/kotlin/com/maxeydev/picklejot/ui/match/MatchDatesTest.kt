package com.maxeydev.picklejot.ui.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MatchDatesTest {
    @Test
    fun `a date survives the round trip through the date picker's utc millis`() {
        listOf("2026-09-24", "2026-01-01", "2024-02-29", "2026-12-31").forEach { value ->
            val date = AppDate.parse(value)
            assertEquals(date, utcEpochMillisToAppDate(date.toUtcEpochMillis()))
        }
    }

    @Test
    fun `utc midnight maps to that calendar day`() {
        assertEquals(1_790_208_000_000L, AppDate.parse("2026-09-24").toUtcEpochMillis())
    }

    @Test
    fun `a short date in the current year leaves the year out`() {
        val today = AppDate.parse("2026-09-30")

        assertEquals("Sep 20", formatMatchDateShort(AppDate.parse("2026-09-20"), today, Locale.US))
    }

    @Test
    fun `a short date from another year keeps the year`() {
        val today = AppDate.parse("2026-01-02")

        assertEquals("Dec 31, 2025", formatMatchDateShort(AppDate.parse("2025-12-31"), today, Locale.US))
    }

    @Test
    fun `the spoken form of a date spells the month out in full`() {
        assertEquals("September 20, 2026", formatMatchDateLong(AppDate.parse("2026-09-20"), Locale.US))
    }
}
