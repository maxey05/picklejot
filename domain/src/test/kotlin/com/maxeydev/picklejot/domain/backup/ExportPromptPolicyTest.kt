package com.maxeydev.picklejot.domain.backup

import com.maxeydev.picklejot.domain.datetime.AppInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportPromptPolicyTest {
    private val start = AppInstant.parse("2026-09-01T00:00:00Z")
    private val tracking = ExportPromptState(trackingSince = start, anchorCount = 10)

    private fun daysAfterStart(days: Int): AppInstant =
        AppInstant.fromEpochMilliseconds(start.toEpochMilliseconds() + days * DAY_MILLIS)

    @Test
    fun `the first evaluation only starts tracking and never prompts`() {
        val result = ExportPromptPolicy.evaluate(ExportPromptState(), matchCount = 40, now = start)

        assertEquals(start, result.trackingSince)
        assertEquals(40, result.anchorCount)
        assertNull(result.pending)
    }

    @Test
    fun `twenty four new matches do not prompt`() {
        val result = ExportPromptPolicy.evaluate(tracking, matchCount = 34, now = daysAfterStart(1))

        assertNull(result.pending)
    }

    @Test
    fun `the twenty fifth new match prompts once and moves the anchor`() {
        val result = ExportPromptPolicy.evaluate(tracking, matchCount = 35, now = daysAfterStart(1))

        assertEquals(ExportPromptReason.MATCHES_ADDED, result.pending)
        assertEquals(35, result.anchorCount)
        assertEquals(daysAfterStart(1), result.lastPromptAt)
    }

    @Test
    fun `a dismissed count prompt does not return for the same matches`() {
        val prompted = ExportPromptPolicy.evaluate(tracking, matchCount = 35, now = daysAfterStart(1))
        val dismissed = ExportPromptPolicy.afterDismissal(prompted)

        val again = ExportPromptPolicy.evaluate(dismissed, matchCount = 36, now = daysAfterStart(2))

        assertNull(again.pending)
    }

    @Test
    fun `a second threshold crossed while a prompt is showing does not stack or repeat`() {
        val prompted = ExportPromptPolicy.evaluate(tracking, matchCount = 35, now = daysAfterStart(1))

        val whileShowing = ExportPromptPolicy.evaluate(prompted, matchCount = 60, now = daysAfterStart(2))
        val dismissed = ExportPromptPolicy.afterDismissal(whileShowing)
        val afterDismissal = ExportPromptPolicy.evaluate(dismissed, matchCount = 60, now = daysAfterStart(3))

        assertEquals(ExportPromptReason.MATCHES_ADDED, whileShowing.pending)
        assertEquals(60, whileShowing.anchorCount)
        assertNull(afterDismissal.pending)
    }

    @Test
    fun `deleting matches lowers the anchor so the next twenty five count from the lower total`() {
        val lowered = ExportPromptPolicy.evaluate(tracking, matchCount = 4, now = daysAfterStart(1))

        val result = ExportPromptPolicy.evaluate(lowered, matchCount = 29, now = daysAfterStart(2))

        assertEquals(4, lowered.anchorCount)
        assertEquals(ExportPromptReason.MATCHES_ADDED, result.pending)
    }

    @Test
    fun `thirty days without an export prompts periodically`() {
        val result = ExportPromptPolicy.evaluate(tracking, matchCount = 12, now = daysAfterStart(30))

        assertEquals(ExportPromptReason.PERIODIC, result.pending)
    }

    @Test
    fun `twenty nine days do not prompt`() {
        val result = ExportPromptPolicy.evaluate(tracking, matchCount = 12, now = daysAfterStart(29))

        assertNull(result.pending)
    }

    @Test
    fun `no periodic prompt is shown when there is nothing to back up`() {
        val empty = tracking.copy(anchorCount = 0)

        val result = ExportPromptPolicy.evaluate(empty, matchCount = 0, now = daysAfterStart(90))

        assertNull(result.pending)
    }

    @Test
    fun `a periodic prompt resets its own clock so it does not nag daily`() {
        val prompted = ExportPromptPolicy.evaluate(tracking, matchCount = 12, now = daysAfterStart(30))
        val dismissed = ExportPromptPolicy.afterDismissal(prompted)

        val nextDay = ExportPromptPolicy.evaluate(dismissed, matchCount = 12, now = daysAfterStart(31))
        val nextMonth = ExportPromptPolicy.evaluate(dismissed, matchCount = 12, now = daysAfterStart(60))

        assertNull(nextDay.pending)
        assertEquals(ExportPromptReason.PERIODIC, nextMonth.pending)
    }

    @Test
    fun `an export restarts the periodic clock, resets the anchor and clears the prompt`() {
        val prompted = ExportPromptPolicy.evaluate(tracking, matchCount = 35, now = daysAfterStart(1))

        val exported = ExportPromptPolicy.afterExport(prompted, matchCount = 35, now = daysAfterStart(5))
        val soon = ExportPromptPolicy.evaluate(exported, matchCount = 35, now = daysAfterStart(34))
        val later = ExportPromptPolicy.evaluate(exported, matchCount = 35, now = daysAfterStart(35))

        assertNull(exported.pending)
        assertEquals(daysAfterStart(5), exported.lastExportAt)
        assertNull(soon.pending)
        assertEquals(ExportPromptReason.PERIODIC, later.pending)
    }

    @Test
    fun `an import does not immediately trigger a count prompt`() {
        val imported = ExportPromptPolicy.afterImport(tracking, matchCount = 50)

        val result = ExportPromptPolicy.evaluate(imported, matchCount = 50, now = daysAfterStart(1))

        assertNull(result.pending)
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
