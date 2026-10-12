@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.person

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.maxeydev.picklejot.domain.person.Person
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(
    tableName = "person",
    indices = [Index(value = ["normalized_name"], unique = true)],
)
data class PersonEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)

internal fun PersonEntity.toDomain(): Person =
    Person(
        id = Uuid.parse(id),
        displayName = displayName,
        createdAt = createdAt,
    )
