package com.keenanhutka.seniorsudoku.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CellLabelTest {
    @Test
    fun aGivenIsNamedByItsRowAndColumn() {
        assertEquals("Row 1, column 1, given 5", cellLabel(0, 5, given = true, wrong = false, notes = emptySet(), selected = false))
    }

    @Test
    fun anEmptySquareSaysEmpty() {
        assertEquals("Row 1, column 2, empty", cellLabel(1, 0, given = false, wrong = false, notes = emptySet(), selected = false))
    }

    @Test
    fun theLastSquareIsRowNineColumnNine() {
        assertEquals("Row 9, column 9, 4", cellLabel(80, 4, given = false, wrong = false, notes = emptySet(), selected = false))
    }

    @Test
    fun aWrongNumberIsCalledWrongAndASelectedSquareSaysSo() {
        assertEquals(
            "Row 2, column 1, 7, wrong, selected",
            cellLabel(9, 7, given = false, wrong = true, notes = emptySet(), selected = true),
        )
    }

    @Test
    fun notesAreReadInOrder() {
        assertEquals(
            "Row 3, column 3, notes 1, 4, 9",
            cellLabel(20, 0, given = false, wrong = false, notes = setOf(9, 1, 4), selected = false),
        )
    }

    @Test
    fun theLineUnderTheTitleNamesTheSelectedSquare() {
        assertEquals("Tap a square.", selectionLine(null, 0, given = false, wrong = false))
        assertEquals("Row 4, column 2.", selectionLine(28, 0, given = false, wrong = false))
        assertEquals("Row 4, column 2. Given 8.", selectionLine(28, 8, given = true, wrong = false))
        assertEquals("Row 4, column 2. 3 is wrong.", selectionLine(28, 3, given = false, wrong = true))
        assertEquals("Row 4, column 2. Notes 1, 4.", selectionLine(28, 0, given = false, wrong = false, notes = setOf(4, 1)))
        assertEquals("Select", selectionAction(false))
        assertEquals("Clear", selectionAction(true))
    }

    @Test
    fun aCellNumberOutsideTheBoardIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            cellLabel(81, 0, given = false, wrong = false, notes = emptySet(), selected = false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            selectionLine(-1, 0, given = false, wrong = false)
        }
    }
}
