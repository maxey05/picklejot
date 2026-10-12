@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.edit

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.photo.PhotoRef
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MatchDraftTest {
    private fun newDraft(format: MatchFormat = MatchFormat.DOUBLES): MatchDraft =
        MatchDraft.forNewMatch(
            matchId = "00000000-0000-0000-0000-000000000001",
            format = format,
            date = AppDate.parse("2026-09-24"),
            startTime = AppTime.parse("20:30"),
        )

    @Test
    fun `a new draft needs only a result to be saveable`() {
        val draft = newDraft()
        assertFalse(draft.canSave())
        assertTrue(draft.copy(result = MatchResult.WIN).canSave())
    }

    @Test
    fun `switching doubles to singles clears the partner and second opponent and says so`() {
        val doubles =
            newDraft()
                .withPersonName(PersonSlot.OPPONENT_1, "Ana")
                .withPersonName(PersonSlot.OPPONENT_2, "Ben")
                .withPersonName(PersonSlot.PARTNER, "Cy")

        val singles = doubles.withFormat(MatchFormat.SINGLES)

        assertEquals(listOf("Ana", ""), singles.opponentNames)
        assertEquals("", singles.partnerName)
        assertTrue(singles.clearedOnFormatSwitch)
        assertEquals(listOf(PersonSlot.OPPONENT_1), singles.visibleSlots())
    }

    @Test
    fun `switching back to doubles does not resurrect cleared names`() {
        val roundTrip =
            newDraft()
                .withPersonName(PersonSlot.OPPONENT_2, "Ben")
                .withPersonName(PersonSlot.PARTNER, "Cy")
                .withFormat(MatchFormat.SINGLES)
                .withFormat(MatchFormat.DOUBLES)

        assertEquals("", roundTrip.nameIn(PersonSlot.OPPONENT_2))
        assertEquals("", roundTrip.nameIn(PersonSlot.PARTNER))
        assertFalse(roundTrip.clearedOnFormatSwitch)
    }

    @Test
    fun `switching to singles with nothing to clear does not claim it cleared anything`() {
        val singles = newDraft().withPersonName(PersonSlot.OPPONENT_1, "Ana").withFormat(MatchFormat.SINGLES)
        assertFalse(singles.clearedOnFormatSwitch)
        assertEquals("Ana", singles.nameIn(PersonSlot.OPPONENT_1))
    }

    @Test
    fun `a name repeated in another slot is flagged against the first slot and blocks save`() {
        val draft =
            newDraft()
                .copy(result = MatchResult.WIN)
                .withPersonName(PersonSlot.OPPONENT_1, "Dave")
                .withPersonName(PersonSlot.PARTNER, "  DAVE ")

        assertEquals(mapOf(PersonSlot.PARTNER to PersonSlot.OPPONENT_1), draft.duplicateSlots())
        assertFalse(draft.canSave())
    }

    @Test
    fun `the same name in both opponent slots is flagged`() {
        val draft =
            newDraft()
                .withPersonName(PersonSlot.OPPONENT_1, "Ana")
                .withPersonName(PersonSlot.OPPONENT_2, "ana")

        assertEquals(mapOf(PersonSlot.OPPONENT_2 to PersonSlot.OPPONENT_1), draft.duplicateSlots())
    }

    @Test
    fun `blank slots are never duplicates of each other`() {
        assertTrue(newDraft().duplicateSlots().isEmpty())
    }

    @Test
    fun `negative and non digit input is stripped from scores`() {
        val draft = newDraft().withGameAdded().withGameScores(0, "-11", "9a")
        assertEquals(GameDraft("11", "9"), draft.games.single())
    }

    @Test
    fun `game numbers stay contiguous after a middle row is removed`() {
        val draft =
            newDraft()
                .withGameAdded()
                .withGameAdded()
                .withGameAdded()
                .withGameScores(0, "11", "5")
                .withGameScores(1, "3", "11")
                .withGameScores(2, "11", "9")
                .withGameRemoved(1)

        assertEquals(listOf(1, 2), draft.completeGames().map { it.gameNumber })
        assertEquals(listOf(11, 11), draft.completeGames().map { it.myScore })
        assertEquals(
            listOf(1, 2),
            draft.copy(result = MatchResult.WIN).toUiState(isSaving = false, isFinished = false).games.map {
                it.gameNumber
            },
        )
    }

    @Test
    fun `any non negative scoring format is accepted`() {
        val draft =
            newDraft()
                .copy(result = MatchResult.WIN)
                .withGameAdded()
                .withGameAdded()
                .withGameScores(0, "21", "19")
                .withGameScores(1, "15", "14")

        assertTrue(draft.canSave())
        assertEquals(listOf(21, 15), draft.completeGames().map { it.myScore })
    }

    @Test
    fun `a half filled game row blocks save until completed or removed`() {
        val halfFilled =
            newDraft()
                .copy(result = MatchResult.WIN)
                .withGameAdded()
                .withGameScores(0, "11", "")

        assertFalse(halfFilled.canSave())
        assertTrue(halfFilled.withGameScores(0, "11", "7").canSave())
        assertTrue(halfFilled.withGameRemoved(0).canSave())
    }

    @Test
    fun `an untouched blank game row is ignored rather than blocking save`() {
        val draft = newDraft().copy(result = MatchResult.LOSS).withGameAdded()
        assertTrue(draft.canSave())
        assertTrue(draft.completeGames().isEmpty())
    }

    @Test
    fun `the advisory appears only when complete scores disagree with the tapped result`() {
        val lostOnScores =
            newDraft()
                .withGameAdded()
                .withGameAdded()
                .withGameScores(0, "5", "11")
                .withGameScores(1, "7", "11")

        assertNull(lostOnScores.toUiState(isSaving = false, isFinished = false).advisory)
        assertEquals(
            MatchResult.LOSS,
            lostOnScores.copy(result = MatchResult.WIN).toUiState(isSaving = false, isFinished = false).advisory,
        )
        assertNull(
            lostOnScores.copy(result = MatchResult.LOSS).toUiState(isSaving = false, isFinished = false).advisory,
        )
    }

    @Test
    fun `the advisory never blocks save`() {
        val draft =
            newDraft()
                .copy(result = MatchResult.WIN)
                .withGameAdded()
                .withGameScores(0, "0", "11")

        val state = draft.toUiState(isSaving = false, isFinished = false)
        assertEquals(MatchResult.LOSS, state.advisory)
        assertTrue(state.canSave)
        assertEquals(MatchResult.WIN, state.result)
    }

    @Test
    fun `a tied or empty score list gives no advisory`() {
        val tied =
            newDraft()
                .copy(result = MatchResult.WIN)
                .withGameAdded()
                .withGameAdded()
                .withGameScores(0, "11", "5")
                .withGameScores(1, "5", "11")

        assertNull(tied.toUiState(isSaving = false, isFinished = false).advisory)
        assertNull(newDraft().copy(result = MatchResult.WIN).toUiState(isSaving = false, isFinished = false).advisory)
    }

    @Test
    fun `duration shows when both ends exist and is positive across midnight`() {
        val lateSession = newDraft().copy(startTime = "23:15", endTime = "00:45")
        val state = lateSession.toUiState(isSaving = false, isFinished = false)
        assertEquals(1.hours + 30.minutes, state.duration)
        assertTrue(state.endsNextDay)
    }

    @Test
    fun `either time endpoint can be cleared and the duration disappears`() {
        val both = newDraft().copy(startTime = "18:30", endTime = "19:45")
        assertEquals(1.hours + 15.minutes, both.toUiState(isSaving = false, isFinished = false).duration)
        assertNull(both.copy(startTime = null).toUiState(isSaving = false, isFinished = false).duration)
        assertNull(both.copy(endTime = null).toUiState(isSaving = false, isFinished = false).duration)
    }

    @Test
    fun `a saving or finished form cannot be saved again`() {
        val ready = newDraft().copy(result = MatchResult.WIN)
        assertFalse(ready.toUiState(isSaving = true, isFinished = false).canSave)
        assertFalse(ready.toUiState(isSaving = false, isFinished = true).canSave)
    }

    @Test
    fun `a draft survives a round trip through its saved form`() {
        val draft =
            newDraft()
                .copy(result = MatchResult.LOSS, endTime = "22:10", location = "Ayala Triangle", notes = "Windy")
                .withPersonName(PersonSlot.OPPONENT_1, "Ana")
                .withPersonName(PersonSlot.PARTNER, "Cy")
                .withGameAdded()
                .withGameScores(0, "9", "11")

        val encoded = Json.encodeToString(MatchDraft.serializer(), draft)
        assertEquals(draft, Json.decodeFromString(MatchDraft.serializer(), encoded))
    }

    private fun person(name: String): Person = Person(Uuid.random(), name, AppInstant.fromEpochMilliseconds(0))

    @Test
    fun `selecting a person binds its id and typing in that slot unbinds it`() {
        val dave = person("Dave R.")

        val bound = newDraft().withPersonSelected(PersonSlot.PARTNER, dave)
        assertEquals("Dave R.", bound.nameIn(PersonSlot.PARTNER))
        assertEquals(dave.id.toString(), bound.idIn(PersonSlot.PARTNER))

        val retyped = bound.withPersonName(PersonSlot.PARTNER, "Dave R")
        assertNull(retyped.idIn(PersonSlot.PARTNER))
    }

    @Test
    fun `an unchanged name keeps its binding`() {
        val dave = person("Dave R.")
        val bound = newDraft().withPersonSelected(PersonSlot.OPPONENT_2, dave)

        val retyped = bound.withPersonName(PersonSlot.OPPONENT_2, "Dave R.")

        assertEquals(dave.id.toString(), retyped.idIn(PersonSlot.OPPONENT_2))
    }

    @Test
    fun `switching to singles drops the bindings of the cleared slots`() {
        val ana = person("Ana")
        val ben = person("Ben")
        val cy = person("Cy")
        val doubles =
            newDraft()
                .withPersonSelected(PersonSlot.OPPONENT_1, ana)
                .withPersonSelected(PersonSlot.OPPONENT_2, ben)
                .withPersonSelected(PersonSlot.PARTNER, cy)

        val singles = doubles.withFormat(MatchFormat.SINGLES)

        assertEquals(ana.id.toString(), singles.idIn(PersonSlot.OPPONENT_1))
        assertNull(singles.idIn(PersonSlot.OPPONENT_2))
        assertNull(singles.idIn(PersonSlot.PARTNER))
        assertEquals(setOf(ana.id.toString()), singles.boundIdsOutside(PersonSlot.OPPONENT_2))
    }

    @Test
    fun `ids bound in other visible slots are reported for exclusion`() {
        val ana = person("Ana")
        val cy = person("Cy")
        val draft = newDraft().withPersonSelected(PersonSlot.OPPONENT_1, ana).withPersonSelected(PersonSlot.PARTNER, cy)

        assertEquals(setOf(cy.id.toString()), draft.boundIdsOutside(PersonSlot.OPPONENT_1))
        assertEquals(setOf(ana.id.toString(), cy.id.toString()), draft.boundIdsOutside(PersonSlot.OPPONENT_2))
    }

    @Test
    fun `log another copies the session fields and leaves the per-game fields blank`() {
        val source =
            Match(
                id = Uuid.random(),
                format = MatchFormat.DOUBLES,
                date = AppDate.parse("2026-09-20"),
                result = MatchResult.LOSS,
                createdAt = AppInstant.fromEpochMilliseconds(1_000),
                updatedAt = AppInstant.fromEpochMilliseconds(1_000),
                startTime = AppTime.parse("18:00"),
                endTime = AppTime.parse("19:30"),
                location = "Ayala Triangle",
                opponents = listOf(person("Ana"), person("Ben")),
                partner = person("Cy"),
                games = listOf(GameScore(1, 9, 11)),
                paddle = "Selkirk",
                notes = "Windy",
                photos = listOf(PhotoRef(Uuid.random(), "photos/a.jpg", 10, 10, 100, 0)),
            )

        val draft =
            logAnotherDraft(
                source = source,
                matchId = "00000000-0000-0000-0000-000000000009",
                startTime = AppTime.parse("21:05"),
            )

        assertTrue(draft.isNew)
        assertEquals("00000000-0000-0000-0000-000000000009", draft.matchId)
        assertEquals(MatchFormat.DOUBLES, draft.format)
        assertEquals("2026-09-20", draft.date)
        assertEquals("Ayala Triangle", draft.location)
        assertEquals(listOf("Ana", "Ben"), draft.opponentNames)
        assertEquals(source.opponents.map { it.id.toString() }, draft.opponentIds)
        assertEquals("Cy", draft.partnerName)
        assertEquals(source.partner?.id?.toString(), draft.partnerId)
        assertNull(draft.result)
        assertTrue(draft.games.isEmpty())
        assertEquals("21:05", draft.startTime)
        assertNull(draft.endTime)
        assertEquals("", draft.notes)
        assertEquals("", draft.paddle)
    }

    @Test
    fun `a bound draft survives a round trip through its saved form`() {
        val dave = person("Dave R.")
        val draft = newDraft().withPersonSelected(PersonSlot.OPPONENT_1, dave)

        val encoded = Json.encodeToString(MatchDraft.serializer(), draft)
        val restored = Json.decodeFromString(MatchDraft.serializer(), encoded)

        assertEquals(draft, restored)
    }
}
