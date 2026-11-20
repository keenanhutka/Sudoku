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
    }

    @Test
    fun aKeyRejectsANumberOutsideOneToNine() {
        assertThrows(IllegalArgumentException::class.java) { numberKeyLabel(0, entered = false) }
    }
}
