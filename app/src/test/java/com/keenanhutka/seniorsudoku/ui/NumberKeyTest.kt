package com.keenanhutka.seniorsudoku.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberKeyTest {
    @Test
    fun aKeyWorksOnlyOnAMarkedSquareThatIsNotAClue() {
        assertFalse(keysAcceptDigit(hasSelection = false, given = false))
        assertFalse(keysAcceptDigit(hasSelection = true, given = true))
        assertTrue(keysAcceptDigit(hasSelection = true, given = false))
        assertFalse(keysAcceptDigit(hasSelection = true, given = false, notesOn = true, cellEmpty = false))
        assertTrue(keysAcceptDigit(hasSelection = true, given = false, notesOn = true, cellEmpty = true))
    }

    @Test
    fun eraseWorksOnlyWhenTheSquareHasANumberOrANote() {
        assertFalse(canEraseCell(hasSelection = false, given = false, hasDigit = false, hasNotes = false))
        assertFalse(canEraseCell(hasSelection = true, given = true, hasDigit = true, hasNotes = false))
        assertFalse(canEraseCell(hasSelection = true, given = false, hasDigit = false, hasNotes = false))
        assertTrue(canEraseCell(hasSelection = true, given = false, hasDigit = true, hasNotes = false))
        assertTrue(canEraseCell(hasSelection = true, given = false, hasDigit = false, hasNotes = true))
    }

    @Test
    fun theEnteredNumberIsNamed() {
        assertEquals("5", numberKeyLabel(5, entered = false))
        assertEquals("5, entered", numberKeyLabel(5, entered = true))
        assertEquals("5, wrong", numberKeyLabel(5, entered = true, wrong = true))
        assertEquals("5, noted", numberKeyLabel(5, entered = true, noted = true))
        assertEquals("Enter", numberKeyAction(notesOn = false, noted = false))
        assertEquals("Add note", numberKeyAction(notesOn = true, noted = false))
        assertEquals("Remove note", numberKeyAction(notesOn = true, noted = true))
    }

    @Test
    fun aKeyRejectsANumberOutsideOneToNine() {
        assertThrows(IllegalArgumentException::class.java) { numberKeyLabel(0, entered = false) }
    }

    @Test
    fun aShortScreenShrinksTheKeysInsteadOfCoveringTheGrid() {
        assertEquals(56, fittedKeyHeight(preferredPx = 56, spacePx = 800, gapPx = 8))
        assertEquals(40, fittedKeyHeight(preferredPx = 56, spacePx = 184, gapPx = 8))
        assertEquals(1, fittedKeyHeight(preferredPx = 56, spacePx = 10, gapPx = 8))
    }
}
