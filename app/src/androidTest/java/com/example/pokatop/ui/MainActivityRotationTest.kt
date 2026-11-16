package com.example.pokatop.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.SystemClock
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pokatop.R
import com.example.pokatop.data.Prefs
import com.example.pokatop.timer.Phase
import com.example.pokatop.timer.SessionCodec
import com.example.pokatop.timer.SessionConfig
import com.example.pokatop.timer.SessionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityRotationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun seedFocusingSession() {
        val now = SystemClock.elapsedRealtime()
        val state = SessionState(
            config = SessionConfig("classic", 25, 5, 15, 4, true),
            phase = Phase.GRACE,
            startedAtWall = System.currentTimeMillis(),
            segmentStart = now,
            graceStart = now + GRACE_NEVER_EXPIRES_DURING_TEST_MS
        )
        Prefs(context).pendingSession = SessionCodec.encode(state)
    }

    @After
    fun clear() {
        Prefs(context).pendingSession = null
    }

    @Test
    fun rotatingKeepsSessionWithoutDistraction() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            rotate(scenario, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            rotate(scenario, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            scenario.onActivity { activity ->
                val shown = activity.findViewById<TextView>(R.id.distractions_value).text.toString()
                assertEquals(activity.resources.getQuantityString(R.plurals.times_count, 0, 0), shown)
            }
        }
    }

    companion object {
        private const val GRACE_NEVER_EXPIRES_DURING_TEST_MS = 600_000L
    }

    private fun rotate(scenario: ActivityScenario<MainActivity>, orientation: Int) {
        scenario.onActivity { it.requestedOrientation = orientation }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1500)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
