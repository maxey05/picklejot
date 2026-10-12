package com.maxeydev.picklejot.data.backup

import kotlinx.serialization.Serializable

@Serializable
internal data class ExportFile(
    val format: String,
    val schemaVersion: Int,
    val exportedAt: String,
    val people: List<ExportPerson>,
    val matches: List<ExportMatch>,
)

@Serializable
internal data class ExportPerson(
    val id: String,
    val displayName: String,
    val createdAt: Long,
)

@Serializable
internal data class ExportMatch(
    val id: String,
    val format: String,
    val date: String,
    val result: String,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val paddle: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val opponentIds: List<String>,
    val partnerId: String?,
    val games: List<ExportGame>,
    val photos: List<ExportPhoto>,
)

@Serializable
internal data class ExportGame(
    val gameNumber: Int,
    val myScore: Int,
    val opponentScore: Int,
)

@Serializable
internal data class ExportPhoto(
    val id: String,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val byteSize: Long,
    val sortIndex: Int,
)
