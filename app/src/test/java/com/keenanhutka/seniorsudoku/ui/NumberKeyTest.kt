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
    }

    @Test
    fun theEnteredNumberIsNamed() {
        assertEquals("5", numberKeyLabel(5, entered = false))
        assertEquals("5, entered", numberKeyLabel(5, entered = true))
        assertEquals("5, wrong", numberKeyLabel(5, entered = true, wrong = true))
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
