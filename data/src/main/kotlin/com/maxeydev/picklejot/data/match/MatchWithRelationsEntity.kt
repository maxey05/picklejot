@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import androidx.room.Embedded
import androidx.room.Relation
import com.maxeydev.picklejot.data.person.PersonWithRoleEntity
import com.maxeydev.picklejot.data.person.toDomain
import com.maxeydev.picklejot.data.photo.PhotoEntity
import com.maxeydev.picklejot.data.photo.toDomain
import com.maxeydev.picklejot.data.photo.toEntity
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class MatchWithRelationsEntity(
    @Embedded
    val match: MatchEntity,
    @Relation(
        entity = MatchPersonEntity::class,
        parentColumn = "id",
        entityColumn = "match_id",
    )
    val people: List<PersonWithRoleEntity>,
    @Relation(parentColumn = "id", entityColumn = "match_id")
    val games: List<GameScoreEntity>,
    @Relation(parentColumn = "id", entityColumn = "match_id")
    val photos: List<PhotoEntity>,
)

internal fun MatchWithRelationsEntity.toDomain(): Match =
    Match(
        id = Uuid.parse(match.id),
        format = MatchFormat.valueOf(match.format),
        date = match.date,
        result = MatchResult.valueOf(match.result),
        createdAt = match.createdAt,
        updatedAt = match.updatedAt,
        startTime = match.startTime,
        endTime = match.endTime,
        location = match.location,
        opponents =
            people
                .filter { it.link.role == MatchPersonEntity.ROLE_OPPONENT }
                .sortedBy { it.link.slot }
                .map { it.person.toDomain() },
        partner =
            people
                .singleOrNull { it.link.role == MatchPersonEntity.ROLE_PARTNER }
                ?.person
                ?.toDomain(),
        games = games.sortedBy { it.gameNumber }.map { it.toDomain() },
        paddle = match.paddle,
        notes = match.notes,
        photos = photos.sortedBy { it.sortIndex }.map { it.toDomain() },
    )

internal fun Match.toEntity(): MatchEntity =
    MatchEntity(
        id = id.toString(),
        format = format.name,
        date = date,
        result = result.name,
        startTime = startTime,
        endTime = endTime,
        location = location,
        paddle = paddle,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Match.toMatchPersonRows(): List<MatchPersonEntity> {
    val matchId = id.toString()
    val opponentRows =
        opponents.mapIndexed { index, person ->
            MatchPersonEntity(
                matchId = matchId,
                personId = person.id.toString(),
                role = MatchPersonEntity.ROLE_OPPONENT,
                slot = index,
            )
        }
    val partnerRow =
        partner?.let {
            MatchPersonEntity(
                matchId = matchId,
                personId = it.id.toString(),
                role = MatchPersonEntity.ROLE_PARTNER,
                slot = 0,
            )
        }
    return opponentRows + listOfNotNull(partnerRow)
}

internal fun Match.toGameScoreRows(): List<GameScoreEntity> = games.map { it.toEntity(id.toString()) }

internal fun Match.toPhotoRows(): List<PhotoEntity> = photos.map { it.toEntity(id.toString()) }
