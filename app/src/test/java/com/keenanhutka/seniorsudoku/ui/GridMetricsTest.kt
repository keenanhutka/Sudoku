package com.keenanhutka.seniorsudoku.ui

import androidx.compose.ui.graphics.Color
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
        val boxes = intArrayOf(
            spans[0] + spans[1] + spans[2],
            spans[3] + spans[4] + spans[5],
            spans[6] + spans[7] + spans[8],
        )
        assertEquals(boxes[0], boxes[1])
        assertEquals(boxes[1], boxes[2])
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
                val boxes = intArrayOf(
                    spans[0] + spans[1] + spans[2],
                    spans[3] + spans[4] + spans[5],
                    spans[6] + spans[7] + spans[8],
                )
                assertTrue(boxes.max() - boxes.min() <= 1)
                for (index in 0 until edges.lastIndex) {
                    assertTrue(edges[index + 1] >= edges[index])
                }
            }
        }
    }

    @Test
    fun theSelectionFrameMeetsBothTheThickLineAndTheThinLine() {
        val density = 2.8125f
        val board = (368f * density).roundToInt()
        val inset = (2f * density).roundToInt()
        val thin = (1.5f * density).roundToInt().coerceAtLeast(1)
        val stroke = sharpStroke((2f * density).roundToInt())
        val edges = cellEdges(inset, cellSpans(board, inset))
        // Square 0 is on the outer border and a thin line.
        val left = frameCenter(edges[0], 1, lineHalfPx(0, inset, thin), stroke)
        val right = frameCenter(edges[1], -1, lineHalfPx(1, inset, thin), stroke)
        assertEquals(edges[0] + inset, left - stroke / 2)
        assertEquals(edges[1] - (thin + 1) / 2, right + stroke / 2)
        val span = edges[1] - edges[0]
        val digit = (368f - 4f) * density / 9f * 0.62f
        val clearance = (span - digit) / 2f
        assertTrue(clearance > inset + stroke)
    }

    @Test
    fun aRelatedSquareFrameMeetsTheLineAndStaysThinner() {
        val density = 2.8125f
        val inset = (2f * density).roundToInt()
        val thin = (1.5f * density).roundToInt().coerceAtLeast(1)
        val peer = sharpStroke((1f * density).roundToInt())
        val marked = sharpStroke((2f * density).roundToInt())
        assertEquals(0, peer % 2)
        assertEquals(0, marked % 2)
        assertTrue(peer < marked)
        val edge = 40
        val center = frameCenter(edge, 1, lineHalfPx(1, inset, thin), peer)
        assertEquals(edge + (thin + 1) / 2, center - peer / 2)
    }

    @Test
    fun aMarkedSquareIsSolidAndItsDigitStaysReadable() {
        val paper = Color(0xFFF4EFE4)
        val ink = Color(0xFF1C1812)
        val fill = squareFill(marked = true, wrong = false, peer = false, peerWash = Color.Transparent)
        val digit = squareDigitColor(marked = true, wrong = false, given = true, paper = paper, ink = ink)
        assertEquals(paper, digit)
        assertNotEquals(fill, digit)
        assertNotEquals(Color.Transparent, fill)
        val wrongFill = squareFill(marked = true, wrong = true, peer = false, peerWash = Color.Transparent)
        val wrongDigit = squareDigitColor(marked = true, wrong = true, given = false, paper = paper, ink = ink)
        assertEquals(paper, wrongDigit)
        assertNotEquals(wrongFill, fill)
        assertNotEquals(wrongFill, wrongDigit)
    }
}
