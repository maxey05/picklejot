package com.maxeydev.picklejot.data.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PhotoStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun photoRoot(): File = temporaryFolder.newFolder("files")

    private fun writeFile(
        root: File,
        relativePath: String,
    ): File =
        File(root, relativePath).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }

    @Test
    fun `every listed photo file is deleted`() {
        val root = photoRoot()
        val first = writeFile(root, "photos/a.jpg")
        val second = writeFile(root, "photos/b.jpg")

        PhotoStore(root).deletePhotoFiles(listOf("photos/a.jpg", "photos/b.jpg"))

        assertFalse(first.exists())
        assertFalse(second.exists())
    }

    @Test
    fun `files that are not listed are left alone`() {
        val root = photoRoot()
        writeFile(root, "photos/a.jpg")
        val kept = writeFile(root, "photos/keep.jpg")

        PhotoStore(root).deletePhotoFiles(listOf("photos/a.jpg"))

        assertTrue(kept.exists())
    }

    @Test
    fun `a photo file that is already gone is not an error`() {
        PhotoStore(photoRoot()).deletePhotoFiles(listOf("photos/missing.jpg"))
    }

    @Test
    fun `an empty list deletes nothing`() {
        val root = photoRoot()
        val kept = writeFile(root, "photos/a.jpg")

        PhotoStore(root).deletePhotoFiles(emptyList())

        assertTrue(kept.exists())
    }

    @Test
    fun `a path that climbs out of storage is refused and the outside file survives`() {
        val root = photoRoot()
        val outside = temporaryFolder.newFile("outside.jpg")

        assertThrows(IllegalArgumentException::class.java) {
            PhotoStore(root).deletePhotoFiles(listOf("../outside.jpg"))
        }
        assertTrue(outside.exists())
    }

    @Test
    fun `a path that resolves to the storage root itself is refused`() {
        val root = photoRoot()

        assertThrows(IllegalArgumentException::class.java) {
            PhotoStore(root).deletePhotoFiles(listOf("photos/.."))
        }
        assertTrue(root.exists())
    }

    @Test
    fun `a stored relative path resolves to the file inside app storage`() {
        val root = photoRoot()
        val written = writeFile(root, "photos/a.jpg")

        assertEquals(written.canonicalFile, PhotoStore(root).resolve("photos/a.jpg"))
    }

    @Test
    fun `resolving a path that climbs out of storage is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoStore(photoRoot()).resolve("../outside.jpg")
        }
    }

    @Test
    fun `new photo paths are unique and live inside the photo directory`() {
        val store = PhotoStore(photoRoot())

        val first = store.newPhotoPath()
        val second = store.newPhotoPath()

        assertTrue(first.startsWith("photos/"))
        assertTrue(first.endsWith(".jpg"))
        assertFalse(first == second)
    }

    @Test
    fun `orphan sweep removes old unreferenced files and keeps referenced or recent ones`() {
        val root = photoRoot()
        val referenced = writeFile(root, "photos/kept.jpg").apply { setLastModified(1_000) }
        val oldOrphan = writeFile(root, "photos/old.jpg").apply { setLastModified(1_000) }
        val recentOrphan = writeFile(root, "photos/draft.jpg").apply { setLastModified(9_000) }

        val removed = PhotoStore(root).deleteOrphans(setOf("photos/kept.jpg"), olderThanMillis = 5_000)

        assertEquals(listOf("photos/old.jpg"), removed)
        assertTrue(referenced.exists())
        assertFalse(oldOrphan.exists())
        assertTrue(recentOrphan.exists())
    }

    @Test
    fun `orphan sweep with no photo directory yet does nothing`() {
        assertEquals(emptyList<String>(), PhotoStore(photoRoot()).deleteOrphans(emptySet(), olderThanMillis = 5_000))
    }
}
