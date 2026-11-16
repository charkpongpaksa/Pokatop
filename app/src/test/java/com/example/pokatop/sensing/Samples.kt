package com.example.pokatop.sensing

import kotlin.math.cos
import kotlin.math.sin

data class Sample(val x: Float, val y: Float, val z: Float, val t: Long)

object Samples {
    fun steady(x: Float, y: Float, z: Float, fromMs: Long, toMs: Long, stepMs: Long = 20): List<Sample> =
        (fromMs until toMs step stepMs).map { Sample(x, y, z, it) }

    fun jitter(fromMs: Long, toMs: Long, center: Float = -9.6f, amp: Float = 1.2f, stepMs: Long = 20): List<Sample> =
        (fromMs until toMs step stepMs).mapIndexed { i, t ->
            Sample(0f, 0f, center + amp * sin(i * 1.3).toFloat(), t)
        }

    fun tilted(deg: Double, fromMs: Long, toMs: Long, stepMs: Long = 20): List<Sample> {
        val rad = Math.toRadians(deg)
        return steady(0f, (9.8 * sin(rad)).toFloat(), (-9.8 * cos(rad)).toFloat(), fromMs, toMs, stepMs)
    }

    fun feed(detector: PostureDetector, samples: List<Sample>): List<Posture> =
        samples.map { detector.onSample(it.x, it.y, it.z, it.t) }

    fun changes(postures: List<Posture>): Int =
        postures.zipWithNext().count { (a, b) -> a != b }
}
