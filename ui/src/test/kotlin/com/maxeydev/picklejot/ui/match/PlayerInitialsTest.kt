package com.maxeydev.picklejot.ui.match

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerInitialsTest {
    @Test
    fun `a single name gives one letter`() {
        assertEquals("M", playerInitials("Mitska"))
    }

    @Test
    fun `two names give both initials`() {
        assertEquals("CM", playerInitials("Connor McGreggor"))
    }

    @Test
    fun `more than two names use the first and last`() {
        assertEquals("JT", playerInitials("John Paul Tchaivosky"))
    }

    @Test
    fun `lowercase names are capitalised`() {
        assertEquals("AB", playerInitials("ana bautista"))
    }

    @Test
    fun `extra spaces are ignored`() {
        assertEquals("AB", playerInitials("  Ana    Bautista "))
    }

    @Test
    fun `a blank name gives nothing`() {
        assertEquals("", playerInitials("   "))
    }
}
