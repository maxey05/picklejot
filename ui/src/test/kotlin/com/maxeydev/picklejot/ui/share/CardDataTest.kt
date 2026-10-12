@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.share

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.share.CardDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CardDataTest {
    private fun person(name: String): Person = Person(Uuid.random(), name, AppInstant.fromEpochMilliseconds(0))

    private fun match(
        format: MatchFormat = MatchFormat.DOUBLES,
        result: MatchResult = MatchResult.WIN,
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
        games: List<GameScore> = emptyList(),
        location: String? = null,
        startTime: AppTime? = null,
        endTime: AppTime? = null,
    ): Match =
        Match(
            id = Uuid.random(),
            format = format,
            date = AppDate.parse("2026-09-20"),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(0),
            updatedAt = AppInstant.fromEpochMilliseconds(0),
            opponents = opponents,
            partner = partner,
            games = games,
            location = location,
            startTime = startTime,
            endTime = endTime,
        )

    @Test
    fun `a doubles card carries the partner the time the opponents the games and the location`() {
        val data =
            buildCardData(
                match =
                    match(
                        opponents = listOf(person("Ana"), person("Ben")),
                        partner = person("Cy"),
                        games = listOf(GameScore(2, 8, 11), GameScore(1, 11, 9)),
                        location = "  Ayala Triangle ",
                        startTime = AppTime.parse("21:46"),
                        endTime = AppTime.parse("23:02"),
                    ),
                displayName = " Matty ",
                photoDataUri = null,
                labels = FakeCardLabels(),
            )

        assertEquals("Matty", data.displayName)
        assertEquals(CardEntry("Against", listOf("Ana", "Ben")), data.opponents)
        assertEquals(CardEntry("With", listOf("Cy")), data.partner)
        assertEquals(CardEntry("Games", listOf("11–9", "8–11")), data.games)
        assertEquals(CardEntry("Time", listOf("76m")), data.time)
        assertEquals("Ayala Triangle", data.location)
    }

    @Test
    fun `hidden details are left off the card and the rest stay`() {
        val data =
            buildCardData(
                match =
                    match(
                        opponents = listOf(person("Ana")),
                        partner = person("Cy"),
                        games = listOf(GameScore(1, 11, 9)),
                        location = "Ayala Triangle",
                        startTime = AppTime.parse("21:46"),
                        endTime = AppTime.parse("23:02"),
                    ),
                displayName = "Matty",
                photoDataUri = null,
                labels = FakeCardLabels(),
                hidden = setOf(CardDetail.GAME_SCORES, CardDetail.OPPONENTS, CardDetail.PARTNER, CardDetail.LOCATION),
            )

        assertNull(data.games)
        assertNull(data.opponents)
        assertNull(data.partner)
        assertNull(data.location)
        assertEquals(CardEntry("Time", listOf("76m")), data.time)
    }

    @Test
    fun `hiding one detail leaves the others untouched`() {
        val full =
            match(
                opponents = listOf(person("Ana")),
                partner = person("Cy"),
                games = listOf(GameScore(1, 11, 9)),
                location = "Ayala Triangle",
            )

        val data =
            buildCardData(full, "Matty", null, FakeCardLabels(), hidden = setOf(CardDetail.LOCATION))

        assertNull(data.location)
        assertEquals(CardEntry("Against", listOf("Ana")), data.opponents)
        assertEquals(CardEntry("With", listOf("Cy")), data.partner)
        assertEquals(CardEntry("Games", listOf("11–9")), data.games)
    }

    @Test
    fun `the card never says whether the match was won`() {
        val win = buildCardData(match(result = MatchResult.WIN), "Matty", null, FakeCardLabels())
        val loss = buildCardData(match(result = MatchResult.LOSS), "Matty", null, FakeCardLabels())

        assertEquals(win, loss)
    }

    @Test
    fun `the details a match can show follow what it has`() {
        val bare = match(format = MatchFormat.SINGLES)
        val full =
            match(
                opponents = listOf(person("Ana")),
                partner = person("Cy"),
                games = listOf(GameScore(1, 11, 9)),
                location = "Ayala Triangle",
            )
        val blankLocation = match(location = "   ")

        assertEquals(emptySet<CardDetail>(), bare.cardDetails())
        assertEquals(CardDetail.entries.toSet(), full.cardDetails())
        assertFalse(CardDetail.LOCATION in blankLocation.cardDetails())
    }

    @Test
    fun `the wordmark is on the card unless it is switched off`() {
        val shown = buildCardData(match(), "Matty", null, FakeCardLabels())
        val hidden = buildCardData(match(), "Matty", null, FakeCardLabels(), showWordmark = false)

        assertEquals("Picklejot", shown.brand)
        assertEquals("", hidden.brand)
        assertEquals(shown.copy(brand = ""), hidden)
    }

    @Test
    fun `optional fields are absent rather than blank placeholders`() {
        val data =
            buildCardData(
                match = match(format = MatchFormat.SINGLES, result = MatchResult.LOSS, location = "   "),
                displayName = "",
                photoDataUri = null,
                labels = FakeCardLabels(),
            )

        assertEquals("", data.displayName)
        assertNull(data.opponents)
        assertNull(data.partner)
        assertNull(data.games)
        assertNull(data.time)
        assertNull(data.location)
        assertNull(data.photo)
    }

    @Test
    fun `hostile characters in names survive as data and cannot close the script call`() {
        val hostile = "Dave \"The Wall\" O'Neil \\ </script><img src=x onerror=alert(1)>   end"
        val data =
            CardData(
                brand = "Picklejot",
                displayName = hostile,
                meta = "m",
                opponents = CardEntry("Against", listOf("José 🏓 Ñuñez", "李雷", hostile)),
            )

        val call = CardDataSerializer.renderCall(data, "0f1e2d3c-token")
        val encoded = CardDataSerializer.toJson(data)

        assertTrue(call.startsWith("window.renderCard({"))
        assertTrue(call.endsWith("}, \"0f1e2d3c-token\");"))
        assertFalse(encoded.contains("<"))
        assertFalse(encoded.contains(">"))
        assertFalse(encoded.contains(" "))
        assertEquals(data, CardDataSerializer.decode(encoded))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a render token that is not a plain identifier is refused`() {
        CardDataSerializer.renderCall(
            CardData(brand = "b", displayName = "", meta = "m"),
            "x\"); alert(1); (\"",
        )
    }

    @Test
    fun `only bundled asset urls are allowed to load`() {
        assertTrue(isAllowedCardUrl("file:///android_asset/card/index.html"))
        assertTrue(isAllowedCardUrl("file:///android_asset/card/card.css"))
        assertFalse(isAllowedCardUrl("https://example.com/tracker.png"))
        assertFalse(isAllowedCardUrl("http://10.0.2.2/x"))
        assertFalse(isAllowedCardUrl("file:///data/data/com.maxeydev.picklejot/files/photos/a.jpg"))
        assertFalse(isAllowedCardUrl("file:///android_asset/../../data/x"))
        assertFalse(isAllowedCardUrl("content://com.maxeydev.picklejot.fileprovider/cards/a.jpg"))
    }
}
