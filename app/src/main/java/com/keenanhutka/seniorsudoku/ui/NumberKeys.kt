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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KeyHeight = 56.dp
private val KeyShape = RoundedCornerShape(8.dp)

/** A number key works only on a marked square that is not a clue. */
internal fun keysAcceptDigit(hasSelection: Boolean, given: Boolean): Boolean = hasSelection && !given

/** What TalkBack reads for one number key. */
internal fun numberKeyLabel(digit: Int, entered: Boolean): String {
    require(digit in 1..9) { "A digit is 1 to 9." }
    return if (entered) "$digit, entered" else digit.toString()
}

/** The 1 to 9 keys, plus Erase. Two rows, so each key stays large. */
@Composable
fun NumberKeys(
    enabled: Boolean,
    enteredDigit: Int,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DigitRow(digits = intArrayOf(1, 2, 3, 4, 5), enabled = enabled, enteredDigit = enteredDigit, onDigit = onDigit)
        Row(
            modifier = Modifier.fillMaxWidth().height(KeyHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (digit in 6..9) {
                DigitKey(
                    digit = digit,
                    enabled = enabled,
                    entered = enteredDigit == digit,
                    onClick = { onDigit(digit) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            WordKey(
                text = "Erase",
                enabled = enabled,
                onClick = onErase,
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun DigitRow(
    digits: IntArray,
    enabled: Boolean,
    enteredDigit: Int,
    onDigit: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(KeyHeight),
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
        fontSize = 32,
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
        fontSize = 20,
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
    fontSize: Int,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val fill = MaterialTheme.colorScheme.primary
    val onFill = MaterialTheme.colorScheme.onPrimary
    val size = fontSize.sp
    Box(
        modifier
            .clip(KeyShape)
            .background(if (entered) fill else paper)
            .border(2.dp, if (entered) fill else ink, KeyShape)
            .clickable(enabled = enabled, onClickLabel = action, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (entered) onFill else ink,
            fontSize = size,
            lineHeight = size,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
