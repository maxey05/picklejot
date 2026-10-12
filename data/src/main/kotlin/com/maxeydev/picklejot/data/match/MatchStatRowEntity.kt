package com.maxeydev.picklejot.data.match

import androidx.room.ColumnInfo
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.stats.MatchStatLine
import kotlinx.datetime.LocalDate

data class MatchStatRowEntity(
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "result")
    val result: String,
)

internal fun MatchStatRowEntity.toDomain(): MatchStatLine =
    MatchStatLine(
        date = date,
        format = MatchFormat.valueOf(format),
        result = MatchResult.valueOf(result),
    )
