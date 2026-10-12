package com.maxeydev.picklejot.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

const val DB_BACKUP_DIRECTORY = "db-backups"

class DatabaseBackup(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun backupIfMigrationPending(targetVersion: Int): File? =
        withContext(ioDispatcher) {
            val databaseFile = context.getDatabasePath(PICKLEJOT_DB_NAME)
            if (!databaseFile.exists()) {
                return@withContext null
            }
            val onDiskVersion = readSchemaVersion(databaseFile)
            if (onDiskVersion >= targetVersion) {
                return@withContext null
            }

            val destination = File(currentVersionDirectory(), "schema-$onDiskVersion-${System.currentTimeMillis()}")
            if (!destination.mkdirs()) {
                throw IllegalStateException("Could not create the pre-migration backup directory: $destination")
            }
            copyDatabaseFiles(databaseFile, destination)
            discardBackupsFromOtherAppVersions()
            destination
        }

    private fun readSchemaVersion(databaseFile: File): Int =
        SQLiteDatabase
            .openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY)
            .use { it.version }

    private fun copyDatabaseFiles(
        databaseFile: File,
        destination: File,
    ) {
        val companions = listOf(databaseFile, File("${databaseFile.path}-wal"), File("${databaseFile.path}-shm"))
        companions.filter { it.exists() }.forEach { source ->
            source.copyTo(File(destination, source.name), overwrite = true)
        }
    }

    private fun backupRoot(): File = File(context.noBackupFilesDir, DB_BACKUP_DIRECTORY)

    private fun currentVersionDirectory(): File = File(backupRoot(), appVersionCode().toString())

    private fun discardBackupsFromOtherAppVersions() {
        val keep = appVersionCode().toString()
        backupRoot().listFiles().orEmpty().filter { it.isDirectory && it.name != keep }.forEach { stale ->
            stale.deleteRecursively()
        }
    }

    private fun appVersionCode(): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }
}
