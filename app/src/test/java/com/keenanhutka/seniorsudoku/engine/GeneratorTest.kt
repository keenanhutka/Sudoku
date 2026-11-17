package com.keenanhutka.seniorsudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorTest {
    @Test
    fun twentyPuzzlesOfEachLevelAreLegalAndGraded() {
        for (level in Level.entries) {
            repeat(20) {
                val puzzle = newPuzzle(level)
                assertEquals(level, puzzle.level)
                assertTrue(puzzle.clueCount >= level.minimumClues)
                assertTrue(puzzle.clueCount <= level.minimumClues + 1)
                assertEquals(puzzle.clueCount, puzzle.board.toText().count { it != '0' })
                assertSymmetric(puzzle.board)
                assertGivensMatch(puzzle)
                assertFinishedGrid(puzzle.solution)
                assertEquals(1, countSolutions(puzzle.board, limit = 2))
                val singles = solveWithSinglesOnly(puzzle.board)
                assertEquals(puzzle.grade, singles.hiddenSingles)
                when (level) {
                    Level.Easy -> {
                        assertEquals(0, puzzle.grade)
                        assertTrue(singles.solved)
                    }
                    Level.Medium -> {
                        assertTrue(puzzle.grade in 1..4)
                        assertTrue(singles.solved)
                    }
                    Level.Hard -> {
                        assertTrue(puzzle.grade >= 5)
                        assertTrue(!singles.solved)
                    }
                }
            }
        }
    }

    @Test
    fun theSameDateGivesTheSamePuzzle() {
        val today = PuzzleDate(2026, 11, 17)
        val first = dailyPuzzle(today)
        val second = dailyPuzzle(today)
        assertEquals(Level.Medium, first.level)
        assertEquals(first.board.toText(), second.board.toText())
        assertEquals(first.solution.toText(), second.solution.toText())
        assertEquals(first.clueCount, second.clueCount)
        assertEquals(first.grade, second.grade)
        val tomorrow = dailyPuzzle(PuzzleDate(2026, 11, 18))
        assertTrue(first.board.toText() != tomorrow.board.toText())
    }

    private fun assertSymmetric(board: Board) {
        for (index in 0 until Board.CELL_COUNT) {
            val turned = Board.CELL_COUNT - 1 - index
            assertEquals(board[index] == 0, board[turned] == 0)
        }
    }

    private fun assertGivensMatch(puzzle: Puzzle) {
        for (index in 0 until Board.CELL_COUNT) {
            if (puzzle.board[index] != 0) {
                assertEquals(puzzle.solution[index], puzzle.board[index])
            }
        }
    }

    private fun assertFinishedGrid(board: Board) {
        val digits = (1..9).toSet()
        for (house in 0 until Board.SIZE) {
            assertEquals(digits, board.row(house).toSet())
            assertEquals(digits, board.column(house).toSet())
            assertEquals(digits, board.box(house).toSet())
        }
    }
}
