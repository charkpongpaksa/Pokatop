package com.example.pokatop.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.pokatop.R
import com.example.pokatop.data.PokatopDbHelper
import com.example.pokatop.timer.DailyStats
import com.example.pokatop.util.Format
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date

class StatsActivity : AppCompatActivity() {

    private lateinit var db: PokatopDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        findViewById<View>(R.id.main).applySystemBarPadding()
        db = PokatopDbHelper(this)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<View>(R.id.btn_all_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun render() {
        val todayStart = DailyStats.startOfDay(System.currentTimeMillis())
        val starts = DailyStats.dayStarts(todayStart, DAYS)
        val records = db.getSessionsSince(starts.first())
        val today = DailyStats.summarize(records, todayStart)
        val week = DailyStats.summarize(records, starts.first())

        findViewById<TextView>(R.id.stat_today).text = Format.duration(this, today.focusMs)
        findViewById<TextView>(R.id.stat_week).text = Format.duration(this, week.focusMs)
        findViewById<TextView>(R.id.stat_rounds).text = resources.getQuantityString(R.plurals.rounds_count, week.rounds, week.rounds)
        findViewById<TextView>(R.id.stat_distractions).text = resources.getQuantityString(R.plurals.times_count, week.distractions, week.distractions)

        val minutes = DailyStats.focusPerDay(records, starts).map { (it / 60_000).toInt() }
        val dayFormat = SimpleDateFormat("EEE", resources.configuration.locales[0])
        val labels = starts.mapIndexed { i, start ->
            if (i == starts.lastIndex) getString(R.string.today) else dayFormat.format(Date(start))
        }
        val chart = findViewById<WeekBarChartView>(R.id.chart)
        chart.setData(minutes, labels)
        chart.contentDescription = getString(R.string.stats_chart_desc_fmt, minutes.last())
    }

    companion object {
        private const val DAYS = 7
    }
}
