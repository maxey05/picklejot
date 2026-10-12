package com.maxeydev.picklejot.data.person

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.maxeydev.picklejot.domain.person.PersonUsage
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class PersonUsageRowEntity(
    @Embedded
    val person: PersonEntity,
    @ColumnInfo(name = "last_played_on")
    val lastPlayedOn: LocalDate,
    @ColumnInfo(name = "last_logged_at")
    val lastLoggedAt: Instant,
)

internal fun PersonUsageRowEntity.toDomain(): PersonUsage =
    PersonUsage(
        person = person.toDomain(),
        lastPlayedOn = lastPlayedOn,
        lastLoggedAt = lastLoggedAt,
    )
