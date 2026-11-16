package com.example.pokatop.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.pokatop.R

class WeekBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private var minutes: List<Int> = emptyList()
    private var labels: List<String> = emptyList()
    private val bar = RectF()

    private val mint = ContextCompat.getColor(context, R.color.pk_mint)
    private val pastel = ContextCompat.getColor(context, R.color.pk_pastel)
    private val stub = ContextCompat.getColor(context, R.color.pk_surface2)
    private val text = ContextCompat.getColor(context, R.color.pk_text)
    private val text2 = ContextCompat.getColor(context, R.color.pk_text2)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12f * resources.displayMetrics.scaledDensity
        isFakeBoldText = true
        color = text
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13f * resources.displayMetrics.scaledDensity
    }

    fun setData(minutes: List<Int>, labels: List<String>) {
        this.minutes = minutes
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (minutes.isEmpty()) return
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop.toFloat()
        val labelHeight = labelPaint.textSize * 1.8f
        val valueHeight = valuePaint.textSize * 1.6f
        val baseline = height - paddingBottom - labelHeight
        val chartHeight = baseline - top - valueHeight
        val slot = (right - left) / minutes.size
        val barWidth = minOf(slot * 0.6f, 30f * density)
        val max = (minutes.maxOrNull() ?: 0).coerceAtLeast(1)
        val radius = 6f * density

        minutes.forEachIndexed { i, m ->
            val cx = left + slot * i + slot / 2
            val last = i == minutes.lastIndex
            val h = if (m <= 0) 2f * density else maxOf(6f * density, chartHeight * m / max)
            bar.set(cx - barWidth / 2, baseline - h, cx + barWidth / 2, baseline)
            barPaint.color = when {
                last -> mint
                m > 0 -> pastel
                else -> stub
            }
            canvas.drawRoundRect(bar, radius, radius, barPaint)
            if (m > 0) canvas.drawText(m.toString(), cx, baseline - h - valuePaint.textSize * 0.5f, valuePaint)
            labelPaint.color = if (last) text else text2
            labelPaint.isFakeBoldText = last
            canvas.drawText(labels.getOrElse(i) { "" }, cx, baseline + labelPaint.textSize * 1.4f, labelPaint)
        }
    }
}
