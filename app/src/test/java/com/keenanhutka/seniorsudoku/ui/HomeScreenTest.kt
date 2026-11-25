package com.keenanhutka.seniorsudoku.ui

import com.keenanhutka.seniorsudoku.engine.HintResult
import com.keenanhutka.seniorsudoku.engine.HintStep
import com.keenanhutka.seniorsudoku.engine.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenTest {
    @Test
    fun theHomeScreenNamesEachPuzzleAndWhatTheTapDoes() {
        assertEquals("Today's puzzle", HomeChoice.Today.label)
        assertEquals("Play today's puzzle", HomeChoice.Today.action)
        assertEquals("Easy", HomeChoice.Easy.label)
        assertEquals("Play an easy puzzle", HomeChoice.Easy.action)
        assertEquals("Medium", HomeChoice.Medium.label)
        assertEquals("Play a medium puzzle", HomeChoice.Medium.action)
        assertEquals("Hard", HomeChoice.Hard.label)
        assertEquals("Play a hard puzzle", HomeChoice.Hard.action)
        assertEquals("Today's puzzle", playTitle(Level.Medium, today = true))
        assertEquals("Easy", playTitle(Level.Easy, today = false))
        assertEquals("Medium", playTitle(Level.Medium, today = false))
        assertEquals("Hard", playTitle(Level.Hard, today = false))
        assertEquals("This puzzle is not saved yet.", LeaveWarning)
    }

    @Test
    fun aHintNamesTheStepAndAWrongNumberBlocksIt() {
        val step = HintStep("Naked single", 4, 8, 2, "In row 4, the digit 2 has only one place to go: column 8.")
        val hint = HintResult.Single(step)
        assertEquals(step.sentence, hintMessage(hint))
        assertEquals(34, hintIndex(hint))
        assertEquals(HintMistake, hintMessage(HintResult.MistakeFound(40)))
        assertEquals(NoHint, hintMessage(null))
        assertEquals(null, hintIndex(null))
        assertEquals(40, hintIndex(HintResult.MistakeFound(40)))
        assertFalse(keepNotesOn(notesOn = true, hint))
        assertFalse(keepNotesOn(notesOn = true, HintResult.MistakeFound(40)))
        assertTrue(keepNotesOn(notesOn = true, null))
        assertFalse(keepNotesOn(notesOn = false, hint))
        assertEquals(34 to false, selectionAfterTap(selected = 34, tapped = 34, hintShowing = true))
        assertEquals(0 to true, selectionAfterTap(selected = 34, tapped = 0, hintShowing = true))
        assertEquals(null to false, selectionAfterTap(selected = 34, tapped = 34, hintShowing = false))
        assertEquals(34 to false, selectionAfterTap(selected = 34, tapped = 34, hintShowing = false, mistakeShowing = true))
        assertEquals(0 to false, selectionAfterTap(selected = 34, tapped = 0, hintShowing = false, mistakeShowing = true))
        val kept = noticeAfterChange(HintMistake, listOf(12, 20), unfinished = true)
        assertEquals(HintMistake, kept.message)
        assertEquals(12, kept.mark)
        assertEquals(null, noticeAfterChange(HintMistake, emptyList(), unfinished = true).message)
        val counted = noticeAfterChange("1 number looks wrong.", listOf(4, 9), unfinished = true)
        assertEquals("2 numbers look wrong.", counted.message)
        assertEquals(4, counted.mark)
        assertEquals(9, noticeAfterChange("1 number looks wrong.", listOf(4, 9), unfinished = true, selected = 9).mark)
        assertEquals(4, noticeAfterChange("1 number looks wrong.", listOf(4, 9), unfinished = true, selected = 3).mark)
        assertEquals(null, noticeAfterChange("1 number looks wrong.", emptyList(), unfinished = true).message)
        assertEquals(null, noticeAfterChange("Row 4 can only be a 2.", listOf(4), unfinished = true).message)
        assertTrue(askingToFix(HintMistake))
        assertTrue(askingToFix("1 number looks wrong."))
        assertTrue(askingToFix(mistakeMessage(3)))
        assertFalse(askingToFix("No mistakes."))
        assertFalse(askingToFix(null))
        assertEquals(56f, noticeHeight(linePx = 28f, capPx = 56f), 0f)
        assertEquals(36f, noticeHeight(linePx = 36f, capPx = 56f), 0f)
        assertEquals(70f, noticeHeight(linePx = 70f, capPx = 56f), 0f)
    }
}
