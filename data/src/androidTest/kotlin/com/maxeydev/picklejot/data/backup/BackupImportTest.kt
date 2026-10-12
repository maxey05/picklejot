@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.backup.ImportOutcome
import com.maxeydev.picklejot.domain.backup.ImportProblem
import com.maxeydev.picklejot.domain.backup.ImportSummary
import com.maxeydev.picklejot.domain.entitlement.CanAddMatch
import com.maxeydev.picklejot.domain.entitlement.FreeTier
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import kotlin.uuid.ExperimentalUuidApi

@RunWith(AndroidJUnit4::class)
class BackupImportTest {
    private var harness = BackupHarness()

    @After
    fun tearDown() {
        harness.close()
    }

    private fun replaceHarness(next: BackupHarness) {
        harness.close()
        harness = next
    }

    private suspend fun import(file: ExportFile): ImportOutcome =
        harness.repository.importFrom(harness.writeImport("import.json", ExportFixtures.text(file)))

    private suspend fun importText(text: String): ImportOutcome =
        harness.repository.importFrom(harness.writeImport("import.json", text))

    private fun summaryOf(outcome: ImportOutcome): ImportSummary {
        assertTrue(outcome.toString(), outcome is ImportOutcome.Imported)
        return (outcome as ImportOutcome.Imported).summary
    }

    private fun problemOf(outcome: ImportOutcome): ImportProblem {
        assertTrue(outcome.toString(), outcome is ImportOutcome.Rejected)
        return (outcome as ImportOutcome.Rejected).problem
    }

    @Test
    fun `a_person_already_present_by_id_is_reused_not_duplicated`() =
        runBlocking {
            val existing = harness.people.findOrCreatePerson("Sam Rivera")
            val sam = ExportFixtures.person(id = existing.id.toString(), name = "Sam Rivera")
            val match = ExportFixtures.match(opponentIds = listOf(sam.id), games = listOf(ExportFixtures.game()))

            summaryOf(import(ExportFixtures.file(listOf(sam), listOf(match))))

            val people = harness.allPeople()
            assertEquals(1, people.size)
            assertEquals(existing.id, people.single().id)
        }

    @Test
    fun `a_person_with_a_different_id_but_the_same_normalized_name_merges_onto_the_existing_person`() =
        runBlocking {
            val existing = harness.people.findOrCreatePerson("Sam Rivera")
            val sam = ExportFixtures.person(name = "  SAM   rivera ")
            val match = ExportFixtures.match(opponentIds = listOf(sam.id), games = listOf(ExportFixtures.game()))

            summaryOf(import(ExportFixtures.file(listOf(sam), listOf(match))))

            val people = harness.allPeople()
            assertEquals(1, people.size)
            assertEquals(existing.id, people.single().id)
            val imported = harness.allMatches().single()
            assertEquals(existing.id, imported.opponents.single().id)
        }

    @Test
    fun `a_brand_new_person_is_inserted`() =
        runBlocking {
            harness.people.findOrCreatePerson("Existing Player")
            val (newcomer, file) = ExportFixtures.simple()

            summaryOf(import(file))

            assertEquals(2, harness.allPeople().size)
            assertTrue(harness.allPeople().any { it.id.toString() == newcomer.id })
        }

    @Test
    fun `a_free_account_importing_thirty_matches_keeps_all_thirty`() =
        runBlocking {
            val sam = ExportFixtures.person()
            val summary = summaryOf(import(ExportFixtures.file(listOf(sam), ExportFixtures.datedMatches(30, sam))))

            assertEquals(30, summary.added)
            assertEquals(0, summary.heldBack)
            assertEquals(30, harness.matchCount())
        }

    @Test
    fun `a_free_account_importing_exactly_fifty_matches_keeps_all_fifty`() =
        runBlocking {
            val sam = ExportFixtures.person()
            val summary = summaryOf(import(ExportFixtures.file(listOf(sam), ExportFixtures.datedMatches(50, sam))))

            assertEquals(50, summary.added)
            assertEquals(0, summary.heldBack)
            assertEquals(50, harness.matchCount())
        }

    @Test
    fun `a_free_account_importing_sixty_matches_keeps_the_newest_fifty_and_holds_back_ten`() =
        runBlocking {
            val sam = ExportFixtures.person()
            val matches = ExportFixtures.datedMatches(60, sam)
            val summary = summaryOf(import(ExportFixtures.file(listOf(sam), matches)))

            assertEquals(50, summary.added)
            assertEquals(10, summary.heldBack)
            val stored = harness.database.matchDao().allMatchIds().toSet()
            assertEquals(matches.drop(10).map { it.id }.toSet(), stored)
        }

    @Test
    fun `a_pro_account_importing_sixty_matches_keeps_all_sixty`() {
        replaceHarness(BackupHarness(isPro = true))
        runBlocking {
            val sam = ExportFixtures.person()
            val summary = summaryOf(import(ExportFixtures.file(listOf(sam), ExportFixtures.datedMatches(60, sam))))

            assertEquals(60, summary.added)
            assertEquals(0, summary.heldBack)
            assertEquals(60, harness.matchCount())
        }
    }

    @Test
    fun `existing_matches_count_towards_the_cap_so_only_the_free_slots_are_filled_with_the_newest`() =
        runBlocking {
            harness.seed(count = 45, opponent = "Local Rival", firstDate = "2023-01-01")
            val sam = ExportFixtures.person()
            val matches = ExportFixtures.datedMatches(10, sam)

            val summary = summaryOf(import(ExportFixtures.file(listOf(sam), matches)))

            assertEquals(5, summary.added)
            assertEquals(5, summary.heldBack)
            assertEquals(FreeTier.MATCH_LIMIT, harness.matchCount())
            val stored = harness.database.matchDao().allMatchIds().toSet()
            assertTrue(matches.drop(5).all { it.id in stored })
            assertTrue(matches.take(5).none { it.id in stored })
        }

    @Test
    fun `held_back_matches_create_no_people`() =
        runBlocking {
            harness.seed(count = 45, opponent = "Local Rival", firstDate = "2023-01-01")
            val older = ExportFixtures.person(name = "Only In Old Matches")
            val newer = ExportFixtures.person(name = "In New Matches")
            val oldMatches = ExportFixtures.datedMatches(5, older).map { it.copy(date = "2020-01-01") }
            val newMatches = ExportFixtures.datedMatches(5, newer).map { it.copy(date = "2026-06-01") }

            summaryOf(import(ExportFixtures.file(listOf(older, newer), oldMatches + newMatches)))

            val names = harness.allPeople().map { it.displayName }
            assertTrue(names.toString(), "In New Matches" in names)
            assertFalse(names.toString(), "Only In Old Matches" in names)
        }

    @Test
    fun `after_a_capped_import_the_free_cap_is_still_enforced_for_new_matches`() =
        runBlocking {
            val sam = ExportFixtures.person()
            summaryOf(import(ExportFixtures.file(listOf(sam), ExportFixtures.datedMatches(60, sam))))

            val canAdd = CanAddMatch(harness.matches, harness.entitlements)

            assertEquals(FreeTier.MATCH_LIMIT, harness.matchCount())
            assertFalse(canAdd())
        }

    @Test
    fun `a_free_account_below_the_cap_after_import_can_still_add_a_match`() =
        runBlocking {
            val sam = ExportFixtures.person()
            summaryOf(import(ExportFixtures.file(listOf(sam), ExportFixtures.datedMatches(49, sam))))

            assertTrue(CanAddMatch(harness.matches, harness.entitlements)())
        }

    @Test
    fun `matches_whose_photo_files_are_missing_are_imported_without_the_photo_record_and_counted`() =
        runBlocking {
            val present = "photos/present.jpg"
            harness.photoFile(present)
            val sam = ExportFixtures.person()
            val match =
                ExportFixtures.match(
                    opponentIds = listOf(sam.id),
                    games = listOf(ExportFixtures.game()),
                    photos =
                        listOf(
                            ExportFixtures.photo(path = present, sortIndex = 0),
                            ExportFixtures.photo(path = "photos/missing.jpg", sortIndex = 1),
                        ),
                )

            val summary = summaryOf(import(ExportFixtures.file(listOf(sam), listOf(match))))

            assertEquals(1, summary.added)
            assertEquals(1, summary.photosNotRestored)
            assertEquals(listOf(present), harness.allMatches().single().photos.map { it.relativePath })
        }

    @Test
    fun `malformed_or_wrong_schema_files_are_rejected_with_the_right_problem_and_change_nothing`() =
        runBlocking {
            harness.seedRich()
            val matchesBefore = harness.allMatches()
            val peopleBefore = harness.allPeople()
            val valid = ExportFixtures.text(ExportFixtures.simple().second)
            val cases =
                mapOf(
                    "not json at all" to ImportProblem.NOT_READABLE,
                    valid.take(valid.length / 2) to ImportProblem.NOT_READABLE,
                    """{"format":"something-else","schemaVersion":1}""" to ImportProblem.NOT_A_PICKLEJOT_EXPORT,
                    """{"format":"picklejot-export","schemaVersion":99,"matches":"x"}""" to ImportProblem.NEWER_VERSION,
                    valid.replace("\"matches\"", "\"other\"") to ImportProblem.CORRUPT_CONTENT,
                )

            cases.forEach { (text, expected) ->
                assertEquals(text, expected, problemOf(importText(text)))
                assertEquals(matchesBefore, harness.allMatches())
                assertEquals(peopleBefore, harness.allPeople())
            }
        }

    @Test
    fun `a_corrupt_record_deep_in_the_file_rejects_the_whole_file_and_changes_nothing`() =
        runBlocking {
            harness.seed(count = 2)
            val sam = ExportFixtures.person()
            val good = ExportFixtures.datedMatches(5, sam)
            val bad = ExportFixtures.match(id = "not-a-uuid", opponentIds = listOf(sam.id))

            val outcome = import(ExportFixtures.file(listOf(sam), good + bad))

            assertEquals(ImportProblem.CORRUPT_CONTENT, problemOf(outcome))
            assertEquals(2, harness.matchCount())
            assertEquals(1, harness.allPeople().size)
        }

    @Test
    fun `a_write_failure_part_way_through_rolls_back_every_row_including_people`() =
        runBlocking {
            harness.seed(count = 2, opponent = "Local Rival")
            harness.database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_on_poison BEFORE INSERT ON game_score " +
                    "WHEN NEW.my_score = 999 BEGIN SELECT RAISE(ABORT, 'poisoned score'); END",
            )
            val sam = ExportFixtures.person(name = "Rolled Back Rival")
            val good = ExportFixtures.datedMatches(4, sam)
            val poisoned =
                ExportFixtures.match(
                    date = "2030-01-01",
                    opponentIds = listOf(sam.id),
                    games = listOf(ExportFixtures.game(mine = 999)),
                )
            val peopleBefore = harness.allPeople()
            val matchesBefore = harness.allMatches()

            val outcome = import(ExportFixtures.file(listOf(sam), good + poisoned))

            assertEquals(ImportProblem.WRITE_FAILED, problemOf(outcome))
            assertEquals(matchesBefore, harness.allMatches())
            assertEquals(peopleBefore, harness.allPeople())
            assertEquals(2, harness.matchCount())
        }

    @Test
    fun `a_source_that_cannot_be_opened_is_not_readable`() {
        replaceHarness(BackupHarness(openSource = { null }))
        runBlocking {
            assertEquals(ImportProblem.NOT_READABLE, problemOf(harness.repository.importFrom("content://gone")))
        }
    }

    @Test
    fun `a_source_that_throws_while_reading_is_not_readable`() {
        replaceHarness(BackupHarness(openSource = { throw SecurityException("revoked") }))
        runBlocking {
            assertEquals(ImportProblem.NOT_READABLE, problemOf(harness.repository.importFrom("content://revoked")))
        }
    }

    @Test
    fun `a_source_larger_than_the_limit_is_rejected_as_too_large_without_touching_the_database`() {
        replaceHarness(BackupHarness(openSource = { EndlessSpaces(MAX_IMPORT_BYTES + 1L) }))
        runBlocking {
            assertEquals(ImportProblem.TOO_LARGE, problemOf(harness.repository.importFrom("content://huge")))
            assertEquals(0, harness.matchCount())
        }
    }

    @Test
    fun `a_utf8_byte_order_mark_at_the_start_of_the_file_is_tolerated`() =
        runBlocking {
            val (_, file) = ExportFixtures.simple()

            val summary = summaryOf(importText("﻿" + ExportFixtures.text(file)))

            assertEquals(1, summary.added)
        }

    @Test
    fun `a_successful_import_moves_the_prompt_anchor_so_imported_matches_do_not_trigger_a_prompt`() =
        runBlocking {
            val sam = ExportFixtures.person()
            summaryOf(import(ExportFixtures.file(listOf(sam), ExportFixtures.datedMatches(30, sam))))

            assertEquals(30, harness.promptStore.state.value.anchorCount)
        }

    private class EndlessSpaces(
        private var remaining: Long,
    ) : InputStream() {
        override fun read(): Int {
            if (remaining <= 0) {
                return -1
            }
            remaining--
            return ' '.code
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (remaining <= 0) {
                return -1
            }
            val count = minOf(length.toLong(), remaining).toInt()
            buffer.fill(' '.code.toByte(), offset, offset + count)
            remaining -= count
            return count
        }
    }
}
