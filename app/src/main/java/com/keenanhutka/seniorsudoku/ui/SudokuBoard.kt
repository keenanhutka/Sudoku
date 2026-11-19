package com.keenanhutka.seniorsudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.keenanhutka.seniorsudoku.engine.Board
import com.keenanhutka.seniorsudoku.game.Game

private val ThickLine = 4.dp
private val ThinLine = 1.5.dp
private val WrongInk = Color(0xFF8E2A2A)

/**
 * The 9 by 9 grid. Each square gets an equal share of the screen,
 * so the tap target is as large as the phone allows.
 */
@Composable
fun SudokuBoard(
    game: Game,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val entry = MaterialTheme.colorScheme.primary
    val selectedWash = entry.copy(alpha = 0.28f)
    val peerWash = entry.copy(alpha = 0.12f)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val cell = (side - ThickLine) / Board.SIZE
        Box(Modifier.size(side)) {
            GridLines(ink)
            Column(Modifier.padding(ThickLine / 2).fillMaxSize()) {
                for (row in 0 until Board.SIZE) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (column in 0 until Board.SIZE) {
                            val index = row * Board.SIZE + column
                            Square(
                                game = game,
                                index = index,
                                selected = selected,
                                cell = cell,
                                ink = ink,
                                entry = entry,
                                wash = when {
                                    selected == index -> selectedWash
                                    selected != null && sees(selected, index) -> peerWash
                                    else -> Color.Transparent
                                },
                                onSelect = onSelect,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridLines(ink: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val thick = ThickLine.toPx()
        val thin = ThinLine.toPx()
        val inset = thick / 2f
        val span = size.width - thick
        val cell = span / Board.SIZE
        for (line in 0..Board.SIZE) {
            val at = inset + line * cell
            val stroke = if (line % Board.BOX == 0) thick else thin
            val color = if (line % Board.BOX == 0) ink else ink.copy(alpha = 0.45f)
            drawLine(color, Offset(at, inset), Offset(at, size.height - inset), stroke)
            drawLine(color, Offset(inset, at), Offset(size.width - inset, at), stroke)
        }
    }
}

@Composable
private fun Square(
    game: Game,
    index: Int,
    selected: Int?,
    cell: Dp,
    ink: Color,
    entry: Color,
    wash: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val digit = game.board[index]
    val given = game.isGiven(index)
    val notes = game.notesAt(index)
    val label = cellLabel(
        index = index,
        digit = digit,
        given = given,
        wrong = game.isWrong(index),
        notes = notes,
        selected = selected == index,
    )
    val density = LocalDensity.current
    // toSp already accounts for the system text size, so the digit stays
    // the same share of the square when that setting is turned up.
    val digitSize = with(density) { (cell.toPx() * 0.62f).toSp() }
    val noteSize = with(density) { (cell.toPx() * 0.22f).toSp() }
    val digitColor = when {
        game.isWrong(index) -> WrongInk
        given -> ink
        else -> entry
    }
    Box(
        modifier = modifier
            .background(wash)
            .clickable(onClickLabel = "Select") { onSelect(index) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (digit != 0) {
            Text(
                text = digit.toString(),
                modifier = Modifier.clearAndSetSemantics {},
                color = digitColor,
                fontSize = digitSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        } else if (notes.isNotEmpty()) {
            NoteGrid(notes, noteSize, ink)
        }
    }
}

@Composable
private fun NoteGrid(notes: Set<Int>, fontSize: TextUnit, ink: Color) {
    Column(Modifier.fillMaxSize()) {
        for (row in 0 until Board.BOX) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (column in 0 until Board.BOX) {
                    val digit = row * Board.BOX + column + 1
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (digit in notes) {
                            Text(
                                text = digit.toString(),
                                modifier = Modifier.clearAndSetSemantics {},
                                color = ink,
                                fontSize = fontSize,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Same row, column, or box. A square does not see itself. */
private fun sees(index: Int, other: Int): Boolean {
    if (index == other) return false
    val row = index / Board.SIZE
    val column = index % Board.SIZE
    val otherRow = other / Board.SIZE
    val otherColumn = other % Board.SIZE
    if (row == otherRow || column == otherColumn) return true
    return row / Board.BOX == otherRow / Board.BOX && column / Board.BOX == otherColumn / Board.BOX
}
