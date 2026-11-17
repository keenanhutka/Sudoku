package com.keenanhutka.seniorsudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SinglesTest {
    @Test
    fun nakedSingleNamesTheOnlyDigit() {
        val digits = IntArray(Board.CELL_COUNT)
        intArrayOf(2, 3, 4, 5, 6, 7, 8, 9).forEachIndexed { column, digit ->
            digits[column + 1] = digit
        }
        val step = findNextSingle(Board.fromDigits(digits))
        assertEquals("Naked single", step?.technique)
        assertEquals(1, step?.row)
        assertEquals(1, step?.column)
        assertEquals(1, step?.digit)
        assertEquals(
            "Row 1, column 1 can only be a 1, " +
                "because every other digit already appears in its row, column or box.",
            step?.sentence,
        )
    }

    @Test
    fun hiddenSingleNamesTheOnlyPlaceInTheRow() {
        // Row 4 is missing 2, 5 and 9, so no cell in that row is a naked single.
        // A 2 already sits in column 7 and in column 9, so row 4 can put a 2
        // only in column 8.
        val digits = IntArray(Board.CELL_COUNT)
        val row = 3 * Board.SIZE
        intArrayOf(1, 3, 4, 6, 7, 8).forEachIndexed { column, digit ->
            digits[row + column] = digit
        }
        // These two 2s sit in different boxes, so the grid is legal.
        // They block column 7 and column 9. Column 8 is the only spot left in row 4.
        digits[6] = 2
        digits[7 * Board.SIZE + 8] = 2
        val step = findNextSingle(Board.fromDigits(digits))
        assertEquals("Hidden single", step?.technique)
        assertEquals(4, step?.row)
        assertEquals(8, step?.column)
        assertEquals(2, step?.digit)
        assertEquals(
            "In row 4, the digit 2 has only one place to go: column 8.",
            step?.sentence,
        )
    }

    @Test
    fun wrongFilledCellIsAMistakeNotAHint() {
        val solution = Board.fromText(SOLUTION)
        val wrongDigit = if (solution[0] == 1) 2 else 1
        val wrong = solution.withDigit(0, wrongDigit)
        assertEquals(HintResult.MistakeFound, findHint(wrong, solution))
    }

    @Test
    fun matchingCluesGetAHintInsteadOfAMistake() {
        val solution = Board.fromText(SOLUTION)
        val puzzle = solution.withDigit(0, 0)
        val hint = findHint(puzzle, solution)
        assertTrue(hint is HintResult.Single)
        assertEquals(solution[0], (hint as HintResult.Single).step.digit)
    }

    @Test
    fun singlesFinishAGridThatWasOpenedOneCellPerRow() {
        val solution = Board.fromText(SOLUTION)
        var puzzle = solution
        for (row in 0 until 4) {
            puzzle = puzzle.withDigit(row * Board.SIZE, 0)
        }
        val result = solveWithSinglesOnly(puzzle)
        result.steps.forEachIndexed { number, step ->
            println("${number + 1}. ${step.technique}. ${step.sentence}")
        }
        assertTrue(result.solved)
        assertEquals(4, result.nakedSingles)
        assertEquals(0, result.hiddenSingles)
        assertEquals(solution.toText(), replay(puzzle, result.steps).toText())
    }

    @Test
    fun samplePuzzlePrintsAPlainExplanation() {
        val puzzle = Board.fromText(SAMPLE)
        val solution = Board.fromText(SOLUTION)
        val result = solveWithSinglesOnly(puzzle)
        println("Solved with singles only: ${result.solved}")
        println("Naked singles: ${result.nakedSingles}. Hidden singles: ${result.hiddenSingles}.")
        result.steps.forEachIndexed { number, step ->
            val index = (step.row - 1) * Board.SIZE + (step.column - 1)
            assertEquals(0, puzzle[index])
            assertEquals(solution[index], step.digit)
            assertTrue(step.sentence.startsWith("Row ") || step.sentence.startsWith("In "))
            println("${number + 1}. ${step.technique}. ${step.sentence}")
        }
        assertTrue(result.steps.isNotEmpty())
        assertTrue(result.solved)
        assertEquals(0, result.hiddenSingles)
        assertEquals(solution.toText(), replay(puzzle, result.steps).toText())
    }

    @Test
    fun singlesStopWhenTheGridStillNeedsMoreThanSingles() {
        val puzzle = Board.fromText("0".repeat(Board.CELL_COUNT))
        val result = solveWithSinglesOnly(puzzle)
        assertTrue(!result.solved)
        assertEquals(0, result.nakedSingles)
        assertEquals(0, result.hiddenSingles)
        assertNull(findNextSingle(puzzle))
    }

    private fun replay(start: Board, steps: List<HintStep>): Board {
        var board = start
        for (step in steps) {
            val index = (step.row - 1) * Board.SIZE + (step.column - 1)
            board = board.withDigit(index, step.digit)
        }
        return board
    }

    private companion object {
        const val SAMPLE =
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        const val SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    }
}
