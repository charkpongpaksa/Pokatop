package com.example.pokatop.timer

import com.example.pokatop.data.SessionRecord
import java.util.Calendar
import java.util.TimeZone

object DailyStats {
    const val DAY_MS = 86_400_000L

    data class Summary(val focusMs: Long, val rounds: Int, val distractions: Int)

    fun startOfDay(wallMs: Long, tz: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(tz).apply {
            timeInMillis = wallMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun dayStarts(todayStart: Long, days: Int): List<Long> =
        (days - 1 downTo 0).map { todayStart - it * DAY_MS }

    fun focusPerDay(records: List<SessionRecord>, dayStarts: List<Long>): List<Long> {
        val buckets = LongArray(dayStarts.size)
        if (dayStarts.isEmpty()) return buckets.toList()
        val end = dayStarts.last() + DAY_MS
        records.forEach { r ->
            if (r.startedAt < dayStarts.first() || r.startedAt >= end) return@forEach
            val index = dayStarts.indexOfLast { it <= r.startedAt }
            if (index >= 0) buckets[index] += r.focusMs
        }
        return buckets.toList()
    }

    fun summarize(records: List<SessionRecord>, fromMs: Long): Summary {
        val inRange = records.filter { it.startedAt >= fromMs }
        return Summary(
            focusMs = inRange.sumOf { it.focusMs },
            rounds = inRange.sumOf { it.rounds },
            distractions = inRange.sumOf { it.distractions ?: 0 }
        )
    }
}
