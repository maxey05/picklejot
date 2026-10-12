@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.person

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SuggestPeopleTest {
    private fun usage(
        name: String,
        playedOn: String,
        loggedAtMillis: Long = 0,
    ): PersonUsage =
        PersonUsage(
            person = Person(Uuid.random(), name, AppInstant.fromEpochMilliseconds(0)),
            lastPlayedOn = AppDate.parse(playedOn),
            lastLoggedAt = AppInstant.fromEpochMilliseconds(loggedAtMillis),
        )

    private fun names(people: List<Person>): List<String> = people.map { it.displayName }

    @Test
    fun `R suggests Dave R by word start`() {
        val candidates = listOf(usage("Dave R.", "2026-09-01"), usage("Ana", "2026-09-02"))
        assertEquals(listOf("Dave R."), names(suggestPeople("R", candidates)))
    }

    @Test
    fun `matching is case insensitive`() {
        val candidates = listOf(usage("Dave R.", "2026-09-01"))
        assertEquals(listOf("Dave R."), names(suggestPeople("dAVE", candidates)))
    }

    @Test
    fun `matching ignores accents in either direction`() {
        val candidates = listOf(usage("José", "2026-09-01"), usage("Renee", "2026-09-01"))
        assertEquals(listOf("José"), names(suggestPeople("jose", candidates)))
        assertEquals(listOf("Renee"), names(suggestPeople("René", candidates)))
    }

    @Test
    fun `a mid-word fragment suggests nobody`() {
        val candidates = listOf(usage("Dave R.", "2026-09-01"))
        assertTrue(suggestPeople("av", candidates).isEmpty())
    }

    @Test
    fun `the person played most recently ranks first, not alphabetically`() {
        val candidates =
            listOf(
                usage("Ben A.", "2026-08-01"),
                usage("Ben C.", "2026-09-20"),
                usage("Ben B.", "2026-09-10"),
            )
        assertEquals(listOf("Ben C.", "Ben B.", "Ben A."), names(suggestPeople("ben", candidates)))
    }

    @Test
    fun `on the same play date the most recently logged ranks first`() {
        val candidates =
            listOf(
                usage("Ben A.", "2026-09-20", loggedAtMillis = 1_000),
                usage("Ben B.", "2026-09-20", loggedAtMillis = 5_000),
            )
        assertEquals(listOf("Ben B.", "Ben A."), names(suggestPeople("ben", candidates)))
    }

    @Test
    fun `excluded people are never suggested`() {
        val taken = usage("Ben A.", "2026-09-20")
        val candidates = listOf(taken, usage("Ben B.", "2026-09-01"))
        assertEquals(listOf("Ben B."), names(suggestPeople("ben", candidates, excludedIds = setOf(taken.person.id))))
    }

    @Test
    fun `results are capped at the limit`() {
        val candidates = (1..20).map { usage("Player $it", "2026-09-${it.toString().padStart(2, '0')}") }
        val suggested = suggestPeople("player", candidates)
        assertEquals(5, suggested.size)
        assertEquals("Player 20", suggested.first().displayName)
    }

    @Test
    fun `a blank query suggests nobody`() {
        val candidates = listOf(usage("Ana", "2026-09-01"))
        assertTrue(suggestPeople("   ", candidates).isEmpty())
    }

    @Test
    fun `no candidates means no suggestions`() {
        assertTrue(suggestPeople("a", emptyList()).isEmpty())
    }
}
