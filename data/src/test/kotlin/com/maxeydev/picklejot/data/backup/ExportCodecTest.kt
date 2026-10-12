@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.backup

import com.maxeydev.picklejot.domain.backup.ImportProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ExportCodecTest {
    private val sam = ExportFixtures.person(name = "Sam Rivera")
    private val alex = ExportFixtures.person(name = "Alex Kim")

    private fun problemOf(text: String): ImportProblem? = (ExportCodec.decode(text) as? DecodeResult.Invalid)?.problem

    private fun problemOf(file: ExportFile): ImportProblem? = problemOf(ExportFixtures.text(file))

    private fun validOf(file: ExportFile): ParsedExport {
        val decoded = ExportCodec.decode(ExportFixtures.text(file))
        assertTrue("expected a valid export but got $decoded", decoded is DecodeResult.Valid)
        return (decoded as DecodeResult.Valid).export
    }

    @Test
    fun `an export carries its format name and a schema version from the first release`() {
        val (_, file) = ExportFixtures.simple()

        val text = ExportFixtures.text(file)

        assertTrue(text.contains("\"format\": \"picklejot-export\""))
        assertTrue(text.contains("\"schemaVersion\": 1"))
    }

    @Test
    fun `a valid export decodes to the same matches, people, scores and photo metadata`() {
        val photo = ExportFixtures.photo()
        val match =
            ExportFixtures.match(
                format = "DOUBLES",
                result = "LOSS",
                opponentIds = listOf(sam.id, alex.id),
                partnerId = ExportFixtures.person(name = "Pat Lee").id,
                games = listOf(ExportFixtures.game(1, 9, 11), ExportFixtures.game(2, 11, 8)),
                photos = listOf(photo),
            )
        val partner = ExportFixtures.person(id = match.partnerId!!, name = "Pat Lee")

        val parsed = validOf(ExportFixtures.file(listOf(sam, alex, partner), listOf(match)))

        val decoded = parsed.matches.single()
        assertEquals(3, parsed.people.size)
        assertEquals(match.id, decoded.match.id)
        assertEquals("DOUBLES", decoded.match.format)
        assertEquals("LOSS", decoded.match.result)
        assertEquals(listOf(sam.id, alex.id), decoded.opponentIds)
        assertEquals(partner.id, decoded.partnerId)
        assertEquals(listOf(9 to 11, 11 to 8), decoded.games.map { it.myScore to it.opponentScore })
        assertEquals(photo.relativePath, decoded.photos.single().relativePath)
        assertEquals(match.notes, decoded.match.notes)
        assertEquals("18:30", decoded.match.startTime.toString())
    }

    @Test
    fun `notes with quotes, commas and line breaks survive the round trip`() {
        val (_, file) = ExportFixtures.simple()

        val parsed = validOf(file)

        assertEquals(file.matches.single().notes, parsed.matches.single().match.notes)
    }

    @Test
    fun `ids are canonicalised so an upper case id matches its lower case twin`() {
        val upper = sam.copy(id = sam.id.uppercase())
        val match = ExportFixtures.match(opponentIds = listOf(sam.id))

        val parsed = validOf(ExportFixtures.file(listOf(upper), listOf(match)))

        assertEquals(sam.id, parsed.people.single().id)
        assertEquals(listOf(sam.id), parsed.matches.single().opponentIds)
    }

    @Test
    fun `person names are trimmed and normalised for matching`() {
        val padded = ExportFixtures.person(name = "  Sam   Rivera ")

        val parsed = validOf(ExportFixtures.file(listOf(padded), emptyList()))

        assertEquals("Sam   Rivera", parsed.people.single().displayName)
        assertEquals("sam rivera", parsed.people.single().normalizedName)
    }

    @Test
    fun `a field added by a later app version inside the same schema is ignored`() {
        val (_, file) = ExportFixtures.simple()
        val text = ExportFixtures.text(file).replace("\"exportedAt\"", "\"futureField\": 7,\n  \"exportedAt\"")

        assertTrue(ExportCodec.decode(text) is DecodeResult.Valid)
    }

    @Test
    fun `an empty file is not readable`() {
        assertEquals(ImportProblem.NOT_READABLE, problemOf(""))
    }

    @Test
    fun `text that is not json is not readable`() {
        assertEquals(ImportProblem.NOT_READABLE, problemOf("this is not json at all"))
    }

    @Test
    fun `a truncated export is not readable`() {
        val (_, file) = ExportFixtures.simple()
        val text = ExportFixtures.text(file)

        assertEquals(ImportProblem.NOT_READABLE, problemOf(text.take(text.length / 2)))
        assertEquals(ImportProblem.NOT_READABLE, problemOf(text.dropLast(3)))
    }

    @Test
    fun `json that is not an object is not a picklejot export`() {
        assertEquals(ImportProblem.NOT_A_PICKLEJOT_EXPORT, problemOf("[]"))
        assertEquals(ImportProblem.NOT_A_PICKLEJOT_EXPORT, problemOf("null"))
        assertEquals(ImportProblem.NOT_A_PICKLEJOT_EXPORT, problemOf("{}"))
    }

    @Test
    fun `a file with another format name is not a picklejot export`() {
        val file = ExportFixtures.file(emptyList(), emptyList(), format = "something-else")

        assertEquals(ImportProblem.NOT_A_PICKLEJOT_EXPORT, problemOf(file))
    }

    @Test
    fun `an export made before the rename to picklejot still imports`() {
        val file = ExportFixtures.file(emptyList(), emptyList(), format = "picklelog-export")

        validOf(file)
    }

    @Test
    fun `a file with no schema version is not a picklejot export`() {
        val original = ExportFixtures.text(ExportFixtures.file(emptyList(), emptyList()))
        val text = original.replace("\"schemaVersion\"", "\"other\"")

        assertEquals(ImportProblem.NOT_A_PICKLEJOT_EXPORT, problemOf(text))
    }

    @Test
    fun `a schema version below one is not a picklejot export`() {
        val file = ExportFixtures.file(emptyList(), emptyList(), version = 0)
        assertEquals(ImportProblem.NOT_A_PICKLEJOT_EXPORT, problemOf(file))
    }

    @Test
    fun `an export from a newer schema is refused with its own message`() {
        val newer = ExportFixtures.file(emptyList(), emptyList(), version = ExportCodec.SCHEMA_VERSION + 1)

        assertEquals(ImportProblem.NEWER_VERSION, problemOf(newer))
    }

    @Test
    fun `a newer schema is refused even when its structure no longer matches`() {
        val text = """{"format":"picklejot-export","schemaVersion":99,"matches":"a different shape"}"""

        assertEquals(ImportProblem.NEWER_VERSION, problemOf(text))
    }

    @Test
    fun `a missing required section is corrupt content`() {
        val original = ExportFixtures.text(ExportFixtures.file(emptyList(), emptyList()))
        val text = original.replace("\"matches\"", "\"other\"")

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(text))
    }

    @Test
    fun `a malformed id is corrupt content`() {
        val match = ExportFixtures.match(id = "not-a-uuid")

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(emptyList(), listOf(match))))
    }

    @Test
    fun `an unknown format or result is corrupt content`() {
        val badFormat = ExportFixtures.match(format = "TRIPLES")
        val badResult = ExportFixtures.match(result = "DRAW")

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(emptyList(), listOf(badFormat))))
        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(emptyList(), listOf(badResult))))
    }

    @Test
    fun `an unreadable date or time is corrupt content`() {
        val badDate = ExportFixtures.match(date = "31/12/2026")
        val badTime = ExportFixtures.match().copy(startTime = "quarter past six")

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(emptyList(), listOf(badDate))))
        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(emptyList(), listOf(badTime))))
    }

    @Test
    fun `a match that names a person who is not in the file is corrupt content`() {
        val match = ExportFixtures.match(opponentIds = listOf(Uuid.random().toString()))

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(listOf(sam), listOf(match))))
    }

    @Test
    fun `duplicate ids are corrupt content`() {
        val id = ExportFixtures.newId()
        val twoMatches = listOf(ExportFixtures.match(id = id), ExportFixtures.match(id = id))
        val twoPeople = listOf(sam, sam.copy(displayName = "Someone Else"))

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(emptyList(), twoMatches)))
        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(twoPeople, emptyList())))
    }

    @Test
    fun `two people with the same normalised name are corrupt content`() {
        val twin = ExportFixtures.person(name = "sam   RIVERA")

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(listOf(sam, twin), emptyList())))
    }

    @Test
    fun `a person with a blank name is corrupt content`() {
        val blank = ExportFixtures.person(name = "   ")

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(listOf(blank), emptyList())))
    }

    @Test
    fun `a singles match with a partner is corrupt content`() {
        val match = ExportFixtures.match(format = "SINGLES", opponentIds = listOf(sam.id), partnerId = alex.id)

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(listOf(sam, alex), listOf(match))))
    }

    @Test
    fun `too many opponents for the format is corrupt content`() {
        val singles = ExportFixtures.match(format = "SINGLES", opponentIds = listOf(sam.id, alex.id))

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(listOf(sam, alex), listOf(singles))))
    }

    @Test
    fun `one person in two slots of the same match is corrupt content`() {
        val match = ExportFixtures.match(format = "DOUBLES", opponentIds = listOf(sam.id, sam.id))

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(listOf(sam), listOf(match))))
    }

    @Test
    fun `repeated game numbers or negative scores are corrupt content`() {
        val repeated = ExportFixtures.match(games = listOf(ExportFixtures.game(1), ExportFixtures.game(1)))
        val negative = ExportFixtures.match(games = listOf(ExportFixtures.game(1, mine = -1)))

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(emptyList(), listOf(repeated))))
        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(ExportFixtures.file(emptyList(), listOf(negative))))
    }

    @Test
    fun `a photo path that leaves the photos folder is corrupt content`() {
        val escapes =
            listOf(
                "../databases/picklejot.db",
                "photos/../x.jpg",
                "/etc/passwd",
                "other/x.jpg",
                "photos/a/b.jpg",
                "photos/",
            )
        escapes.forEach { path ->
            val match = ExportFixtures.match(photos = listOf(ExportFixtures.photo(path = path)))

            val problem = problemOf(ExportFixtures.file(emptyList(), listOf(match)))
            assertEquals(path, ImportProblem.CORRUPT_CONTENT, problem)
        }
    }

    @Test
    fun `a photo shared by two matches is corrupt content`() {
        val shared = ExportFixtures.photo(path = "photos/shared.jpg")
        val other = ExportFixtures.photo(path = "photos/shared.jpg")
        val file =
            ExportFixtures.file(
                emptyList(),
                listOf(ExportFixtures.match(photos = listOf(shared)), ExportFixtures.match(photos = listOf(other))),
            )

        assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(file))
    }

    @Test
    fun `a valid photo record is accepted`() {
        val match = ExportFixtures.match(photos = listOf(ExportFixtures.photo(path = "photos/a1.jpg")))

        assertNotNull(validOf(ExportFixtures.file(emptyList(), listOf(match))).matches.single().photos.single())
    }
}
