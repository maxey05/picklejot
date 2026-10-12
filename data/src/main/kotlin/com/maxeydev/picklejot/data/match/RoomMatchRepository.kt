@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import androidx.room.withTransaction
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.data.photo.toEntity
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.FreeTextField
import com.maxeydev.picklejot.domain.match.FreeTextUsage
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchListItem
import com.maxeydev.picklejot.domain.match.MatchRepository
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.match.SearchTerm
import com.maxeydev.picklejot.domain.match.requireValidRoster
import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.stats.AdvancedMatchLine
import com.maxeydev.picklejot.domain.stats.MatchStatLine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private typealias FilteredListQuery = (
    format: String?,
    result: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    opponentId: String?,
    location: String?,
    textPattern: String?,
    namePattern: String?,
    limit: Int,
) -> Flow<List<MatchListRowEntity>>

class RoomMatchRepository(
    private val database: PicklejotDatabase,
    private val photoStore: PhotoStore,
    private val ioDispatcher: CoroutineDispatcher,
) : MatchRepository {
    private val matchDao = database.matchDao()

    override fun observeListPage(
        sort: MatchSort,
        limit: Int,
        filter: FilterState,
        search: SearchTerm?,
    ): Flow<List<MatchListItem>> {
        require(limit > 0) { "A list page needs a positive limit, but was $limit." }
        val query = listQueryFor(sort)
        val rows =
            query(
                filter.format?.name,
                filter.result?.name,
                filter.fromDate,
                filter.toDate,
                filter.opponentId?.toString(),
                filter.location,
                search?.textPattern,
                search?.namePattern,
                limit,
            )
        return rows
            .map { page -> page.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    private fun listQueryFor(sort: MatchSort): FilteredListQuery =
        when (sort) {
            MatchSort.DATE_NEWEST -> matchDao::observeListByDateNewest
            MatchSort.DATE_OLDEST -> matchDao::observeListByDateOldest
            MatchSort.RESULT_WINS_FIRST -> matchDao::observeListByWinsFirst
            MatchSort.RESULT_LOSSES_FIRST -> matchDao::observeListByLossesFirst
            MatchSort.OPPONENT_A_TO_Z -> matchDao::observeListByOpponent
            MatchSort.LOCATION_A_TO_Z -> matchDao::observeListByLocation
            MatchSort.DURATION_SHORTEST -> matchDao::observeListByDurationShortest
            MatchSort.DURATION_LONGEST -> matchDao::observeListByDurationLongest
        }

    override fun observeStatLines(filter: FilterState): Flow<List<MatchStatLine>> =
        matchDao
            .observeStatLines(
                format = filter.format?.name,
                result = filter.result?.name,
                fromDate = filter.fromDate,
                toDate = filter.toDate,
                opponentId = filter.opponentId?.toString(),
                location = filter.location,
                textPattern = null,
                namePattern = null,
            ).map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeAdvancedLines(filter: FilterState): Flow<List<AdvancedMatchLine>> {
        val rows =
            matchDao.observeAdvancedRows(
                format = filter.format?.name,
                result = filter.result?.name,
                fromDate = filter.fromDate,
                toDate = filter.toDate,
                opponentId = filter.opponentId?.toString(),
                location = filter.location,
                textPattern = null,
                namePattern = null,
            )
        val people =
            matchDao.observeAdvancedPeople(
                format = filter.format?.name,
                result = filter.result?.name,
                fromDate = filter.fromDate,
                toDate = filter.toDate,
                opponentId = filter.opponentId?.toString(),
                location = filter.location,
                textPattern = null,
                namePattern = null,
            )
        return combine(rows, people, ::toAdvancedLines).flowOn(ioDispatcher)
    }

    override fun observeById(id: Uuid): Flow<Match?> =
        matchDao
            .observeById(id.toString())
            .map { row -> row?.toDomain() }
            .flowOn(ioDispatcher)

    override fun observeMatchCount(): Flow<Int> = matchDao.observeMatchCount().flowOn(ioDispatcher)

    override fun observeMatchNumber(id: Uuid): Flow<Int> =
        matchDao.observeMatchNumber(id.toString()).flowOn(ioDispatcher)

    override fun observeFilteredMatchCount(
        filter: FilterState,
        search: SearchTerm?,
    ): Flow<Int> =
        matchDao
            .observeFilteredMatchCount(
                format = filter.format?.name,
                result = filter.result?.name,
                fromDate = filter.fromDate,
                toDate = filter.toDate,
                opponentId = filter.opponentId?.toString(),
                location = filter.location,
                textPattern = search?.textPattern,
                namePattern = search?.namePattern,
            ).flowOn(ioDispatcher)

    override fun observePriorValues(field: FreeTextField): Flow<List<FreeTextUsage>> {
        val rows =
            when (field) {
                FreeTextField.LOCATION -> matchDao.observePriorLocations()
                FreeTextField.PADDLE -> matchDao.observePriorPaddles()
            }
        return rows
            .map { values -> values.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveMatch(
        match: Match,
        removedPhotoIds: Set<Uuid>,
    ) {
        match.requireValidRoster()
        val matchId = match.id.toString()
        val keptIds = match.photos.map { it.id }.toSet()
        val removed = (removedPhotoIds - keptIds).map { it.toString() }
        withContext(ioDispatcher) {
            val removedPaths =
                database.withTransaction {
                    val paths = if (removed.isEmpty()) emptyList() else matchDao.photoPathsAmong(matchId, removed)
                    matchDao.upsertMatch(match.toEntity())
                    matchDao.deletePeopleFor(matchId)
                    matchDao.deleteGamesFor(matchId)
                    if (removed.isNotEmpty()) {
                        matchDao.deletePhotosAmong(matchId, removed)
                    }
                    matchDao.insertPeople(match.toMatchPersonRows())
                    matchDao.insertGames(match.toGameScoreRows())
                    matchDao.upsertPhotos(match.toPhotoRows())
                    paths
                }
            photoStore.deletePhotoFiles(removedPaths)
        }
    }

    override suspend fun appendPhoto(
        matchId: Uuid,
        photo: ImportedPhoto,
    ): Boolean {
        val id = matchId.toString()
        return withContext(ioDispatcher) {
            database.withTransaction {
                if (!matchDao.matchExists(id)) {
                    return@withTransaction false
                }
                val nextIndex = (matchDao.maxPhotoSortIndex(id) ?: -1) + 1
                matchDao.upsertPhotos(listOf(photo.toPhotoRef(Uuid.random(), nextIndex).toEntity(id)))
                true
            }
        }
    }

    suspend fun referencedPhotoPaths(): Set<String> =
        withContext(ioDispatcher) {
            matchDao.allPhotoPaths().toSet()
        }

    override suspend fun deleteMatch(id: Uuid) {
        val matchId = id.toString()
        withContext(ioDispatcher) {
            val photoPaths =
                database.withTransaction {
                    val paths = matchDao.photoPathsFor(matchId)
                    matchDao.deleteMatch(matchId)
                    paths
                }
            photoStore.deletePhotoFiles(photoPaths)
        }
    }
}
