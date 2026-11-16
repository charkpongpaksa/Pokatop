package com.example.pokatop.sensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaiveFlipDetectorTest {

    @Test
    fun faceDownBelowThreshold() {
        val d = NaiveFlipDetector()
        Samples.feed(d, Samples.steady(0f, 0f, -9.8f, 0, 1000))
        assertEquals(Posture.FACE_DOWN, d.posture)
    }

    @Test
    fun liftedWhenUpright() {
        val d = NaiveFlipDetector()
        Samples.feed(d, Samples.steady(0f, 9.8f, 0f, 0, 1000))
        assertEquals(Posture.LIFTED, d.posture)
    }

    @Test
    fun flickersOnJitter() {
        val d = NaiveFlipDetector()
        val postures = Samples.feed(d, Samples.jitter(0, 5000))
        assertTrue(Samples.changes(postures) > 10)
    }

    @Test
    fun naiveCallsTiltedPhoneLifted() {
        val d = NaiveFlipDetector()
        Samples.feed(d, Samples.tilted(25.0, 0, 1000))
        assertEquals(Posture.LIFTED, d.posture)
    }
}
