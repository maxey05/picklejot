package com.maxeydev.picklejot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxeydev.picklejot.data.db.PICKLEJOT_DB_NAME
import com.maxeydev.picklejot.data.photo.PHOTO_DIRECTORY
import com.maxeydev.picklejot.data.profile.USER_PROFILE_FILE_NAME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRulesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private data class Rule(
        val section: String,
        val kind: String,
        val domain: String,
        val path: String,
    )

    private fun rulesIn(resourceName: String): List<Rule> {
        val id = context.resources.getIdentifier(resourceName, "xml", context.packageName)
        assertTrue("$resourceName is not packaged in the app", id != 0)
        val parser = context.resources.getXml(id)
        try {
            val rules = mutableListOf<Rule>()
            var section = ""
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "cloud-backup", "device-transfer", "full-backup-content" -> section = parser.name
                        "include", "exclude" ->
                            rules +=
                                Rule(
                                    section = section,
                                    kind = parser.name,
                                    domain = parser.getAttributeValue(null, "domain").orEmpty(),
                                    path = parser.getAttributeValue(null, "path").orEmpty(),
                                )
                    }
                }
                event = parser.next()
            }
            return rules
        } finally {
            parser.close()
        }
    }

    private fun List<Rule>.excludes(
        section: String,
        domain: String,
        path: String,
    ): Boolean =
        any {
            it.section == section && it.kind == "exclude" && it.domain == domain && it.path.trimEnd('/') == path
        }

    @Test
    fun both_rules_files_are_packaged_and_declared() {
        assertTrue(rulesIn("data_extraction_rules").isNotEmpty())
        assertTrue(rulesIn("full_backup_content").isNotEmpty())
    }

    @Test
    fun the_entitlement_never_travels_by_backup_or_transfer() {
        val extraction = rulesIn("data_extraction_rules")
        assertTrue(extraction.excludes("cloud-backup", "file", USER_PROFILE_FILE_NAME))
        assertTrue(extraction.excludes("device-transfer", "file", USER_PROFILE_FILE_NAME))
        assertTrue(rulesIn("full_backup_content").excludes("full-backup-content", "file", USER_PROFILE_FILE_NAME))
    }

    @Test
    fun photos_are_excluded_from_cloud_backup_on_api_31_and_above() {
        assertTrue(rulesIn("data_extraction_rules").excludes("cloud-backup", "file", PHOTO_DIRECTORY))
    }

    @Test
    fun photos_are_excluded_from_backup_on_api_30_and_below() {
        assertTrue(rulesIn("full_backup_content").excludes("full-backup-content", "file", PHOTO_DIRECTORY))
    }

    @Test
    fun photos_are_kept_for_device_to_device_transfer() {
        val rules = rulesIn("data_extraction_rules")

        assertFalse(rules.excludes("device-transfer", "file", PHOTO_DIRECTORY))
        assertTrue(rules.any { it.section == "device-transfer" && it.kind == "include" && it.domain == "file" })
    }

    @Test
    fun the_pre_migration_database_copy_lives_in_the_directory_auto_backup_never_includes() {
        val noBackup = context.noBackupFilesDir.canonicalFile

        assertFalse(noBackup.path.startsWith(File(context.dataDir, "databases").canonicalPath))
        assertFalse(noBackup.path.startsWith(context.filesDir.canonicalPath))
        assertEquals(context.dataDir.canonicalFile, noBackup.parentFile)
        assertEquals("no_backup", noBackup.name)
    }

    @Test
    fun the_database_lives_in_the_default_directory_that_auto_backup_includes() {
        val defaultDirectory = File(context.dataDir, "databases").canonicalFile

        assertFalse(PICKLEJOT_DB_NAME.contains('/'))
        assertEquals(defaultDirectory, context.getDatabasePath(PICKLEJOT_DB_NAME).canonicalFile.parentFile)
    }

    @Test
    fun the_photo_store_writes_under_the_folder_the_rules_name() {
        val photos = File(context.filesDir, PHOTO_DIRECTORY)

        assertEquals(context.filesDir.canonicalFile, photos.canonicalFile.parentFile)
    }
}
