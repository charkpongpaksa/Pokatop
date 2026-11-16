package com.example.pokatop.timer

import com.example.pokatop.data.SessionRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DailyStatsTest {

    private val bangkok = TimeZone.getTimeZone("Asia/Bangkok")
    private val hour = 3_600_000L
    private val minute = 60_000L

    private fun wall(year: Int, month: Int, day: Int, h: Int, m: Int): Long =
        Calendar.getInstance(bangkok).apply {
            clear()
            set(year, month - 1, day, h, m)
        }.timeInMillis

    private fun record(startedAt: Long, focusMs: Long, rounds: Int = 1, distractions: Int? = 0) =
        SessionRecord(
            startedAt = startedAt,
            endedAt = startedAt + focusMs,
            focusMin = 25,
            breakMin = 5,
            longBreakMin = 15,
            detection = distractions != null,
            focusMs = focusMs,
            rounds = rounds,
            distractions = distractions
        )

    private val today = wall(2026, 11, 17, 0, 0)

    @Test
    fun startOfDayIsLocalMidnight() {
        assertEquals(today, DailyStats.startOfDay(wall(2026, 11, 17, 15, 42), bangkok))
    }

    @Test
    fun dayStartsAreSevenAscending() {
        val starts = DailyStats.dayStarts(today, 7)
        assertEquals(7, starts.size)
        assertEquals(today, starts.last())
        assertEquals(today - 6 * DailyStats.DAY_MS, starts.first())
    }

    @Test
    fun recordCountsOnItsStartDay() {
        val buckets = DailyStats.focusPerDay(listOf(record(today + hour, 30 * minute)), DailyStats.dayStarts(today, 7))
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 0L, 30 * minute), buckets)
    }

    @Test
    fun crossMidnightCountsOnStartDay() {
        val start = wall(2026, 11, 16, 23, 50)
        val buckets = DailyStats.focusPerDay(listOf(record(start, 40 * minute)), DailyStats.dayStarts(today, 7))
        assertEquals(40 * minute, buckets[5])
        assertEquals(0L, buckets[6])
    }

    @Test
    fun oldRecordsIgnored() {
        val old = today - 10 * DailyStats.DAY_MS
        val buckets = DailyStats.focusPerDay(listOf(record(old, 30 * minute)), DailyStats.dayStarts(today, 7))
        assertEquals(0L, buckets.sum())
    }

    @Test
    fun summarizeSkipsNullDistractions() {
        val records = listOf(
            record(today + hour, 50 * minute, rounds = 2, distractions = 3),
            record(today + 2 * hour, 25 * minute, rounds = 1, distractions = null),
            record(today - DailyStats.DAY_MS, 25 * minute, rounds = 1, distractions = 5)
        )
        assertEquals(DailyStats.Summary(75 * minute, 3, 3), DailyStats.summarize(records, today))
    }
}
