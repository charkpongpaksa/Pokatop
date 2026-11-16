package com.example.pokatop.ui

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pokatop.R
import com.example.pokatop.data.SessionRecord
import com.example.pokatop.timer.DailyStats
import com.example.pokatop.timer.Presets
import com.example.pokatop.util.Format
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(private val onDelete: (SessionRecord) -> Unit) : RecyclerView.Adapter<SessionAdapter.Holder>() {

    private var items: List<SessionRecord> = emptyList()

    fun submit(list: List<SessionRecord>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val focus = view.findViewById<TextView>(R.id.focus)
        private val whenText = view.findViewById<TextView>(R.id.`when`)
        private val detail = view.findViewById<TextView>(R.id.detail)
        private val delete = view.findViewById<ImageButton>(R.id.delete)

        fun bind(r: SessionRecord) {
            val ctx = itemView.context
            focus.text = ctx.getString(R.string.history_focus_fmt, Format.duration(ctx, r.focusMs))
            whenText.text = ctx.getString(R.string.history_when_fmt, dayLabel(r.startedAt), time(r.startedAt), time(r.endedAt))
            val preset = Presets.match(r.focusMin, r.breakMin, r.longBreakMin)
            val name = ctx.getString(MainActivity.presetNameRes(preset?.key ?: Presets.CUSTOM))
            val distraction = if (r.distractions == null) {
                ctx.getString(R.string.history_plain)
            } else {
                ctx.resources.getQuantityString(R.plurals.distractions_count, r.distractions, r.distractions)
            }
            detail.text = ctx.getString(R.string.history_detail_fmt, name, r.focusMin, r.breakMin, ctx.resources.getQuantityString(R.plurals.rounds_count, r.rounds, r.rounds), distraction)
            delete.setOnClickListener { onDelete(r) }
        }

        private fun time(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))

        private fun dayLabel(ms: Long): String {
            val ctx = itemView.context
            val today = DailyStats.startOfDay(System.currentTimeMillis())
            return when {
                ms >= today -> ctx.getString(R.string.today)
                ms >= today - DailyStats.DAY_MS -> ctx.getString(R.string.yesterday)
                else -> SimpleDateFormat("EEE d MMM", currentLocale(ctx)).format(Date(ms))
            }
        }

        private fun currentLocale(ctx: android.content.Context): Locale = ctx.resources.configuration.locales[0]
    }
}
