package com.keenanhutka.seniorsudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private val KeyShape = RoundedCornerShape(8.dp)
private val DigitText = 32.sp
private val WordText = 22.sp

/** A number key works only on a marked square that is not a clue. Notes need an empty square. */
internal fun keysAcceptDigit(
    hasSelection: Boolean,
    given: Boolean,
    notesOn: Boolean = false,
    cellEmpty: Boolean = true,
): Boolean {
    if (!hasSelection || given) return false
    return !notesOn || cellEmpty
}

/** Erase works only when the marked square has a number or a note the player added. */
internal fun canEraseCell(hasSelection: Boolean, given: Boolean, hasDigit: Boolean, hasNotes: Boolean): Boolean {
    return hasSelection && !given && (hasDigit || hasNotes)
}

/** A correct number already in the square is not offered again. A wrong one stays on so the tap can remove it. */
internal fun digitKeyEnabled(
    padEnabled: Boolean,
    notesOn: Boolean,
    alreadyEntered: Boolean,
    wrong: Boolean = false,
    heldOff: Boolean = false,
): Boolean {
    if (!padEnabled) return false
    if (notesOn) return true
    if (alreadyEntered && wrong) return true
    if (heldOff) return false
    return !alreadyEntered
}

/** The removed numbers stay off, except while Notes is writing pencil marks. */
internal fun heldOffKey(notesOn: Boolean, digit: Int, heldDigits: Set<Int>): Boolean {
    return !notesOn && digit in heldDigits
}
internal fun quietHeldKey(held: Boolean, enabled: Boolean): Boolean = held && !enabled

/** A key that will not take a tap should look quiet, except the number already in the square. */
internal fun quietKey(enabled: Boolean, entered: Boolean): Boolean = !enabled && !entered
internal fun wrongKeyOutlined(wrong: Boolean): Boolean = wrong

/** What TalkBack reads for one number key. */
internal fun numberKeyLabel(
    digit: Int,
    entered: Boolean,
    wrong: Boolean = false,
    noted: Boolean = false,
    given: Boolean = false,
    heldOff: Boolean = false,
    pencil: Boolean = false,
    needsErase: Boolean = false,
): String {
    require(digit in 1..9) { "A digit is 1 to 9." }
    return when {
        entered && given -> "$digit, given"
        entered && wrong -> "$digit, wrong"
        entered && noted -> "$digit, noted"
        entered -> "$digit, entered"
        heldOff && pencil -> "$digit, key stays off"
        heldOff -> "$digit, not for this square"
        needsErase -> "$digit, erase first"
        else -> digit.toString()
    }
}

/** What TalkBack calls the tap on a number key. */
internal fun numberKeyAction(notesOn: Boolean, noted: Boolean, wrong: Boolean = false): String = when {
    wrong -> "Remove"
    notesOn && noted -> "Remove note"
    notesOn -> "Add note"
    else -> "Enter"
}

private const val KEY_ROWS = 4

/**
 * How tall one key is, in pixels.
 * A short screen shrinks the keys so they cannot push the grid off.
 */
internal fun fittedKeyHeight(preferredPx: Int, spacePx: Int, gapPx: Int): Int {
    require(preferredPx > 0) { "A key must be taller than nothing." }
    require(gapPx >= 0) { "A gap cannot be negative." }
    if (spacePx <= 0) return preferredPx
    val gaps = gapPx * (KEY_ROWS - 1)
    val fitted = (spacePx - gaps) / KEY_ROWS
    if (fitted <= 0) return 1
    return min(preferredPx, fitted)
}

/**
 * The 1 to 9 keys, plus Erase.
 * Three across, so the keys stay large and the same size. Erase has its own row
 * so the word is not cut off.
 */
@Composable
fun NumberKeys(
    enabled: Boolean,
    enteredDigit: Int,
    enteredIsWrong: Boolean,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp? = null,
    eraseEnabled: Boolean = enabled,
    notesOn: Boolean = false,
    notedDigits: Set<Int> = emptySet(),
    given: Boolean = false,
    heldOffDigits: Set<Int> = emptySet(),
) {
    val height = fittedHeight(maxHeight)
    // 1 stays on the left even when the phone language is right to left.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            DigitRow(intArrayOf(1, 2, 3), enabled, enteredDigit, enteredIsWrong, notesOn, notedDigits, given, heldOffDigits, onDigit, height)
            DigitRow(intArrayOf(4, 5, 6), enabled, enteredDigit, enteredIsWrong, notesOn, notedDigits, given, heldOffDigits, onDigit, height)
            DigitRow(intArrayOf(7, 8, 9), enabled, enteredDigit, enteredIsWrong, notesOn, notedDigits, given, heldOffDigits, onDigit, height)
            WordKey(
                text = "Erase",
                enabled = eraseEnabled,
                onClick = onErase,
                modifier = Modifier.fillMaxWidth().height(height),
            )
        }
    }
}

/** Tall enough for the digit, and never taller than the space it was given. */
@Composable
private fun fittedHeight(maxHeight: Dp?): Dp {
    val preferred = keyHeight()
    val cap = maxHeight ?: return preferred
    val density = LocalDensity.current
    val px = with(density) {
        fittedKeyHeight(preferred.roundToPx(), cap.roundToPx(), 8.dp.roundToPx())
    }
    return with(density) { px.toDp() }
}

/** Tall enough for the digit, and taller when the system text size is turned up. */
@Composable
private fun keyHeight(): Dp {
    val density = LocalDensity.current
    val needed = with(density) { DigitText.toPx() + 24.dp.toPx() }
    val floor = with(density) { 56.dp.toPx() }
    return with(density) { max(needed, floor).toDp() }
}

@Composable
private fun DigitRow(
    digits: IntArray,
    enabled: Boolean,
    enteredDigit: Int,
    enteredIsWrong: Boolean,
    notesOn: Boolean,
    notedDigits: Set<Int>,
    given: Boolean,
    heldOffDigits: Set<Int>,
    onDigit: (Int) -> Unit,
    height: Dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (digit in digits) {
            val pencil = digit in notedDigits
            val entered = if (notesOn) pencil else enteredDigit == digit
            val wrong = entered && enteredIsWrong && !notesOn
            val held = heldOffKey(notesOn, digit, heldOffDigits)
            val needsErase = !notesOn && !given && enteredDigit != 0 && !enteredIsWrong
            val keyOn = digitKeyEnabled(enabled, notesOn, entered && !notesOn, wrong, heldOff = held)
            DigitKey(
                digit = digit,
                enabled = keyOn,
                quiet = quietKey(keyOn, entered),
                heldOff = held && !entered,
                pencil = pencil,
                needsErase = needsErase,
                entered = entered,
                wrong = wrong,
                noted = notesOn && entered,
                notesOn = notesOn,
                given = given && entered,
                onClick = { onDigit(digit) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun DigitKey(
    digit: Int,
    enabled: Boolean,
    entered: Boolean,
    wrong: Boolean,
    quiet: Boolean,
    heldOff: Boolean,
    pencil: Boolean,
    needsErase: Boolean,
    noted: Boolean,
    notesOn: Boolean,
    given: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    KeyFace(
        text = digit.toString(),
        description = numberKeyLabel(digit, entered, wrong, noted, given, heldOff = heldOff, pencil = pencil, needsErase = needsErase),
        action = numberKeyAction(notesOn, noted, wrong),
        enabled = enabled,
        entered = entered,
        wrong = wrong,
        quiet = quiet,
        onClick = onClick,
        modifier = modifier,
        fontSize = DigitText,
    )
}

@Composable
private fun WordKey(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    KeyFace(
        text = text,
        description = text,
        action = text,
        enabled = enabled,
        entered = false,
        wrong = false,
        quiet = false,
        onClick = onClick,
        modifier = modifier,
        fontSize = WordText,
    )
}

@Composable
private fun KeyFace(
    text: String,
    description: String,
    action: String,
    enabled: Boolean,
    entered: Boolean,
    wrong: Boolean,
    quiet: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    fontSize: TextUnit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val fill = MaterialTheme.colorScheme.primary
    val onFill = MaterialTheme.colorScheme.onPrimary
    val outlined = wrongKeyOutlined(wrong)
    val borderColor = when {
        outlined -> WrongInk
        entered -> fill
        else -> ink
    }
    val textColor = when {
        outlined -> WrongInk
        entered -> onFill
        else -> ink
    }
    Box(
        modifier
            .alpha(if (quiet) 0.72f else 1f)
            .clip(KeyShape)
            .background(if (entered && !outlined) fill else paper)
            .border(2.dp, borderColor, KeyShape)
            .clickable(enabled = enabled, onClickLabel = action, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.clearAndSetSemantics {},
            color = textColor,
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
