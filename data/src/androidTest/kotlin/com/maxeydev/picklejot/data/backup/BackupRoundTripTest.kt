@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.backup.ExportOutcome
import com.maxeydev.picklejot.domain.backup.ImportOutcome
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.ExperimentalUuidApi

@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private lateinit var harness: BackupHarness

    @Before
    fun setUp() {
        harness = BackupHarness()
    }

    @After
    fun tearDown() {
        harness.close()
    }

    private suspend fun exportPath(): String {
        val outcome = harness.repository.export()
        assertTrue(outcome.toString(), outcome is ExportOutcome.Ready)
        return (outcome as ExportOutcome.Ready).filePath
    }

    @Test
    fun `an_export_wiped_and_re_imported_restores_every_match_person_score_and_photo_record`() =
        runBlocking {
            harness.seedRich()
            harness.seed(count = 3, opponent = "Dee Park", firstDate = "2025-02-01")
            val matchesBefore = harness.allMatches()
            val peopleBefore = harness.allPeople()
            val path = exportPath()

            harness.database.clearAllTables()
            assertEquals(0, harness.matchCount())
            assertTrue(harness.allPeople().isEmpty())

            val outcome = harness.repository.importFrom(path)

            assertTrue(outcome.toString(), outcome is ImportOutcome.Imported)
            val summary = (outcome as ImportOutcome.Imported).summary
            assertEquals(5, summary.added)
            assertEquals(0, summary.alreadyPresent)
            assertEquals(0, summary.heldBack)
            assertEquals(0, summary.photosNotRestored)
            assertEquals(matchesBefore, harness.allMatches())
            assertEquals(peopleBefore, harness.allPeople())
        }

    @Test
    fun `the_round_trip_preserves_opponent_order_partner_and_every_game`() =
        runBlocking {
            val ids = harness.seedRich()
            val path = exportPath()
            harness.database.clearAllTables()

            harness.repository.importFrom(path)

            val rich = harness.allMatches().first { it.id == ids.first() }
            assertEquals(listOf("Ana Cruz", "Ben Ortiz"), rich.opponents.map { it.displayName })
            assertEquals("Cy Lim", rich.partner?.displayName)
            assertEquals(3, rich.games.size)
            assertEquals(1, rich.photos.size)
        }

    @Test
    fun `importing_the_same_export_again_adds_nothing_and_reports_every_match_as_already_present`() =
        runBlocking {
            harness.seedRich()
            val path = exportPath()
            val before = harness.allMatches()

            val outcome = harness.repository.importFrom(path)

            val summary = (outcome as ImportOutcome.Imported).summary
            assertEquals(0, summary.added)
            assertEquals(2, summary.alreadyPresent)
            assertEquals(before, harness.allMatches())
        }

    @Test
    fun `importing_twice_after_a_wipe_adds_the_matches_once`() =
        runBlocking {
            harness.seed(count = 4)
            val path = exportPath()
            harness.database.clearAllTables()

            harness.repository.importFrom(path)
            val second = harness.repository.importFrom(path)

            assertEquals(4, harness.matchCount())
            assertEquals(0, (second as ImportOutcome.Imported).summary.added)
            assertEquals(1, harness.allPeople().size)
        }
}
