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
    fun theNotesButtonSaysWhetherTheTapTurnsNotesOnOrOff() {
        assertEquals("Turn notes on", notesButtonAction(notesOn = false))
        assertEquals("Turn notes off", notesButtonAction(notesOn = true))
    }

    @Test
    fun theLineUnderTheTitleNamesTheSelectedSquare() {
        assertEquals("Tap a square.", selectionLine(null, 0, given = false, wrong = false))
        assertEquals("Row 4, column 2.", selectionLine(28, 0, given = false, wrong = false))
        assertEquals("Row 4, column 2. Given 8.", selectionLine(28, 8, given = true, wrong = false))
        assertEquals("Row 4, column 2. 3 is wrong.", selectionLine(28, 3, given = false, wrong = true))
        assertEquals("Row 4, column 2. Notes 1, 4.", selectionLine(28, 0, given = false, wrong = false, notes = setOf(4, 1)))
        assertEquals(
            "Row 4, column 2. 5 is not for this square.",
            selectionLine(28, 0, given = false, wrong = false, heldOff = setOf(5)),
        )
        assertEquals(
            "Row 4, column 2. 3 and 5 are not for this square.",
            selectionLine(28, 0, given = false, wrong = false, heldOff = setOf(5, 3)),
        )
        assertEquals(
            "Row 4, column 2. 7 is wrong. 5 is not for this square.",
            selectionLine(28, 7, given = false, wrong = true, heldOff = setOf(5, 7)),
        )
        assertEquals(
            "Row 4, column 2. Notes 1, 5. 5 stays off.",
            selectionLine(28, 0, given = false, wrong = false, notes = setOf(1, 5), heldOff = setOf(5)),
        )
        assertEquals(
            "Row 4, column 2. Notes 1, 3. 5 is not for this square. 3 stays off.",
            selectionLine(28, 0, given = false, wrong = false, notes = setOf(1, 3), heldOff = setOf(3, 5)),
        )
        assertEquals("Select", selectionAction(false))
        assertEquals("Deselect", selectionAction(true))
        assertEquals("Keep the hint", cellTapAction(marked = true, hintShowing = true))
        assertEquals("Keep this square", cellTapAction(marked = true, hintShowing = false, mistakeShowing = true))
        assertEquals("Deselect", cellTapAction(marked = true, hintShowing = false))
        assertEquals("Select", cellTapAction(marked = false, hintShowing = true))
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

    @Test
    fun checkMyNumbersNamesHowManyAreWrong() {
        assertEquals("No mistakes.", mistakeMessage(0))
        assertEquals("No mistakes. The puzzle is not finished.", mistakeMessage(0, unfinished = true))
        assertEquals("1 number looks wrong.", mistakeMessage(1, unfinished = true))
        assertEquals("2 numbers look wrong.", mistakeMessage(2))
        assertThrows(IllegalArgumentException::class.java) { mistakeMessage(-1) }
    }
}
