package com.maxeydev.picklejot.data.person

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM person ORDER BY display_name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query(
        "SELECT p.*, MAX(m.date) AS last_played_on, MAX(m.created_at) AS last_logged_at " +
            "FROM person p " +
            "INNER JOIN match_person mp ON mp.person_id = p.id " +
            "INNER JOIN `match` m ON m.id = mp.match_id " +
            "GROUP BY p.id " +
            "ORDER BY last_played_on DESC, last_logged_at DESC, p.normalized_name ASC",
    )
    fun observeRecentlyUsed(): Flow<List<PersonUsageRowEntity>>

    @Query("SELECT * FROM person WHERE id = :id")
    suspend fun findById(id: String): PersonEntity?

    @Query("SELECT * FROM person WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicate(person: PersonEntity)

    @Query("SELECT * FROM person ORDER BY created_at ASC, id ASC")
    suspend fun all(): List<PersonEntity>
}
