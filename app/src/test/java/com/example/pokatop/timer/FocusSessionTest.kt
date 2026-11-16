package com.example.pokatop.timer

import com.example.pokatop.sensing.Posture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionTest {

    private val detect = SessionConfig("classic", 25, 5, 15, 4, true)
    private val plain = detect.copy(detection = false)
    private val w = 1_000_000L
    private val round = 1_500_000L
    private val shortBreak = 300_000L

    private fun session(c: SessionConfig = detect) = FocusSession(SessionState(c))
    private fun FocusSession.down(t: Long) = onPosture(Posture.FACE_DOWN, t, w + t)
    private fun FocusSession.up(t: Long) = onPosture(Posture.LIFTED, t, w + t)

    @Test
    fun flipStartsFocusing() {
        val s = session()
        s.down(0)
        assertEquals(Phase.FOCUSING, s.state.phase)
        assertEquals(w, s.state.startedAtWall)
        assertTrue(s.isInProgress)
    }

    @Test
    fun liftWithinGraceKeepsCounting() {
        val s = session()
        s.down(0)
        s.up(60_000)
        s.down(65_000)
        s.tick(120_000)
        assertEquals(Phase.FOCUSING, s.state.phase)
        assertEquals(0, s.state.distractions)
        assertEquals(120_000L, s.roundElapsedMs(120_000))
    }

    @Test
    fun graceExpiryPausesTrimsAndCounts() {
        val s = session()
        s.down(0)
        s.up(60_000)
        s.tick(69_999)
        assertEquals(Phase.GRACE, s.state.phase)
        assertEquals(1L, s.graceRemainingMs(69_999))
        s.tick(70_000)
        assertEquals(Phase.PAUSED, s.state.phase)
        assertEquals(1, s.state.distractions)
        assertEquals(60_000L, s.roundElapsedMs(200_000))
    }

    @Test
    fun flipAfterPauseResumes() {
        val s = session()
        s.down(0)
        s.up(60_000)
        s.tick(70_000)
        s.down(100_000)
        s.tick(160_000)
        assertEquals(Phase.FOCUSING, s.state.phase)
        assertEquals(120_000L, s.roundElapsedMs(160_000))
    }

    @Test
    fun leaveDuringGraceCountsOnceAndTrims() {
        val s = session()
        s.down(0)
        s.up(60_000)
        s.onLeave(63_000)
        assertEquals(Phase.PAUSED, s.state.phase)
        assertEquals(1, s.state.distractions)
        assertEquals(60_000L, s.roundElapsedMs(63_000))
        s.tick(80_000)
        assertEquals(1, s.state.distractions)
    }

    @Test
    fun returnDoesNotResumeFocus() {
        val s = session()
        s.down(0)
        s.onLeave(30_000)
        s.onReturn(90_000)
        assertEquals(Phase.PAUSED, s.state.phase)
        assertEquals(1, s.state.distractions)
        assertEquals(30_000L, s.roundElapsedMs(90_000))
    }

    @Test
    fun roundCompletesIntoShortBreak() {
        val s = session()
        s.down(0)
        assertEquals(listOf(SessionAlert.FOCUS_DONE), s.tick(round))
        assertEquals(Phase.BREAK, s.state.phase)
        assertFalse(s.state.isLongBreak)
        assertEquals(1, s.state.completedRounds)
        assertEquals(shortBreak, s.breakRemainingMs(round))
        assertEquals(3, s.roundsUntilLongBreak())
    }

    @Test
    fun fourthRoundGivesLongBreak() {
        val s = session()
        var t = 0L
        repeat(4) { i ->
            s.up(t)
            s.down(t + 1)
            val done = t + 1 + round
            assertEquals(listOf(SessionAlert.FOCUS_DONE), s.tick(done))
            if (i < 3) {
                assertFalse(s.state.isLongBreak)
                assertEquals(listOf(SessionAlert.BREAK_DONE), s.tick(done + shortBreak))
                t = done + shortBreak + 1
            } else {
                assertTrue(s.state.isLongBreak)
                assertEquals(900_000L, s.breakRemainingMs(done))
            }
        }
        assertEquals(4, s.state.completedRounds)
    }

    @Test
    fun breakEndsIntoWaiting() {
        val s = session()
        s.down(0)
        s.tick(round)
        assertEquals(listOf(SessionAlert.BREAK_DONE), s.tick(round + shortBreak))
        assertEquals(Phase.WAITING, s.state.phase)
    }

    @Test
    fun nextRoundNeedsFreshFlipAfterBreak() {
        val s = session()
        s.down(0)
        s.tick(round)
        s.tick(round + shortBreak)
        val t = round + shortBreak
        s.down(t + 100)
        assertEquals(Phase.WAITING, s.state.phase)
        s.up(t + 200)
        s.down(t + 300)
        assertEquals(Phase.FOCUSING, s.state.phase)
    }

    @Test
    fun liftedDuringBreakLetsNextFlipStartRound() {
        val s = session()
        s.down(0)
        s.tick(round)
        s.up(round + 30_000)
        s.tick(round + shortBreak)
        assertEquals(Phase.WAITING, s.state.phase)
        s.down(round + shortBreak + 5_000)
        assertEquals(Phase.FOCUSING, s.state.phase)
    }

    @Test
    fun breakIgnoresPostureAndPausesOnLeave() {
        val s = session()
        s.down(0)
        s.tick(round)
        s.up(round + 10_000)
        assertEquals(Phase.BREAK, s.state.phase)
        s.onLeave(round + 60_000)
        assertEquals(Phase.BREAK_PAUSED, s.state.phase)
        assertEquals(0, s.state.distractions)
        s.onReturn(round + 660_000)
        assertEquals(Phase.BREAK, s.state.phase)
        assertEquals(240_000L, s.breakRemainingMs(round + 660_000))
    }

    @Test
    fun roundCompletingDuringGraceCounts() {
        val s = session()
        s.down(0)
        s.up(1_495_000)
        assertEquals(listOf(SessionAlert.FOCUS_DONE), s.tick(round))
        assertEquals(0, s.state.distractions)
    }

    @Test
    fun graceThatExpiredBeforeRoundEndPausesEvenIfTickIsLate() {
        val s = session()
        s.down(0)
        s.up(1_480_000)
        assertEquals(emptyList<SessionAlert>(), s.tick(1_500_000))
        assertEquals(Phase.PAUSED, s.state.phase)
        assertEquals(1_480_000L, s.roundElapsedMs(1_500_000))
    }

    @Test
    fun plainModeStartPauseResume() {
        val s = session(plain)
        s.start(0, w)
        s.pause(60_000)
        s.resume(100_000)
        s.tick(160_000)
        assertEquals(120_000L, s.roundElapsedMs(160_000))
        s.onLeave(160_000)
        assertEquals(Phase.PAUSED, s.state.phase)
        assertEquals(0, s.state.distractions)
        s.onPosture(Posture.FACE_DOWN, 170_000, w)
        assertEquals(Phase.PAUSED, s.state.phase)
    }

    @Test
    fun plainModeIgnoresPostureAndDetectionModeIgnoresButtons() {
        val p = session(plain)
        p.onPosture(Posture.FACE_DOWN, 0, w)
        assertEquals(Phase.WAITING, p.state.phase)
        val d = session()
        d.start(0, w)
        assertEquals(Phase.WAITING, d.state.phase)
    }

    @Test
    fun shortSessionYieldsNoRecord() {
        assertNull(session().toRecord(100_000, w))
        val s = session()
        s.down(0)
        assertNull(s.toRecord(59_999, w + 59_999))
        assertNotNull(s.toRecord(60_000, w + 60_000))
    }

    @Test
    fun recordDuringGraceCountsUpToLift() {
        val s = session()
        s.down(0)
        s.up(600_000)
        assertEquals(600_000L, s.toRecord(605_000, w + 605_000)!!.focusMs)
    }

    @Test
    fun recordSumsCompletedAndPartial() {
        val s = session()
        s.down(0)
        s.tick(round)
        s.tick(round + shortBreak)
        val t = round + shortBreak
        s.up(t + 1)
        s.down(t + 2)
        s.up(t + 2 + 300_000)
        s.tick(t + 2 + 310_000)
        val r = s.toRecord(t + 2 + 400_000, 9_000_000)!!
        assertEquals(1_800_000L, r.focusMs)
        assertEquals(1, r.rounds)
        assertEquals(1, r.distractions)
        assertEquals(25, r.focusMin)
        assertEquals(5, r.breakMin)
        assertEquals(15, r.longBreakMin)
        assertTrue(r.detection)
        assertEquals(w, r.startedAt)
        assertEquals(9_000_000L, r.endedAt)
    }

    @Test
    fun plainRecordHasNullDistractions() {
        val s = session(plain)
        s.start(0, w)
        val r = s.toRecord(120_000, w + 120_000)!!
        assertNull(r.distractions)
        assertFalse(r.detection)
    }

    @Test
    fun focusSoFarMatchesRecord() {
        val s = session()
        s.down(0)
        assertEquals(s.toRecord(300_000, w)!!.focusMs, s.focusSoFarMs(300_000))
        s.up(600_000)
        assertEquals(s.toRecord(605_000, w)!!.focusMs, s.focusSoFarMs(605_000))
    }
}
