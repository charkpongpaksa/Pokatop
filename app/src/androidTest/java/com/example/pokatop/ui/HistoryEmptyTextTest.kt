package com.example.pokatop.ui

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokatop.R
import com.example.pokatop.data.PokatopDbHelper
import com.example.pokatop.data.Prefs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryEmptyTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun emptyHistory() {
        context.deleteDatabase(PokatopDbHelper.DB_NAME)
    }

    @After
    fun restore() {
        Prefs(context).detection = true
    }

    @Test
    fun detectionModeTellsUserToFlip() {
        Prefs(context).detection = true
        ActivityScenario.launch(HistoryActivity::class.java).use {
            onView(withText(R.string.history_empty_body)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun plainModeTellsUserToTapStart() {
        Prefs(context).detection = false
        ActivityScenario.launch(HistoryActivity::class.java).use {
            onView(withText(R.string.history_empty_plain_body)).check(matches(isDisplayed()))
        }
    }
}
