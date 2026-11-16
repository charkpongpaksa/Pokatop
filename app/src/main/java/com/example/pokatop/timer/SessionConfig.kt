package com.example.pokatop.timer

data class SessionConfig(
    val presetKey: String,
    val focusMin: Int,
    val shortBreakMin: Int,
    val longBreakMin: Int,
    val longEvery: Int,
    val detection: Boolean,
    val graceMs: Long = 10_000L
) {
    val focusMs: Long get() = focusMin * MINUTE_MS
    val shortBreakMs: Long get() = shortBreakMin * MINUTE_MS
    val longBreakMs: Long get() = longBreakMin * MINUTE_MS

    companion object {
        const val MINUTE_MS = 60_000L
    }
}

fun sessionConfigOf(preset: Preset, longEvery: Int, detection: Boolean): SessionConfig =
    SessionConfig(
        presetKey = preset.key,
        focusMin = preset.focusMin,
        shortBreakMin = preset.shortBreakMin,
        longBreakMin = preset.longBreakMin,
        longEvery = longEvery.coerceIn(Presets.EVERY_RANGE),
        detection = detection
    )
