@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.person.PersonRepository
import com.maxeydev.picklejot.domain.person.PersonUsage
import com.maxeydev.picklejot.domain.person.normalizePersonName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FakePersonRepository(
    initial: List<Person> = emptyList(),
    private val matchSource: FakeMatchRepository? = null,
) : PersonRepository {
    private val people = MutableStateFlow(initial)

    val current: List<Person>
        get() = people.value

    var recentlyUsedSubscriptions: Int = 0
        private set

    val findOrCreateRequests = mutableListOf<String>()

    override fun observeAll(): Flow<List<Person>> = people

    override fun observeRecentlyUsed(): Flow<List<PersonUsage>> {
        recentlyUsedSubscriptions += 1
        val source = matchSource ?: return flowOf(emptyList())
        return source.observeAllMatches().map { matches -> usageFrom(matches) }
    }

    override suspend fun findById(id: Uuid): Person? = people.value.firstOrNull { it.id == id }

    override suspend fun findOrCreatePerson(displayName: String): Person {
        findOrCreateRequests += displayName
        val normalized = normalizePersonName(displayName)
        require(normalized.isNotEmpty()) { "A person needs a display name with at least one non-whitespace character." }
        people.value.firstOrNull { normalizePersonName(it.displayName) == normalized }?.let { return it }
        val created = Person(Uuid.random(), displayName.trim(), AppInstant.fromEpochMilliseconds(0))
        people.update { it + created }
        return created
    }

    private fun usageFrom(matches: List<Match>): List<PersonUsage> =
        matches
            .flatMap { match -> (match.opponents + listOfNotNull(match.partner)).map { it to match } }
            .groupBy({ it.first.id }, { it })
            .map { (_, appearances) ->
                PersonUsage(
                    person = appearances.first().first,
                    lastPlayedOn = appearances.maxOf { it.second.date },
                    lastLoggedAt = appearances.maxOf { it.second.createdAt },
                )
            }
}
