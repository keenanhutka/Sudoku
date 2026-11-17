package com.keenanhutka.seniorsudoku.engine

private const val ALL_DIGITS = 0x1FF

/**
 * Digits that can legally go in an empty cell.
 * An already filled cell has no candidates.
 */
fun candidates(board: Board, index: Int): List<Int> {
    if (board[index] != 0) return emptyList()
    val mask = candidateMask(board.copyCells(), index) ?: return emptyList()
    return digitsIn(mask)
}

/**
 * One finished board, or null when the puzzle has no solution.
 * Stops at the first solution. Uses the empty cell with the fewest candidates.
 */
fun solve(board: Board): Board? {
    val grid = board.copyCells()
    val masks = masksFrom(grid) ?: return null
    return if (search(grid, masks, emptyIndexes(grid), 0, limit = 1) == 1) {
        Board.fromDigits(grid)
    } else {
        null
    }
}

/**
 * How many solutions the board has, stopping as soon as [limit] is reached.
 * A unique puzzle returns 1. An impossible puzzle returns 0.
 */
fun countSolutions(board: Board, limit: Int = 2): Int {
    require(limit >= 1) { "The solution limit must be at least 1." }
    val grid = board.copyCells()
    val masks = masksFrom(grid) ?: return 0
    return search(grid, masks, emptyIndexes(grid), 0, limit)
}

private class Masks(
    val rows: IntArray,
    val columns: IntArray,
    val boxes: IntArray,
)

private fun masksFrom(grid: IntArray): Masks? {
    val rows = IntArray(Board.SIZE) { ALL_DIGITS }
    val columns = IntArray(Board.SIZE) { ALL_DIGITS }
    val boxes = IntArray(Board.SIZE) { ALL_DIGITS }
    for (index in grid.indices) {
        val digit = grid[index]
        if (digit == 0) continue
        val bit = 1 shl (digit - 1)
        val row = index / Board.SIZE
        val column = index % Board.SIZE
        val box = boxIndex(row, column)
        if (rows[row] and bit == 0 || columns[column] and bit == 0 || boxes[box] and bit == 0) {
            return null
        }
        rows[row] = rows[row] xor bit
        columns[column] = columns[column] xor bit
        boxes[box] = boxes[box] xor bit
    }
    return Masks(rows, columns, boxes)
}

private fun emptyIndexes(grid: IntArray): IntArray {
    val empty = ArrayList<Int>()
    for (index in grid.indices) if (grid[index] == 0) empty.add(index)
    return empty.toIntArray()
}

private fun search(grid: IntArray, masks: Masks, empty: IntArray, start: Int, limit: Int): Int {
    var found = 0

    fun visit(position: Int): Boolean {
        if (position == empty.size) {
            found++
            return found >= limit
        }
        var best = position
        var bestMask = 0
        var bestCount = 10
        for (slot in position until empty.size) {
            val index = empty[slot]
            val mask = maskAt(masks, index)
            val count = mask.countOneBits()
            if (count < bestCount) {
                best = slot
                bestMask = mask
                bestCount = count
                if (count <= 1) break
            }
        }
        if (bestCount == 0) return false
        val chosen = empty[best]
        empty[best] = empty[position]
        empty[position] = chosen
        val row = chosen / Board.SIZE
        val column = chosen % Board.SIZE
        val box = boxIndex(row, column)
        var remaining = bestMask
        while (remaining != 0) {
            val bit = remaining and -remaining
            remaining = remaining xor bit
            masks.rows[row] = masks.rows[row] xor bit
            masks.columns[column] = masks.columns[column] xor bit
            masks.boxes[box] = masks.boxes[box] xor bit
            grid[chosen] = digitOf(bit)
            if (visit(position + 1)) return true
            masks.rows[row] = masks.rows[row] xor bit
            masks.columns[column] = masks.columns[column] xor bit
            masks.boxes[box] = masks.boxes[box] xor bit
            grid[chosen] = 0
        }
        empty[position] = empty[best]
        empty[best] = chosen
        return false
    }

    visit(start)
    return found
}

private fun candidateMask(grid: IntArray, index: Int): Int? {
    val masks = masksFrom(grid) ?: return null
    return maskAt(masks, index)
}

private fun maskAt(masks: Masks, index: Int): Int {
    val row = index / Board.SIZE
    val column = index % Board.SIZE
    return masks.rows[row] and masks.columns[column] and masks.boxes[boxIndex(row, column)]
}

private fun boxIndex(row: Int, column: Int): Int = (row / Board.BOX) * Board.BOX + column / Board.BOX

private fun digitsIn(mask: Int): List<Int> {
    val digits = ArrayList<Int>(9)
    var remaining = mask
    while (remaining != 0) {
        val bit = remaining and -remaining
        remaining = remaining xor bit
        digits.add(digitOf(bit))
    }
    return digits
}

private fun digitOf(bit: Int): Int = bit.countTrailingZeroBits() + 1
