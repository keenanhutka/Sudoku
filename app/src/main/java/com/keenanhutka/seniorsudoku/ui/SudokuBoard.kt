package com.keenanhutka.seniorsudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.keenanhutka.seniorsudoku.engine.Board
import com.keenanhutka.seniorsudoku.game.Game
import kotlin.math.roundToInt

private val ThickLine = 4.dp
private val ThinLine = 1.5.dp
/** Darker than the marked-square wash, so a number you typed stays readable. */
private val EntryInk = Color(0xFF6B4418)

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
        // Column 1 stays on the left even when the phone language is right to left.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                Modifier
                    .size(side)
                    .drawWithContent {
                        drawContent()
                        drawGrid(ink)
                    },
            ) {
                Squares(
                    game = game,
                    selected = selected,
                    cell = cell,
                    ink = ink,
                    selectedWash = selectedWash,
                    peerWash = peerWash,
                    onSelect = onSelect,
                )
            }
        }
    }
}

private fun DrawScope.drawGrid(ink: Color) {
    val boardPx = minOf(size.width, size.height).roundToInt()
    val inset = (ThickLine / 2).roundToPx()
    val edges = cellEdges(inset, cellSpans(boardPx, inset))
    val thick = (inset * 2).toFloat()
    val thin = ThinLine.roundToPx().coerceAtLeast(1).toFloat()
    val start = edges.first().toFloat()
    val end = edges.last().toFloat()
    for (line in edges.indices) {
        val at = edges[line].toFloat()
        val stroke = if (line % Board.BOX == 0) thick else thin
        drawLine(ink, Offset(at, start), Offset(at, end), stroke, StrokeCap.Square)
        drawLine(ink, Offset(start, at), Offset(end, at), stroke, StrokeCap.Square)
    }
}

/** How many pixels each square gets. Leftovers are spread so the three boxes stay equal. */
internal fun cellSpans(boardPx: Int, insetPx: Int): IntArray {
    val inner = (boardPx - insetPx * 2).coerceAtLeast(0)
    return IntArray(Board.SIZE) { index ->
        val end = (index + 1) * inner / Board.SIZE
        val start = index * inner / Board.SIZE
        end - start
    }
}

/** Pixel position of each line, including the outer border. */
internal fun cellEdges(insetPx: Int, spans: IntArray): IntArray {
    val edges = IntArray(spans.size + 1)
    var at = insetPx
    for (index in spans.indices) {
        edges[index] = at
        at += spans[index]
    }
    edges[spans.size] = at
    return edges
}

@Composable
private fun Squares(
    game: Game,
    selected: Int?,
    cell: Dp,
    ink: Color,
    selectedWash: Color,
    peerWash: Color,
    onSelect: (Int) -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            for (index in 0 until Board.CELL_COUNT) {
                Square(
                    game = game,
                    index = index,
                    selected = selected,
                    cell = cell,
                    ink = ink,
                    wash = when {
                        selected == index -> selectedWash
                        selected != null && sees(selected, index) -> peerWash
                        else -> Color.Transparent
                    },
                    onSelect = onSelect,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    ) { measurables, constraints ->
        val boardPx = minOf(constraints.maxWidth, constraints.maxHeight)
        val inset = (ThickLine / 2).roundToPx()
        val spans = cellSpans(boardPx, inset)
        val edges = cellEdges(inset, spans)
        val placeables = measurables.mapIndexed { index, measurable ->
            val row = index / Board.SIZE
            val column = index % Board.SIZE
            measurable.measure(Constraints.fixed(spans[column], spans[row]))
        }
        layout(boardPx, boardPx) {
            placeables.forEachIndexed { index, placeable ->
                val row = index / Board.SIZE
                val column = index % Board.SIZE
                placeable.place(edges[column], edges[row])
            }
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
    wash: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val digit = game.board[index]
    val given = game.isGiven(index)
    val notes = game.notesAt(index)
    val marked = selected == index
    val label = cellLabel(
        index = index,
        digit = digit,
        given = given,
        wrong = game.isWrong(index),
        notes = notes,
        selected = marked,
    )
    val density = LocalDensity.current
    // toSp already accounts for the system text size, so the digit stays
    // the same share of the square when that setting is turned up.
    val digitSize = with(density) { (cell.toPx() * 0.62f).toSp() }
    val noteSize = with(density) { (cell.toPx() * 0.22f).toSp() }
    val digitColor = when {
        game.isWrong(index) -> WrongInk
        given -> ink
        else -> EntryInk
    }
    Box(
        modifier = modifier
            .background(wash)
            .clickable(onClickLabel = selectionAction(marked)) { onSelect(index) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (digit != 0) {
            Text(
                text = digit.toString(),
                modifier = Modifier.clearAndSetSemantics {},
                color = digitColor,
                fontSize = digitSize,
                lineHeight = digitSize,
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
                                lineHeight = fontSize,
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
