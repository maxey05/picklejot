@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.person

import com.maxeydev.picklejot.domain.suggest.MAX_SUGGESTIONS
import com.maxeydev.picklejot.domain.suggest.matchesPrefixOrWordStart
import com.maxeydev.picklejot.domain.suggest.suggestionKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val MOST_RECENT_FIRST =
    compareByDescending<PersonUsage> { it.lastPlayedOn }
        .thenByDescending { it.lastLoggedAt }
        .thenBy { suggestionKey(it.person.displayName) }
        .thenBy { it.person.id.toString() }

fun suggestPeople(
    query: String,
    candidates: List<PersonUsage>,
    excludedIds: Set<Uuid> = emptySet(),
    limit: Int = MAX_SUGGESTIONS,
): List<Person> {
    val queryKey = suggestionKey(query)
    if (queryKey.isEmpty() || limit <= 0) {
        return emptyList()
    }
    return candidates
        .asSequence()
        .filter { it.person.id !in excludedIds }
        .filter { matchesPrefixOrWordStart(suggestionKey(it.person.displayName), queryKey) }
        .sortedWith(MOST_RECENT_FIRST)
        .map { it.person }
        .take(limit)
        .toList()
}
