package com.keenanhutka.seniorsudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Extra checks that do not trust the engine's own helper.
 * Candidate lists here are built by looking at the row, column, and box directly.
 */
class EngineQaTest {
    @Test
    fun centerAndCornerBoxesKeepCellOrder() {
        val solved = Board.fromText(SOLUTION)
        assertEquals(listOf(7, 6, 1, 8, 5, 3, 9, 2, 4), solved.box(4))
        assertEquals(listOf(2, 8, 4, 6, 3, 5, 1, 7, 9), solved.box(8))
        assertEquals(listOf(2, 8, 7, 3, 1, 6, 4, 5, 9), solved.column(8))
    }

    @Test
    fun everySingleHoleIsThatCellsDigit() {
        val solved = Board.fromText(SOLUTION)
        for (index in 0 until Board.CELL_COUNT) {
            val puzzle = solved.withDigit(index, 0)
            assertEquals(listOf(solved[index]), plainCandidates(puzzle, index))
            assertEquals(plainCandidates(puzzle, index), candidates(puzzle, index))
            val step = findNextSingle(puzzle)
            assertEquals(solved[index], step?.digit)
            assertEquals(index / Board.SIZE + 1, step?.row)
            assertEquals(index % Board.SIZE + 1, step?.column)
            assertEquals("Naked single", step?.technique)
        }
    }

    @Test
    fun eightUsesAnInTheSentence() {
        val digits = IntArray(Board.CELL_COUNT)
        intArrayOf(1, 2, 3, 4, 5, 6, 7, 9).forEachIndexed { column, digit ->
            digits[column + 1] = digit
        }
        val step = findNextSingle(Board.fromDigits(digits))
        assertEquals(
            "Row 1, column 1 can only be an 8, " +
                "because every other digit already appears in its row, column or box.",
            step?.sentence,
        )
    }

    @Test
    fun sampleFirstStepAndHardPuzzleNeverPlaceAWrongDigit() {
        val sample = Board.fromText(SAMPLE)
        assertEquals(
            "Row 5, column 5 can only be a 5, " +
                "because every other digit already appears in its row, column or box.",
            findNextSingle(sample)?.sentence,
        )
        checkWalk(sample, Board.fromText(SOLUTION))

        val hard = Board.fromText(HARD)
        val started = System.nanoTime()
        assertEquals(1, countSolutions(hard, limit = 2))
        val solved = solve(hard)
        val millis = (System.nanoTime() - started) / 1_000_000
        assertTrue("Hard puzzle took ${millis}ms", millis < 2_000)
        assertEquals(HARD_SOLUTION, solved?.toText())
        checkWalk(hard, solved!!)
    }

    @Test
    fun hiddenSingleInAColumnUsesAPlainSentence() {
        val column = IntArray(Board.CELL_COUNT)
        intArrayOf(1, 3, 4, 5, 6, 7).forEachIndexed { row, digit ->
            column[row * Board.SIZE + 3] = digit
        }
        column[6 * Board.SIZE] = 2
        column[8 * Board.SIZE + 8] = 2
        val step = findNextSingle(Board.fromDigits(column))
        assertEquals("Hidden single", step?.technique)
        assertEquals("In column 4, the digit 2 has only one place to go: row 8.", step?.sentence)
    }

    @Test
    fun aBoxClashHasNoSolutionAndNoHint() {
        val digits = IntArray(Board.CELL_COUNT)
        digits[0] = 5
        digits[10] = 5
        val board = Board.fromDigits(digits)
        assertEquals(0, countSolutions(board, limit = 2))
        assertNull(solve(board))
        assertEquals(emptyList<Int>(), candidates(board, 2))
        assertNull(findNextSingle(board))
    }

    @Test
    fun callsDoNotChangeTheBoardAndAMiddleMistakeWins() {
        val puzzle = Board.fromText(SAMPLE)
        val before = puzzle.toText()
        solve(puzzle)
        countSolutions(puzzle, limit = 2)
        findNextSingle(puzzle)
        solveWithSinglesOnly(puzzle)
        assertEquals(before, puzzle.toText())

        val solved = Board.fromText(SOLUTION)
        val index = 40
        val wrong = solved.withDigit(index, if (solved[index] == 9) 1 else 9)
        assertEquals(HintResult.MistakeFound, findHint(wrong, solved))
        assertNull(findHint(solved, solved))
        val finished = solveWithSinglesOnly(solved)
        assertTrue(finished.solved)
        assertEquals(0, finished.steps.size)
    }

    private fun checkWalk(start: Board, solved: Board) {
        val before = start.toText()
        val walk = solveWithSinglesOnly(start)
        assertEquals(before, start.toText())
        assertEquals(walk.nakedSingles, walk.steps.count { it.technique == "Naked single" })
        assertEquals(walk.hiddenSingles, walk.steps.count { it.technique == "Hidden single" })
        var board = start
        for (step in walk.steps) {
            assertEquals(plainNext(board), step)
            val index = (step.row - 1) * Board.SIZE + (step.column - 1)
            assertEquals(0, board[index])
            assertEquals(solved[index], step.digit)
            assertTrue(step.digit in plainCandidates(board, index))
            board = board.withDigit(index, step.digit)
        }
        if (walk.solved) assertEquals(solved.toText(), board.toText())
    }

    private fun plainNext(board: Board): HintStep? {
        for (index in 0 until Board.CELL_COUNT) {
            if (board[index] != 0) continue
            val options = plainCandidates(board, index)
            if (options.size == 1) return naked(index, options[0])
        }
        for (kind in 0..2) {
            for (house in 0 until 9) {
                val cells = houseCells(kind, house)
                val placed = BooleanArray(10)
                for (index in cells) if (board[index] != 0) placed[board[index]] = true
                for (digit in 1..9) {
                    if (placed[digit]) continue
                    val spots = cells.filter { board[it] == 0 && digit in plainCandidates(board, it) }
                    if (spots.size == 1) return hidden(kind, house, spots[0], digit)
                }
            }
        }
        return null
    }

    private fun plainCandidates(board: Board, index: Int): List<Int> {
        if (board[index] != 0) return emptyList()
        val seen = BooleanArray(10)
        val row = index / Board.SIZE
        val column = index % Board.SIZE
        for (i in 0 until Board.SIZE) {
            seen[board[row * Board.SIZE + i]] = true
            seen[board[i * Board.SIZE + column]] = true
        }
        val startRow = row / Board.BOX * Board.BOX
        val startColumn = column / Board.BOX * Board.BOX
        for (r in startRow until startRow + Board.BOX) {
            for (c in startColumn until startColumn + Board.BOX) {
                seen[board[r * Board.SIZE + c]] = true
            }
        }
        return (1..9).filter { !seen[it] }
    }

    private fun naked(index: Int, digit: Int): HintStep {
        val row = index / Board.SIZE + 1
        val column = index % Board.SIZE + 1
        val article = if (digit == 8) "an" else "a"
        return HintStep(
            technique = "Naked single",
            row = row,
            column = column,
            digit = digit,
            sentence = "Row $row, column $column can only be $article $digit, " +
                "because every other digit already appears in its row, column or box.",
        )
    }

    private fun hidden(kind: Int, house: Int, index: Int, digit: Int): HintStep {
        val row = index / Board.SIZE + 1
        val column = index % Board.SIZE + 1
        val number = house + 1
        val sentence = when (kind) {
            0 -> "In row $number, the digit $digit has only one place to go: column $column."
            1 -> "In column $number, the digit $digit has only one place to go: row $row."
            else -> {
                val startRow = house / 3 * 3 + 1
                val startColumn = house % 3 * 3 + 1
                "In the box covering rows $startRow to ${startRow + 2} and columns " +
                    "$startColumn to ${startColumn + 2}, the digit $digit has only one place to go: " +
                    "row $row, column $column."
            }
        }
        return HintStep("Hidden single", row, column, digit, sentence)
    }

    private fun houseCells(kind: Int, house: Int): List<Int> = when (kind) {
        0 -> List(9) { house * 9 + it }
        1 -> List(9) { it * 9 + house }
        else -> {
            val startRow = house / 3 * 3
            val startColumn = house % 3 * 3
            buildList {
                for (row in startRow until startRow + 3) {
                    for (column in startColumn until startColumn + 3) {
                        add(row * 9 + column)
                    }
                }
            }
        }
    }

    private companion object {
        const val SAMPLE =
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        const val SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
        const val HARD =
            "800000000003600000070090200050007000000045700000100030001000068008500010090000400"
        const val HARD_SOLUTION =
            "812753649943682175675491283154237896369845721287169534521974368438526917796318452"
    }
}
