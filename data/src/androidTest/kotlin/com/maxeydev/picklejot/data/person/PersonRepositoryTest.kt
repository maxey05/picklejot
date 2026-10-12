@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.person

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.ExperimentalUuidApi

@RunWith(AndroidJUnit4::class)
class PersonRepositoryTest {
    private lateinit var database: PicklejotDatabase
    private lateinit var people: RoomPersonRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, PicklejotDatabase::class.java)
                .build()
        people = RoomPersonRepository(database, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `Dave_and_dave_resolve_to_one_person_row`() =
        runBlocking {
            val first = people.findOrCreatePerson("Dave")
            val second = people.findOrCreatePerson("dave")

            assertEquals(first.id, second.id)
            assertEquals(1, people.observeAll().first().size)
        }

    @Test
    fun `the_original_display_name_survives_a_later_differently_cased_entry`() =
        runBlocking {
            people.findOrCreatePerson("Dave")
            val again = people.findOrCreatePerson("DAVE")

            assertEquals("Dave", again.displayName)
            assertEquals(listOf("Dave"), people.observeAll().first().map { it.displayName })
        }

    @Test
    fun `an_accented_name_typed_two_ways_resolves_to_one_person_row`() =
        runBlocking {
            val precomposed = people.findOrCreatePerson("José")
            val decomposed = people.findOrCreatePerson("José")

            assertEquals(precomposed.id, decomposed.id)
            assertEquals(1, people.observeAll().first().size)
        }

    @Test
    fun `two_concurrent_calls_for_the_same_new_name_create_exactly_one_person`() =
        runBlocking {
            val results =
                coroutineScope {
                    listOf(
                        async(Dispatchers.IO) { people.findOrCreatePerson("Priya") },
                        async(Dispatchers.IO) { people.findOrCreatePerson("Priya") },
                    ).awaitAll()
                }

            assertEquals(results[0].id, results[1].id)
            assertEquals(1, people.observeAll().first().size)
        }
}
