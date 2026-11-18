package com.keenanhutka.seniorsudoku.game

import com.keenanhutka.seniorsudoku.engine.Board
import com.keenanhutka.seniorsudoku.engine.HintResult
import com.keenanhutka.seniorsudoku.engine.Level
import com.keenanhutka.seniorsudoku.engine.findHint
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

    @Test
    fun aSolutionThatRepeatsANumberIsRejected() {
        val holes = (0 until Board.CELL_COUNT).filter { puzzle.board[it] == 0 }
        val left = holes.first()
        val right = holes.first { it != left && it / Board.SIZE == left / Board.SIZE }
        val bad = puzzle.copy(solution = puzzle.solution.withDigit(right, puzzle.solution[left]))
        assertThrows(IllegalArgumentException::class.java) { Game.start(bad) }
    }

    @Test
    fun followingHintsFinishesAnEasyGameAndUndoRestoresTheNote() {
        val hole = firstHole()
        var game = Game.start(puzzle).toggleNote(hole, 8)
        assertEquals(setOf(8), game.notesAt(hole))
        var holeOpen = true
        var moves = 0
        while (!game.isComplete()) {
            val hint = findHint(game.board, game.solution)
            assertTrue(hint is HintResult.Single)
            val step = (hint as HintResult.Single).step
            val index = (step.row - 1) * Board.SIZE + (step.column - 1)
            assertEquals(0, game.board[index])
            assertEquals(puzzle.solution[index], step.digit)
            if (holeOpen && index != hole) assertEquals(setOf(8), game.notesAt(hole))
            game = game.enterDigit(index, step.digit)
            assertTrue(game.notesAt(index).isEmpty())
            assertFalse(game.isWrong(index))
            if (index == hole) holeOpen = false
            moves++
            assertTrue(moves <= Board.CELL_COUNT)
        }
        assertTrue(game.wrongCells().isEmpty())
        repeat(moves) { game = game.undo() }
        assertEquals(puzzle.board.toText(), game.board.toText())
        assertEquals(setOf(8), game.notesAt(hole))
        val backToStart = game.undo()
        assertTrue(backToStart.notesAt(hole).isEmpty())
        assertFalse(backToStart.canUndo())
    }

    @Test
    fun aSolutionWithAnEmptyCellIsRejected() {
        val hole = firstHole()
        val bad = puzzle.copy(solution = puzzle.solution.withDigit(hole, 0))
        assertThrows(IllegalArgumentException::class.java) { Game.start(bad) }
    }

    @Test
    fun undoOnTheSameGameTwiceAgreesAndANewMoveDropsTheOtherBranch() {
        val hole = firstHole()
        val other = secondHole(hole)
        val played = Game.start(puzzle).enterDigit(hole, otherDigit(hole))
        val firstUndo = played.undo()
        val secondUndo = played.undo()
        assertEquals(firstUndo.board.toText(), secondUndo.board.toText())
        assertFalse(firstUndo.canUndo())
        val branched = firstUndo.enterDigit(other, puzzle.solution[other])
        assertEquals(puzzle.solution[other], branched.board[other])
        assertEquals(0, branched.board[hole])
        val back = branched.undo()
        assertEquals(0, back.board[other])
        assertEquals(0, back.board[hole])
        assertFalse(back.canUndo())
    }

    @Test
    fun aWrongNumberBlocksAHintUntilItIsUndone() {
        val game = Game.start(puzzle)
        val hint = findHint(game.board, game.solution)
        assertTrue(hint is HintResult.Single)
        val step = (hint as HintResult.Single).step
        val index = (step.row - 1) * Board.SIZE + (step.column - 1)
        assertEquals(0, game.board[index])
        assertEquals(puzzle.solution[index], step.digit)
        val wrong = game.enterDigit(index, otherDigit(index))
        assertTrue(findHint(wrong.board, wrong.solution) is HintResult.MistakeFound)
        val fixed = wrong.enterDigit(index, step.digit)
        assertFalse(fixed.isWrong(index))
        val next = findHint(fixed.board, fixed.solution)
        if (next is HintResult.Single) {
            val nextIndex = (next.step.row - 1) * Board.SIZE + (next.step.column - 1)
            assertEquals(puzzle.solution[nextIndex], next.step.digit)
        }
    }

    @Test
    fun randomMovesMatchASeparateRecord() {
        val puzzle = newPuzzle(Level.Medium, Random(2))
        var game = Game.start(puzzle)
        val clues = game.clues.toText()
        val solution = game.solution.toText()
        var text = game.board.toText()
        var notes = List(Board.CELL_COUNT) { emptySet<Int>() }
        val past = ArrayDeque<Pair<String, List<Set<Int>>>>()
        val random = Random(7)
        repeat(50) {
            val index = random.nextInt(Board.CELL_COUNT)
            when (random.nextInt(4)) {
                0 -> {
                    val digit = random.nextInt(1, 10)
                    if (clues[index] == '0' && !(text[index].digitToInt() == digit && notes[index].isEmpty())) {
                        past.addLast(text to notes)
                        text = text.replaceRange(index, index + 1, digit.toString())
                        notes = notes.replaced(index, emptySet())
                    }
                    game = game.enterDigit(index, digit)
                }
                1 -> {
                    if (clues[index] == '0' && (text[index] != '0' || notes[index].isNotEmpty())) {
                        past.addLast(text to notes)
                        text = text.replaceRange(index, index + 1, "0")
                        notes = notes.replaced(index, emptySet())
                    }
                    game = game.clearCell(index)
                }
                2 -> {
                    val digit = random.nextInt(1, 10)
                    if (clues[index] == '0' && text[index] == '0') {
                        past.addLast(text to notes)
                        val next = if (digit in notes[index]) notes[index] - digit else notes[index] + digit
                        notes = notes.replaced(index, next)
                    }
                    game = game.toggleNote(index, digit)
                }
                else -> {
                    if (past.isNotEmpty()) {
                        val previous = past.removeLast()
                        text = previous.first
                        notes = previous.second
                    }
                    game = game.undo()
                }
            }
            assertEquals(text, game.board.toText())
            assertEquals(clues, game.clues.toText())
            assertEquals(solution, game.solution.toText())
            assertEquals(past.isNotEmpty(), game.canUndo())
            assertEquals(text == solution, game.isComplete())
            val wrong = (0 until Board.CELL_COUNT).filter { cell ->
                text[cell] != '0' && text[cell].digitToInt() != solution[cell].digitToInt()
            }
            assertEquals(wrong, game.wrongCells())
            for (cell in 0 until Board.CELL_COUNT) {
                assertEquals(notes[cell], game.notesAt(cell))
                if (text[cell] != '0') assertTrue(game.notesAt(cell).isEmpty())
                if (clues[cell] != '0') assertEquals(clues[cell], text[cell])
            }
        }
        while (game.canUndo()) game = game.undo()
        assertEquals(puzzle.board.toText(), game.board.toText())
        for (cell in 0 until Board.CELL_COUNT) assertTrue(game.notesAt(cell).isEmpty())
    }

    private fun List<Set<Int>>.replaced(index: Int, notes: Set<Int>): List<Set<Int>> {
        val copy = toMutableList()
        copy[index] = notes
        return copy
    }

    private fun firstGiven(): Int = (0 until Board.CELL_COUNT).first { puzzle.board[it] != 0 }

    private fun firstHole(): Int = (0 until Board.CELL_COUNT).first { puzzle.board[it] == 0 }

    private fun secondHole(first: Int): Int =
        (0 until Board.CELL_COUNT).first { it != first && puzzle.board[it] == 0 }

    private fun otherDigit(index: Int): Int = if (puzzle.solution[index] == 1) 2 else 1
}
