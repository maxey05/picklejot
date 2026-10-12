package com.maxeydev.picklejot.seed

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.maxeydev.picklejot.PicklejotApplication
import kotlinx.coroutines.runBlocking
import java.io.File

const val SEED_METHOD = "seed"
const val SEEDED_MATCH_COUNT_KEY = "seeded_match_count"
private const val SEED_MARKER_NAME = "benchmark-seed-complete"

class SeedProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(
        method: String,
        arg: String?,
        extras: Bundle?,
    ): Bundle {
        require(method == SEED_METHOD) { "SeedProvider only understands '$SEED_METHOD', not '$method'." }
        val application = requireNotNull(context).applicationContext as PicklejotApplication
        val seededCount =
            runBlocking {
                val container = application.container.await()
                SeedFixture(
                    matchRepository = container.matchRepository,
                    personRepository = container.personRepository,
                    filesDirectory = application.filesDir,
                    markerFile = File(application.noBackupFilesDir, SEED_MARKER_NAME),
                ).seedOnce()
            }
        return Bundle().apply { putInt(SEEDED_MATCH_COUNT_KEY, seededCount) }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0
}
