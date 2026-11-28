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
    heldOff: Set<Int> = emptySet(),
): String {
    if (index == null) return "Tap a square."
    require(index in 0 until Board.CELL_COUNT) { "Cell is out of range." }
    val row = index / Board.SIZE + 1
    val column = index % Board.SIZE + 1
    val where = "Row $row, column $column."
    val body = when {
        digit == 0 && notes.isNotEmpty() -> "$where Notes ${notes.sorted().joinToString(", ")}."
        digit == 0 -> where
        given -> "$where Given $digit."
        wrong -> "$where $digit is wrong."
        else -> "$where $digit."
    }
    val off = (heldOff - digit).filter { it in 1..9 }.toSet()
    val extra = listOfNotNull(
        heldOffSentence(off - notes),
        staysOffSentence(off.intersect(notes)),
    ).joinToString(" ")
    return if (extra.isEmpty()) body else "$body $extra"
}

/** Names the number keys that will not type in this square. */
internal fun heldOffSentence(numbers: Set<Int>): String? {
    val sorted = numbers.filter { it in 1..9 }.sorted()
    if (sorted.isEmpty()) return null
    val names = when (sorted.size) {
        1 -> sorted[0].toString()
        2 -> "${sorted[0]} and ${sorted[1]}"
        else -> sorted.dropLast(1).joinToString(", ") + ", and " + sorted.last()
    }
    val verb = if (sorted.size == 1) "is" else "are"
    return "$names $verb not for this square."
}

/** A pencil mark can stay. The number key still will not type it. */
internal fun staysOffSentence(numbers: Set<Int>): String? {
    val sorted = numbers.filter { it in 1..9 }.sorted()
    if (sorted.isEmpty()) return null
    val names = when (sorted.size) {
        1 -> sorted[0].toString()
        2 -> "${sorted[0]} and ${sorted[1]}"
        else -> sorted.dropLast(1).joinToString(", ") + ", and " + sorted.last()
    }
    val verb = if (sorted.size == 1) "key stays" else "keys stay"
    return "The $names $verb off."
}

/** What TalkBack calls the tap. A second tap removes the mark. It does not erase the number. */
fun selectionAction(selected: Boolean): String = if (selected) "Deselect" else "Select"

/** TalkBack on a marked square. A second tap keeps a hint or a wrong-number warning. */
fun cellTapAction(marked: Boolean, hintShowing: Boolean, mistakeShowing: Boolean = false): String = when {
    marked && hintShowing -> "Keep the hint"
    marked && mistakeShowing -> "Keep this square"
    else -> selectionAction(marked)
}

/** What TalkBack calls the tap on the Notes button. The label shows the current state. */
fun notesButtonAction(notesOn: Boolean): String = if (notesOn) "Turn notes off" else "Turn notes on"

/** The sentence under the title after Check my numbers. */
fun mistakeMessage(count: Int, unfinished: Boolean = false): String {
    require(count >= 0) { "A mistake count cannot be negative." }
    return when {
        count == 1 -> "1 number looks wrong."
        count > 1 -> "$count numbers look wrong."
        unfinished -> "No mistakes. The puzzle is not finished."
        else -> "No mistakes."
    }
}
