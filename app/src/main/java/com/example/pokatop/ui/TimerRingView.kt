package com.example.pokatop.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.pokatop.R
import kotlin.math.min

class TimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val strokeWidthPx = 16f * density
    private val arcBounds = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = ContextCompat.getColor(context, R.color.pk_surface2)
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.pk_mint)
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        color = ContextCompat.getColor(context, R.color.pk_text)
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.pk_text2)
    }

    var ringColor: Int
        get() = progressPaint.color
        set(value) {
            progressPaint.color = value
            invalidate()
        }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var centerText: String = ""
        set(value) {
            field = value
            contentDescription = "$value $subText"
            invalidate()
        }

    var subText: String = ""
        set(value) {
            field = value
            contentDescription = "$centerText $value"
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom).toFloat()
        val cx = paddingLeft + (width - paddingLeft - paddingRight) / 2f
        val cy = paddingTop + (height - paddingTop - paddingBottom) / 2f
        val radius = size / 2f - strokeWidthPx / 2f
        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawOval(arcBounds, trackPaint)
        if (progress > 0f) canvas.drawArc(arcBounds, -90f, 360f * progress, false, progressPaint)

        centerPaint.textSize = size * 0.28f
        val maxTextWidth = size * 0.78f
        val textWidth = centerPaint.measureText(centerText)
        if (textWidth > maxTextWidth) centerPaint.textSize *= maxTextWidth / textWidth
        subPaint.textSize = size * 0.08f
        canvas.drawText(centerText, cx, cy + centerPaint.textSize * 0.3f, centerPaint)
        canvas.drawText(subText, cx, cy + centerPaint.textSize * 0.3f + subPaint.textSize * 1.6f, subPaint)
    }
}
