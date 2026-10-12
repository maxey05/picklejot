@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.person

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface PersonRepository {
    fun observeAll(): Flow<List<Person>>

    fun observeRecentlyUsed(): Flow<List<PersonUsage>>

    suspend fun findById(id: Uuid): Person?

    suspend fun findOrCreatePerson(displayName: String): Person
}
