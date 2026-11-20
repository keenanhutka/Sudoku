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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

private val KeyShape = RoundedCornerShape(8.dp)
private val DigitText = 32.sp
private val WordText = 22.sp

/** A number key works only on a marked square that is not a clue. */
internal fun keysAcceptDigit(hasSelection: Boolean, given: Boolean): Boolean = hasSelection && !given

/** What TalkBack reads for one number key. */
internal fun numberKeyLabel(digit: Int, entered: Boolean): String {
    require(digit in 1..9) { "A digit is 1 to 9." }
    return if (entered) "$digit, entered" else digit.toString()
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
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val height = keyHeight()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DigitRow(intArrayOf(1, 2, 3), enabled, enteredDigit, onDigit, height)
        DigitRow(intArrayOf(4, 5, 6), enabled, enteredDigit, onDigit, height)
        DigitRow(intArrayOf(7, 8, 9), enabled, enteredDigit, onDigit, height)
        WordKey(
            text = "Erase",
            enabled = enabled,
            onClick = onErase,
            modifier = Modifier.fillMaxWidth().height(height),
        )
    }
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
    onDigit: (Int) -> Unit,
    height: Dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (digit in digits) {
            DigitKey(
                digit = digit,
                enabled = enabled,
                entered = enteredDigit == digit,
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
    onClick: () -> Unit,
    modifier: Modifier,
) {
    KeyFace(
        text = digit.toString(),
        description = numberKeyLabel(digit, entered),
        action = "Enter",
        enabled = enabled,
        entered = entered,
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
    onClick: () -> Unit,
    modifier: Modifier,
    fontSize: TextUnit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val fill = MaterialTheme.colorScheme.primary
    val onFill = MaterialTheme.colorScheme.onPrimary
    val borderColor = when {
        entered -> fill
        enabled -> ink
        else -> ink.copy(alpha = 0.35f)
    }
    Box(
        modifier
            .clip(KeyShape)
            .background(if (entered) fill else paper)
            .border(2.dp, borderColor, KeyShape)
            .clickable(enabled = enabled, onClickLabel = action, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.clearAndSetSemantics {},
            color = if (entered) onFill else ink,
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
