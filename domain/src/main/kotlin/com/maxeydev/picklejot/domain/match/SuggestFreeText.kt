package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.suggest.MAX_SUGGESTIONS
import com.maxeydev.picklejot.domain.suggest.matchesPrefixOrWordStart
import com.maxeydev.picklejot.domain.suggest.suggestionKey

private val MOST_RECENT_FIRST =
    compareByDescending<FreeTextUsage> { it.lastPlayedOn }
        .thenByDescending { it.lastLoggedAt }
        .thenBy { suggestionKey(it.value) }

private const val DEFAULT_RECENT_LIMIT = 3

fun recentFreeText(
    candidates: List<FreeTextUsage>,
    limit: Int = DEFAULT_RECENT_LIMIT,
): List<String> =
    candidates
        .asSequence()
        .filter { it.value.isNotBlank() }
        .sortedWith(MOST_RECENT_FIRST)
        .distinctBy { suggestionKey(it.value) }
        .map { it.value }
        .take(limit)
        .toList()

fun suggestFreeText(
    query: String,
    candidates: List<FreeTextUsage>,
    limit: Int = MAX_SUGGESTIONS,
): List<String> {
    val queryKey = suggestionKey(query)
    if (queryKey.isEmpty() || limit <= 0) {
        return emptyList()
    }
    return candidates
        .asSequence()
        .filter { it.value.isNotBlank() }
        .sortedWith(MOST_RECENT_FIRST)
        .distinctBy { suggestionKey(it.value) }
        .filter { suggestionKey(it.value) != queryKey }
        .filter { matchesPrefixOrWordStart(suggestionKey(it.value), queryKey) }
        .map { it.value }
        .take(limit)
        .toList()
}
