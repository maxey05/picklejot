@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import androidx.room.ColumnInfo
import androidx.room.Relation
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchListItem
import com.maxeydev.picklejot.domain.match.MatchResult
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class MatchListRowEntity(
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "result")
    val result: String,
    @ColumnInfo(name = "first_opponent")
    val firstOpponent: String?,
    @ColumnInfo(name = "second_opponent")
    val secondOpponent: String?,
    @ColumnInfo(name = "partner_name")
    val partnerName: String?,
    @ColumnInfo(name = "location")
    val location: String?,
    @ColumnInfo(name = "primary_photo_path")
    val primaryPhotoPath: String?,
    @Relation(parentColumn = "id", entityColumn = "match_id")
    val games: List<GameScoreEntity>,
)

internal fun MatchListRowEntity.toDomain(): MatchListItem =
    MatchListItem(
        id = Uuid.parse(id),
        date = date,
        format = MatchFormat.valueOf(format),
        result = MatchResult.valueOf(result),
        opponentNames = listOfNotNull(firstOpponent, secondOpponent),
        games = games.sortedBy { it.gameNumber }.map { it.toDomain() },
        primaryPhotoPath = primaryPhotoPath,
        partnerName = partnerName,
        location = location,
    )
