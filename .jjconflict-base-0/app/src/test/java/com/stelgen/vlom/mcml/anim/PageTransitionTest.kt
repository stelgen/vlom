package com.stelgen.vlom.mcml.anim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты S1.4: математика переходов страниц и звуковой карты.
 */
class PageTransitionTest {

    @Test
    fun `enter slides from right with fade-in`() {
        val t = PageTransition(TransitionType.ENTER, durationMs = 100)
        val f0 = t.frame(0, Interpolators.linear)
        assertEquals(1f, f0.offset, 0.001f)
        assertEquals(0f, f0.alpha, 0.001f)

        val fEnd = t.frame(100, Interpolators.linear)
        assertEquals(0f, fEnd.offset, 0.001f)
        assertEquals(1f, fEnd.alpha, 0.001f)
    }

    @Test
    fun `leave slides to left with fade-out`() {
        val t = PageTransition(TransitionType.LEAVE, durationMs = 100)
        val fEnd = t.frame(100, Interpolators.linear)
        assertEquals(-1f, fEnd.offset, 0.001f)
        assertEquals(0f, fEnd.alpha, 0.001f)
    }

    @Test
    fun `enterBack mirrors enter`() {
        val t = PageTransition(TransitionType.ENTER_BACK, durationMs = 100)
        val f0 = t.frame(0, Interpolators.linear)
        assertEquals(-1f, f0.offset, 0.001f)
        assertEquals(0f, f0.alpha, 0.001f)
    }

    @Test
    fun `elapsed beyond duration clamps to final`() {
        val t = PageTransition(TransitionType.ENTER, durationMs = 50)
        val f = t.frame(99999)
        assertEquals(0f, f.offset, 0.001f)
        assertEquals(1f, f.alpha, 0.001f)
    }

    @Test
    fun `easeOutCubic starts fast ends soft`() {
        val t = PageTransition(TransitionType.ENTER, durationMs = 100)
        val halfLinear = t.frame(50, Interpolators.linear).alpha
        val halfEase = t.frame(50).alpha
        assertTrue("ease must be ahead of linear mid-way", halfEase > halfLinear)
        assertEquals(1f, t.frame(100).alpha, 0.001f)
    }

    @Test
    fun `zero duration renders final frame`() {
        val t = PageTransition(TransitionType.ENTER, durationMs = 0)
        assertEquals(1f, t.frame(0).alpha, 0.001f)
    }
}
