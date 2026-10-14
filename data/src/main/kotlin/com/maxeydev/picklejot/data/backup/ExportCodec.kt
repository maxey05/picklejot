@file:OptIn(ExperimentalUuidApi::class, ExperimentalSerializationApi::class)

package com.maxeydev.picklejot.data.backup

import com.maxeydev.picklejot.data.match.GameScoreEntity
import com.maxeydev.picklejot.data.match.MatchEntity
import com.maxeydev.picklejot.data.match.MatchPersonEntity
import com.maxeydev.picklejot.data.match.MatchWithRelationsEntity
import com.maxeydev.picklejot.data.person.PersonEntity
import com.maxeydev.picklejot.data.photo.PHOTO_DIRECTORY
import com.maxeydev.picklejot.data.photo.PhotoEntity
import com.maxeydev.picklejot.domain.backup.ImportProblem
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.maxOpponentsFor
import com.maxeydev.picklejot.domain.person.normalizePersonName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.intOrNull
import java.io.BufferedInputStream
import java.io.InputStream
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
private class ExportHeader(
    val format: JsonElement? = null,
    val schemaVersion: JsonElement? = null,
)

internal data class ParsedMatch(
    val match: MatchEntity,
    val opponentIds: List<String>,
    val partnerId: String?,
    val games: List<GameScoreEntity>,
    val photos: List<PhotoEntity>,
)

internal data class ParsedExport(
    val people: List<PersonEntity>,
    val matches: List<ParsedMatch>,
)

internal sealed interface DecodeResult {
    data class Valid(
        val export: ParsedExport,
    ) : DecodeResult

    data class Invalid(
        val problem: ImportProblem,
    ) : DecodeResult
}

internal object ExportCodec {
    const val FORMAT_NAME = "picklejot-export"
    private val ACCEPTED_FORMAT_NAMES = setOf(FORMAT_NAME, "picklelog-export")
    const val SCHEMA_VERSION = 1
    private val UTF8_BOM = intArrayOf(0xEF, 0xBB, 0xBF)

    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    fun encode(file: ExportFile): String = json.encodeToString(ExportFile.serializer(), file)

    fun decode(text: String): DecodeResult = decode { text.byteInputStream() }

    // Streams the file twice (a cheap header pass, then the full decode) so no String or JsonElement
    // tree of the whole file is ever held. IOExceptions from `open` or the stream propagate to the caller.
    fun decode(open: () -> InputStream): DecodeResult {
        val header =
            try {
                open().withoutBom().use { json.decodeFromStream(ExportHeader.serializer(), it) }
            } catch (unreadable: IllegalArgumentException) {
                return unreadableProblem(open)
            }
        val formatName = (header.format as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (formatName !in ACCEPTED_FORMAT_NAMES) {
            return DecodeResult.Invalid(ImportProblem.NOT_A_PICKLEJOT_EXPORT)
        }
        val version =
            (header.schemaVersion as? JsonPrimitive)?.intOrNull
                ?: return DecodeResult.Invalid(ImportProblem.NOT_A_PICKLEJOT_EXPORT)
        if (version > SCHEMA_VERSION) {
            return DecodeResult.Invalid(ImportProblem.NEWER_VERSION)
        }
        if (version < 1) {
            return DecodeResult.Invalid(ImportProblem.NOT_A_PICKLEJOT_EXPORT)
        }
        return try {
            DecodeResult.Valid(validate(open().withoutBom().use { json.decodeFromStream(ExportFile.serializer(), it) }))
        } catch (corrupt: IllegalArgumentException) {
            DecodeResult.Invalid(ImportProblem.CORRUPT_CONTENT)
        }
    }

    // The header pass fails on bad syntax or on a root that is not an object; only a tree parse tells them apart.
    private fun unreadableProblem(open: () -> InputStream): DecodeResult.Invalid =
        try {
            open().withoutBom().use { json.decodeFromStream(JsonElement.serializer(), it) }
            DecodeResult.Invalid(ImportProblem.NOT_A_PICKLEJOT_EXPORT)
        } catch (unreadable: IllegalArgumentException) {
            DecodeResult.Invalid(ImportProblem.NOT_READABLE)
        }

    private fun InputStream.withoutBom(): InputStream {
        val buffered = BufferedInputStream(this)
        buffered.mark(UTF8_BOM.size)
        if (!UTF8_BOM.all { buffered.read() == it }) {
            buffered.reset()
        }
        return buffered
    }

    fun build(
        people: List<PersonEntity>,
        matches: List<MatchWithRelationsEntity>,
        exportedAt: Instant,
    ): ExportFile =
        ExportFile(
            format = FORMAT_NAME,
            schemaVersion = SCHEMA_VERSION,
            exportedAt = exportedAt.toString(),
            people =
                people.map {
                    ExportPerson(
                        id = it.id,
                        displayName = it.displayName,
                        createdAt = it.createdAt.toEpochMilliseconds(),
                    )
                },
            matches = matches.map { it.toExportMatch() },
        )

    private fun MatchWithRelationsEntity.toExportMatch(): ExportMatch =
        ExportMatch(
            id = match.id,
            format = match.format,
            date = match.date.toString(),
            result = match.result,
            startTime = match.startTime?.toString(),
            endTime = match.endTime?.toString(),
            location = match.location,
            paddle = match.paddle,
            notes = match.notes,
            createdAt = match.createdAt.toEpochMilliseconds(),
            updatedAt = match.updatedAt.toEpochMilliseconds(),
            opponentIds =
                people
                    .filter { it.link.role == MatchPersonEntity.ROLE_OPPONENT }
                    .sortedBy { it.link.slot }
                    .map { it.person.id },
            partnerId = people.singleOrNull { it.link.role == MatchPersonEntity.ROLE_PARTNER }?.person?.id,
            games =
                games
                    .sortedBy { it.gameNumber }
                    .map { ExportGame(it.gameNumber, it.myScore, it.opponentScore) },
            photos =
                photos
                    .sortedBy { it.sortIndex }
                    .map { ExportPhoto(it.id, it.relativePath, it.width, it.height, it.byteSize, it.sortIndex) },
        )

    private fun validate(file: ExportFile): ParsedExport {
        val people = file.people.map(::parsePerson)
        require(people.map { it.id }.toSet().size == people.size) { "Duplicate person id." }
        require(people.map { it.normalizedName }.toSet().size == people.size) { "Duplicate person name." }
        val knownPeople = people.map { it.id }.toSet()
        val matches = file.matches.map { parseMatch(it, knownPeople) }
        require(matches.map { it.match.id }.toSet().size == matches.size) { "Duplicate match id." }
        val photos = matches.flatMap { it.photos }
        require(photos.map { it.id }.toSet().size == photos.size) { "Duplicate photo id." }
        require(photos.map { it.relativePath }.toSet().size == photos.size) { "Duplicate photo path." }
        return ParsedExport(people = people, matches = matches)
    }

    private fun parsePerson(person: ExportPerson): PersonEntity {
        val normalized = normalizePersonName(person.displayName)
        require(normalized.isNotEmpty()) { "A person needs a name." }
        return PersonEntity(
            id = canonicalId(person.id),
            displayName = person.displayName.trim(),
            normalizedName = normalized,
            createdAt = Instant.fromEpochMilliseconds(person.createdAt),
        )
    }

    private fun parseMatch(
        exported: ExportMatch,
        knownPeople: Set<String>,
    ): ParsedMatch {
        val matchId = canonicalId(exported.id)
        val format = requireNotNull(MatchFormat.entries.firstOrNull { it.name == exported.format }) { "Bad format." }
        val result = requireNotNull(MatchResult.entries.firstOrNull { it.name == exported.result }) { "Bad result." }
        val opponentIds = exported.opponentIds.map(::canonicalId)
        val partnerId = exported.partnerId?.let(::canonicalId)
        val assigned = opponentIds + listOfNotNull(partnerId)
        require(assigned.all { it in knownPeople }) { "A match refers to a person who is not in the file." }
        require(assigned.size == assigned.distinct().size) { "A person fills two slots on one match." }
        require(opponentIds.size <= maxOpponentsFor(format)) { "Too many opponents for the format." }
        require(format != MatchFormat.SINGLES || partnerId == null) { "A singles match has no partner." }
        require(exported.games.map { it.gameNumber }.toSet().size == exported.games.size) { "Duplicate game number." }
        require(exported.games.all { it.myScore >= 0 && it.opponentScore >= 0 }) { "Negative score." }
        return ParsedMatch(
            match =
                MatchEntity(
                    id = matchId,
                    format = format.name,
                    date = LocalDate.parse(exported.date),
                    result = result.name,
                    startTime = exported.startTime?.let(LocalTime::parse),
                    endTime = exported.endTime?.let(LocalTime::parse),
                    location = exported.location,
                    paddle = exported.paddle,
                    notes = exported.notes,
                    createdAt = Instant.fromEpochMilliseconds(exported.createdAt),
                    updatedAt = Instant.fromEpochMilliseconds(exported.updatedAt),
                ),
            opponentIds = opponentIds,
            partnerId = partnerId,
            games = exported.games.map { GameScoreEntity(matchId, it.gameNumber, it.myScore, it.opponentScore) },
            photos = exported.photos.map { parsePhoto(matchId, it) },
        )
    }

    private fun parsePhoto(
        matchId: String,
        photo: ExportPhoto,
    ): PhotoEntity {
        val path = photo.relativePath
        require(path.startsWith("$PHOTO_DIRECTORY/") && path.count { it == '/' } == 1 && ".." !in path) {
            "A photo path must name a file inside the photos folder."
        }
        require(path.length > PHOTO_DIRECTORY.length + 1 && '\\' !in path) { "A photo path is malformed." }
        require(photo.byteSize >= 0) { "Negative photo size." }
        return PhotoEntity(
            id = canonicalId(photo.id),
            matchId = matchId,
            relativePath = path,
            width = photo.width,
            height = photo.height,
            byteSize = photo.byteSize,
            sortIndex = photo.sortIndex,
        )
    }

    private fun canonicalId(raw: String): String = Uuid.parse(raw).toString()
}
