package com.keenanhutka.seniorsudoku.engine

/**
 * A 9 by 9 Sudoku grid. 81 digits, and 0 means empty.
 * Plain Kotlin. This package must not import Android.
 */
class Board private constructor(private val cells: IntArray) {
    init {
        require(cells.size == CELL_COUNT) { "A board has 81 cells." }
        require(cells.all { it in 0..9 }) { "Each cell must be a digit from 0 to 9." }
    }

    operator fun get(index: Int): Int = cells[index]

    operator fun get(row: Int, column: Int): Int = cells[row * SIZE + column]

    fun row(row: Int): List<Int> = List(SIZE) { column -> cells[row * SIZE + column] }

    fun column(column: Int): List<Int> = List(SIZE) { row -> cells[row * SIZE + column] }

    fun box(box: Int): List<Int> {
        val startRow = (box / BOX) * BOX
        val startColumn = (box % BOX) * BOX
        return buildList(SIZE) {
            for (row in startRow until startRow + BOX) {
                for (column in startColumn until startColumn + BOX) {
                    add(cells[row * SIZE + column])
                }
            }
        }
    }

    fun withDigit(index: Int, digit: Int): Board {
        require(digit in 0..9)
        val next = cells.copyOf()
        next[index] = digit
        return Board(next)
    }

    /** 81 characters, each a digit. Zero is an empty cell. */
    fun toText(): String = buildString(CELL_COUNT) {
        for (digit in cells) append(digit.toString())
    }

    internal fun copyCells(): IntArray = cells.copyOf()

    companion object {
        const val SIZE = 9
        const val BOX = 3
        const val CELL_COUNT = 81

        fun fromDigits(digits: IntArray): Board = Board(digits.copyOf())

        fun fromText(text: String): Board {
            require(text.length == CELL_COUNT) { "Board text must be 81 characters." }
            val digits = IntArray(CELL_COUNT) { index ->
                val character = text[index]
                require(character in '0'..'9') { "Board text must be digits only." }
                character.digitToInt()
            }
            return Board(digits)
        }
    }
}
