package com.keenanhutka.seniorsudoku.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenanhutka.seniorsudoku.engine.Level
import com.keenanhutka.seniorsudoku.engine.newPuzzle
import com.keenanhutka.seniorsudoku.game.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val ToolHeight = 56.dp
private val ToolShape = RoundedCornerShape(8.dp)
private const val BlockedNote = "That number is already in this row, column, or box."

/** The opening screen. Tap a square, then tap a number to write it. */
@Composable
fun HomeScreen() {
    // Kept when the phone turns, so the numbers you typed are not wiped.
    var game by remember { mutableStateOf(Game.start(newPuzzle(Level.Easy, Random(1)))) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var notesOn by remember { mutableStateOf(false) }
    var checkMessage by remember { mutableStateOf<String?>(null) }
    var closed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val debuggable = isDebuggable()
    val digit = selected?.let { game.board[it] } ?: 0
    val given = selected?.let { game.isGiven(it) } == true
    val wrong = selected?.let { game.isWrong(it) } == true
    val notes = selected?.let { game.notesAt(it) } ?: emptySet()
    val cellEmpty = digit == 0
    val accept = keysAcceptDigit(selected != null, given, notesOn, cellEmpty)
    val canErase = selected != null && !given

    fun clearNotice() {
        checkMessage = null
    }

    fun openPuzzle(level: Level) {
        loading = true
        scope.launch {
            val puzzle = withContext(Dispatchers.Default) { newPuzzle(level) }
            game = Game.start(puzzle)
            selected = null
            notesOn = false
            checkMessage = null
            closed = false
            loading = false
        }
    }

    val onDigit: (Int) -> Unit = onDigit@{ number ->
        val index = selected ?: return@onDigit
        if (game.isGiven(index)) return@onDigit
        if (notesOn) {
            if (game.board[index] != 0) return@onDigit
            val next = game.toggleNote(index, number)
            if (next === game && number !in game.notesAt(index)) {
                checkMessage = BlockedNote
                return@onDigit
            }
            game = next
            if (checkMessage == BlockedNote) checkMessage = null
        } else {
            game = game.enterDigit(index, number)
            clearNotice()
        }
    }
    val onErase: () -> Unit = onErase@{
        val index = selected ?: return@onErase
        if (game.isGiven(index)) return@onErase
        game = game.clearCell(index)
        clearNotice()
    }
    val onUndo: () -> Unit = {
        game = game.undo()
        clearNotice()
    }
    val onCheck: () -> Unit = {
        checkMessage = mistakeMessage(game.wrongCells().size)
    }
    val onFill: () -> Unit = {
        game = game.revealSolution()
        clearNotice()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            loading -> CenteredMessage("Making a puzzle...")
            closed -> ClosedScreen(onNewPuzzle = { openPuzzle(game.level) })
            game.isComplete() -> WellDone(
                onNext = { openPuzzle(game.level) },
                onDone = {
                    // Step 9 returns to the home screen from here.
                    // Step 17 may show an ad after Done. Next puzzle never shows one.
                    closed = true
                },
            )
            else -> PlayScreen(
                game = game,
                selected = selected,
                digit = digit,
                given = given,
                wrong = wrong,
                notes = notes,
                notesOn = notesOn,
                checkMessage = checkMessage,
                accept = accept,
                canErase = canErase,
                canUndo = game.canUndo(),
                debuggable = debuggable,
                onSelect = { index ->
                    selected = if (selected == index) null else index
                    if (checkMessage == BlockedNote) checkMessage = null
                },
                onNotes = {
                    notesOn = !notesOn
                    if (checkMessage == BlockedNote) checkMessage = null
                },
                onUndo = onUndo,
                onCheck = onCheck,
                onFill = onFill,
                onDigit = onDigit,
                onErase = onErase,
            )
        }
    }
}

@Composable
private fun PlayScreen(
    game: Game,
    selected: Int?,
    digit: Int,
    given: Boolean,
    wrong: Boolean,
    notes: Set<Int>,
    notesOn: Boolean,
    checkMessage: String?,
    accept: Boolean,
    canErase: Boolean,
    canUndo: Boolean,
    debuggable: Boolean,
    onSelect: (Int) -> Unit,
    onNotes: () -> Unit,
    onUndo: () -> Unit,
    onCheck: () -> Unit,
    onFill: () -> Unit,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(8.dp),
    ) {
        val sideways = maxWidth > maxHeight
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val toolRows = if (debuggable) 3 else 2
        val toolHeight = ToolHeight * toolRows + 8.dp * (toolRows - 1)
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
                    ScreenHeader(selected, digit, given, wrong, notes, checkMessage)
                    SudokuBoard(
                        game = game,
                        selected = selected,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
                Column(
                    Modifier.width(minOf(screenWidth * 0.42f, 340.dp)).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlayTools(
                        notesOn = notesOn,
                        canUndo = canUndo,
                        debuggable = debuggable,
                        onNotes = onNotes,
                        onUndo = onUndo,
                        onCheck = onCheck,
                        onFill = onFill,
                    )
                    NumberKeys(
                        enabled = accept,
                        eraseEnabled = canErase,
                        enteredDigit = if (selected == null) 0 else digit,
                        enteredIsWrong = wrong,
                        notesOn = notesOn,
                        notedDigits = notes,
                        onDigit = onDigit,
                        onErase = onErase,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        maxHeight = (screenHeight - toolHeight - 8.dp).coerceAtLeast(48.dp),
                    )
                }
            }
        } else {
            val keyFloor = 48.dp * 4 + 8.dp * 3
            val keyCap = (screenHeight - 80.dp - toolHeight - 40.dp - 240.dp).coerceAtLeast(keyFloor)
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScreenHeader(selected, digit, given, wrong, notes, checkMessage)
                SudokuBoard(
                    game = game,
                    selected = selected,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PlayTools(
                    notesOn = notesOn,
                    canUndo = canUndo,
                    debuggable = debuggable,
                    onNotes = onNotes,
                    onUndo = onUndo,
                    onCheck = onCheck,
                    onFill = onFill,
                )
                NumberKeys(
                    enabled = accept,
                    eraseEnabled = canErase,
                    enteredDigit = if (selected == null) 0 else digit,
                    enteredIsWrong = wrong,
                    notesOn = notesOn,
                    notedDigits = notes,
                    onDigit = onDigit,
                    onErase = onErase,
                    modifier = Modifier.fillMaxWidth(),
                    maxHeight = keyCap,
                )
            }
        }
    }
}

@Composable
private fun PlayTools(
    notesOn: Boolean,
    canUndo: Boolean,
    debuggable: Boolean,
    onNotes: () -> Unit,
    onUndo: () -> Unit,
    onCheck: () -> Unit,
    onFill: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolButton(
                text = if (notesOn) "Notes on" else "Notes off",
                filled = notesOn,
                onClick = onNotes,
                modifier = Modifier.weight(1f).height(ToolHeight),
            )
            ToolButton(
                text = "Undo",
                enabled = canUndo,
                onClick = onUndo,
                modifier = Modifier.weight(1f).height(ToolHeight),
            )
        }
        ToolButton(
            text = "Check my numbers",
            onClick = onCheck,
            modifier = Modifier.fillMaxWidth().height(ToolHeight),
        )
        if (debuggable) {
            ToolButton(
                text = "Fill solution",
                onClick = onFill,
                modifier = Modifier.fillMaxWidth().height(ToolHeight),
            )
        }
    }
}

@Composable
private fun ToolButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val fill = MaterialTheme.colorScheme.primary
    val onFill = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier
            .clip(ToolShape)
            .background(if (filled) fill else paper)
            .border(2.dp, if (filled) fill else ink, ToolShape)
            .clickable(enabled = enabled, onClickLabel = text, onClick = onClick)
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.clearAndSetSemantics {},
            color = if (filled) onFill else ink,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ScreenHeader(
    selected: Int?,
    digit: Int,
    given: Boolean,
    wrong: Boolean,
    notes: Set<Int>,
    checkMessage: String?,
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
            wrong = wrong,
            notes = notes,
        ),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    if (checkMessage != null) {
        Text(
            text = checkMessage,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (checkMessage == "No mistakes.") {
                MaterialTheme.colorScheme.onBackground
            } else {
                WrongInk
            },
        )
    }
}

@Composable
private fun WellDone(onNext: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Well done",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "The puzzle is finished.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        ToolButton(
            text = "Next puzzle",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(ToolHeight),
        )
        ToolButton(
            text = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(ToolHeight),
        )
    }
}

@Composable
private fun ClosedScreen(onNewPuzzle: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Senior Sudoku",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "The puzzle is closed.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        ToolButton(
            text = "New puzzle",
            onClick = onNewPuzzle,
            modifier = Modifier.fillMaxWidth().height(ToolHeight),
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun isDebuggable(): Boolean {
    val context = LocalContext.current
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
