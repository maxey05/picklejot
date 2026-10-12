package com.maxeydev.picklejot.data.match

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.maxeydev.picklejot.data.photo.PhotoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface MatchDao {
    @Transaction
    @Query("$FILTERED_MATCH_LIST ORDER BY $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByDateNewest(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
        limit: Int,
    ): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$FILTERED_MATCH_LIST ORDER BY $OLDEST_FIRST $PAGE_LIMIT")
    fun observeListByDateOldest(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
        limit: Int,
    ): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$FILTERED_MATCH_LIST ORDER BY $WINS_BEFORE_LOSSES, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByWinsFirst(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
        limit: Int,
    ): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$FILTERED_MATCH_LIST ORDER BY $LOSSES_BEFORE_WINS, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByLossesFirst(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
        limit: Int,
    ): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$FILTERED_MATCH_LIST ORDER BY $NO_OPPONENT_LAST_THEN_A_TO_Z, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByOpponent(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
        limit: Int,
    ): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$FILTERED_MATCH_LIST ORDER BY $NO_LOCATION_LAST_THEN_A_TO_Z, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByLocation(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
        limit: Int,
    ): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$FILTERED_MATCH_LIST ORDER BY $NO_DURATION_LAST, $DURATION_SECONDS ASC, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByDurationShortest(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
        limit: Int,
    ): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$FILTERED_MATCH_LIST ORDER BY $NO_DURATION_LAST, $DURATION_SECONDS DESC, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByDurationLongest(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
        limit: Int,
    ): Flow<List<MatchListRowEntity>>

    @Query("$MATCH_STAT_SELECT $MATCH_LIST_FILTER")
    fun observeStatLines(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
    ): Flow<List<MatchStatRowEntity>>

    @Query("$ADVANCED_MATCH_SELECT $MATCH_LIST_FILTER")
    fun observeAdvancedRows(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
    ): Flow<List<AdvancedMatchRowEntity>>

    @Query(MATCH_PEOPLE_FOR_FILTER)
    fun observeAdvancedPeople(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
    ): Flow<List<MatchPersonRefEntity>>

    @Transaction
    @Query("SELECT COUNT(*) FROM `match`")
    fun observeMatchCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM `match` m $MATCH_LIST_FILTER")
    fun observeFilteredMatchCount(
        format: String?,
        result: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        opponentId: String?,
        location: String?,
        textPattern: String?,
        namePattern: String?,
    ): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM `match` m, `match` t WHERE t.id = :id AND (m.date < t.date OR " +
            "(m.date = t.date AND (m.created_at < t.created_at OR " +
            "(m.created_at = t.created_at AND m.id <= t.id))))",
    )
    fun observeMatchNumber(id: String): Flow<Int>

    @Query("SELECT * FROM `match` WHERE id = :id")
    fun observeById(id: String): Flow<MatchWithRelationsEntity?>

    @Query(
        "SELECT location AS value, MAX(date) AS last_played_on, MAX(created_at) AS last_logged_at " +
            "FROM `match` WHERE location IS NOT NULL AND location != '' " +
            "GROUP BY location ORDER BY last_played_on DESC, last_logged_at DESC",
    )
    fun observePriorLocations(): Flow<List<FreeTextUsageRowEntity>>

    @Query(
        "SELECT paddle AS value, MAX(date) AS last_played_on, MAX(created_at) AS last_logged_at " +
            "FROM `match` WHERE paddle IS NOT NULL AND paddle != '' " +
            "GROUP BY paddle ORDER BY last_played_on DESC, last_logged_at DESC",
    )
    fun observePriorPaddles(): Flow<List<FreeTextUsageRowEntity>>

    @Upsert
    suspend fun upsertMatch(match: MatchEntity)

    @Insert
    suspend fun insertPeople(rows: List<MatchPersonEntity>)

    @Insert
    suspend fun insertGames(rows: List<GameScoreEntity>)

    @Query("DELETE FROM match_person WHERE match_id = :matchId")
    suspend fun deletePeopleFor(matchId: String)

    @Query("DELETE FROM game_score WHERE match_id = :matchId")
    suspend fun deleteGamesFor(matchId: String)

    @Upsert
    suspend fun upsertPhotos(rows: List<PhotoEntity>)

    @Query("SELECT relative_path FROM photo WHERE match_id = :matchId AND id IN (:photoIds)")
    suspend fun photoPathsAmong(
        matchId: String,
        photoIds: List<String>,
    ): List<String>

    @Query("DELETE FROM photo WHERE match_id = :matchId AND id IN (:photoIds)")
    suspend fun deletePhotosAmong(
        matchId: String,
        photoIds: List<String>,
    )

    @Query("SELECT MAX(sort_index) FROM photo WHERE match_id = :matchId")
    suspend fun maxPhotoSortIndex(matchId: String): Int?

    @Query("SELECT EXISTS(SELECT 1 FROM `match` WHERE id = :matchId)")
    suspend fun matchExists(matchId: String): Boolean

    @Query("SELECT relative_path FROM photo")
    suspend fun allPhotoPaths(): List<String>

    @Query("SELECT relative_path FROM photo WHERE match_id = :matchId")
    suspend fun photoPathsFor(matchId: String): List<String>

    @Query("DELETE FROM `match` WHERE id = :id")
    suspend fun deleteMatch(id: String)

    @Transaction
    @Query("SELECT * FROM `match` ORDER BY date ASC, created_at ASC, id ASC")
    suspend fun allWithRelations(): List<MatchWithRelationsEntity>

    @Query("SELECT id FROM `match`")
    suspend fun allMatchIds(): List<String>

    @Query("SELECT COUNT(*) FROM `match`")
    suspend fun matchCount(): Int

    @Query("SELECT id FROM photo")
    suspend fun allPhotoIds(): List<String>
}
