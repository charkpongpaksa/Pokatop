package com.example.pokatop.sensing

class FilteredFlipDetector(
    private val alpha: Float = 0.8f,
    private val downZ: Float = -8.0f,
    private val upZ: Float = -6.0f,
    private val downHoldMs: Long = 800,
    private val upHoldMs: Long = 300
) : PostureDetector {

    override var posture: Posture = Posture.UNKNOWN
        private set

    private var gravityZ: Float? = null

    override val screenDown: Boolean
        get() = gravityZ?.let { it <= downZ } ?: false
    private var downSince: Long? = null
    private var upSince: Long? = null

    override fun onSample(x: Float, y: Float, z: Float, timeMs: Long): Posture {
        val g = gravityZ?.let { alpha * it + (1 - alpha) * z } ?: z
        gravityZ = g
        when {
            g <= downZ -> {
                upSince = null
                val since = downSince ?: timeMs.also { downSince = it }
                if (timeMs - since >= downHoldMs) posture = Posture.FACE_DOWN
            }
            g >= upZ -> {
                downSince = null
                val since = upSince ?: timeMs.also { upSince = it }
                if (timeMs - since >= upHoldMs) posture = Posture.LIFTED
            }
            else -> {
                downSince = null
                upSince = null
            }
        }
        return posture
    }

    override fun reset() {
        posture = Posture.UNKNOWN
        gravityZ = null
        downSince = null
        upSince = null
    }
}
