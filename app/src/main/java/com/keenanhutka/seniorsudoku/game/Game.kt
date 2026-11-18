package com.keenanhutka.seniorsudoku.game

import com.keenanhutka.seniorsudoku.engine.Board
import com.keenanhutka.seniorsudoku.engine.Level
import com.keenanhutka.seniorsudoku.engine.Puzzle

/**
 * One puzzle being played. Clues cannot be changed.
 * Each change returns a new game, so undo can put the old one back.
 */
class Game private constructor(
    val level: Level,
    val clues: Board,
    val solution: Board,
    val board: Board,
    private val notes: List<Set<Int>>,
    private val history: List<Moment>,
) {
    fun isGiven(index: Int): Boolean {
        requireCell(index)
        return clues[index] != 0
    }

    /** A copy, so changing the set does not change this game or undo. */
    fun notesAt(index: Int): Set<Int> {
        requireCell(index)
        return notes[index].toSet()
    }

    /** A number the player wrote that is not the solution. */
    fun isWrong(index: Int): Boolean {
        requireCell(index)
        val digit = board[index]
        return digit != 0 && digit != solution[index]
    }

    fun wrongCells(): List<Int> = (0 until Board.CELL_COUNT).filter { isWrong(it) }

    fun isComplete(): Boolean {
        for (index in 0 until Board.CELL_COUNT) {
            if (board[index] != solution[index]) return false
        }
        return true
    }

    fun canUndo(): Boolean = history.isNotEmpty()

    /** Writes a digit in an empty or player-filled cell, and clears its notes. */
    fun enterDigit(index: Int, digit: Int): Game {
        requireCell(index)
        require(digit in 1..9) { "A digit is 1 to 9." }
        if (isGiven(index)) return this
        if (board[index] == digit && notes[index].isEmpty()) return this
        val nextNotes = notes.toMutableList()
        nextNotes[index] = emptySet()
        return remember(board.withDigit(index, digit), nextNotes)
    }

    /** Erases a number or notes the player added. A clue stays. */
    fun clearCell(index: Int): Game {
        requireCell(index)
        if (isGiven(index)) return this
        if (board[index] == 0 && notes[index].isEmpty()) return this
        val nextNotes = notes.toMutableList()
        nextNotes[index] = emptySet()
        return remember(board.withDigit(index, 0), nextNotes)
    }

    /** A small note in an empty cell. Calling it again removes that note. */
    fun toggleNote(index: Int, digit: Int): Game {
        requireCell(index)
        require(digit in 1..9) { "A digit is 1 to 9." }
        if (isGiven(index) || board[index] != 0) return this
        val next = if (digit in notes[index]) notes[index] - digit else notes[index] + digit
        val nextNotes = notes.toMutableList()
        nextNotes[index] = next
        return remember(board, nextNotes)
    }

    fun undo(): Game {
        val previous = history.lastOrNull() ?: return this
        return Game(level, clues, solution, previous.board, previous.notes, history.dropLast(1))
    }

    private fun remember(board: Board, notes: List<Set<Int>>): Game {
        return Game(level, clues, solution, board, notes, history + Moment(this.board, this.notes))
    }

    private fun requireCell(index: Int) {
        require(index in 0 until Board.CELL_COUNT) { "Cell is out of range." }
    }

    private data class Moment(val board: Board, val notes: List<Set<Int>>)

    companion object {
        fun start(puzzle: Puzzle): Game {
            requireFinished(puzzle.solution)
            for (index in 0 until Board.CELL_COUNT) {
                val clue = puzzle.board[index]
                require(clue == 0 || clue == puzzle.solution[index]) {
                    "A clue does not match the solution."
                }
            }
            return Game(
                level = puzzle.level,
                clues = puzzle.board,
                solution = puzzle.solution,
                board = puzzle.board,
                notes = List(Board.CELL_COUNT) { emptySet() },
                history = emptyList(),
            )
        }

        private fun requireFinished(solution: Board) {
            for (index in 0 until Board.CELL_COUNT) {
                require(solution[index] in 1..9) { "The solution has an empty cell." }
            }
            for (house in 0 until Board.SIZE) {
                require(solution.row(house).toSet().size == Board.SIZE) {
                    "The solution repeats a number in a row."
                }
                require(solution.column(house).toSet().size == Board.SIZE) {
                    "The solution repeats a number in a column."
                }
                require(solution.box(house).toSet().size == Board.SIZE) {
                    "The solution repeats a number in a box."
                }
            }
        }
    }
}
