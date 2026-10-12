@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.backup

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.plusDays
import com.maxeydev.picklejot.domain.profile.Entitlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImportCapTest {
    private val free = Entitlement(isPro = false)
    private val pro = Entitlement(isPro = true, purchaseToken = "token")
    private val firstDay = AppDate.parse("2026-01-01")

    private fun candidates(count: Int): List<ImportCandidate> =
        (0 until count).map { index ->
            ImportCandidate(
                id = Uuid.random(),
                date = firstDay.plusDays(index),
                createdAt = AppInstant.fromEpochMilliseconds(index.toLong()),
            )
        }

    @Test
    fun `thirty incoming matches onto an empty free account all fit`() {
        val incoming = candidates(30)

        val selection = ImportCap.select(incoming, existingCount = 0, entitlement = free)

        assertEquals(30, selection.accepted.size)
        assertEquals(0, selection.heldBack)
    }

    @Test
    fun `fifty incoming matches onto an empty free account fit exactly`() {
        val selection = ImportCap.select(candidates(50), existingCount = 0, entitlement = free)

        assertEquals(50, selection.accepted.size)
        assertEquals(0, selection.heldBack)
    }

    @Test
    fun `sixty incoming matches on free import the newest fifty and hold back ten`() {
        val incoming = candidates(60)

        val selection = ImportCap.select(incoming, existingCount = 0, entitlement = free)

        val expected = incoming.sortedByDescending { it.date }.take(50).map { it.id }.toSet()
        assertEquals(expected, selection.accepted)
        assertEquals(10, selection.heldBack)
    }

    @Test
    fun `the oldest matches are the ones held back`() {
        val incoming = candidates(60)

        val selection = ImportCap.select(incoming, existingCount = 0, entitlement = free)

        val oldestTen = incoming.sortedBy { it.date }.take(10).map { it.id }
        assertTrue(oldestTen.none { it in selection.accepted })
    }

    @Test
    fun `existing matches use up free slots before the import does`() {
        val incoming = candidates(30)

        val selection = ImportCap.select(incoming, existingCount = 30, entitlement = free)

        assertEquals(20, selection.accepted.size)
        assertEquals(10, selection.heldBack)
    }

    @Test
    fun `a full free account accepts nothing and holds everything back`() {
        val selection = ImportCap.select(candidates(5), existingCount = 50, entitlement = free)

        assertTrue(selection.accepted.isEmpty())
        assertEquals(5, selection.heldBack)
    }

    @Test
    fun `an account already over the cap is treated as having no free slots`() {
        val selection = ImportCap.select(candidates(5), existingCount = 60, entitlement = free)

        assertTrue(selection.accepted.isEmpty())
        assertEquals(5, selection.heldBack)
    }

    @Test
    fun `pro imports everything at any size`() {
        val selection = ImportCap.select(candidates(600), existingCount = 400, entitlement = pro)

        assertEquals(600, selection.accepted.size)
        assertEquals(0, selection.heldBack)
    }

    @Test
    fun `matches on the same date are ordered by when they were logged`() {
        val sameDay = AppDate.parse("2026-05-05")
        val early = ImportCandidate(Uuid.random(), sameDay, AppInstant.fromEpochMilliseconds(1))
        val late = ImportCandidate(Uuid.random(), sameDay, AppInstant.fromEpochMilliseconds(2))

        val selection = ImportCap.select(listOf(early, late), existingCount = 49, entitlement = free)

        assertTrue(late.id in selection.accepted)
        assertFalse(early.id in selection.accepted)
        assertEquals(1, selection.heldBack)
    }

    @Test
    fun `the same input always gives the same selection`() {
        val incoming = candidates(60)

        val first = ImportCap.select(incoming, existingCount = 0, entitlement = free)
        val second = ImportCap.select(incoming.reversed(), existingCount = 0, entitlement = free)

        assertEquals(first, second)
    }
}
