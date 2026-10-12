@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PhotoOrderTest {
    private fun photo(
        name: String,
        sortIndex: Int,
    ): PhotoRef =
        PhotoRef(
            id = Uuid.random(),
            relativePath = "photos/$name.jpg",
            width = 10,
            height = 10,
            byteSize = 100,
            sortIndex = sortIndex,
        )

    @Test
    fun `moving an item forward and back keeps every other item in order`() {
        val list = listOf("a", "b", "c", "d")

        assertEquals(listOf("b", "c", "a", "d"), list.movedTo(from = 0, to = 2))
        assertEquals(listOf("d", "a", "b", "c"), list.movedTo(from = 3, to = 0))
        assertEquals(list, list.movedTo(from = 1, to = 1))
    }

    @Test
    fun `moving past either end changes nothing`() {
        val list = listOf("a", "b")

        assertEquals(list, list.movedTo(from = 0, to = -1))
        assertEquals(list, list.movedTo(from = 1, to = 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `moving an item that does not exist is a bug`() {
        listOf("a").movedTo(from = 3, to = 0)
    }

    @Test
    fun `the primary photo is the one with the lowest sort index not the first inserted`() {
        val later = photo("later", sortIndex = 1)
        val first = photo("first", sortIndex = 0)

        assertEquals(first, listOf(later, first).primaryPhoto())
        assertEquals(listOf(first, later), listOf(later, first).inDisplayOrder())
        assertNull(emptyList<PhotoRef>().primaryPhoto())
    }

    @Test
    fun `reindexing gives an explicit sequential sort index in display order`() {
        val reordered = listOf(photo("b", 7), photo("a", 3)).reindexed()

        assertEquals(listOf(0, 1), reordered.map { it.sortIndex })
        assertEquals(listOf("photos/b.jpg", "photos/a.jpg"), reordered.map { it.relativePath })
    }

    @Test
    fun `an imported photo becomes a photo ref with the id and position it is given`() {
        val id = Uuid.random()
        val imported = ImportedPhoto(relativePath = "photos/x.jpg", width = 2048, height = 1536, byteSize = 400_000)

        val ref = imported.toPhotoRef(id, sortIndex = 2)

        assertEquals(PhotoRef(id, "photos/x.jpg", 2048, 1536, 400_000, 2), ref)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an imported photo with no size is rejected`() {
        ImportedPhoto(relativePath = "photos/x.jpg", width = 0, height = 10, byteSize = 1)
    }
}
