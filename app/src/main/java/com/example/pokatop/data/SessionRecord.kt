package com.example.pokatop.data

data class SessionRecord(
    val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val focusMin: Int,
    val breakMin: Int,
    val longBreakMin: Int,
    val detection: Boolean,
    val focusMs: Long,
    val rounds: Int,
    val distractions: Int?
)
