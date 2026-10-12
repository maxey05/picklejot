@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.FreeTextField
import com.maxeydev.picklejot.domain.match.FreeTextUsage
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchListItem
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.match.SearchTerm
import com.maxeydev.picklejot.domain.match.deriveDuration
import com.maxeydev.picklejot.domain.match.requireValidRoster
import com.maxeydev.picklejot.domain.person.normalizePersonName
import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.stats.AdvancedMatchLine
import com.maxeydev.picklejot.domain.stats.MatchStatLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ListPageRequest(
    val sort: MatchSort,
    val limit: Int,
    val filter: FilterState,
    val search: SearchTerm?,
)

class FakeMatchRepository(
    initial: List<Match> = emptyList(),
) : MatchRepository {
    private val matches = MutableStateFlow(initial.associateBy { it.id })

    val saved = mutableListOf<Match>()
    val deletedIds = mutableListOf<Uuid>()
    val removedPhotos = mutableSetOf<Uuid>()
    val appendedPhotos: MutableList<Pair<Uuid, ImportedPhoto>> = CopyOnWriteArrayList()
    val requestedPages: MutableList<Pair<MatchSort, Int>> = CopyOnWriteArrayList()
    val requestedQueries: MutableList<ListPageRequest> = CopyOnWriteArrayList()

    val current: List<Match>
        get() = matches.value.values.toList()

    override fun observeListPage(
        sort: MatchSort,
        limit: Int,
        filter: FilterState,
        search: SearchTerm?,
    ): Flow<List<MatchListItem>> {
        require(limit > 0) { "A list page needs a positive limit, but was $limit." }
        requestedPages += sort to limit
        requestedQueries += ListPageRequest(sort, limit, filter, search)
        return matches.map { byId ->
            byId.values
                .filter { it.passes(filter) && it.contains(search) }
                .sortedWith(orderFor(sort))
                .take(limit)
                .map { it.toListItem() }
        }
    }

    override fun observeStatLines(filter: FilterState): Flow<List<MatchStatLine>> =
        matches.map { byId ->
            byId.values
                .filter { it.passes(filter) }
                .map { MatchStatLine(date = it.date, format = it.format, result = it.result) }
        }

    override fun observeAdvancedLines(filter: FilterState): Flow<List<AdvancedMatchLine>> =
        matches.map { byId ->
            byId.values
                .filter { it.passes(filter) }
                .map { match ->
                    AdvancedMatchLine(
                        date = match.date,
                        format = match.format,
                        result = match.result,
                        opponentIds = match.opponents.map { it.id },
                        partnerId = match.partner?.id,
                        location = match.location,
                        paddle = match.paddle,
                    )
                }
        }

    override fun observeById(id: Uuid): Flow<Match?> = matches.map { byId -> byId[id] }

    override fun observeMatchCount(): Flow<Int> = matches.map { byId -> byId.size }

    override fun observeMatchNumber(id: Uuid): Flow<Int> =
        matches.map { byId ->
            val target = byId[id] ?: return@map 0
            val order =
                compareBy<Match> { it.date }
                    .thenBy { it.createdAt }
                    .thenBy { it.id.toString() }
            byId.values.count { order.compare(it, target) <= 0 }
        }

    override fun observeFilteredMatchCount(
        filter: FilterState,
        search: SearchTerm?,
    ): Flow<Int> = matches.map { byId -> byId.values.count { it.passes(filter) && it.contains(search) } }

    fun observeAllMatches(): Flow<List<Match>> = matches.map { byId -> byId.values.toList() }

    override fun observePriorValues(field: FreeTextField): Flow<List<FreeTextUsage>> =
        matches.map { byId ->
            byId.values
                .mapNotNull { match ->
                    val value =
                        when (field) {
                            FreeTextField.LOCATION -> match.location
                            FreeTextField.PADDLE -> match.paddle
                        }
                    value?.takeIf { it.isNotEmpty() }?.let { it to match }
                }.groupBy({ it.first }, { it.second })
                .map { (value, used) ->
                    FreeTextUsage(
                        value = value,
                        lastPlayedOn = used.maxOf { it.date },
                        lastLoggedAt = used.maxOf { it.createdAt },
                    )
                }
        }

    override suspend fun saveMatch(
        match: Match,
        removedPhotoIds: Set<Uuid>,
    ) {
        match.requireValidRoster()
        saved += match
        removedPhotos += removedPhotoIds
        matches.update { byId ->
            val existing = byId[match.id]
            val keptLate =
                existing
                    ?.photos
                    .orEmpty()
                    .filter { old -> old.id !in removedPhotoIds && match.photos.none { it.id == old.id } }
            byId + (match.id to match.copy(photos = match.photos + keptLate))
        }
    }

    override suspend fun appendPhoto(
        matchId: Uuid,
        photo: ImportedPhoto,
    ): Boolean {
        val existing = matches.value[matchId] ?: return false
        val nextIndex = (existing.photos.maxOfOrNull { it.sortIndex } ?: -1) + 1
        val appended = existing.copy(photos = existing.photos + photo.toPhotoRef(Uuid.random(), nextIndex))
        appendedPhotos += matchId to photo
        matches.update { byId -> byId + (matchId to appended) }
        return true
    }

    override suspend fun deleteMatch(id: Uuid) {
        deletedIds += id
        matches.update { byId -> byId - id }
    }

    private fun orderFor(sort: MatchSort): Comparator<Match> {
        val newestFirst =
            compareByDescending<Match> { it.date }
                .thenByDescending { it.createdAt }
                .thenBy { it.id.toString() }
        return when (sort) {
            MatchSort.DATE_NEWEST -> newestFirst
            MatchSort.DATE_OLDEST ->
                compareBy<Match> { it.date }
                    .thenBy { it.createdAt }
                    .thenBy { it.id.toString() }
            MatchSort.RESULT_WINS_FIRST -> compareBy<Match> { it.result != MatchResult.WIN }.then(newestFirst)
            MatchSort.RESULT_LOSSES_FIRST -> compareBy<Match> { it.result != MatchResult.LOSS }.then(newestFirst)
            MatchSort.OPPONENT_A_TO_Z ->
                compareBy<Match> { it.opponents.isEmpty() }
                    .thenBy { match -> match.opponents.firstOrNull()?.let { normalizePersonName(it.displayName) } }
                    .then(newestFirst)
            MatchSort.LOCATION_A_TO_Z ->
                compareBy<Match> { it.location.isNullOrEmpty() }
                    .thenBy { it.location.orEmpty().lowercase() }
                    .then(newestFirst)
            MatchSort.DURATION_SHORTEST ->
                compareBy<Match> { deriveDuration(it.startTime, it.endTime) == null }
                    .thenBy { deriveDuration(it.startTime, it.endTime) }
                    .then(newestFirst)
            MatchSort.DURATION_LONGEST ->
                compareBy<Match> { deriveDuration(it.startTime, it.endTime) == null }
                    .thenByDescending { deriveDuration(it.startTime, it.endTime) }
                    .then(newestFirst)
        }
    }

    private fun Match.passes(filter: FilterState): Boolean =
        (filter.format == null || format == filter.format) &&
            (filter.result == null || result == filter.result) &&
            (filter.fromDate?.let { date >= it } ?: true) &&
            (filter.toDate?.let { date <= it } ?: true) &&
            (filter.location == null || location == filter.location) &&
            (filter.opponentId == null || opponents.any { it.id == filter.opponentId })

    private fun Match.contains(search: SearchTerm?): Boolean {
        if (search == null) {
            return true
        }
        val needle = search.text.lowercase()
        val freeText = listOfNotNull(location, paddle, notes).any { it.lowercase().contains(needle) }
        val names =
            (opponents + listOfNotNull(partner)).any {
                normalizePersonName(it.displayName).contains(normalizePersonName(search.text))
            }
        return freeText || names
    }

    private fun Match.toListItem(): MatchListItem =
        MatchListItem(
            id = id,
            date = date,
            format = format,
            result = result,
            opponentNames = opponents.map { it.displayName },
            games = games.sortedBy { it.gameNumber },
            primaryPhotoPath = photos.minByOrNull { it.sortIndex }?.relativePath,
            partnerName = partner?.displayName,
            location = location,
        )
}
