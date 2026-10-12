@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import androidx.room.ColumnInfo
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.stats.AdvancedMatchLine
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class AdvancedMatchRowEntity(
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "result")
    val result: String,
    @ColumnInfo(name = "location")
    val location: String?,
    @ColumnInfo(name = "paddle")
    val paddle: String?,
)

data class MatchPersonRefEntity(
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "person_id")
    val personId: String,
    @ColumnInfo(name = "role")
    val role: String,
)

internal fun toAdvancedLines(
    rows: List<AdvancedMatchRowEntity>,
    people: List<MatchPersonRefEntity>,
): List<AdvancedMatchLine> {
    val peopleByMatch = people.groupBy { it.matchId }
    return rows.map { row ->
        val roster = peopleByMatch[row.id].orEmpty()
        AdvancedMatchLine(
            date = row.date,
            format = MatchFormat.valueOf(row.format),
            result = MatchResult.valueOf(row.result),
            opponentIds =
                roster
                    .filter { it.role == MatchPersonEntity.ROLE_OPPONENT }
                    .map { Uuid.parse(it.personId) },
            partnerId =
                roster
                    .firstOrNull { it.role == MatchPersonEntity.ROLE_PARTNER }
                    ?.let { Uuid.parse(it.personId) },
            location = row.location,
            paddle = row.paddle,
        )
    }
}
