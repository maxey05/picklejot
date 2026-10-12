package com.maxeydev.picklejot.data.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DirectoryAppCacheTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun write(
        root: File,
        relativePath: String,
        size: Int,
    ) {
        File(root, relativePath).apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(size))
        }
    }

    @Test
    fun `the size counts every file in every folder`() =
        runTest {
            val root = temporaryFolder.newFolder("cache")
            write(root, "cards/a.jpg", 100)
            write(root, "camera/b.jpg", 50)
            write(root, "loose.tmp", 7)

            assertEquals(157L, DirectoryAppCache(root, Dispatchers.Unconfined).sizeBytes())
        }

    @Test
    fun `clearing empties the cache but keeps the folder itself`() =
        runTest {
            val root = temporaryFolder.newFolder("cache")
            write(root, "cards/a.jpg", 100)
            write(root, "exports/picklejot.json", 20)
            val cache = DirectoryAppCache(root, Dispatchers.Unconfined)

            cache.clear()

            assertTrue(root.isDirectory)
            assertTrue(root.listFiles().orEmpty().isEmpty())
            assertEquals(0L, cache.sizeBytes())
        }

    @Test
    fun `a cache folder that does not exist yet is simply empty`() =
        runTest {
            val missing = File(temporaryFolder.root, "never-created")
            val cache = DirectoryAppCache(missing, Dispatchers.Unconfined)

            cache.clear()

            assertEquals(0L, cache.sizeBytes())
        }
}
