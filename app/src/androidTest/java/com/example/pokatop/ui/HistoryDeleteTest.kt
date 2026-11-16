package com.example.pokatop.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pokatop.R
import com.example.pokatop.data.PokatopDbHelper
import com.example.pokatop.data.SessionRecord
import org.hamcrest.Matcher
import org.hamcrest.Matchers.any
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDeleteTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun record(startedAt: Long) = SessionRecord(
        startedAt = startedAt,
        endedAt = startedAt + 1_500_000,
        focusMin = 25,
        breakMin = 5,
        longBreakMin = 15,
        detection = true,
        focusMs = 1_500_000,
        rounds = 1,
        distractions = 0
    )

    @Before
    fun seed() {
        context.deleteDatabase(PokatopDbHelper.DB_NAME)
        PokatopDbHelper(context).use {
            val now = System.currentTimeMillis()
            it.insertSession(record(now - 7_200_000))
            it.insertSession(record(now - 3_600_000))
        }
    }

    @After
    fun clear() {
        context.deleteDatabase(PokatopDbHelper.DB_NAME)
    }

    private fun count(): Int = PokatopDbHelper(context).use { it.getSessions().size }

    private fun clickDeleteOnFirstRow() {
        onView(withId(R.id.sessions)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChild(R.id.delete))
        )
    }

    @Test
    fun cancelKeepsRow() {
        ActivityScenario.launch(HistoryActivity::class.java).use {
            clickDeleteOnFirstRow()
            onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click())
            assertEquals(2, count())
        }
    }

    @Test
    fun confirmDeletesRow() {
        ActivityScenario.launch(HistoryActivity::class.java).use {
            clickDeleteOnFirstRow()
            onView(withText(R.string.delete)).inRoot(isDialog()).perform(click())
            assertEquals(1, count())
        }
    }

    @Test
    fun deleteAllShowsEmpty() {
        ActivityScenario.launch(HistoryActivity::class.java).use {
            onView(withId(R.id.action_delete_all)).perform(click())
            onView(withText(R.string.delete)).inRoot(isDialog()).perform(click())
            assertEquals(0, count())
            onView(withId(R.id.empty)).check(matches(isDisplayed()))
        }
    }

    private fun clickChild(id: Int): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = any(View::class.java)
        override fun getDescription(): String = "click child $id"
        override fun perform(uiController: UiController, view: View) {
            view.findViewById<View>(id).performClick()
            uiController.loopMainThreadUntilIdle()
        }
    }
}
