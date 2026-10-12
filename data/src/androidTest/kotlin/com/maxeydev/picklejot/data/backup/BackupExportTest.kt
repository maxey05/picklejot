@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.backup.ExportOutcome
import com.maxeydev.picklejot.domain.backup.ExportPromptReason
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTime
import kotlin.uuid.ExperimentalUuidApi

private const val LARGE_LIBRARY = 1_000

private const val EXPORT_TIME_BUDGET_MILLIS = 5_000L

@RunWith(AndroidJUnit4::class)
class BackupExportTest {
    private lateinit var harness: BackupHarness

    @Before
    fun setUp() {
        harness = BackupHarness()
    }

    @After
    fun tearDown() {
        harness.close()
    }

    private suspend fun readyExport(): ExportOutcome.Ready {
        val outcome = harness.repository.export()
        assertTrue(outcome.toString(), outcome is ExportOutcome.Ready)
        return outcome as ExportOutcome.Ready
    }

    @Test
    fun `the_export_file_is_named_with_the_date_and_lives_in_the_export_directory`() =
        runBlocking {
            harness.seed(count = 3)

            val ready = readyExport()

            assertTrue(ready.fileName, Regex("picklejot-export-\\d{4}-\\d{2}-\\d{2}\\.json").matches(ready.fileName))
            assertEquals(harness.exportDirectory.canonicalPath, File(ready.filePath).parentFile?.canonicalPath)
            assertTrue(File(ready.filePath).exists())
            assertEquals(3, ready.matchCount)
        }

    @Test
    fun `the_export_declares_its_format_and_schema_version_and_lists_matches_and_people`() =
        runBlocking {
            harness.seedRich()

            val root = Json.parseToJsonElement(File(readyExport().filePath).readText()) as JsonObject

            assertEquals("picklejot-export", root.getValue("format").jsonPrimitive.content)
            assertEquals(ExportCodec.SCHEMA_VERSION, root.getValue("schemaVersion").jsonPrimitive.int)
            assertNotNull(root["exportedAt"])
            assertEquals(2, (root.getValue("matches") as JsonArray).size)
            assertEquals(3, (root.getValue("people") as JsonArray).size)
        }

    @Test
    fun `photos_are_exported_as_records_only_with_no_image_data`() =
        runBlocking {
            harness.seedRich()

            val root = Json.parseToJsonElement(File(readyExport().filePath).readText()) as JsonObject

            val photos =
                (root.getValue("matches") as JsonArray)
                    .flatMap { (it as JsonObject).getValue("photos") as JsonArray }
                    .map { it as JsonObject }
            assertEquals(1, photos.size)
            assertEquals(
                setOf("id", "relativePath", "width", "height", "byteSize", "sortIndex"),
                photos.single().keys,
            )
        }

    @Test
    fun `an_empty_database_still_exports_a_valid_file`() =
        runBlocking {
            val ready = readyExport()

            assertEquals(0, ready.matchCount)
            val root = Json.parseToJsonElement(File(ready.filePath).readText()) as JsonObject
            assertEquals(0, (root.getValue("matches") as JsonArray).size)
            assertTrue(root.getValue("format") is JsonPrimitive)
        }

    @Test
    fun `exporting_again_replaces_the_previous_export_file`() =
        runBlocking {
            harness.seed(count = 2)
            readyExport()
            harness.clock.current = harness.clock.current + 2.days

            readyExport()

            assertEquals(1, harness.exportDirectory.listFiles().orEmpty().size)
        }

    @Test
    fun `a_thousand_match_library_exports_within_the_time_budget_and_re_imports_completely`() {
        val library = BackupHarness(isPro = true)
        try {
            runBlocking {
                library.seed(count = LARGE_LIBRARY)
                var outcome: ExportOutcome? = null

                val elapsed = measureTime { outcome = library.repository.export() }

                val ready = outcome as ExportOutcome.Ready
                assertEquals(LARGE_LIBRARY, ready.matchCount)
                assertTrue("export took $elapsed", elapsed.inWholeMilliseconds < EXPORT_TIME_BUDGET_MILLIS)
                library.database.clearAllTables()
                library.repository.importFrom(ready.filePath)
                assertEquals(LARGE_LIBRARY, library.matchCount())
            }
        } finally {
            library.close()
        }
    }

    @Test
    fun `recording_that_an_export_was_shared_stores_the_time_and_clears_the_pending_prompt`() =
        runBlocking {
            harness.seed(count = 4)
            harness.promptStore.update { it.copy(pending = ExportPromptReason.PERIODIC) }

            harness.repository.recordExportShared()

            val state = harness.promptStore.state.value
            assertEquals(harness.clock.current, state.lastExportAt)
            assertNull(state.pending)
            assertEquals(4, state.anchorCount)
        }

    @Test
    fun `an_export_that_was_never_shared_leaves_the_last_export_empty`() =
        runBlocking {
            harness.seed(count = 2)

            readyExport()

            assertNull(harness.promptStore.state.value.lastExportAt)
        }

    @Test
    fun `evaluating_after_twenty_five_new_matches_raises_a_matches_added_prompt_once`() =
        runBlocking {
            harness.seed(count = 5)
            harness.repository.evaluateExportPrompt()
            harness.seed(count = 25, opponent = "Another Rival", firstDate = "2025-01-01")

            harness.repository.evaluateExportPrompt()
            harness.repository.evaluateExportPrompt()

            val state = harness.promptStore.state.value
            assertEquals(ExportPromptReason.MATCHES_ADDED, state.pending)
            assertEquals(30, state.anchorCount)
        }

    @Test
    fun `dismissing_a_prompt_clears_it_and_the_same_event_does_not_prompt_again`() =
        runBlocking {
            harness.seed(count = 5)
            harness.repository.evaluateExportPrompt()
            harness.seed(count = 25, opponent = "Another Rival", firstDate = "2025-01-01")
            harness.repository.evaluateExportPrompt()

            harness.repository.dismissExportPrompt()
            harness.repository.evaluateExportPrompt()

            assertNull(harness.promptStore.state.value.pending)
        }
}
