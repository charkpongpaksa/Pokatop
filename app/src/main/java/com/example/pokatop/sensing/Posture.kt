package com.example.pokatop.sensing

enum class Posture { UNKNOWN, FACE_DOWN, LIFTED }

interface PostureDetector {
    val posture: Posture
    val screenDown: Boolean
    fun onSample(x: Float, y: Float, z: Float, timeMs: Long): Posture
    fun reset()
}
