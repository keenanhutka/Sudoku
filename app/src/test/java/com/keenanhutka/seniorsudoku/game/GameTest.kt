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
        val hole = openCell(2)
        val other = (0 until Board.CELL_COUNT).first {
            it != hole && puzzle.board[it] == 0 && freeDigits(it).isNotEmpty()
        }
        val wrong = otherDigit(hole)
        val (first, second) = freeDigits(hole)
        val otherNote = freeDigits(other).first()
        val game = Game.start(puzzle)
            .toggleNote(hole, first)
            .toggleNote(hole, second)
            .toggleNote(other, otherNote)
        assertEquals(setOf(first, second), game.notesAt(hole))
        val placed = game.enterDigit(hole, wrong)
        assertEquals(wrong, placed.board[hole])
        assertTrue(placed.notesAt(hole).isEmpty())
        if (sees(hole, other) && otherNote == wrong) assertTrue(placed.notesAt(other).isEmpty())
        else assertEquals(setOf(otherNote), placed.notesAt(other))
        assertTrue(placed.isWrong(hole))
        assertEquals(listOf(hole), placed.wrongCells())
        val undone = placed.undo()
        assertEquals(0, undone.board[hole])
        assertEquals(setOf(first, second), undone.notesAt(hole))
        assertEquals(setOf(otherNote), undone.notesAt(other))
        assertFalse(undone.isWrong(hole))
    }

    @Test
    fun togglingANoteTwiceRemovesItAndUndoRestoresIt() {
        val hole = firstHole()
        val note = freeDigit(hole)
        val noted = Game.start(puzzle).toggleNote(hole, note)
        val cleared = noted.toggleNote(hole, note)
        assertTrue(cleared.notesAt(hole).isEmpty())
        assertEquals(setOf(note), cleared.undo().notesAt(hole))
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
    fun changingTheNotesYouWereGivenDoesNotChangeTheGameOrUndo() {
        val hole = openCell(3)
        val (note, second, third) = freeDigits(hole)
        val game = Game.start(puzzle).toggleNote(hole, note).toggleNote(hole, second)
        val kept = game.notesAt(hole)
        val expected = kept.toSet()
        val leaked = kept as MutableSet<Int>
        leaked.add((1..9).first { it !in expected })
        assertEquals(expected, game.notesAt(hole))
        val next = game.toggleNote(hole, third)
        assertEquals(setOf(note, second, third), next.notesAt(hole))
        assertEquals(setOf(note, second), next.undo().notesAt(hole))
    }

    @Test
    fun hintsFinishMediumButNotHard() {
        for (level in listOf(Level.Medium, Level.Hard)) {
            val made = newPuzzle(level, Random(3))
            var game = Game.start(made)
            var guard = 0
            while (guard++ < Board.CELL_COUNT) {
                val hint = findHint(game.board, game.solution) as? HintResult.Single ?: break
                val index = (hint.step.row - 1) * Board.SIZE + (hint.step.column - 1)
                assertEquals(0, game.board[index])
                assertEquals(made.solution[index], hint.step.digit)
                game = game.enterDigit(index, hint.step.digit)
                assertFalse(game.isWrong(index))
            }
            assertTrue(game.wrongCells().isEmpty())
            if (level == Level.Hard) {
                assertFalse(game.isComplete())
                assertTrue(game.board.toText().contains('0'))
            } else {
                assertTrue(game.isComplete())
            }
        }
    }

    @Test
    fun clearThenUndoThreeTimesRestoresTheNoteTheNumberAndTheStart() {
        val hole = firstHole()
        val note = freeDigit(hole)
        val noted = Game.start(puzzle).toggleNote(hole, note)
        val placed = noted.enterDigit(hole, otherDigit(hole))
        val cleared = placed.clearCell(hole)
        assertEquals(0, cleared.board[hole])
        assertTrue(cleared.notesAt(hole).isEmpty())
        val backToNumber = cleared.undo()
        assertEquals(otherDigit(hole), backToNumber.board[hole])
        assertTrue(backToNumber.notesAt(hole).isEmpty())
        val backToNote = backToNumber.undo()
        assertEquals(0, backToNote.board[hole])
        assertEquals(setOf(note), backToNote.notesAt(hole))
        val start = backToNote.undo()
        assertTrue(start.notesAt(hole).isEmpty())
        assertFalse(start.canUndo())
        assertEquals(puzzle.board.toText(), start.board.toText())
    }

    @Test
    fun aCorrectNumberThatIsNotTheFirstHintStillFinishesMedium() {
        val made = newPuzzle(Level.Medium, Random(4))
        val first = findHint(made.board, made.solution) as HintResult.Single
        val hintIndex = (first.step.row - 1) * Board.SIZE + (first.step.column - 1)
        val other = (0 until Board.CELL_COUNT).first { it != hintIndex && made.board[it] == 0 }
        var game = Game.start(made).enterDigit(other, made.solution[other])
        assertFalse(game.isWrong(other))
        var guard = 0
        while (!game.isComplete()) {
            val hint = findHint(game.board, game.solution)
            assertTrue(hint is HintResult.Single)
            val step = (hint as HintResult.Single).step
            val index = (step.row - 1) * Board.SIZE + (step.column - 1)
            assertEquals(made.solution[index], step.digit)
            game = game.enterDigit(index, step.digit)
            assertTrue(++guard <= Board.CELL_COUNT)
        }
    }

    @Test
    fun aCellNumberOutsideTheBoardIsRejected() {
        val game = Game.start(puzzle)
        val hole = firstHole()
        assertThrows(IllegalArgumentException::class.java) { game.notesAt(-1) }
        assertThrows(IllegalArgumentException::class.java) { game.notesAt(Board.CELL_COUNT) }
        assertThrows(IllegalArgumentException::class.java) { game.isWrong(Board.CELL_COUNT) }
        assertThrows(IllegalArgumentException::class.java) { game.isGiven(-1) }
        assertThrows(IllegalArgumentException::class.java) { game.enterDigit(hole, 0) }
        assertThrows(IllegalArgumentException::class.java) { game.toggleNote(hole, 10) }
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
        val noteDigit = (1..9).first { candidate ->
            (0 until Board.CELL_COUNT).none { sees(hole, it) && puzzle.board[it] == candidate }
        }
        var game = Game.start(puzzle).toggleNote(hole, noteDigit)
        assertEquals(setOf(noteDigit), game.notesAt(hole))
        var holeOpen = true
        var noteCleared = false
        var moves = 0
        while (!game.isComplete()) {
            val hint = findHint(game.board, game.solution)
            assertTrue(hint is HintResult.Single)
            val step = (hint as HintResult.Single).step
            val index = (step.row - 1) * Board.SIZE + (step.column - 1)
            assertEquals(0, game.board[index])
            assertEquals(puzzle.solution[index], step.digit)
            if (holeOpen && index != hole) {
                if (noteCleared) assertTrue(game.notesAt(hole).isEmpty())
                else assertEquals(setOf(noteDigit), game.notesAt(hole))
            }
            game = game.enterDigit(index, step.digit)
            assertTrue(game.notesAt(index).isEmpty())
            assertFalse(game.isWrong(index))
            if (index != hole && sees(hole, index) && step.digit == noteDigit) noteCleared = true
            if (index == hole) holeOpen = false
            moves++
            assertTrue(moves <= Board.CELL_COUNT)
        }
        assertTrue(game.wrongCells().isEmpty())
        repeat(moves) { game = game.undo() }
        assertEquals(puzzle.board.toText(), game.board.toText())
        assertEquals(setOf(noteDigit), game.notesAt(hole))
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
                    val unchanged = text[index].digitToInt() == digit &&
                        notes[index].isEmpty() &&
                        !peerHasNote(notes, index, digit)
                    if (clues[index] == '0' && !unchanged) {
                        past.addLast(text to notes)
                        text = text.replaceRange(index, index + 1, digit.toString())
                        notes = notes.clearedFor(index, digit)
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
                        val removing = digit in notes[index]
                        val blocked = !removing && digitIsOnBoard(text, index, digit)
                        if (!blocked) {
                            past.addLast(text to notes)
                            val next = if (removing) notes[index] - digit else notes[index] + digit
                            notes = notes.replaced(index, next)
                        }
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

    @Test
    fun placingANumberRemovesThatNoteFromTheSameRowAndUndoBringsItBack() {
        val peer = (0 until Board.CELL_COUNT).first { cell ->
            puzzle.board[cell] == 0 && freeDigits(cell).size >= 2 &&
                (0 until Board.CELL_COUNT).any { it != cell && puzzle.board[it] == 0 && sees(cell, it) }
        }
        val hole = (0 until Board.CELL_COUNT).first { it != peer && puzzle.board[it] == 0 && sees(peer, it) }
        val (digit, extra) = freeDigits(peer)
        val noted = Game.start(puzzle).toggleNote(peer, digit).toggleNote(peer, extra)
        assertEquals(setOf(digit, extra), noted.notesAt(peer))
        val placed = noted.enterDigit(hole, digit)
        assertEquals(setOf(extra), placed.notesAt(peer))
        val cleared = placed.clearCell(hole)
        assertEquals(setOf(extra), cleared.notesAt(peer))
        assertEquals(setOf(extra), cleared.undo().notesAt(peer))
        assertEquals(setOf(digit, extra), placed.undo().notesAt(peer))
    }

    @Test
    fun placingANumberRemovesThatNoteFromAColumnABoxAndNowhereElse() {
        val (down, downPeer) = emptyPair { a, b -> a % Board.SIZE == b % Board.SIZE }
        val (across, boxPeer) = emptyPair { a, b ->
            a / Board.SIZE != b / Board.SIZE &&
                a % Board.SIZE != b % Board.SIZE &&
                boxId(a) == boxId(b) &&
                freeDigits(b).size >= 2
        }
        assertNoteClearedOnlyForPeers(down, downPeer)
        assertNoteClearedOnlyForPeers(across, boxPeer)

        val (digit, nextDigit) = freeDigits(boxPeer)
        val noted = Game.start(puzzle).toggleNote(boxPeer, digit).toggleNote(boxPeer, nextDigit)
        val placed = noted.enterDigit(across, digit)
        assertEquals(setOf(nextDigit), placed.notesAt(boxPeer))
        val replaced = placed.enterDigit(across, nextDigit)
        assertTrue(replaced.notesAt(boxPeer).isEmpty())
        val after = replaced.toggleNote(boxPeer, digit)
        assertEquals(nextDigit, after.board[across])
        assertEquals(setOf(digit), after.notesAt(boxPeer))
        val back = after.undo().undo()
        assertEquals(digit, back.board[across])
        assertEquals(setOf(nextDigit), back.notesAt(boxPeer))
    }

    private fun assertNoteClearedOnlyForPeers(cell: Int, peer: Int) {
        val digit = freeDigits(peer).first()
        val far = (0 until Board.CELL_COUNT).first {
            it != cell && it != peer && puzzle.board[it] == 0 && boxId(it) != boxId(cell) &&
                it / Board.SIZE != cell / Board.SIZE && it % Board.SIZE != cell % Board.SIZE &&
                digit in freeDigits(it)
        }
        val noted = Game.start(puzzle).toggleNote(peer, digit).toggleNote(far, digit)
        val placed = noted.enterDigit(cell, digit)
        assertTrue(placed.notesAt(peer).isEmpty())
        assertEquals(setOf(digit), placed.notesAt(far))
        assertEquals(setOf(digit), placed.undo().notesAt(peer))
    }

    private fun emptyPair(accepts: (Int, Int) -> Boolean): Pair<Int, Int> {
        for (a in 0 until Board.CELL_COUNT) {
            if (puzzle.board[a] != 0) continue
            for (b in 0 until Board.CELL_COUNT) {
                if (a == b || puzzle.board[b] != 0 || freeDigits(b).isEmpty()) continue
                if (accepts(a, b)) return a to b
            }
        }
        error("This puzzle has no empty pair for the check.")
    }

    private fun boxId(index: Int): Int {
        val row = index / Board.SIZE
        val column = index % Board.SIZE
        return (row / Board.BOX) * Board.BOX + column / Board.BOX
    }

    @Test
    fun aNoteOfANumberAlreadyOnTheBoardIsIgnored() {
        val given = firstGiven()
        val digit = puzzle.board[given]
        val hole = holeThatSees(given)
        val game = Game.start(puzzle).toggleNote(hole, digit)
        assertTrue(game.notesAt(hole).isEmpty())
        assertFalse(game.canUndo())
        val allowed = (1..9).first { candidate ->
            candidate != digit && (0 until Board.CELL_COUNT).none { sees(hole, it) && puzzle.board[it] == candidate }
        }
        val noted = game.toggleNote(hole, allowed)
        assertEquals(setOf(allowed), noted.notesAt(hole))
    }

    private fun openCell(freeCount: Int): Int =
        (0 until Board.CELL_COUNT).first { puzzle.board[it] == 0 && freeDigits(it).size >= freeCount }

    private fun freeDigits(index: Int): List<Int> =
        (1..9).filter { candidate ->
            (0 until Board.CELL_COUNT).none { sees(index, it) && puzzle.board[it] == candidate }
        }

    private fun freeDigit(index: Int, except: Int = 0): Int =
        freeDigits(index).first { it != except }

    private fun holeThatSees(index: Int): Int =
        (0 until Board.CELL_COUNT).first { it != index && puzzle.board[it] == 0 && sees(index, it) }

    private fun sees(index: Int, other: Int): Boolean {
        if (index == other) return false
        val row = index / Board.SIZE
        val column = index % Board.SIZE
        val otherRow = other / Board.SIZE
        val otherColumn = other % Board.SIZE
        if (row == otherRow || column == otherColumn) return true
        return row / Board.BOX == otherRow / Board.BOX && column / Board.BOX == otherColumn / Board.BOX
    }

    private fun peerHasNote(notes: List<Set<Int>>, index: Int, digit: Int): Boolean =
        (0 until Board.CELL_COUNT).any { sees(index, it) && digit in notes[it] }

    private fun digitIsOnBoard(text: String, index: Int, digit: Int): Boolean =
        (0 until Board.CELL_COUNT).any { sees(index, it) && text[it].digitToInt() == digit }

    private fun List<Set<Int>>.clearedFor(index: Int, digit: Int): List<Set<Int>> {
        val next = toMutableList()
        next[index] = emptySet()
        for (other in 0 until Board.CELL_COUNT) {
            if (sees(index, other) && digit in next[other]) next[other] = next[other] - digit
        }
        return next
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
