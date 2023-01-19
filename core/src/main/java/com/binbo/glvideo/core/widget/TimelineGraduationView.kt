package com.binbo.glvideo.core.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Range
import android.view.View
import com.binbo.glvideo.core.R
import com.binbo.glvideo.core.ext.dip
import com.binbo.glvideo.core.ext.getTextBounds

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/23
 * @time 14:15
 */
class TimelineGraduationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private val TAG = "VideoTimelineGraduationView"

    private lateinit var linePaint: Paint
    private lateinit var textPaint: Paint

    private var lowerTime: String = ""
    private var lmiddleTime: String = ""
    private var middleTime: String = ""
    private var rmiddleTime: String = ""
    private var upperTime: String = ""
    private var deltaTime: String = ""

    private var lowerTimeBounds = Rect()
    private var lmiddleTimeBounds = Rect()
    private var middleTimeBounds = Rect()
    private var rmiddleTimeBounds = Rect()
    private var upperTimeBounds = Rect()

    private val density: Float
        get() = resources.displayMetrics.density

    var timeRange: Range<Long> = Range(0L, 600000L)
        set(value) {
            field = value
            if (value.upper - value.lower >= 5000000L) {
                lowerTime = value.lower.toTimeStr()
                lmiddleTime = (value.lower + (value.upper - value.lower) / 4).toTimeStr()
                middleTime = (value.lower + (value.upper - value.lower) / 2).toTimeStr()
                rmiddleTime = (value.upper - (value.upper - value.lower) / 4).toTimeStr()
                upperTime = value.upper.toTimeStr()
            } else {
                lowerTime = value.lower.toTimeStrForShortRange()
                lmiddleTime = (value.lower + (value.upper - value.lower) / 4).toTimeStrForShortRange()
                middleTime = (value.lower + (value.upper - value.lower) / 2).toTimeStrForShortRange()
                rmiddleTime = (value.upper - (value.upper - value.lower) / 4).toTimeStrForShortRange()
                upperTime = value.upper.toTimeStrForShortRange()
            }
            deltaTime = ((value.upper - value.lower) / 4).toDeltaTimeStr()
            lowerTimeBounds = lowerTime.getTextBounds(textPaint)
            lmiddleTimeBounds = lmiddleTime.getTextBounds(textPaint)
            middleTimeBounds = middleTime.getTextBounds(textPaint)
            rmiddleTimeBounds = rmiddleTime.getTextBounds(textPaint)
            upperTimeBounds = upperTime.getTextBounds(textPaint)
            postInvalidate()
        }

    init {
        initComponents(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun initComponents(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TimelineGraduationView, defStyleAttr, defStyleRes)

        linePaint = Paint().apply {
            color = typedArray.getColor(R.styleable.TimelineGraduationView_lineColor, Color.WHITE)
            isAntiAlias = true
        }

        textPaint = Paint().apply {
            color = typedArray.getColor(R.styleable.TimelineGraduationView_textColor, Color.WHITE)
            textSize = typedArray.getDimensionPixelSize(
                R.styleable.TimelineGraduationView_textSize,
                context.resources.getDimensionPixelSize(R.dimen.graducation_text_size_default)
            ).toFloat()
            typeface =
                Typeface.defaultFromStyle(typedArray.getInteger(R.styleable.TimelineGraduationView_typeface, Typeface.defaultFromStyle(Typeface.NORMAL).style))
            textAlign = Paint.Align.CENTER // Text draw is started in the middle
            isLinearText = true
            isAntiAlias = true
        }

        // Recycle the TypedArray.
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val y = dip(12f)
        val d = (width - paddingLeft - paddingRight) / 8f
        val dp4 = dip(4)
        val dp8 = dip(8)

        // draw the top line
        canvas.drawLine(paddingLeft.toFloat(), y, width.toFloat() - paddingRight, y, linePaint)

        // draw the 5 vertical lines
        canvas.drawLine(paddingLeft.toFloat(), y, paddingLeft.toFloat(), y + dp8, linePaint)
        canvas.drawLine(paddingLeft.toFloat() + d * 2, y, paddingLeft.toFloat() + d * 2, y + dp8, linePaint)
        canvas.drawLine(width / 2f, y, width / 2f, y + dp8, linePaint)
        canvas.drawLine(width.toFloat() - paddingRight - d * 2, y, width.toFloat() - paddingRight - d * 2, y + dp8, linePaint)
        canvas.drawLine(width.toFloat() - paddingRight, y, width.toFloat() - paddingRight, y + dp8, linePaint)

        // draw the other small vertical lines
        canvas.drawLine(paddingLeft.toFloat() + d, y, paddingLeft.toFloat() + d, y + dp4, linePaint)
        canvas.drawLine(paddingLeft.toFloat() + d * 3, y, paddingLeft.toFloat() + d * 3, y + dp4, linePaint)
        canvas.drawLine(width.toFloat() - paddingRight - d, y, width.toFloat() - paddingRight - d, y + dp4, linePaint)
        canvas.drawLine(width.toFloat() - paddingRight - d * 3, y, width.toFloat() - paddingRight - d * 3, y + dp4, linePaint)

        // draw graduation text
        if (lowerTime.isNotBlank()) {
            canvas.drawText(lowerTime, 0, lowerTime.length, paddingLeft.toFloat(), y - dp4, textPaint)
        }
        if (lmiddleTime.isNotBlank()) {
            canvas.drawText(lmiddleTime, 0, lmiddleTime.length, paddingLeft + d * 2, y - dp4, textPaint)
        }
        if (middleTime.isNotBlank()) {
            canvas.drawText(middleTime, 0, middleTime.length, width / 2f, y - dp4, textPaint)
        }
        if (rmiddleTime.isNotBlank()) {
            canvas.drawText(rmiddleTime, 0, rmiddleTime.length, width / 2f + d * 2, y - dp4, textPaint)
        }
        if (upperTime.isNotBlank()) {
            canvas.drawText(upperTime, 0, upperTime.length, width.toFloat() - paddingRight, y - dp4, textPaint)
        }

        // draw the delta text, ex. +/-0.6s
        canvas.drawText("-$deltaTime", 0, deltaTime.length + 1, width / 2f - d, y + dp8 * 1.7f, textPaint)
        canvas.drawText("+$deltaTime", 0, deltaTime.length + 1, width / 2f + d, y + dp8 * 1.7f, textPaint)
    }

    private fun Long.toTimeStr(): String {
        val s = this / 1000000L
        val hours = s / 3600
        val minutes = s / 60 % 60
        var seconds = s % 60

        return when {
            hours > 0 -> String.format("$hours:%02d:%02d", minutes, seconds)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds)
            seconds >= 0 -> "${seconds}s"
            else -> "00:00"
        }
    }

    // smaller than 5s
    private fun Long.toTimeStrForShortRange(): String {
        return String.format("%.1fs", this / 1000000f)
    }

    private fun Long.toDeltaTimeStr(): String {
        return String.format("%.1fs", this / 1000000f)
    }
}