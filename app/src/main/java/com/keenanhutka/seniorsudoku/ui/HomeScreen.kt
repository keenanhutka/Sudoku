package com.keenanhutka.seniorsudoku.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keenanhutka.seniorsudoku.engine.Board
import com.keenanhutka.seniorsudoku.engine.HintResult
import com.keenanhutka.seniorsudoku.engine.Level
import com.keenanhutka.seniorsudoku.engine.dailyPuzzle
import com.keenanhutka.seniorsudoku.engine.findHint
import com.keenanhutka.seniorsudoku.engine.newPuzzle
import com.keenanhutka.seniorsudoku.engine.todayPuzzleDate
import com.keenanhutka.seniorsudoku.game.Game
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

private val ToolHeight = 56.dp
private val ToolShape = RoundedCornerShape(8.dp)
private const val BlockedNote = "That number is already in this row, column, or box."
private const val CouldNotMake = "Could not make a puzzle. Try again."
internal const val LeaveWarning = "This puzzle is not saved yet."
internal const val HintMistake = "A number is wrong. Fix it before a hint."
internal const val NoHint = "No simple hint right now."

/** True when the sentence is asking the player to fix a wrong number. */
internal fun askingToFix(message: String?): Boolean {
    if (message == null) return false
    return message == HintMistake || message.endsWith("wrong.")
}

/** The sentence Hint shows. A wrong number blocks the hint. */
internal fun hintMessage(result: HintResult?): String = when (result) {
    is HintResult.MistakeFound -> HintMistake
    is HintResult.Single -> result.step.sentence
    null -> NoHint
}

/** The square the hint is talking about. A mistake marks the first wrong square. */
internal fun hintIndex(result: HintResult?): Int? = when (result) {
    is HintResult.Single -> (result.step.row - 1) * Board.SIZE + (result.step.column - 1)
    is HintResult.MistakeFound -> result.index
    null -> null
}

/**
 * Keep a hint when only the marked square changed, and it is empty or wrong.
 * A wrong number stays under the hint so Notes cannot hide the number keys.
 */
internal fun keepTeachingHint(
    hintShowing: Boolean,
    before: Board,
    after: Board,
    selected: Int?,
    selectedWrong: Boolean,
): Boolean {
    if (!hintShowing || selected == null || selected !in 0 until Board.CELL_COUNT) return false
    for (index in 0 until Board.CELL_COUNT) {
        if (index == selected) continue
        if (before[index] != after[index]) return false
    }
    return after[selected] == 0 || selectedWrong
}

/** Leaving a hint on a wrong number keeps a warning, so Notes stay off. */
internal fun warningAfterLeavingHint(clearHint: Boolean, leftCellWrong: Boolean): Boolean =
    clearHint && leftCellWrong

internal fun notesLocked(message: String?, hintShowing: Boolean): Boolean =
    hintShowing || askingToFix(message)

/** A hint already on screen stays. Asking again must not replace that sentence. */
internal fun keepCurrentHint(hintShowing: Boolean): Boolean = hintShowing

internal fun keepNotesOn(notesOn: Boolean, result: HintResult?): Boolean =
    notesOn && result == null

/**
 * Tapping the marked square keeps a hint or a wrong-number warning.
 * Moving to another square clears a hint. A wrong number stays wrong, so that warning stays.
 * The second value is true when the hint sentence should go away.
 */
internal fun selectionAfterTap(
    selected: Int?,
    tapped: Int,
    hintShowing: Boolean,
    mistakeShowing: Boolean = false,
): Pair<Int?, Boolean> {
    if ((hintShowing || mistakeShowing) && selected == tapped) return selected to false
    val next = if (selected == tapped) null else tapped
    return next to (hintShowing && next != selected)
}

/**
 * After a number changes, keep a wrong-number warning only while one remains.
 * A check count is updated. A hint about the next step goes away.
 */
internal data class NoticeUpdate(val message: String?, val mark: Int?)

internal fun noticeAfterChange(
    message: String?,
    wrongCells: List<Int>,
    unfinished: Boolean,
    selected: Int? = null,
    selectedDigit: Int = -1,
): NoticeUpdate {
    if (message != HintMistake && !askingToFix(message)) return NoticeUpdate(null, null)
    if (wrongCells.isEmpty()) return NoticeUpdate(null, null)
    val stay = selected != null && (selected in wrongCells || selectedDigit == 0)
    val mark = if (stay) selected else wrongCells.first()
    val text = if (message == HintMistake) HintMistake else mistakeMessage(wrongCells.size, unfinished)
    return NoticeUpdate(text, mark)
}

/** How tall the hint box is, in pixels. Never slices a line in half. */
internal fun noticeHeight(linePx: Float, capPx: Float): Float {
    if (linePx <= 0f || capPx <= 0f) return capPx.coerceAtLeast(0f)
    val lines = (capPx / linePx).toInt().coerceAtLeast(1)
    return linePx * lines
}

private enum class Page { Home, Loading, Play }

/** A choice on the home screen. Today's puzzle is the same puzzle all day. */
internal enum class HomeChoice(val label: String, val action: String, val level: Level, val daily: Boolean) {
    Today("Today's puzzle", "Play today's puzzle", Level.Medium, true),
    Easy("Easy", "Play an easy puzzle", Level.Easy, false),
    Medium("Medium", "Play a medium puzzle", Level.Medium, false),
    Hard("Hard", "Play a hard puzzle", Level.Hard, false),
}

/** The name on the play screen. Today's puzzle is not just called Medium. */
internal fun playTitle(level: Level, today: Boolean): String =
    if (today) HomeChoice.Today.label else level.name

/** The opening screen. Pick a puzzle, then tap a square and a number. */
@Composable
fun HomeScreen() {
    // Kept when the phone turns, so the numbers you typed are not wiped.
    var game by remember { mutableStateOf<Game?>(null) }
    var page by remember { mutableStateOf(Page.Home) }
    var today by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var notesOn by remember { mutableStateOf(false) }
    var checkMessage by remember { mutableStateOf<String?>(null) }
    var noticeIsError by remember { mutableStateOf(false) }
    var hintShowing by remember { mutableStateOf(false) }
    var homeNotice by remember { mutableStateOf<String?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val debuggable = isDebuggable()
    val digit = selected?.let { game?.board?.get(it) } ?: 0
    val given = selected?.let { game?.isGiven(it) } == true
    val wrong = selected?.let { game?.isWrong(it) } == true
    val notes = selected?.let { game?.notesAt(it) } ?: emptySet()
    val cellEmpty = digit == 0
    val accept = keysAcceptDigit(selected != null, given, notesOn, cellEmpty)
    val canErase = canEraseCell(selected != null, given, digit != 0, notes.isNotEmpty())

    fun clearNotice() {
        checkMessage = null
        hintShowing = false
    }

    fun openPuzzle(level: Level, daily: Boolean) {
        if (loading) return
        loading = true
        page = Page.Loading
        homeNotice = null
        scope.launch {
            val puzzle = try {
                withContext(Dispatchers.Default) {
                    if (daily) dailyPuzzle(todayPuzzleDate()) else newPuzzle(level)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IllegalStateException) {
                homeNotice = CouldNotMake
                loading = false
                page = Page.Home
                return@launch
            }
            game = Game.start(puzzle)
            today = daily
            selected = null
            notesOn = false
            checkMessage = null
            hintShowing = false
            loading = false
            page = Page.Play
        }
    }

    fun returnHome() {
        confirmLeave = false
        page = Page.Home
        today = false
        selected = null
        notesOn = false
        checkMessage = null
        hintShowing = false
    }

    fun afterNumberChange(next: Game) {
        val wasHint = hintShowing
        val index = selected
        val before = game
        game = next
        val selectedWrong = index != null && next.isWrong(index)
        if (before != null && keepTeachingHint(wasHint, before.board, next.board, index, selectedWrong)) {
            notesOn = false
            return
        }
        val digitNow = if (index != null) next.board[index] else -1
        val update = noticeAfterChange(
            checkMessage,
            next.wrongCells(),
            unfinished = !next.isComplete(),
            selected = index,
            selectedDigit = digitNow,
        )
        if (update.message != null) {
            checkMessage = update.message
            noticeIsError = true
            hintShowing = false
            if (update.mark != null) selected = update.mark
        } else {
            clearNotice()
        }
    }

    val onDigit: (Int) -> Unit = onDigit@{ number ->
        val playing = game ?: return@onDigit
        val index = selected ?: return@onDigit
        if (playing.isGiven(index)) return@onDigit
        if (notesOn && !hintShowing) {
            if (playing.board[index] != 0) return@onDigit
            val next = playing.toggleNote(index, number)
            if (next === playing && number !in playing.notesAt(index)) {
                checkMessage = BlockedNote
                noticeIsError = true
                hintShowing = false
                return@onDigit
            }
            game = next
            if (checkMessage == BlockedNote) checkMessage = null
        } else {
            val next = playing.enterDigit(index, number)
            if (next === playing) return@onDigit
            afterNumberChange(next)
        }
    }
    val onErase: () -> Unit = onErase@{
        val playing = game ?: return@onErase
        val index = selected ?: return@onErase
        if (playing.isGiven(index)) return@onErase
        val next = playing.clearCell(index)
        if (next === playing) return@onErase
        afterNumberChange(next)
    }
    val onUndo: () -> Unit = onUndo@{
        val playing = game ?: return@onUndo
        val next = playing.undo()
        if (next === playing) return@onUndo
        afterNumberChange(next)
    }
    val onCheck: () -> Unit = onCheck@{
        val playing = game ?: return@onCheck
        val wrongs = playing.wrongCells()
        val message = mistakeMessage(wrongs.size, unfinished = !playing.isComplete())
        checkMessage = message
        noticeIsError = !message.startsWith("No mistakes")
        hintShowing = false
        if (wrongs.isNotEmpty()) {
            selected = wrongs.first()
            notesOn = false
        }
    }
    val onHint: () -> Unit = onHint@{
        if (keepCurrentHint(hintShowing)) return@onHint
        val playing = game ?: return@onHint
        val result = findHint(playing.board, playing.solution)
        checkMessage = hintMessage(result)
        noticeIsError = result is HintResult.MistakeFound
        hintShowing = result is HintResult.Single
        val index = hintIndex(result)
        if (index != null) selected = index
        notesOn = keepNotesOn(notesOn, result)
    }
    val onFill: () -> Unit = onFill@{
        val playing = game ?: return@onFill
        val next = playing.revealSolution()
        if (next !== playing) {
            game = next
            clearNotice()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val current = game
        when {
            page == Page.Loading || loading -> CenteredMessage("Making a puzzle...")
            confirmLeave -> LeavePuzzle(
                onStay = { confirmLeave = false },
                onLeave = { returnHome() },
            )
            page == Page.Home || current == null -> HomeMenu(
                notice = homeNotice,
                onChoice = { choice -> openPuzzle(choice.level, choice.daily) },
            )
            current.isComplete() -> WellDone(
                onNext = { openPuzzle(current.level, daily = false) },
                onDone = {
                    // Step 17 may show an ad after Done. Next puzzle never shows one.
                    returnHome()
                },
            )
            else -> PlayScreen(
                game = current,
                title = playTitle(current.level, today),
                selected = selected,
                digit = digit,
                given = given,
                wrong = wrong,
                notes = notes,
                notesOn = notesOn,
                checkMessage = checkMessage,
                noticeIsError = noticeIsError,
                accept = accept,
                canErase = canErase,
                canUndo = current.canUndo(),
                debuggable = debuggable,
                hintShowing = hintShowing,
                onSelect = { index ->
                    val previous = selected
                    val (next, clearHint) = selectionAfterTap(
                        previous,
                        index,
                        hintShowing,
                        mistakeShowing = askingToFix(checkMessage),
                    )
                    selected = next
                    if (clearHint || checkMessage == BlockedNote) {
                        val leftWrong = current?.let { playing ->
                            previous != null && playing.isWrong(previous)
                        } == true
                        if (warningAfterLeavingHint(clearHint, leftWrong)) {
                            checkMessage = HintMistake
                            noticeIsError = true
                            hintShowing = false
                            notesOn = false
                        } else {
                            checkMessage = null
                            hintShowing = false
                        }
                    }
                },
                onNotes = {
                    if (!notesLocked(checkMessage, hintShowing)) {
                        notesOn = !notesOn
                        if (checkMessage == BlockedNote) checkMessage = null
                    }
                },
                onUndo = onUndo,
                onCheck = onCheck,
                onHint = onHint,
                onFill = onFill,
                onDigit = onDigit,
                onErase = onErase,
                onHome = { confirmLeave = true },
            )
        }
    }
}

@Composable
private fun PlayScreen(
    game: Game,
    title: String,
    selected: Int?,
    digit: Int,
    given: Boolean,
    wrong: Boolean,
    notes: Set<Int>,
    notesOn: Boolean,
    checkMessage: String?,
    noticeIsError: Boolean,
    accept: Boolean,
    canErase: Boolean,
    canUndo: Boolean,
    debuggable: Boolean,
    hintShowing: Boolean,
    onSelect: (Int) -> Unit,
    onNotes: () -> Unit,
    onUndo: () -> Unit,
    onCheck: () -> Unit,
    onHint: () -> Unit,
    onFill: () -> Unit,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onHome: () -> Unit,
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
                    ScreenHeader(title, selected, digit, given, wrong, notes, checkMessage, noticeIsError, onHome)
                    SudokuBoard(
                        game = game,
                        selected = selected,
                        onSelect = onSelect,
                        hintShowing = hintShowing,
                        mistakeShowing = askingToFix(checkMessage),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
                Column(
                    Modifier.width(minOf(screenWidth * 0.42f, 340.dp)).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlayTools(
                        notesOn = notesOn,
                        notesEnabled = !notesLocked(checkMessage, hintShowing),
                        canUndo = canUndo,
                        debuggable = debuggable,
                        onNotes = onNotes,
                        onUndo = onUndo,
                        onCheck = onCheck,
                        onHint = onHint,
                        onFill = onFill,
                    )
                    NumberKeys(
                        enabled = accept,
                        eraseEnabled = canErase,
                        enteredDigit = if (selected == null) 0 else digit,
                        enteredIsWrong = wrong,
                        notesOn = notesOn,
                        notedDigits = notes,
                        given = given,
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
                ScreenHeader(title, selected, digit, given, wrong, notes, checkMessage, noticeIsError, onHome)
                SudokuBoard(
                    game = game,
                    selected = selected,
                    onSelect = onSelect,
                    hintShowing = hintShowing,
                    mistakeShowing = askingToFix(checkMessage),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PlayTools(
                    notesOn = notesOn,
                    notesEnabled = !notesLocked(checkMessage, hintShowing),
                    canUndo = canUndo,
                    debuggable = debuggable,
                    onNotes = onNotes,
                    onUndo = onUndo,
                    onCheck = onCheck,
                    onHint = onHint,
                    onFill = onFill,
                )
                NumberKeys(
                    enabled = accept,
                    eraseEnabled = canErase,
                    enteredDigit = if (selected == null) 0 else digit,
                    enteredIsWrong = wrong,
                    notesOn = notesOn,
                    notedDigits = notes,
                    given = given,
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
    notesEnabled: Boolean,
    canUndo: Boolean,
    debuggable: Boolean,
    onNotes: () -> Unit,
    onUndo: () -> Unit,
    onCheck: () -> Unit,
    onHint: () -> Unit,
    onFill: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolButton(
                text = if (notesOn) "Notes on" else "Notes off",
                action = notesButtonAction(notesOn),
                filled = notesOn,
                enabled = notesEnabled,
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolButton(
                text = "Hint",
                action = "Show a hint",
                onClick = onHint,
                modifier = Modifier.widthIn(min = 88.dp).height(ToolHeight),
            )
            ToolButton(
                text = "Check my numbers",
                onClick = onCheck,
                modifier = Modifier.weight(1f).height(ToolHeight),
                fontSize = 18.sp,
            )
        }
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
    action: String = text,
    fontSize: TextUnit = 20.sp,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val fill = MaterialTheme.colorScheme.primary
    val onFill = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.72f)
            .clip(ToolShape)
            .background(if (filled) fill else paper)
            .border(2.dp, if (filled) fill else ink, ToolShape)
            .clickable(enabled = enabled, onClickLabel = action, onClick = onClick)
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.clearAndSetSemantics {},
            color = if (filled) onFill else ink,
            fontSize = fontSize,
            lineHeight = fontSize,
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
    title: String,
    selected: Int?,
    digit: Int,
    given: Boolean,
    wrong: Boolean,
    notes: Set<Int>,
    checkMessage: String?,
    noticeIsError: Boolean,
    onHome: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
        )
        ToolButton(
            text = "Home",
            action = "Ask to leave",
            onClick = onHome,
            modifier = Modifier.widthIn(min = 96.dp).height(homeButtonHeight()),
        )
    }
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
        val noticeScroll = remember(checkMessage) { ScrollState(0) }
        val noticeMax = with(LocalDensity.current) {
            val line = MaterialTheme.typography.titleLarge.lineHeight
            val linePx = if (line == TextUnit.Unspecified) 0f else line.toPx()
            noticeHeight(linePx, 56.dp.toPx()).toDp()
        }
        Text(
            text = checkMessage,
            modifier = Modifier
                .heightIn(max = noticeMax)
                .verticalScroll(noticeScroll)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (noticeIsError) {
                WrongInk
            } else {
                MaterialTheme.colorScheme.onBackground
            },
        )
    }
}

@Composable
private fun HomeMenu(notice: String?, onChoice: (HomeChoice) -> Unit) {
    val buttonHeight = homeButtonHeight()
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Senior Sudoku",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Pick a puzzle.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (notice != null) {
            Text(
                text = notice,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WrongInk,
                textAlign = TextAlign.Center,
            )
        }
        for (choice in HomeChoice.entries) {
            ToolButton(
                text = choice.label,
                action = choice.action,
                onClick = { onChoice(choice) },
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
            )
        }
    }
}

@Composable
private fun homeButtonHeight(): Dp {
    val density = LocalDensity.current
    return with(density) {
        max(56.dp.toPx(), 20.sp.toPx() + 28.dp.toPx()).toDp()
    }
}

@Composable
private fun LeavePuzzle(onStay: () -> Unit, onLeave: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Leave this puzzle?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = LeaveWarning,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        ToolButton(
            text = "Stay",
            action = "Keep playing",
            onClick = onStay,
            modifier = Modifier.fillMaxWidth().height(homeButtonHeight()),
        )
        ToolButton(
            text = "Leave",
            action = "Leave this puzzle",
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth().height(homeButtonHeight()),
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
