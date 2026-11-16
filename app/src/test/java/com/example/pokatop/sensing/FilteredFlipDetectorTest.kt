package com.example.pokatop.sensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilteredFlipDetectorTest {

    private fun settledFaceDown(): FilteredFlipDetector {
        val d = FilteredFlipDetector()
        Samples.feed(d, Samples.steady(0f, 0f, -9.8f, 0, 2000))
        assertEquals(Posture.FACE_DOWN, d.posture)
        return d
    }

    @Test
    fun steadyFaceDownAfterHold() {
        val d = FilteredFlipDetector()
        Samples.feed(d, Samples.steady(0f, 0f, -9.8f, 0, 700))
        assertNotEquals(Posture.FACE_DOWN, d.posture)
        Samples.feed(d, Samples.steady(0f, 0f, -9.8f, 700, 900))
        assertEquals(Posture.FACE_DOWN, d.posture)
    }

    @Test
    fun screenDownReportedBeforeHoldCompletes() {
        val d = FilteredFlipDetector()
        Samples.feed(d, Samples.steady(0f, 9.8f, 0f, 0, 1000))
        assertTrue(!d.screenDown)
        Samples.feed(d, Samples.steady(0f, 0f, -9.8f, 1000, 1200))
        assertNotEquals(Posture.FACE_DOWN, d.posture)
        assertTrue(d.screenDown)
        Samples.feed(d, Samples.steady(0f, 9.8f, 0f, 1200, 1400))
        assertTrue(!d.screenDown)
    }

    @Test
    fun jitterStaysFaceDown() {
        val d = settledFaceDown()
        val postures = Samples.feed(d, Samples.jitter(2000, 7000))
        assertEquals(0, Samples.changes(postures))
        assertEquals(Posture.FACE_DOWN, d.posture)
    }

    @Test
    fun tiltedFaceDown25DegIsFaceDown() {
        val d = FilteredFlipDetector()
        Samples.feed(d, Samples.tilted(25.0, 0, 2000))
        assertEquals(Posture.FACE_DOWN, d.posture)
    }

    @Test
    fun liftDetectedWithin600ms() {
        val d = settledFaceDown()
        val postures = Samples.feed(d, Samples.steady(0f, 9.8f, 0f, 2000, 2600))
        assertEquals(Posture.LIFTED, postures.last())
    }

    @Test
    fun briefSpike100msIgnored() {
        val d = settledFaceDown()
        Samples.feed(d, Samples.steady(0f, 9.8f, 0f, 2000, 2100))
        val after = Samples.feed(d, Samples.steady(0f, 0f, -9.8f, 2100, 3000))
        assertTrue(after.all { it == Posture.FACE_DOWN })
    }

    @Test
    fun hoverBetweenThresholdsHolds() {
        val d = settledFaceDown()
        val postures = Samples.feed(d, Samples.jitter(2000, 6000, center = -7.0f, amp = 0.5f))
        assertEquals(0, Samples.changes(postures))
        assertEquals(Posture.FACE_DOWN, d.posture)
    }

    @Test
    fun filteredVsNaiveOnSameJitter() {
        val jitter = Samples.jitter(0, 5000)
        val naive = Samples.changes(Samples.feed(NaiveFlipDetector(), jitter))
        val filtered = FilteredFlipDetector()
        Samples.feed(filtered, Samples.steady(0f, 0f, -9.8f, -2000, 0))
        val filteredChanges = Samples.changes(Samples.feed(filtered, jitter))
        println("jitter 5 s @50 Hz, z = -9.6 ± 1.2: naive posture changes = $naive, filtered posture changes = $filteredChanges")
        assertTrue(naive > 10)
        assertEquals(0, filteredChanges)
    }
}
