@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FilterStateTest {
    private val everything =
        FilterState(
            format = MatchFormat.DOUBLES,
            result = MatchResult.WIN,
            fromDate = AppDate.parse("2026-01-01"),
            toDate = AppDate.parse("2026-03-31"),
            opponentId = Uuid.random(),
            location = "BGC Courts",
        )

    @Test
    fun `no filter is inactive and reports no active kinds`() {
        assertFalse(FilterState.NONE.isActive)
        assertEquals(emptyList<FilterKind>(), FilterState.NONE.activeKinds)
    }

    @Test
    fun `every set filter is reported once in a stable order`() {
        assertTrue(everything.isActive)
        assertEquals(FilterKind.entries.toList(), everything.activeKinds)
    }

    @Test
    fun `a date range open at either end still counts as one active date filter`() {
        assertEquals(listOf(FilterKind.DATE_RANGE), FilterState(fromDate = AppDate.parse("2026-01-01")).activeKinds)
        assertEquals(listOf(FilterKind.DATE_RANGE), FilterState(toDate = AppDate.parse("2026-01-01")).activeKinds)
    }

    @Test
    fun `clearing one filter leaves every other filter in place`() {
        FilterKind.entries.forEach { cleared ->
            val remaining = everything.without(cleared)

            assertEquals(FilterKind.entries.filter { it != cleared }, remaining.activeKinds)
        }
    }

    @Test
    fun `clearing the date range removes both ends`() {
        val cleared = everything.without(FilterKind.DATE_RANGE)

        assertEquals(null, cleared.fromDate)
        assertEquals(null, cleared.toDate)
    }

    @Test
    fun `clearing every kind one by one ends at no filter`() {
        val cleared = FilterKind.entries.fold(everything) { state, kind -> state.without(kind) }

        assertEquals(FilterState.NONE, cleared)
    }

    @Test
    fun `a range ending before it starts is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            FilterState(fromDate = AppDate.parse("2026-02-01"), toDate = AppDate.parse("2026-01-31"))
        }
    }

    @Test
    fun `a single-day range is allowed`() {
        val day = AppDate.parse("2026-02-01")

        assertEquals(listOf(FilterKind.DATE_RANGE), FilterState(fromDate = day, toDate = day).activeKinds)
    }

    @Test
    fun `a blank location filter is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { FilterState(location = "  ") }
    }
}
