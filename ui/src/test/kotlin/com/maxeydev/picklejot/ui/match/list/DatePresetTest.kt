@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.list

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.FilterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi

class DatePresetTest {
    private val today = AppDate.parse("2026-09-30")

    @Test
    fun `this month runs from the first of the month to today`() {
        assertEquals(
            AppDate.parse("2026-09-01") to today,
            DatePreset.THIS_MONTH.rangeEndingOn(today),
        )
    }

    @Test
    fun `this year runs from january first to today`() {
        assertEquals(
            AppDate.parse("2026-01-01") to today,
            DatePreset.THIS_YEAR.rangeEndingOn(today),
        )
    }

    @Test
    fun `last three months reaches back three calendar months`() {
        assertEquals(
            AppDate.parse("2026-06-30") to today,
            DatePreset.LAST_THREE_MONTHS.rangeEndingOn(today),
        )
    }

    @Test
    fun `last three months clamps to the end of a shorter month`() {
        val endOfMay = AppDate.parse("2026-05-31")

        assertEquals(
            AppDate.parse("2026-02-28") to endOfMay,
            DatePreset.LAST_THREE_MONTHS.rangeEndingOn(endOfMay),
        )
    }

    @Test
    fun `a filter whose range equals a preset is recognised as that preset`() {
        DatePreset.entries.forEach { preset ->
            val (from, to) = preset.rangeEndingOn(today)

            assertEquals(preset, FilterState(fromDate = from, toDate = to).matchingDatePreset(today))
        }
    }

    @Test
    fun `a custom range or no range matches no preset`() {
        val custom = FilterState(fromDate = AppDate.parse("2026-09-02"), toDate = today)

        assertNull(custom.matchingDatePreset(today))
        assertNull(FilterState.NONE.matchingDatePreset(today))
    }
}
