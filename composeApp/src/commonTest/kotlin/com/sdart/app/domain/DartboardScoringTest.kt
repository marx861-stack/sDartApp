package com.sdart.app.domain

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DartboardScoringTest {

    private val center = Offset(500f, 500f)
    private val radius = 400f

    @Test
    fun testBullseyeHit() {
        val hit = calculateDartHit(
            tapOffset = center,
            center = center,
            radius = radius
        )
        assertEquals(25, hit.segment)
        assertEquals(2, hit.multiplier)
        assertEquals(50, hit.points)
        assertEquals("DBull", hit.displayText)
        assertTrue(hit.isBullseye)
        assertFalse(hit.isOuterBull)
        assertFalse(hit.isMiss)
    }

    @Test
    fun testOuterBullHit() {
        // Zwischen 0.037 und 0.093 des Radius (z.B. 0.06)
        val outerBullPos = Offset(center.x, center.y - radius * 0.06f)
        val hit = calculateDartHit(
            tapOffset = outerBullPos,
            center = center,
            radius = radius
        )
        assertEquals(25, hit.segment)
        assertEquals(1, hit.multiplier)
        assertEquals(25, hit.points)
        assertEquals("Bull", hit.displayText)
        assertTrue(hit.isOuterBull)
        assertFalse(hit.isBullseye)
        assertFalse(hit.isMiss)
    }

    @Test
    fun testTriple20Hit() {
        // 12 Uhr Richtung, Radiusanteil 0.60 (zwischen 0.571 und 0.629)
        val triple20Pos = Offset(center.x, center.y - radius * 0.60f)
        val hit = calculateDartHit(
            tapOffset = triple20Pos,
            center = center,
            radius = radius
        )
        assertEquals(20, hit.segment)
        assertEquals(3, hit.multiplier)
        assertEquals(60, hit.points)
        assertEquals("T20", hit.displayText)
        assertFalse(hit.isMiss)
    }

    @Test
    fun testDouble20Hit() {
        // 12 Uhr Richtung, Radiusanteil 0.97 (zwischen 0.941 und 1.000)
        val double20Pos = Offset(center.x, center.y - radius * 0.97f)
        val hit = calculateDartHit(
            tapOffset = double20Pos,
            center = center,
            radius = radius
        )
        assertEquals(20, hit.segment)
        assertEquals(2, hit.multiplier)
        assertEquals(40, hit.points)
        assertEquals("D20", hit.displayText)
        assertFalse(hit.isMiss)
    }

    @Test
    fun testSingle20Hit() {
        // 12 Uhr Richtung, Inneres Single-Feld (z.B. Radiusanteil 0.35)
        val single20Pos = Offset(center.x, center.y - radius * 0.35f)
        val hit = calculateDartHit(
            tapOffset = single20Pos,
            center = center,
            radius = radius
        )
        assertEquals(20, hit.segment)
        assertEquals(1, hit.multiplier)
        assertEquals(20, hit.points)
        assertEquals("20", hit.displayText)
        assertFalse(hit.isMiss)
    }

    @Test
    fun testMissOutsideDoubleRing() {
        // Außerhalb des Doppelrings (Radiusanteil 1.05)
        val missPos = Offset(center.x, center.y - radius * 1.05f)
        val hit = calculateDartHit(
            tapOffset = missPos,
            center = center,
            radius = radius
        )
        assertEquals(0, hit.segment)
        assertEquals(0, hit.multiplier)
        assertEquals(0, hit.points)
        assertEquals("Miss", hit.displayText)
        assertTrue(hit.isMiss)
    }

    @Test
    fun testCompatibilityOverload() {
        val triple20Pos = Offset(center.x, center.y - radius * 0.60f)
        val scorePair = calculateDartScore(
            tapOffset = triple20Pos,
            center = center,
            radius = radius
        )
        assertEquals(Pair(20, 3), scorePair)
    }
}
