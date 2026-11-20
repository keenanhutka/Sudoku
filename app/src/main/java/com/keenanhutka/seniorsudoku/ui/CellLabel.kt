package com.keenanhutka.seniorsudoku.ui

import com.keenanhutka.seniorsudoku.engine.Board

/** Words TalkBack reads for one square. Rows and columns start at 1. */
fun cellLabel(
    index: Int,
    digit: Int,
    given: Boolean,
    wrong: Boolean,
    notes: Set<Int>,
    selected: Boolean,
): String {
    require(index in 0 until Board.CELL_COUNT) { "Cell is out of range." }
    val row = index / Board.SIZE + 1
    val column = index % Board.SIZE + 1
    val body = when {
        given && digit != 0 -> "given $digit"
        digit == 0 && notes.isEmpty() -> "empty"
        digit == 0 -> "notes ${notes.sorted().joinToString(", ")}"
        wrong -> "$digit, wrong"
        else -> digit.toString()
    }
    val selectedWord = if (selected) ", selected" else ""
    return "Row $row, column $column, $body$selectedWord"
}

/** The line under the title. Null means nothing is selected. */
fun selectionLine(
    index: Int?,
    digit: Int,
    given: Boolean,
    wrong: Boolean,
    notes: Set<Int> = emptySet(),
): String {
    if (index == null) return "Tap a square."
    require(index in 0 until Board.CELL_COUNT) { "Cell is out of range." }
    val row = index / Board.SIZE + 1
    val column = index % Board.SIZE + 1
    val where = "Row $row, column $column."
    return when {
        digit == 0 && notes.isNotEmpty() -> "$where Notes ${notes.sorted().joinToString(", ")}."
        digit == 0 -> where
        given -> "$where Given $digit."
        wrong -> "$where $digit is wrong."
        else -> "$where $digit."
    }
}

/** What TalkBack calls the tap. A second tap clears the mark. */
fun selectionAction(selected: Boolean): String = if (selected) "Clear" else "Select"
