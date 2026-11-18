package com.keenanhutka.seniorsudoku.game

import com.keenanhutka.seniorsudoku.engine.Board
import com.keenanhutka.seniorsudoku.engine.Level
import com.keenanhutka.seniorsudoku.engine.newPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private val puzzle = newPuzzle(Level.Easy, Random(1))

    @Test
    fun aNewGameShowsOnlyTheClues() {
        val game = Game.start(puzzle)
        assertEquals(Level.Easy, game.level)
        assertEquals(puzzle.board.toText(), game.board.toText())
        assertFalse(game.canUndo())
        assertFalse(game.isComplete())
        assertTrue(game.wrongCells().isEmpty())
        for (index in 0 until Board.CELL_COUNT) {
            assertTrue(game.notesAt(index).isEmpty())
            assertEquals(puzzle.board[index] != 0, game.isGiven(index))
        }
    }

    @Test
    fun aClueCannotBeChangedNotedOrCleared() {
        val game = Game.start(puzzle)
        val given = firstGiven()
        val next = game.enterDigit(given, otherDigit(given)).clearCell(given).toggleNote(given, 4)
        assertEquals(game.board.toText(), next.board.toText())
        assertTrue(next.notesAt(given).isEmpty())
        assertFalse(next.canUndo())
    }

    @Test
    fun notesComeBackWhenAPlacedNumberIsUndone() {
        val hole = firstHole()
        val other = secondHole(hole)
        val wrong = otherDigit(hole)
        val game = Game.start(puzzle)
            .toggleNote(hole, 1)
            .toggleNote(hole, 8)
            .toggleNote(other, 5)
        assertEquals(setOf(1, 8), game.notesAt(hole))
        val placed = game.enterDigit(hole, wrong)
        assertEquals(wrong, placed.board[hole])
        assertTrue(placed.notesAt(hole).isEmpty())
        assertEquals(setOf(5), placed.notesAt(other))
        assertTrue(placed.isWrong(hole))
        assertEquals(listOf(hole), placed.wrongCells())
        val undone = placed.undo()
        assertEquals(0, undone.board[hole])
        assertEquals(setOf(1, 8), undone.notesAt(hole))
        assertEquals(setOf(5), undone.notesAt(other))
        assertFalse(undone.isWrong(hole))
    }

    @Test
    fun togglingANoteTwiceRemovesItAndUndoRestoresIt() {
        val hole = firstHole()
        val noted = Game.start(puzzle).toggleNote(hole, 8)
        val cleared = noted.toggleNote(hole, 8)
        assertTrue(cleared.notesAt(hole).isEmpty())
        assertEquals(setOf(8), cleared.undo().notesAt(hole))
    }

    @Test
    fun aNoteCannotBeWrittenOnAFilledCell() {
        val hole = firstHole()
        val filled = Game.start(puzzle).enterDigit(hole, puzzle.solution[hole])
        assertFalse(filled.isWrong(hole))
        assertTrue(filled.toggleNote(hole, 2).notesAt(hole).isEmpty())
    }

    @Test
    fun clearingACellThenUndoingRestoresTheNumber() {
        val hole = firstHole()
        val placed = Game.start(puzzle).enterDigit(hole, puzzle.solution[hole])
        val cleared = placed.clearCell(hole)
        assertEquals(0, cleared.board[hole])
        assertEquals(puzzle.solution[hole], cleared.undo().board[hole])
    }

    @Test
    fun enteringTheSameDigitTwiceDoesNotAddASecondUndo() {
        val hole = firstHole()
        val once = Game.start(puzzle).enterDigit(hole, puzzle.solution[hole])
        val twice = once.enterDigit(hole, puzzle.solution[hole])
        val back = twice.undo()
        assertEquals(0, back.board[hole])
        assertFalse(back.canUndo())
    }

    @Test
    fun undoOnANewGameDoesNothing() {
        val game = Game.start(puzzle)
        assertEquals(game.board.toText(), game.undo().board.toText())
    }

    @Test
    fun theEarlierGameDoesNotChange() {
        val game = Game.start(puzzle)
        val before = game.board.toText()
        game.enterDigit(firstHole(), otherDigit(firstHole()))
        assertEquals(before, game.board.toText())
    }

    @Test
    fun fillingTheSolutionCompletesTheGame() {
        var game = Game.start(puzzle)
        val wrongHole = firstHole()
        for (index in 0 until Board.CELL_COUNT) {
            if (game.isGiven(index) || index == wrongHole) continue
            game = game.enterDigit(index, puzzle.solution[index])
        }
        val wrong = otherDigit(wrongHole)
        val almost = game.enterDigit(wrongHole, wrong)
        assertFalse(almost.isComplete())
        val done = almost.enterDigit(wrongHole, puzzle.solution[wrongHole])
        assertTrue(done.isComplete())
        assertTrue(done.wrongCells().isEmpty())
    }

    @Test
    fun aClueThatDisagreesWithTheSolutionIsRejected() {
        val given = firstGiven()
        val badDigit = otherDigit(given)
        val bad = puzzle.copy(board = puzzle.board.withDigit(given, badDigit))
        assertThrows(IllegalArgumentException::class.java) { Game.start(bad) }
    }

    private fun firstGiven(): Int = (0 until Board.CELL_COUNT).first { puzzle.board[it] != 0 }

    private fun firstHole(): Int = (0 until Board.CELL_COUNT).first { puzzle.board[it] == 0 }

    private fun secondHole(first: Int): Int =
        (0 until Board.CELL_COUNT).first { it != first && puzzle.board[it] == 0 }

    private fun otherDigit(index: Int): Int = if (puzzle.solution[index] == 1) 2 else 1
}
