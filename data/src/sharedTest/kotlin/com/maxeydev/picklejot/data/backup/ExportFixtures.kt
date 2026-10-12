@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.backup

import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal object ExportFixtures {
    fun newId(): String = Uuid.random().toString()

    fun person(
        id: String = newId(),
        name: String = "Sam Rivera",
        createdAt: Long = 1_000L,
    ) = ExportPerson(id = id, displayName = name, createdAt = createdAt)

    fun game(
        number: Int = 1,
        mine: Int = 11,
        theirs: Int = 7,
    ) = ExportGame(gameNumber = number, myScore = mine, opponentScore = theirs)

    fun photo(
        id: String = newId(),
        path: String = "photos/${newId()}.jpg",
        sortIndex: Int = 0,
    ) = ExportPhoto(id = id, relativePath = path, width = 800, height = 600, byteSize = 4_000L, sortIndex = sortIndex)

    fun match(
        id: String = newId(),
        date: String = "2026-09-01",
        format: String = "SINGLES",
        result: String = "WIN",
        opponentIds: List<String> = emptyList(),
        partnerId: String? = null,
        games: List<ExportGame> = emptyList(),
        photos: List<ExportPhoto> = emptyList(),
        createdAt: Long = 1_000L,
    ) = ExportMatch(
        id = id,
        format = format,
        date = date,
        result = result,
        startTime = "18:30",
        endTime = "19:45",
        location = "Riverside Courts",
        paddle = "Selkirk",
        notes = "Good match, with a \"quote\" and a comma, and\na new line.",
        createdAt = createdAt,
        updatedAt = createdAt,
        opponentIds = opponentIds,
        partnerId = partnerId,
        games = games,
        photos = photos,
    )

    fun file(
        people: List<ExportPerson>,
        matches: List<ExportMatch>,
        format: String = ExportCodec.FORMAT_NAME,
        version: Int = ExportCodec.SCHEMA_VERSION,
    ) = ExportFile(
        format = format,
        schemaVersion = version,
        exportedAt = "2026-09-30T02:00:00Z",
        people = people,
        matches = matches,
    )

    fun text(file: ExportFile): String = ExportCodec.encode(file)

    fun datedMatches(
        count: Int,
        opponent: ExportPerson,
    ): List<ExportMatch> =
        (0 until count).map { index ->
            match(
                date = LocalDate.of(2025, 1, 1).plusDays(index.toLong()).toString(),
                opponentIds = listOf(opponent.id),
                games = listOf(game()),
                createdAt = 1_000L + index,
            )
        }

    fun simple(): Pair<ExportPerson, ExportFile> {
        val sam = person()
        return sam to file(listOf(sam), listOf(match(opponentIds = listOf(sam.id), games = listOf(game()))))
    }
}
