package com.keenanhutka.seniorsudoku.ui

import com.keenanhutka.seniorsudoku.engine.Level
import org.junit.Assert.assertEquals
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
        assertEquals("Your numbers are not saved yet.", LeaveWarning)
    }
}
