package com.maxeydev.picklejot.data.match

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.maxeydev.picklejot.data.person.PersonEntity

@Entity(
    tableName = "match_person",
    primaryKeys = ["match_id", "role", "slot"],
    indices = [Index(value = ["person_id"])],
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["match_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
)
data class MatchPersonEntity(
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "person_id")
    val personId: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "slot")
    val slot: Int,
) {
    companion object {
        const val ROLE_OPPONENT = "OPPONENT"
        const val ROLE_PARTNER = "PARTNER"
    }
}
