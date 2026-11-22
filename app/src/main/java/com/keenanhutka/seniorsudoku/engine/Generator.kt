package com.keenanhutka.seniorsudoku.engine

import java.util.Calendar
import kotlin.random.Random

/** Easy has 40 clues. Medium has 32. Hard has 28. */
enum class Level(val minimumClues: Int) {
    Easy(40),
    Medium(32),
    Hard(28),
}

/** A puzzle, the finished grid it came from, and how many hidden singles it took. */
data class Puzzle(
    val level: Level,
    val board: Board,
    val solution: Board,
    val clueCount: Int,
    val grade: Int,
)

/** A calendar day. No clock and no network. */
data class PuzzleDate(val year: Int, val month: Int, val day: Int) {
    init {
        require(year in 1..9999) { "Year is out of range." }
        require(month in 1..12) { "Month is out of range." }
        require(day in 1..daysInMonth(year, month)) { "Day is out of range." }
    }
}

/**
 * A new puzzle of this level.
 * Easy uses no hidden singles. Medium uses 1 to 4.
 * Hard uses 5 or more, and singles alone cannot finish it.
 */
fun newPuzzle(level: Level, random: Random = Random): Puzzle {
    repeat(MAX_ATTEMPTS) {
        val puzzle = attempt(level, random) ?: return@repeat
        return puzzle
    }
    error("Could not make a ${level.name} puzzle after $MAX_ATTEMPTS tries.")
}

/**
 * Medium puzzle for this date. The same date always returns the same puzzle.
 */
fun dailyPuzzle(date: PuzzleDate): Puzzle {
    val seed = date.year * 372L + date.month * 31L + date.day
    return newPuzzle(Level.Medium, Random(seed))
}

/** The phone's calendar day. No clock time and no network. */
fun todayPuzzleDate(now: Calendar = Calendar.getInstance()): PuzzleDate {
    return PuzzleDate(
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH),
    )
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 0
}

private const val MAX_ATTEMPTS = 60

private val seedGrid: IntArray = intArrayOf(
    5, 3, 4, 6, 7, 8, 9, 1, 2,
    6, 7, 2, 1, 9, 5, 3, 4, 8,
    1, 9, 8, 3, 4, 2, 5, 6, 7,
    8, 5, 9, 7, 6, 1, 4, 2, 3,
    4, 2, 6, 8, 5, 3, 7, 9, 1,
    7, 1, 3, 9, 2, 4, 8, 5, 6,
    9, 6, 1, 5, 3, 7, 2, 8, 4,
    2, 8, 7, 4, 1, 9, 6, 3, 5,
    3, 4, 5, 2, 8, 6, 1, 7, 9,
)

private const val CENTER_SLOT = 40

private val pairA: IntArray = IntArray(41) { slot -> if (slot == CENTER_SLOT) CENTER_SLOT else slot }
private val pairB: IntArray = IntArray(41) { slot -> if (slot == CENTER_SLOT) CENTER_SLOT else 80 - slot }

private fun attempt(level: Level, random: Random): Puzzle? {
    val solution = shuffledSolution(random)
    val grid = solution.copyOf()
    dig(grid, level.minimumClues, random)
    val clueCount = grid.count { it != 0 }
    if (clueCount != level.minimumClues) return null
    val board = Board.fromDigits(grid)
    val singles = solveWithSinglesOnly(board)
    if (!gradeMatches(level, singles)) return null
    return Puzzle(
        level = level,
        board = board,
        solution = Board.fromDigits(solution),
        clueCount = clueCount,
        grade = singles.hiddenSingles,
    )
}

private fun gradeMatches(level: Level, singles: SinglesSolve): Boolean = when (level) {
    Level.Easy -> singles.hiddenSingles == 0 && singles.solved
    Level.Medium -> singles.hiddenSingles in 1..4 && singles.solved
    Level.Hard -> singles.hiddenSingles >= 5 && !singles.solved
}

/** Only rearranges a known finished grid. It does not invent one. */
private fun shuffledSolution(random: Random): IntArray {
    val relabeled = relabel(seedGrid, random)
    return reorder(relabeled, axisOrder(random), axisOrder(random))
}

private fun relabel(grid: IntArray, random: Random): IntArray {
    val map = IntArray(10)
    val digits = (1..9).shuffled(random)
    for (index in digits.indices) map[index + 1] = digits[index]
    return IntArray(grid.size) { index -> map[grid[index]] }
}

/** Bands (or stacks) can swap. Inside one band, the three rows can swap. */
private fun axisOrder(random: Random): IntArray {
    val groups = (0..2).shuffled(random)
    val order = IntArray(Board.SIZE)
    var dest = 0
    for (group in groups) {
        for (offset in (0..2).shuffled(random)) {
            order[dest] = group * Board.BOX + offset
            dest++
        }
    }
    return order
}

private fun reorder(grid: IntArray, rowOrder: IntArray, columnOrder: IntArray): IntArray {
    val next = IntArray(Board.CELL_COUNT)
    for (row in 0 until Board.SIZE) {
        for (column in 0 until Board.SIZE) {
            next[row * Board.SIZE + column] = grid[rowOrder[row] * Board.SIZE + columnOrder[column]]
        }
    }
    return next
}

private fun dig(grid: IntArray, floor: Int, random: Random) {
    // The center is a pair by itself. Take it before anything else. From a
    // finished grid it always has one solution, and taking it later can fail
    // once other clues are gone, which would leave one clue too many.
    removePair(grid, CENTER_SLOT, floor)
    repeat(8) {
        var removedAny = false
        val order = (pairA.indices).shuffled(random)
        for (slot in order) {
            if (removePair(grid, slot, floor)) removedAny = true
        }
        if (!removedAny) return
    }
}

private fun removePair(grid: IntArray, slot: Int, floor: Int): Boolean {
    val first = pairA[slot]
    val second = pairB[slot]
    if (grid[first] == 0 || grid[second] == 0) return false
    val removing = if (first == second) 1 else 2
    if (grid.count { it != 0 } - removing < floor) return false
    val savedFirst = grid[first]
    val savedSecond = grid[second]
    grid[first] = 0
    grid[second] = 0
    if (countSolutions(Board.fromDigits(grid), limit = 2) == 1) return true
    grid[first] = savedFirst
    grid[second] = savedSecond
    return false
}
