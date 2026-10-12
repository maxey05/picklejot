package com.maxeydev.picklejot.data.match

import androidx.room.ColumnInfo
import com.maxeydev.picklejot.domain.match.FreeTextUsage
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class FreeTextUsageRowEntity(
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "last_played_on")
    val lastPlayedOn: LocalDate,
    @ColumnInfo(name = "last_logged_at")
    val lastLoggedAt: Instant,
)

internal fun FreeTextUsageRowEntity.toDomain(): FreeTextUsage =
    FreeTextUsage(
        value = value,
        lastPlayedOn = lastPlayedOn,
        lastLoggedAt = lastLoggedAt,
    )
