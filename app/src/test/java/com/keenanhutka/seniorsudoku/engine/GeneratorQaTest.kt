package com.keenanhutka.seniorsudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Checks the puzzle maker does not drift from its own solution,
 * and that a calendar date is a real day.
 */
class GeneratorQaTest {
    @Test
    fun seededPuzzlesMatchTheirSolutionOnEveryPlacedDigit() {
        val seen = mutableSetOf<String>()
        for (level in Level.entries) {
            for (seed in 1..8) {
                val puzzle = newPuzzle(level, Random(seed.toLong()))
                seen += puzzle.board.toText()
                assertEquals(level, puzzle.level)
                assertEquals(puzzle.solution.toText(), solve(puzzle.board)?.toText())
                assertEquals(level.minimumClues, puzzle.clueCount)
                assertEquals(0, puzzle.board[40])
                var board = puzzle.board
                val walk = solveWithSinglesOnly(board)
                for (step in walk.steps) {
                    assertEquals(plainNextStep(board), step)
                    for (index in 0 until Board.CELL_COUNT) {
                        if (board[index] != 0) continue
                        assertEquals(
                            plainCandidates(board, index),
                            candidates(board, index).toSet(),
                        )
                    }
                    val index = (step.row - 1) * Board.SIZE + (step.column - 1)
                    assertEquals(0, board[index])
                    assertEquals(puzzle.solution[index], step.digit)
                    board = board.withDigit(index, step.digit)
                }
                if (level == Level.Hard) {
                    assertTrue(!walk.solved)
                    assertTrue(board.toText().contains('0'))
                } else {
                    assertEquals(puzzle.solution.toText(), board.toText())
                }
                val before = puzzle.board.toText()
                solve(puzzle.board)
                solveWithSinglesOnly(puzzle.board)
                assertEquals(before, puzzle.board.toText())
            }
        }
        assertTrue(seen.size > 8)
    }

    @Test
    fun onePuzzleAgreesWithASeparateSolver() {
        val easy = newPuzzle(Level.Easy, Random(99))
        val hard = newPuzzle(Level.Hard, Random(99))
        assertEquals(1, separateCount(easy.board, limit = 2))
        assertEquals(easy.solution.toText(), separateSolve(easy.board))
        assertEquals(1, separateCount(hard.board, limit = 2))
        assertEquals(hard.solution.toText(), separateSolve(hard.board))
        val two = Board.fromText(
            "915270683204603015683905274102706809706009152009152006391027060527408001068300507",
        )
        assertEquals(2, separateCount(two, limit = 3))
    }

    @Test
    fun aLeapYearOfDailyPuzzlesAreAllDifferent() {
        val texts = HashSet<String>()
        for (month in 1..12) {
            for (day in 1..31) {
                val date = try {
                    PuzzleDate(2024, month, day)
                } catch (_: IllegalArgumentException) {
                    continue
                }
                assertTrue(texts.add(dailyPuzzle(date).board.toText()))
            }
        }
        assertEquals(366, texts.size)
        assertTrue(
            dailyPuzzle(PuzzleDate(2024, 2, 29)).board.toText() !=
                dailyPuzzle(PuzzleDate(2026, 1, 1)).board.toText(),
        )
    }

    @Test
    fun aYearOfDailyPuzzlesAreDifferentMediumPuzzles() {
        val texts = HashSet<String>()
        for (month in 1..12) {
            for (day in 1..31) {
                val date = try {
                    PuzzleDate(2026, month, day)
                } catch (_: IllegalArgumentException) {
                    continue
                }
                val puzzle = dailyPuzzle(date)
                assertEquals(Level.Medium, puzzle.level)
                assertTrue(puzzle.grade in 1..4)
                assertTrue(texts.add(puzzle.board.toText()))
                for (index in 0 until Board.CELL_COUNT) {
                    assertEquals(puzzle.board[index] == 0, puzzle.board[80 - index] == 0)
                    if (puzzle.board[index] != 0) {
                        assertEquals(puzzle.solution[index], puzzle.board[index])
                    }
                }
            }
        }
        assertEquals(365, texts.size)
    }

    @Test
    fun impossibleDaysAreRejectedAndLeapDaysAreNot() {
        assertThrows(IllegalArgumentException::class.java) { PuzzleDate(2026, 2, 29) }
        assertThrows(IllegalArgumentException::class.java) { PuzzleDate(2026, 11, 31) }
        assertThrows(IllegalArgumentException::class.java) { PuzzleDate(1900, 2, 29) }
        assertThrows(IllegalArgumentException::class.java) { PuzzleDate(2026, 13, 1) }
        val leap = PuzzleDate(2024, 2, 29)
        val century = PuzzleDate(2000, 2, 29)
        assertTrue(dailyPuzzle(leap).board.toText() != dailyPuzzle(PuzzleDate(2024, 3, 1)).board.toText())
        assertEquals(Level.Medium, dailyPuzzle(century).level)
    }

    private fun separateSolve(board: Board): String? {
        val grid = IntArray(Board.CELL_COUNT) { board[it] }
        return if (separateSearch(grid, limit = 1) == 1) grid.joinToString("") else null
    }

    private fun separateCount(board: Board, limit: Int): Int {
        val grid = IntArray(Board.CELL_COUNT) { board[it] }
        return separateSearch(grid, limit)
    }

    /** A plain search that does not use the app's bitmask solver. */
    private fun separateSearch(grid: IntArray, limit: Int): Int {
        val empty = ArrayList<Int>()
        for (index in grid.indices) if (grid[index] == 0) empty.add(index)
        var found = 0

        fun allowed(index: Int, digit: Int): Boolean {
            val row = index / 9
            val column = index % 9
            for (i in 0 until 9) {
                if (grid[row * 9 + i] == digit || grid[i * 9 + column] == digit) return false
            }
            val startRow = row / 3 * 3
            val startColumn = column / 3 * 3
            for (r in startRow until startRow + 3) {
                for (c in startColumn until startColumn + 3) {
                    if (grid[r * 9 + c] == digit) return false
                }
            }
            return true
        }

        fun visit(position: Int): Boolean {
            if (position == empty.size) {
                found++
                return found >= limit
            }
            var best = position
            var bestCount = 10
            for (slot in position until empty.size) {
                var count = 0
                for (digit in 1..9) if (allowed(empty[slot], digit)) count++
                if (count < bestCount) {
                    best = slot
                    bestCount = count
                    if (count <= 1) break
                }
            }
            if (bestCount == 0) return false
            val swap = empty[position]
            empty[position] = empty[best]
            empty[best] = swap
            val index = empty[position]
            for (digit in 1..9) {
                if (!allowed(index, digit)) continue
                grid[index] = digit
                if (visit(position + 1)) return true
                grid[index] = 0
            }
            empty[position] = empty[best]
            empty[best] = index
            return false
        }

        visit(0)
        return found
    }

    /** Candidates from row, column, and box sets. No bit masks. */
    private fun plainCandidates(board: Board, index: Int): Set<Int> {
        if (board[index] != 0) return emptySet()
        val row = index / Board.SIZE
        val column = index % Board.SIZE
        val seen = HashSet<Int>()
        for (other in 0 until Board.SIZE) {
            val across = board[row, other]
            val down = board[other, column]
            if (across != 0) seen += across
            if (down != 0) seen += down
        }
        val startRow = row / Board.BOX * Board.BOX
        val startColumn = column / Board.BOX * Board.BOX
        for (r in startRow until startRow + Board.BOX) {
            for (c in startColumn until startColumn + Board.BOX) {
                val digit = board[r, c]
                if (digit != 0) seen += digit
            }
        }
        return (1..9).filter { it !in seen }.toSet()
    }

    /** The hint the rules describe, written without the app's hint code. */
    private fun plainNextStep(board: Board): HintStep? {
        for (index in 0 until Board.CELL_COUNT) {
            if (board[index] != 0) continue
            val options = plainCandidates(board, index)
            if (options.size == 1) {
                val digit = options.first()
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
        }
        val kinds = listOf("row", "column", "box")
        for (kind in kinds) {
            for (house in 0 until Board.SIZE) {
                val cells = plainHouse(kind, house)
                val placed = BooleanArray(10)
                for (index in cells) {
                    val digit = board[index]
                    if (digit != 0) placed[digit] = true
                }
                for (digit in 1..9) {
                    if (placed[digit]) continue
                    val spots = cells.filter { board[it] == 0 && digit in plainCandidates(board, it) }
                    if (spots.size != 1) continue
                    val index = spots[0]
                    val row = index / Board.SIZE + 1
                    val column = index % Board.SIZE + 1
                    val houseNumber = house + 1
                    val sentence = when (kind) {
                        "row" ->
                            "In row $houseNumber, the digit $digit has only one place to go: column $column."
                        "column" ->
                            "In column $houseNumber, the digit $digit has only one place to go: row $row."
                        else -> {
                            val startRow = (house / Board.BOX) * Board.BOX + 1
                            val startColumn = (house % Board.BOX) * Board.BOX + 1
                            val article = if (digit == 8) "an" else "a"
                            "Row $row, column $column is the only place for $article $digit in the box " +
                                "on rows $startRow to ${startRow + 2}, columns $startColumn to ${startColumn + 2}."
                        }
                    }
                    return HintStep("Hidden single", row, column, digit, sentence)
                }
            }
        }
        return null
    }

    private fun plainHouse(kind: String, house: Int): List<Int> = when (kind) {
        "row" -> List(Board.SIZE) { column -> house * Board.SIZE + column }
        "column" -> List(Board.SIZE) { row -> row * Board.SIZE + house }
        else -> {
            val startRow = (house / Board.BOX) * Board.BOX
            val startColumn = (house % Board.BOX) * Board.BOX
            buildList {
                for (row in startRow until startRow + Board.BOX) {
                    for (column in startColumn until startColumn + Board.BOX) {
                        add(row * Board.SIZE + column)
                    }
                }
            }
        }
    }
}
