package com.maxeydev.picklejot.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB_NAME = "picklejot-migration-test.db"

private const val INSERT_PERSON =
    "INSERT INTO person (id, display_name, normalized_name, created_at) " +
        "VALUES ('person-1', 'Dave', 'dave', 1000)"

private const val INSERT_MATCH =
    "INSERT INTO `match` (id, format, `date`, result, start_time, end_time, location, paddle, notes, " +
        "created_at, updated_at) " +
        "VALUES ('match-1', 'SINGLES', '2026-09-23', 'WIN', NULL, NULL, 'Ayala Triangle', NULL, NULL, 1000, 1000)"

private const val INSERT_MATCH_PERSON =
    "INSERT INTO match_person (match_id, person_id, role, slot) VALUES ('match-1', 'person-1', 'OPPONENT', 0)"

private const val INSERT_GAME_SCORE =
    "INSERT INTO game_score (match_id, game_number, my_score, opponent_score) VALUES ('match-1', 1, 11, 9)"

private const val COUNT_MATCH_PERSON = "SELECT COUNT(*) FROM match_person"

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PicklejotDatabase::class.java,
        )

    @Test
    fun `the_exported_schema_opens_and_validates_against_a_populated_database`() {
        helper.createDatabase(TEST_DB_NAME, PICKLEJOT_DB_VERSION).use { database ->
            database.execSQL(INSERT_PERSON)
            database.execSQL(INSERT_MATCH)
            database.execSQL(INSERT_MATCH_PERSON)
            database.execSQL(INSERT_GAME_SCORE)
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, PICKLEJOT_DB_VERSION, true).use { database ->
            database.query(COUNT_MATCH_PERSON).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }
}
