package com.keenanhutka.seniorsudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {
    @Test
    fun textRoundTripAndHouseReads() {
        val board = Board.fromText(UNIQUE)
        assertEquals(UNIQUE, board.toText())
        assertEquals(listOf(5, 3, 0, 0, 7, 0, 0, 0, 0), board.row(0))
        assertEquals(listOf(5, 6, 0, 8, 4, 7, 0, 0, 0), board.column(0))
        assertEquals(listOf(5, 3, 0, 6, 0, 0, 0, 9, 8), board.box(0))
    }

    @Test
    fun candidatesSkipDigitsAlreadySeen() {
        val board = Board.fromText(UNIQUE)
        assertEquals(listOf(1, 2, 4), candidates(board, 2))
        assertEquals(emptyList<Int>(), candidates(board, 0))
    }

    @Test
    fun realPuzzleHasExactlyOneSolution() {
        val board = Board.fromText(UNIQUE)
        assertEquals(1, countSolutions(board, limit = 2))
        assertEquals(UNIQUE_SOLUTION, solve(board)?.toText())
    }

    @Test
    fun puzzleBuiltToHaveTwoSolutionsCountsAsTwo() {
        // Started from a finished grid and removed clues that still left one
        // solution. The last removal is the one that opened a second solution.
        // Counting past the usual stop of 2 confirms there are not three.
        val board = Board.fromText(TWO_SOLUTIONS)
        assertEquals(2, countSolutions(board, limit = 2))
        assertEquals(2, countSolutions(board, limit = 3))
    }

    @Test
    fun invalidPuzzleHasNoSolution() {
        val doubledFive = "55" + "0".repeat(79)
        val board = Board.fromText(doubledFive)
        assertEquals(0, countSolutions(board, limit = 2))
        assertNull(solve(board))
    }

    @Test
    fun almostEmptyGridStopsAtTheLimitQuickly() {
        val board = Board.fromText("5" + "0".repeat(80))
        val started = System.nanoTime()
        val count = countSolutions(board, limit = 2)
        val millis = (System.nanoTime() - started) / 1_000_000
        assertEquals(2, count)
        assertTrue("Stopped in ${millis}ms", millis < 1_000)
    }

    private companion object {
        const val UNIQUE =
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        const val UNIQUE_SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
        const val TWO_SOLUTIONS =
            "915270683204603015683905274102706809706009152009152006391027060527408001068300507"
    }
}
