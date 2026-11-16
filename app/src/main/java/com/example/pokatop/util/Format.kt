package com.example.pokatop.util

import android.content.Context
import com.example.pokatop.R

object Format {

    fun clock(ms: Long): String {
        val total = (ms.coerceAtLeast(0) + 999) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun hoursMinutes(ms: Long): Pair<Int, Int> {
        val minutes = (ms.coerceAtLeast(0) / 60_000).toInt()
        return minutes / 60 to minutes % 60
    }

    fun duration(context: Context, ms: Long): String {
        val (h, m) = hoursMinutes(ms)
        return if (h > 0) context.getString(R.string.duration_h_min, h, m) else context.getString(R.string.duration_min, m)
    }
}
