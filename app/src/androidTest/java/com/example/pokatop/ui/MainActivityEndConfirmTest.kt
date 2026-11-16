package com.example.pokatop.ui

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokatop.R
import com.example.pokatop.data.Prefs
import com.example.pokatop.timer.Phase
import com.example.pokatop.timer.SessionCodec
import com.example.pokatop.timer.SessionConfig
import com.example.pokatop.timer.SessionState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityEndConfirmTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun seedPausedSession() {
        val state = SessionState(
            config = SessionConfig("classic", 25, 5, 15, 4, true),
            phase = Phase.PAUSED,
            startedAtWall = System.currentTimeMillis(),
            roundBankedMs = 120_000,
            segmentStart = SystemClock.elapsedRealtime()
        )
        Prefs(context).pendingSession = SessionCodec.encode(state)
    }

    @After
    fun clear() {
        Prefs(context).pendingSession = null
    }

    @Test
    fun endAsksFirstAndKeepGoingKeepsSession() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btn_end)).perform(click())
            onView(withText(R.string.end_keep_going)).inRoot(isDialog()).perform(click())
            onView(withId(R.id.btn_end)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        }
    }

    @Test
    fun confirmingEndsSession() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btn_end)).perform(click())
            onView(withText(R.string.end_confirm)).inRoot(isDialog()).perform(click())
            onView(withId(R.id.btn_end)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }
}
