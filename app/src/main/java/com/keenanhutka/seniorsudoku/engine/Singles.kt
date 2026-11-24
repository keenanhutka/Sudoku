package com.keenanhutka.seniorsudoku.engine

/**
 * One teaching step. Row and column are counted from 1.
 * The sentence is ready to show to the player.
 */
data class HintStep(
    val technique: String,
    val row: Int,
    val column: Int,
    val digit: Int,
    val sentence: String,
)

/** What a hint request found. A wrong filled cell wins over a new hint. */
sealed class HintResult {
    data class Single(val step: HintStep) : HintResult()
    data object MistakeFound : HintResult()
}

/** How far naked and hidden singles got on their own. */
data class SinglesSolve(
    val solved: Boolean,
    val nakedSingles: Int,
    val hiddenSingles: Int,
    val steps: List<HintStep>,
)

/**
 * The next naked single, or the next hidden single if there is no naked one.
 * Null when singles cannot place another digit. No pairs, triples, or X-Wing.
 */
fun findNextSingle(board: Board): HintStep? {
    for (index in 0 until Board.CELL_COUNT) {
        if (board[index] != 0) continue
        val options = candidates(board, index)
        if (options.size == 1) {
            return nakedStep(index, options[0])
        }
    }
    return findHiddenSingle(board)
}

/**
 * A hint for this board, or a mistake when a filled cell disagrees with the solution.
 * Null when nothing is wrong and singles have no next step.
 */
fun findHint(board: Board, solution: Board): HintResult? {
    for (index in 0 until Board.CELL_COUNT) {
        val placed = board[index]
        if (placed != 0 && placed != solution[index]) return HintResult.MistakeFound
    }
    val step = findNextSingle(board) ?: return null
    return HintResult.Single(step)
}

/**
 * Fills every cell that naked singles and hidden singles can prove.
 * [solved] is true only when those two techniques finish a valid grid.
 */
fun solveWithSinglesOnly(board: Board): SinglesSolve {
    var current = board
    val steps = ArrayList<HintStep>(Board.CELL_COUNT)
    var naked = 0
    var hidden = 0
    repeat(Board.CELL_COUNT + 1) {
        if (isFull(current)) {
            return SinglesSolve(
                solved = countSolutions(current, limit = 1) == 1,
                nakedSingles = naked,
                hiddenSingles = hidden,
                steps = steps,
            )
        }
        val step = findNextSingle(current) ?: return SinglesSolve(false, naked, hidden, steps)
        val index = (step.row - 1) * Board.SIZE + (step.column - 1)
        current = current.withDigit(index, step.digit)
        steps += step
        if (step.technique == NAKED) naked++ else hidden++
    }
    return SinglesSolve(false, naked, hidden, steps)
}

private const val NAKED = "Naked single"
private const val HIDDEN = "Hidden single"

private enum class HouseKind { Row, Column, Box }

private fun nakedStep(index: Int, digit: Int): HintStep {
    val row = index / Board.SIZE + 1
    val column = index % Board.SIZE + 1
    val article = if (digit == 8) "an" else "a"
    return HintStep(
        technique = NAKED,
        row = row,
        column = column,
        digit = digit,
        sentence = "Row $row, column $column can only be $article $digit, " +
            "because every other digit already appears in its row, column or box.",
    )
}

private fun findHiddenSingle(board: Board): HintStep? {
    for (kind in HouseKind.entries) {
        for (house in 0 until Board.SIZE) {
            val cells = houseCells(kind, house)
            val placed = BooleanArray(10)
            for (index in cells) {
                val digit = board[index]
                if (digit != 0) placed[digit] = true
            }
            for (digit in 1..9) {
                if (placed[digit]) continue
                var only = -1
                var count = 0
                for (index in cells) {
                    if (board[index] != 0) continue
                    if (digit in candidates(board, index)) {
                        only = index
                        count++
                        if (count > 1) break
                    }
                }
                if (count == 1) return hiddenStep(kind, house, only, digit)
            }
        }
    }
    return null
}

private fun hiddenStep(kind: HouseKind, house: Int, index: Int, digit: Int): HintStep {
    val row = index / Board.SIZE + 1
    val column = index % Board.SIZE + 1
    val houseNumber = house + 1
    val sentence = when (kind) {
        HouseKind.Row ->
            "In row $houseNumber, the digit $digit has only one place to go: column $column."
        HouseKind.Column ->
            "In column $houseNumber, the digit $digit has only one place to go: row $row."
        HouseKind.Box -> {
            val startRow = (house / Board.BOX) * Board.BOX + 1
            val startColumn = (house % Board.BOX) * Board.BOX + 1
            val article = if (digit == 8) "an" else "a"
            "Row $row, column $column is the only place for $article $digit in the box " +
                "on rows $startRow to ${startRow + 2}, columns $startColumn to ${startColumn + 2}."
        }
    }
    return HintStep(HIDDEN, row, column, digit, sentence)
}

private fun houseCells(kind: HouseKind, house: Int): List<Int> = when (kind) {
    HouseKind.Row -> List(Board.SIZE) { column -> house * Board.SIZE + column }
    HouseKind.Column -> List(Board.SIZE) { row -> row * Board.SIZE + house }
    HouseKind.Box -> {
        val startRow = (house / Board.BOX) * Board.BOX
        val startColumn = (house % Board.BOX) * Board.BOX
        buildList(Board.SIZE) {
            for (row in startRow until startRow + Board.BOX) {
                for (column in startColumn until startColumn + Board.BOX) {
                    add(row * Board.SIZE + column)
                }
            }
        }
    }
}

private fun isFull(board: Board): Boolean {
    for (index in 0 until Board.CELL_COUNT) if (board[index] == 0) return false
    return true
}
