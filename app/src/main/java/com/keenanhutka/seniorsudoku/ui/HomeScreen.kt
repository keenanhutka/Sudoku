package com.keenanhutka.seniorsudoku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keenanhutka.seniorsudoku.engine.Level
import com.keenanhutka.seniorsudoku.engine.newPuzzle
import com.keenanhutka.seniorsudoku.game.Game
import kotlin.random.Random

/** The opening screen. Tap a square, then tap a number to write it. */
@Composable
fun HomeScreen() {
    // Kept when the phone turns, so the numbers you typed are not wiped.
    var game by remember { mutableStateOf(Game.start(newPuzzle(Level.Easy, Random(1)))) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val digit = selected?.let { game.board[it] } ?: 0
    val given = selected?.let { game.isGiven(it) } == true
    val wrong = selected?.let { game.isWrong(it) } == true
    val notes = selected?.let { game.notesAt(it) } ?: emptySet()
    val accept = keysAcceptDigit(selected != null, given)
    val onDigit: (Int) -> Unit = onDigit@{ number ->
        val index = selected ?: return@onDigit
        if (game.isGiven(index)) return@onDigit
        game = game.enterDigit(index, number)
    }
    val onErase: () -> Unit = onErase@{
        val index = selected ?: return@onErase
        if (game.isGiven(index)) return@onErase
        game = game.clearCell(index)
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
        ) {
            val sideways = maxWidth > maxHeight
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            if (sideways) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ScreenHeader(selected, digit, given, wrong, notes)
                        SudokuBoard(
                            game = game,
                            selected = selected,
                            onSelect = { index -> selected = if (selected == index) null else index },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                    NumberKeys(
                        enabled = accept,
                        enteredDigit = if (selected == null) 0 else digit,
                        enteredIsWrong = wrong,
                        onDigit = onDigit,
                        onErase = onErase,
                        modifier = Modifier.width(minOf(screenWidth * 0.42f, 340.dp)).fillMaxHeight(),
                        maxHeight = screenHeight,
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ScreenHeader(selected, digit, given, wrong, notes)
                    SudokuBoard(
                        game = game,
                        selected = selected,
                        onSelect = { index -> selected = if (selected == index) null else index },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    NumberKeys(
                        enabled = accept,
                        enteredDigit = if (selected == null) 0 else digit,
                        enteredIsWrong = wrong,
                        onDigit = onDigit,
                        onErase = onErase,
                        modifier = Modifier.fillMaxWidth(),
                        maxHeight = (screenHeight - 280.dp).coerceAtLeast(48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(selected: Int?, digit: Int, given: Boolean, wrong: Boolean, notes: Set<Int>) {
    Text(
        text = "Senior Sudoku",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = selectionLine(
            index = selected,
            digit = digit,
            given = given,
            wrong = wrong,
            notes = notes,
        ),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
