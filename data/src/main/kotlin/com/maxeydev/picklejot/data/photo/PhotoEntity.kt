@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.photo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.maxeydev.picklejot.data.match.MatchEntity
import com.maxeydev.picklejot.domain.photo.PhotoRef
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(
    tableName = "photo",
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
data class PhotoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "relative_path")
    val relativePath: String,
    @ColumnInfo(name = "width")
    val width: Int,
    @ColumnInfo(name = "height")
    val height: Int,
    @ColumnInfo(name = "byte_size")
    val byteSize: Long,
    @ColumnInfo(name = "sort_index")
    val sortIndex: Int,
)

internal fun PhotoEntity.toDomain(): PhotoRef =
    PhotoRef(
        id = Uuid.parse(id),
        relativePath = relativePath,
        width = width,
        height = height,
        byteSize = byteSize,
        sortIndex = sortIndex,
    )

internal fun PhotoRef.toEntity(matchId: String): PhotoEntity =
    PhotoEntity(
        id = id.toString(),
        matchId = matchId,
        relativePath = relativePath,
        width = width,
        height = height,
        byteSize = byteSize,
        sortIndex = sortIndex,
    )
