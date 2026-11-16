package com.example.pokatop.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PokatopDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $T_SESSIONS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "started_at INTEGER NOT NULL, " +
                "ended_at INTEGER NOT NULL, " +
                "focus_min INTEGER NOT NULL, " +
                "break_min INTEGER NOT NULL, " +
                "long_break_min INTEGER NOT NULL, " +
                "detection INTEGER NOT NULL, " +
                "focus_ms INTEGER NOT NULL, " +
                "rounds INTEGER NOT NULL, " +
                "distractions INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_SESSIONS")
        onCreate(db)
    }

    fun insertSession(r: SessionRecord): Long {
        val values = ContentValues().apply {
            put("started_at", r.startedAt)
            put("ended_at", r.endedAt)
            put("focus_min", r.focusMin)
            put("break_min", r.breakMin)
            put("long_break_min", r.longBreakMin)
            put("detection", if (r.detection) 1 else 0)
            put("focus_ms", r.focusMs)
            put("rounds", r.rounds)
            if (r.distractions == null) putNull("distractions") else put("distractions", r.distractions)
        }
        return writableDatabase.insert(T_SESSIONS, null, values)
    }

    fun getSessions(): List<SessionRecord> = query(null, null)

    fun getSessionsSince(fromMs: Long): List<SessionRecord> =
        query("started_at >= ?", arrayOf(fromMs.toString()))

    fun deleteSession(id: Long): Int =
        writableDatabase.delete(T_SESSIONS, "id = ?", arrayOf(id.toString()))

    fun deleteAllSessions(): Int = writableDatabase.delete(T_SESSIONS, "1", null)

    private fun query(selection: String?, args: Array<String>?): List<SessionRecord> {
        val result = mutableListOf<SessionRecord>()
        readableDatabase.query(T_SESSIONS, COLUMNS, selection, args, null, null, "started_at DESC, id DESC").use { c ->
            while (c.moveToNext()) result.add(c.toRecord())
        }
        return result
    }

    private fun Cursor.toRecord() = SessionRecord(
        id = getLong(0),
        startedAt = getLong(1),
        endedAt = getLong(2),
        focusMin = getInt(3),
        breakMin = getInt(4),
        longBreakMin = getInt(5),
        detection = getInt(6) == 1,
        focusMs = getLong(7),
        rounds = getInt(8),
        distractions = if (isNull(9)) null else getInt(9)
    )

    companion object {
        const val DB_NAME = "pokatop.db"
        private const val DB_VERSION = 1
        private const val T_SESSIONS = "sessions"
        private val COLUMNS = arrayOf(
            "id", "started_at", "ended_at", "focus_min", "break_min", "long_break_min",
            "detection", "focus_ms", "rounds", "distractions"
        )
    }
}
