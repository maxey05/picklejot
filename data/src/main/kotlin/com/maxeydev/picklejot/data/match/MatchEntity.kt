package com.maxeydev.picklejot.data.match

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

@Entity(
    tableName = "match",
    indices = [
        Index(value = ["date"]),
        Index(value = ["result"]),
        Index(value = ["location"]),
    ],
)
data class MatchEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "result")
    val result: String,
    @ColumnInfo(name = "start_time")
    val startTime: LocalTime?,
    @ColumnInfo(name = "end_time")
    val endTime: LocalTime?,
    @ColumnInfo(name = "location")
    val location: String?,
    @ColumnInfo(name = "paddle")
    val paddle: String?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
