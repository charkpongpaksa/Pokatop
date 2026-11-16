package com.example.pokatop.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCodecTest {

    private val full = SessionState(
        config = SessionConfig("custom", 30, 4, 20, 3, true, 10_000L),
        phase = Phase.GRACE,
        startedAtWall = 123L,
        roundBankedMs = 456L,
        segmentStart = 789L,
        graceStart = 1011L,
        breakBankedMs = 1213L,
        isLongBreak = true,
        completedRounds = 2,
        completedFocusMs = 3_600_000L,
        distractions = 5,
        needsFreshFlip = true
    )

    @Test
    fun roundTripPreservesState() {
        assertEquals(full, SessionCodec.decode(SessionCodec.encode(full)))
        val fresh = SessionState(SessionConfig("classic", 25, 5, 15, 4, false))
        assertEquals(fresh, SessionCodec.decode(SessionCodec.encode(fresh)))
    }

    @Test
    fun decodeGarbageReturnsNull() {
        val valid = SessionCodec.encode(full)
        listOf("x", "", "9;1;2", valid.dropLast(5), valid.replace("GRACE", "NOPE")).forEach {
            assertNull(it, SessionCodec.decode(it))
        }
    }

    @Test
    fun decodeRejectsParseableButInvalidValues() {
        val bad = listOf(
            full.copy(config = full.config.copy(longEvery = 0)),
            full.copy(config = full.config.copy(focusMin = 0)),
            full.copy(config = full.config.copy(shortBreakMin = 0)),
            full.copy(config = full.config.copy(longBreakMin = 999)),
            full.copy(distractions = -1),
            full.copy(roundBankedMs = -5),
            full.copy(startedAtWall = null)
        )
        bad.forEach { assertNull(it.toString(), SessionCodec.decode(SessionCodec.encode(it))) }
    }

    @Test
    fun decodeNullReturnsNull() {
        assertNull(SessionCodec.decode(null))
    }
}
