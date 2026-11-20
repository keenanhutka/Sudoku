package com.keenanhutka.seniorsudoku.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class GridMetricsTest {
    @Test
    fun theBorderDoesNotSplitEvenlyOnThisPhone() {
        val density = 2.8125f
        val inset = (2f * density).roundToInt()
        val thick = (4f * density).roundToInt()
        assertNotEquals(inset * 2, thick)
    }

    @Test
    fun theLinesAndTheSquaresShareEveryEdge() {
        val density = 2.8125f
        val board = (368f * density).roundToInt()
        val inset = (2f * density).roundToInt()
        val spans = cellSpans(board, inset)
        val edges = cellEdges(inset, spans)
        assertEquals(inset, edges.first())
        assertEquals(board - inset, edges.last())
        for (index in spans.indices) {
            assertEquals(spans[index], edges[index + 1] - edges[index])
        }
    }

    @Test
    fun leftoverPixelsNeverOpenAGapOrAOverlap() {
        for (board in intArrayOf(0, 1, 8, 17, 100, 365, 1035)) {
            for (inset in intArrayOf(0, 1, 5, 6)) {
                val spans = cellSpans(board, inset)
                val edges = cellEdges(inset, spans)
                assertEquals(10, edges.size)
                assertEquals((board - inset * 2).coerceAtLeast(0), spans.sum())
                assertTrue(spans.all { it >= 0 })
                assertTrue(spans.max() - spans.min() <= 1)
                for (index in 0 until edges.lastIndex) {
                    assertTrue(edges[index + 1] >= edges[index])
                }
            }
        }
    }
}
