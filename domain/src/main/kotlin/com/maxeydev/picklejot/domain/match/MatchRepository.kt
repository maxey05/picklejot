@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.stats.AdvancedMatchLine
import com.maxeydev.picklejot.domain.stats.MatchStatLine
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface MatchRepository {
    fun observeListPage(
        sort: MatchSort,
        limit: Int,
        filter: FilterState = FilterState.NONE,
        search: SearchTerm? = null,
    ): Flow<List<MatchListItem>>

    fun observeStatLines(filter: FilterState = FilterState.NONE): Flow<List<MatchStatLine>>

    fun observeAdvancedLines(filter: FilterState = FilterState.NONE): Flow<List<AdvancedMatchLine>>

    fun observeById(id: Uuid): Flow<Match?>

    fun observeMatchCount(): Flow<Int>

    fun observeMatchNumber(id: Uuid): Flow<Int>

    fun observeFilteredMatchCount(
        filter: FilterState = FilterState.NONE,
        search: SearchTerm? = null,
    ): Flow<Int>

    fun observePriorValues(field: FreeTextField): Flow<List<FreeTextUsage>>

    suspend fun saveMatch(
        match: Match,
        removedPhotoIds: Set<Uuid> = emptySet(),
    )

    suspend fun appendPhoto(
        matchId: Uuid,
        photo: ImportedPhoto,
    ): Boolean

    suspend fun deleteMatch(id: Uuid)
}
