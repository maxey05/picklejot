package com.maxeydev.picklejot.data.match

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.maxeydev.picklejot.domain.match.GameScore

@Entity(
    tableName = "game_score",
    primaryKeys = ["match_id", "game_number"],
    indices = [Index(value = ["match_id"])],
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["match_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GameScoreEntity(
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "game_number")
    val gameNumber: Int,
    @ColumnInfo(name = "my_score")
    val myScore: Int,
    @ColumnInfo(name = "opponent_score")
    val opponentScore: Int,
)

internal fun GameScoreEntity.toDomain(): GameScore =
    GameScore(
        gameNumber = gameNumber,
        myScore = myScore,
        opponentScore = opponentScore,
    )

internal fun GameScore.toEntity(matchId: String): GameScoreEntity =
    GameScoreEntity(
        matchId = matchId,
        gameNumber = gameNumber,
        myScore = myScore,
        opponentScore = opponentScore,
    )
