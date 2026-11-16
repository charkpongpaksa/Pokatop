package com.example.pokatop.timer

import com.example.pokatop.data.SessionRecord
import com.example.pokatop.sensing.Posture

enum class Phase { WAITING, FOCUSING, GRACE, PAUSED, BREAK, BREAK_PAUSED }

enum class SessionAlert { FOCUS_DONE, BREAK_DONE }

data class SessionState(
    val config: SessionConfig,
    val phase: Phase = Phase.WAITING,
    val startedAtWall: Long? = null,
    val roundBankedMs: Long = 0,
    val segmentStart: Long = 0,
    val graceStart: Long = 0,
    val breakBankedMs: Long = 0,
    val isLongBreak: Boolean = false,
    val completedRounds: Int = 0,
    val completedFocusMs: Long = 0,
    val distractions: Int = 0,
    val needsFreshFlip: Boolean = false
)

class FocusSession(state: SessionState) {

    var state: SessionState = state
        private set

    val isInProgress: Boolean
        get() = state.startedAtWall != null

    private val config: SessionConfig
        get() = state.config

    private var lastPosture: Posture = Posture.UNKNOWN

    fun onPosture(p: Posture, now: Long, wallNow: Long) {
        if (!config.detection) return
        lastPosture = p
        if (p == Posture.LIFTED && state.needsFreshFlip) state = state.copy(needsFreshFlip = false)
        val s = state
        when (s.phase) {
            Phase.WAITING -> if (p == Posture.FACE_DOWN && !s.needsFreshFlip) startFocus(now, wallNow)
            Phase.FOCUSING -> if (p == Posture.LIFTED) state = s.copy(phase = Phase.GRACE, graceStart = now)
            Phase.GRACE -> if (p == Posture.FACE_DOWN) state = s.copy(phase = Phase.FOCUSING)
            Phase.PAUSED -> if (p == Posture.FACE_DOWN) state = s.copy(phase = Phase.FOCUSING, segmentStart = now)
            Phase.BREAK, Phase.BREAK_PAUSED -> Unit
        }
    }

    fun start(now: Long, wallNow: Long) {
        if (config.detection || state.phase != Phase.WAITING) return
        startFocus(now, wallNow)
    }

    fun pause(now: Long) {
        val s = state
        if (config.detection || s.phase != Phase.FOCUSING) return
        state = s.copy(phase = Phase.PAUSED, roundBankedMs = s.roundBankedMs + (now - s.segmentStart))
    }

    fun resume(now: Long) {
        if (config.detection || state.phase != Phase.PAUSED) return
        state = state.copy(phase = Phase.FOCUSING, segmentStart = now)
    }

    fun onLeave(now: Long) {
        val s = state
        val penalty = if (config.detection) 1 else 0
        state = when (s.phase) {
            Phase.FOCUSING -> s.copy(
                phase = Phase.PAUSED,
                roundBankedMs = s.roundBankedMs + (now - s.segmentStart),
                distractions = s.distractions + penalty
            )
            Phase.GRACE -> s.copy(
                phase = Phase.PAUSED,
                roundBankedMs = s.roundBankedMs + (s.graceStart - s.segmentStart),
                distractions = s.distractions + penalty
            )
            Phase.BREAK -> s.copy(
                phase = Phase.BREAK_PAUSED,
                breakBankedMs = s.breakBankedMs + (now - s.segmentStart)
            )
            else -> s
        }
    }

    fun onReturn(now: Long) {
        if (state.phase == Phase.BREAK_PAUSED) state = state.copy(phase = Phase.BREAK, segmentStart = now)
    }

    fun tick(now: Long): List<SessionAlert> {
        val s = state
        when (s.phase) {
            Phase.FOCUSING, Phase.GRACE -> {
                val completionAt = s.segmentStart + (config.focusMs - s.roundBankedMs)
                if (s.phase == Phase.GRACE) {
                    val expiresAt = s.graceStart + config.graceMs
                    if (expiresAt <= completionAt && now >= expiresAt) {
                        state = s.copy(
                            phase = Phase.PAUSED,
                            roundBankedMs = s.roundBankedMs + (s.graceStart - s.segmentStart),
                            distractions = s.distractions + 1
                        )
                        return emptyList()
                    }
                }
                if (now >= completionAt) {
                    completeRound(completionAt)
                    return listOf(SessionAlert.FOCUS_DONE)
                }
            }
            Phase.BREAK -> {
                val endAt = s.segmentStart + (breakLengthMs() - s.breakBankedMs)
                if (now >= endAt) {
                    state = s.copy(
                        phase = Phase.WAITING,
                        roundBankedMs = 0,
                        breakBankedMs = 0,
                        needsFreshFlip = config.detection && lastPosture == Posture.FACE_DOWN
                    )
                    return listOf(SessionAlert.BREAK_DONE)
                }
            }
            else -> Unit
        }
        return emptyList()
    }

    fun roundElapsedMs(now: Long): Long {
        val s = state
        return when (s.phase) {
            Phase.FOCUSING, Phase.GRACE -> s.roundBankedMs + (now - s.segmentStart)
            Phase.PAUSED -> s.roundBankedMs
            else -> 0
        }
    }

    fun breakRemainingMs(now: Long): Long {
        val s = state
        val left = when (s.phase) {
            Phase.BREAK -> breakLengthMs() - s.breakBankedMs - (now - s.segmentStart)
            Phase.BREAK_PAUSED -> breakLengthMs() - s.breakBankedMs
            else -> 0
        }
        return left.coerceAtLeast(0)
    }

    fun graceRemainingMs(now: Long): Long =
        if (state.phase == Phase.GRACE) (config.graceMs - (now - state.graceStart)).coerceAtLeast(0) else 0

    fun roundsUntilLongBreak(): Int = config.longEvery - state.completedRounds % config.longEvery

    fun focusSoFarMs(now: Long): Long {
        val s = state
        val partial = when (s.phase) {
            Phase.FOCUSING -> s.roundBankedMs + (now - s.segmentStart)
            Phase.GRACE -> s.roundBankedMs + (s.graceStart - s.segmentStart)
            Phase.PAUSED -> s.roundBankedMs
            else -> 0
        }
        return s.completedFocusMs + partial
    }

    fun toRecord(now: Long, wallNow: Long): SessionRecord? {
        val started = state.startedAtWall ?: return null
        val total = focusSoFarMs(now)
        if (total < MIN_RECORD_MS) return null
        return SessionRecord(
            startedAt = started,
            endedAt = wallNow,
            focusMin = config.focusMin,
            breakMin = config.shortBreakMin,
            longBreakMin = config.longBreakMin,
            detection = config.detection,
            focusMs = total,
            rounds = state.completedRounds,
            distractions = if (config.detection) state.distractions else null
        )
    }

    private fun startFocus(now: Long, wallNow: Long) {
        state = state.copy(
            phase = Phase.FOCUSING,
            segmentStart = now,
            startedAtWall = state.startedAtWall ?: wallNow
        )
    }

    private fun completeRound(at: Long) {
        val s = state
        val rounds = s.completedRounds + 1
        state = s.copy(
            phase = Phase.BREAK,
            completedRounds = rounds,
            completedFocusMs = s.completedFocusMs + config.focusMs,
            roundBankedMs = 0,
            segmentStart = at,
            breakBankedMs = 0,
            isLongBreak = rounds % config.longEvery == 0
        )
    }

    private fun breakLengthMs(): Long = if (state.isLongBreak) config.longBreakMs else config.shortBreakMs

    companion object {
        const val MIN_RECORD_MS = 60_000L
    }
}
