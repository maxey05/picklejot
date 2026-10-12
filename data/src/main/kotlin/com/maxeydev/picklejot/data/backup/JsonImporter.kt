@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.backup

import androidx.room.withTransaction
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.match.MatchPersonEntity
import com.maxeydev.picklejot.data.person.PersonEntity
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.domain.backup.ImportCandidate
import com.maxeydev.picklejot.domain.backup.ImportCap
import com.maxeydev.picklejot.domain.backup.ImportOutcome
import com.maxeydev.picklejot.domain.backup.ImportProblem
import com.maxeydev.picklejot.domain.backup.ImportSummary
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private class ImportAborted : RuntimeException()

internal class JsonImporter(
    private val database: PicklejotDatabase,
    private val photoStore: PhotoStore,
    private val entitlements: EntitlementRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val matchDao = database.matchDao()
    private val personDao = database.personDao()

    suspend fun import(text: String): ImportOutcome {
        val parsed =
            when (val decoded = ExportCodec.decode(text)) {
                is DecodeResult.Invalid -> return ImportOutcome.Rejected(decoded.problem)
                is DecodeResult.Valid -> decoded.export
            }
        val entitlement = entitlements.observeEntitlement().first()
        return withContext(ioDispatcher) {
            try {
                ImportOutcome.Imported(database.withTransaction { merge(parsed, entitlement) })
            } catch (aborted: ImportAborted) {
                ImportOutcome.Rejected(ImportProblem.CORRUPT_CONTENT)
            } catch (failure: RuntimeException) {
                if (failure is CancellationException) {
                    throw failure
                }
                ImportOutcome.Rejected(ImportProblem.WRITE_FAILED)
            }
        }
    }

    private suspend fun merge(
        parsed: ParsedExport,
        entitlement: Entitlement,
    ): ImportSummary {
        val knownMatchIds = matchDao.allMatchIds().toSet()
        val incoming = parsed.matches.filter { it.match.id !in knownMatchIds }
        val candidates = incoming.map { ImportCandidate(Uuid.parse(it.match.id), it.match.date, it.match.createdAt) }
        val selection =
            ImportCap.select(
                candidates = candidates,
                existingCount = matchDao.matchCount(),
                entitlement = entitlement,
            )
        val acceptedIds = selection.accepted.map { it.toString() }.toSet()
        val accepted = incoming.filter { it.match.id in acceptedIds }
        val resolvedPeople = resolvePeople(parsed, accepted)
        val knownPhotoIds = matchDao.allPhotoIds().toSet()
        val knownPhotoPaths = matchDao.allPhotoPaths().toSet()
        var photosNotRestored = 0
        accepted.forEach { match ->
            val opponentIds = match.opponentIds.map { resolvedPeople.getValue(it) }
            val partnerId = match.partnerId?.let { resolvedPeople.getValue(it) }
            val assigned = opponentIds + listOfNotNull(partnerId)
            if (assigned.size != assigned.distinct().size) {
                throw ImportAborted()
            }
            val restorable =
                match.photos.filter {
                    it.id !in knownPhotoIds &&
                        it.relativePath !in knownPhotoPaths &&
                        photoStore.resolve(it.relativePath).exists()
                }
            photosNotRestored += match.photos.size - restorable.size
            matchDao.upsertMatch(match.match)
            matchDao.insertPeople(personLinks(match.match.id, opponentIds, partnerId))
            matchDao.insertGames(match.games)
            matchDao.upsertPhotos(restorable)
        }
        return ImportSummary(
            added = accepted.size,
            alreadyPresent = parsed.matches.size - incoming.size,
            heldBack = selection.heldBack,
            photosNotRestored = photosNotRestored,
        )
    }

    private suspend fun resolvePeople(
        parsed: ParsedExport,
        accepted: List<ParsedMatch>,
    ): Map<String, String> {
        val peopleById = parsed.people.associateBy { it.id }
        val needed = accepted.flatMap { it.opponentIds + listOfNotNull(it.partnerId) }.distinct()
        return needed.associateWith { exportedId -> resolvePerson(peopleById.getValue(exportedId)) }
    }

    private suspend fun resolvePerson(person: PersonEntity): String {
        val byId = personDao.findById(person.id)
        if (byId != null) {
            return byId.id
        }
        val byName = personDao.findByNormalizedName(person.normalizedName)
        if (byName != null) {
            return byName.id
        }
        personDao.insertIgnoringDuplicate(person)
        return person.id
    }

    private fun personLinks(
        matchId: String,
        opponentIds: List<String>,
        partnerId: String?,
    ): List<MatchPersonEntity> {
        val opponents =
            opponentIds.mapIndexed { slot, personId ->
                MatchPersonEntity(matchId, personId, MatchPersonEntity.ROLE_OPPONENT, slot)
            }
        val partner = partnerId?.let { MatchPersonEntity(matchId, it, MatchPersonEntity.ROLE_PARTNER, 0) }
        return opponents + listOfNotNull(partner)
    }
}
