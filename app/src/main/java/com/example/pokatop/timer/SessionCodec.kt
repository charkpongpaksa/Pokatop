package com.example.pokatop.timer

object SessionCodec {
    private const val VERSION = "PK1"
    private const val SEP = ";"
    private const val NULL = "-"
    private const val FIELDS = 19

    fun encode(s: SessionState): String = listOf(
        VERSION,
        s.config.presetKey,
        s.config.focusMin,
        s.config.shortBreakMin,
        s.config.longBreakMin,
        s.config.longEvery,
        s.config.detection,
        s.config.graceMs,
        s.phase.name,
        s.startedAtWall ?: NULL,
        s.roundBankedMs,
        s.segmentStart,
        s.graceStart,
        s.breakBankedMs,
        s.isLongBreak,
        s.completedRounds,
        s.completedFocusMs,
        s.distractions,
        s.needsFreshFlip
    ).joinToString(SEP)

    fun decode(text: String?): SessionState? {
        if (text.isNullOrEmpty()) return null
        val f = text.split(SEP)
        if (f.size != FIELDS || f[0] != VERSION) return null
        val state = try {
            SessionState(
                config = SessionConfig(
                    presetKey = f[1],
                    focusMin = f[2].toInt(),
                    shortBreakMin = f[3].toInt(),
                    longBreakMin = f[4].toInt(),
                    longEvery = f[5].toInt(),
                    detection = f[6].toBooleanStrict(),
                    graceMs = f[7].toLong()
                ),
                phase = Phase.valueOf(f[8]),
                startedAtWall = if (f[9] == NULL) null else f[9].toLong(),
                roundBankedMs = f[10].toLong(),
                segmentStart = f[11].toLong(),
                graceStart = f[12].toLong(),
                breakBankedMs = f[13].toLong(),
                isLongBreak = f[14].toBooleanStrict(),
                completedRounds = f[15].toInt(),
                completedFocusMs = f[16].toLong(),
                distractions = f[17].toInt(),
                needsFreshFlip = f[18].toBooleanStrict()
            )
        } catch (e: IllegalArgumentException) {
            return null
        }
        return state.takeIf(::isValid)
    }

    private fun isValid(s: SessionState): Boolean {
        val c = s.config
        return c.focusMin in Presets.FOCUS_RANGE &&
            c.shortBreakMin in Presets.SHORT_RANGE &&
            c.longBreakMin in Presets.LONG_RANGE &&
            c.longEvery in Presets.EVERY_RANGE &&
            c.graceMs > 0 &&
            s.roundBankedMs >= 0 &&
            s.breakBankedMs >= 0 &&
            s.completedRounds >= 0 &&
            s.completedFocusMs >= 0 &&
            s.distractions >= 0 &&
            (s.phase == Phase.WAITING || s.startedAtWall != null)
    }
}
