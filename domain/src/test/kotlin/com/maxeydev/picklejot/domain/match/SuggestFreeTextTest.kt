package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestFreeTextTest {
    private fun usage(
        value: String,
        playedOn: String,
        loggedAtMillis: Long = 0,
    ): FreeTextUsage =
        FreeTextUsage(
            value = value,
            lastPlayedOn = AppDate.parse(playedOn),
            lastLoggedAt = AppInstant.fromEpochMilliseconds(loggedAtMillis),
        )

    @Test
    fun `prior values matching by prefix or word start are suggested most recent first`() {
        val candidates =
            listOf(
                usage("Ayala Triangle", "2026-08-01"),
                usage("BGC Courts", "2026-09-01"),
                usage("Alabang Town Center", "2026-09-15"),
            )
        assertEquals(listOf("Alabang Town Center", "Ayala Triangle"), suggestFreeText("a", candidates))
        assertEquals(listOf("BGC Courts"), suggestFreeText("court", candidates))
    }

    @Test
    fun `case variants collapse into the most recently used spelling`() {
        val candidates =
            listOf(
                usage("ayala triangle", "2026-08-01"),
                usage("Ayala Triangle", "2026-09-01"),
            )
        assertEquals(listOf("Ayala Triangle"), suggestFreeText("ay", candidates))
    }

    @Test
    fun `the value already typed in full is not suggested back`() {
        val candidates = listOf(usage("Ayala", "2026-09-01"), usage("Ayala Triangle", "2026-08-01"))
        assertEquals(listOf("Ayala Triangle"), suggestFreeText("ayala", candidates))
    }

    @Test
    fun `a mid-word fragment suggests nothing`() {
        assertTrue(suggestFreeText("yal", listOf(usage("Ayala", "2026-09-01"))).isEmpty())
    }

    @Test
    fun `a blank query or no history suggests nothing`() {
        assertTrue(suggestFreeText("", listOf(usage("Ayala", "2026-09-01"))).isEmpty())
        assertTrue(suggestFreeText("a", emptyList()).isEmpty())
    }

    @Test
    fun `recent values are the latest distinct spellings capped at the limit`() {
        val candidates =
            listOf(
                usage("ayala triangle", "2026-08-01"),
                usage("Ayala Triangle", "2026-09-01"),
                usage("BGC Courts", "2026-09-10"),
                usage("Alabang Town Center", "2026-09-15"),
                usage("Makati Sports Club", "2026-07-01"),
            )
        assertEquals(
            listOf("Alabang Town Center", "BGC Courts", "Ayala Triangle"),
            recentFreeText(candidates),
        )
        assertEquals(listOf("Alabang Town Center"), recentFreeText(candidates, limit = 1))
    }

    @Test
    fun `no history or blank values give no recent values`() {
        assertTrue(recentFreeText(emptyList()).isEmpty())
        assertTrue(recentFreeText(listOf(usage("  ", "2026-09-01"))).isEmpty())
    }
}
