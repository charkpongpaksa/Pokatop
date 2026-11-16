package com.example.pokatop.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PokatopDbHelperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: PokatopDbHelper

    private fun record(startedAt: Long, focusMs: Long = 1_500_000, distractions: Int? = 1) = SessionRecord(
        startedAt = startedAt,
        endedAt = startedAt + focusMs,
        focusMin = 25,
        breakMin = 5,
        longBreakMin = 15,
        detection = distractions != null,
        focusMs = focusMs,
        rounds = 1,
        distractions = distractions
    )

    @Before
    fun open() {
        context.deleteDatabase(PokatopDbHelper.DB_NAME)
        db = PokatopDbHelper(context)
    }

    @After
    fun close() {
        db.close()
        context.deleteDatabase(PokatopDbHelper.DB_NAME)
    }

    @Test
    fun insertAndReadNewestFirst() {
        val a = db.insertSession(record(1_000))
        val b = db.insertSession(record(2_000))
        val rows = db.getSessions()
        assertEquals(listOf(b, a), rows.map { it.id })
        assertEquals(record(2_000).copy(id = b), rows.first())
    }

    @Test
    fun nullDistractionsRoundTrip() {
        val id = db.insertSession(record(1_000, distractions = null))
        val row = db.getSessions().single()
        assertEquals(id, row.id)
        assertNull(row.distractions)
        assertEquals(false, row.detection)
    }

    @Test
    fun deleteOne() {
        val a = db.insertSession(record(1_000))
        val b = db.insertSession(record(2_000))
        assertEquals(1, db.deleteSession(a))
        assertEquals(listOf(b), db.getSessions().map { it.id })
    }

    @Test
    fun deleteAll() {
        db.insertSession(record(1_000))
        db.insertSession(record(2_000))
        assertEquals(2, db.deleteAllSessions())
        assertEquals(0, db.getSessions().size)
    }

    @Test
    fun sinceFilters() {
        db.insertSession(record(1_000))
        val b = db.insertSession(record(5_000))
        assertEquals(listOf(b), db.getSessionsSince(5_000).map { it.id })
    }
}
