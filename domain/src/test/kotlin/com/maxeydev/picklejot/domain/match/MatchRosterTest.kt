@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.person.Person
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MatchRosterTest {
    private fun person(displayName: String): Person =
        Person(
            id = Uuid.random(),
            displayName = displayName,
            createdAt = AppInstant.fromEpochMilliseconds(0),
        )

    private fun match(
        format: MatchFormat,
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
    ): Match =
        Match(
            id = Uuid.random(),
            format = format,
            date = AppDate.parse("2026-09-23"),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(0),
            updatedAt = AppInstant.fromEpochMilliseconds(0),
            opponents = opponents,
            partner = partner,
        )

    @Test
    fun `a singles match carrying a partner is rejected`() {
        val invalid = match(MatchFormat.SINGLES, partner = person("Ana"))
        assertThrows(IllegalArgumentException::class.java) { invalid.requireValidRoster() }
    }

    @Test
    fun `a singles match with two opponents is rejected`() {
        val invalid = match(MatchFormat.SINGLES, opponents = listOf(person("Ana"), person("Ben")))
        assertThrows(IllegalArgumentException::class.java) { invalid.requireValidRoster() }
    }

    @Test
    fun `a doubles match with three opponents is rejected`() {
        val invalid =
            match(
                MatchFormat.DOUBLES,
                opponents = listOf(person("Ana"), person("Ben"), person("Cy")),
            )
        assertThrows(IllegalArgumentException::class.java) { invalid.requireValidRoster() }
    }

    @Test
    fun `a singles match with no opponent is accepted`() {
        match(MatchFormat.SINGLES).requireValidRoster()
    }

    @Test
    fun `a singles match with one opponent is accepted`() {
        match(MatchFormat.SINGLES, opponents = listOf(person("Ana"))).requireValidRoster()
    }

    @Test
    fun `a doubles match with no opponents and no partner is accepted`() {
        match(MatchFormat.DOUBLES).requireValidRoster()
    }

    @Test
    fun `a doubles match with two opponents and a partner is accepted`() {
        match(
            MatchFormat.DOUBLES,
            opponents = listOf(person("Ana"), person("Ben")),
            partner = person("Cy"),
        ).requireValidRoster()
    }

    @Test
    fun `a doubles match with a partner but no opponents is accepted`() {
        match(MatchFormat.DOUBLES, partner = person("Cy")).requireValidRoster()
    }

    @Test
    fun `the same person in both opponent slots is rejected`() {
        val ana = person("Ana")
        val invalid = match(MatchFormat.DOUBLES, opponents = listOf(ana, ana))
        assertThrows(IllegalArgumentException::class.java) { invalid.requireValidRoster() }
    }

    @Test
    fun `the same person as partner and first opponent is rejected`() {
        val ana = person("Ana")
        val invalid = match(MatchFormat.DOUBLES, opponents = listOf(ana, person("Ben")), partner = ana)
        assertThrows(IllegalArgumentException::class.java) { invalid.requireValidRoster() }
    }

    @Test
    fun `the same person as partner and second opponent is rejected`() {
        val ben = person("Ben")
        val invalid = match(MatchFormat.DOUBLES, opponents = listOf(person("Ana"), ben), partner = ben)
        assertThrows(IllegalArgumentException::class.java) { invalid.requireValidRoster() }
    }

    @Test
    fun `two different people who share a display name are accepted`() {
        match(
            MatchFormat.DOUBLES,
            opponents = listOf(person("Sam"), person("Sam")),
        ).requireValidRoster()
    }
}
