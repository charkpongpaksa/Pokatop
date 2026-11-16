package com.example.pokatop.ui

import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pokatop.data.Prefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityScreenOnTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun noPendingSession() {
        Prefs(context).pendingSession = null
    }

    @Test
    fun detectionModeKeepsScreenOnWhileWaitingForFlip() {
        Prefs(context).detection = true
        assertTrue(keepScreenOnWhenIdle())
    }

    @Test
    fun plainModeLetsScreenSleepWhileIdle() {
        Prefs(context).detection = false
        try {
            assertFalse(keepScreenOnWhenIdle())
        } finally {
            Prefs(context).detection = true
        }
    }

    private fun keepScreenOnWhenIdle(): Boolean {
        var on = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(800)
            scenario.onActivity {
                on = it.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
            }
        }
        return on
    }
}
