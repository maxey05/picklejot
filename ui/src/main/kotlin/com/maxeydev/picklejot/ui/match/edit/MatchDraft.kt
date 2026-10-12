@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.edit

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.person.normalizePersonName
import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.photo.PhotoRef
import com.maxeydev.picklejot.domain.photo.inDisplayOrder
import com.maxeydev.picklejot.domain.photo.movedTo
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class MatchDraft(
    val matchId: String,
    val isNew: Boolean,
    val format: MatchFormat,
    val date: String,
    val result: MatchResult? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val opponentNames: List<String> = listOf("", ""),
    val opponentIds: List<String?> = listOf(null, null),
    val partnerName: String = "",
    val partnerId: String? = null,
    val games: List<GameDraft> = emptyList(),
    val location: String = "",
    val paddle: String = "",
    val notes: String = "",
    val clearedOnFormatSwitch: Boolean = false,
    val photos: List<PhotoDraft> = emptyList(),
    val removedPhotoIds: List<String> = emptyList(),
) {
    fun withFormat(newFormat: MatchFormat): MatchDraft {
        if (newFormat == format) {
            return this
        }
        return when (newFormat) {
            MatchFormat.SINGLES -> {
                val clearsSomething = nameIn(PersonSlot.OPPONENT_2).isNotBlank() || partnerName.isNotBlank()
                copy(
                    format = MatchFormat.SINGLES,
                    opponentNames = listOf(nameIn(PersonSlot.OPPONENT_1), ""),
                    opponentIds = listOf(idIn(PersonSlot.OPPONENT_1), null),
                    partnerName = "",
                    partnerId = null,
                    clearedOnFormatSwitch = clearsSomething,
                )
            }
            MatchFormat.DOUBLES -> copy(format = MatchFormat.DOUBLES, clearedOnFormatSwitch = false)
        }
    }

    fun withPersonName(
        slot: PersonSlot,
        name: String,
    ): MatchDraft {
        if (name == nameIn(slot)) {
            return this
        }
        return withPerson(slot, name, id = null)
    }

    fun withPersonSelected(
        slot: PersonSlot,
        person: Person,
    ): MatchDraft = withPerson(slot, person.displayName, person.id.toString())

    private fun withPerson(
        slot: PersonSlot,
        name: String,
        id: String?,
    ): MatchDraft =
        when (slot) {
            PersonSlot.OPPONENT_1 ->
                copy(
                    opponentNames = listOf(name, nameIn(PersonSlot.OPPONENT_2)),
                    opponentIds = listOf(id, idIn(PersonSlot.OPPONENT_2)),
                )
            PersonSlot.OPPONENT_2 ->
                copy(
                    opponentNames = listOf(nameIn(PersonSlot.OPPONENT_1), name),
                    opponentIds = listOf(idIn(PersonSlot.OPPONENT_1), id),
                )
            PersonSlot.PARTNER -> copy(partnerName = name, partnerId = id)
        }

    fun withGameAdded(): MatchDraft = copy(games = games + GameDraft())

    fun withGameRemoved(index: Int): MatchDraft = copy(games = games.filterIndexed { position, _ -> position != index })

    fun withGameScores(
        index: Int,
        myScore: String,
        opponentScore: String,
    ): MatchDraft =
        copy(
            games =
                games.mapIndexed { position, game ->
                    if (position == index) {
                        GameDraft.of(myScore, opponentScore)
                    } else {
                        game
                    }
                },
        )

    fun withPhotosAdded(added: List<PhotoDraft>): MatchDraft = copy(photos = photos + added)

    fun withPhotoMoved(
        key: String,
        offset: Int,
    ): MatchDraft {
        val from = photos.indexOfFirst { it.key == key }
        if (from < 0) {
            return this
        }
        return copy(photos = photos.movedTo(from, from + offset))
    }

    fun withPhotoRemoved(key: String): MatchDraft {
        val removed = photos.firstOrNull { it.key == key } ?: return this
        return copy(
            photos = photos.filterNot { it.key == key },
            removedPhotoIds = if (removed.isPersisted) removedPhotoIds + key else removedPhotoIds,
        )
    }

    fun withPhotoImported(
        key: String,
        photo: ImportedPhoto,
    ): MatchDraft = copy(photos = photos.map { if (it.key == key) it.withImported(photo) else it })

    fun readyPhotoRefs(): List<PhotoRef> =
        photos
            .filter { it.isReady }
            .mapIndexed { index, photo -> photo.toPhotoRef(sortIndex = index) }

    fun pendingPhotoKeys(): List<String> = photos.filter { !it.isReady }.map { it.key }

    fun nameIn(slot: PersonSlot): String =
        when (slot) {
            PersonSlot.OPPONENT_1 -> opponentNames.getOrElse(0) { "" }
            PersonSlot.OPPONENT_2 -> opponentNames.getOrElse(1) { "" }
            PersonSlot.PARTNER -> partnerName
        }

    fun idIn(slot: PersonSlot): String? =
        when (slot) {
            PersonSlot.OPPONENT_1 -> opponentIds.getOrNull(0)
            PersonSlot.OPPONENT_2 -> opponentIds.getOrNull(1)
            PersonSlot.PARTNER -> partnerId
        }

    fun boundIdsOutside(slot: PersonSlot): Set<String> =
        visibleSlots()
            .filter { it != slot }
            .mapNotNull { idIn(it) }
            .toSet()

    fun visibleSlots(): List<PersonSlot> =
        when (format) {
            MatchFormat.SINGLES -> listOf(PersonSlot.OPPONENT_1)
            MatchFormat.DOUBLES -> listOf(PersonSlot.OPPONENT_1, PersonSlot.OPPONENT_2, PersonSlot.PARTNER)
        }

    fun duplicateSlots(): Map<PersonSlot, PersonSlot> {
        val firstSlotByName = mutableMapOf<String, PersonSlot>()
        val duplicates = mutableMapOf<PersonSlot, PersonSlot>()
        visibleSlots().forEach { slot ->
            val normalized = normalizePersonName(nameIn(slot))
            if (normalized.isNotEmpty()) {
                val earlier = firstSlotByName[normalized]
                if (earlier == null) {
                    firstSlotByName[normalized] = slot
                } else {
                    duplicates[slot] = earlier
                }
            }
        }
        return duplicates
    }

    fun completeGames(): List<GameScore> =
        games
            .filter { it.isComplete }
            .mapIndexed { index, game ->
                GameScore(
                    gameNumber = index + 1,
                    myScore = game.myScore.toInt(),
                    opponentScore = game.opponentScore.toInt(),
                )
            }

    fun hasIncompleteGame(): Boolean = games.any { it.isIncomplete }

    fun canSaveIgnoringResult(): Boolean = duplicateSlots().isEmpty() && !hasIncompleteGame()

    fun canSave(): Boolean = result != null && canSaveIgnoringResult()

    companion object {
        fun forNewMatch(
            matchId: String,
            format: MatchFormat,
            date: AppDate,
            startTime: AppTime,
        ): MatchDraft =
            MatchDraft(
                matchId = matchId,
                isNew = true,
                format = format,
                date = date.toString(),
                startTime = startTime.toString(),
            )

        fun fromMatch(match: Match): MatchDraft =
            MatchDraft(
                matchId = match.id.toString(),
                isNew = false,
                format = match.format,
                date = match.date.toString(),
                result = match.result,
                startTime = match.startTime?.toString(),
                endTime = match.endTime?.toString(),
                opponentNames =
                    listOf(
                        match.opponents.getOrNull(0)?.displayName.orEmpty(),
                        match.opponents.getOrNull(1)?.displayName.orEmpty(),
                    ),
                opponentIds =
                    listOf(
                        match.opponents.getOrNull(0)?.id?.toString(),
                        match.opponents.getOrNull(1)?.id?.toString(),
                    ),
                partnerName = match.partner?.displayName.orEmpty(),
                partnerId = match.partner?.id?.toString(),
                games =
                    match.games
                        .sortedBy { it.gameNumber }
                        .map {
                            GameDraft(
                                myScore = it.myScore.toString(),
                                opponentScore = it.opponentScore.toString(),
                            )
                        },
                location = match.location.orEmpty(),
                paddle = match.paddle.orEmpty(),
                notes = match.notes.orEmpty(),
                photos = match.photos.inDisplayOrder().map(PhotoDraft::persisted),
            )
    }
}
