package com.keenanhutka.seniorsudoku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
    var game by remember { mutableStateOf(Game.start(newPuzzle(Level.Easy, Random(1)))) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val digit = selected?.let { game.board[it] } ?: 0
    val given = selected?.let { game.isGiven(it) } == true
    val accept = keysAcceptDigit(selected != null, given)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                    wrong = selected?.let { game.isWrong(it) } == true,
                    notes = selected?.let { game.notesAt(it) } ?: emptySet(),
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            SudokuBoard(
                game = game,
                selected = selected,
                onSelect = { index -> selected = if (selected == index) null else index },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            NumberKeys(
                enabled = accept,
                enteredDigit = if (selected == null) 0 else digit,
                onDigit = { number ->
                    val index = selected ?: return@NumberKeys
                    if (game.isGiven(index)) return@NumberKeys
                    game = game.enterDigit(index, number)
                },
                onErase = {
                    val index = selected ?: return@NumberKeys
                    if (game.isGiven(index)) return@NumberKeys
                    game = game.clearCell(index)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
