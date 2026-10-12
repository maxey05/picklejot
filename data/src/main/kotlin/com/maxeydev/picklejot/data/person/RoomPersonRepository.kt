@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.person

import androidx.room.withTransaction
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.person.PersonRepository
import com.maxeydev.picklejot.domain.person.PersonUsage
import com.maxeydev.picklejot.domain.person.normalizePersonName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RoomPersonRepository(
    private val database: PicklejotDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock = Clock.System,
) : PersonRepository {
    private val personDao = database.personDao()

    override fun observeAll(): Flow<List<Person>> =
        personDao
            .observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeRecentlyUsed(): Flow<List<PersonUsage>> =
        personDao
            .observeRecentlyUsed()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun findById(id: Uuid): Person? =
        withContext(ioDispatcher) {
            personDao.findById(id.toString())?.toDomain()
        }

    override suspend fun findOrCreatePerson(displayName: String): Person {
        val normalizedName = normalizePersonName(displayName)
        require(normalizedName.isNotEmpty()) {
            "A person needs a display name with at least one non-whitespace character."
        }
        return withContext(ioDispatcher) {
            database.withTransaction {
                val stored =
                    personDao.findByNormalizedName(normalizedName)
                        ?: insertThenRead(displayName, normalizedName)
                stored.toDomain()
            }
        }
    }

    private suspend fun insertThenRead(
        displayName: String,
        normalizedName: String,
    ): PersonEntity {
        personDao.insertIgnoringDuplicate(
            PersonEntity(
                id = Uuid.random().toString(),
                displayName = displayName.trim(),
                normalizedName = normalizedName,
                createdAt = clock.now(),
            ),
        )
        return personDao.findByNormalizedName(normalizedName)
            ?: error("person row absent immediately after insert for normalized name '$normalizedName'")
    }
}
