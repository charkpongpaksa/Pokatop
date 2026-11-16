package com.example.pokatop.sensing

class NaiveFlipDetector(private val threshold: Float = -9.0f) : PostureDetector {

    override var posture: Posture = Posture.UNKNOWN
        private set

    override val screenDown: Boolean
        get() = posture == Posture.FACE_DOWN

    override fun onSample(x: Float, y: Float, z: Float, timeMs: Long): Posture {
        posture = if (z <= threshold) Posture.FACE_DOWN else Posture.LIFTED
        return posture
    }

    override fun reset() {
        posture = Posture.UNKNOWN
    }
}
