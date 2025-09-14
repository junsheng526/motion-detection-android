package com.kiayee_motiondetectapplication.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawInOutView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    enum class Mode { NONE, DRAW_IN, DRAW_OUT }
    var mode: Mode = Mode.NONE

    val line_in = mutableListOf<PointF>()
    val line_out = mutableListOf<PointF>()

    private val lineInPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val lineOutPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val circlePaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    var onFinishInLine: ((List<PointF>) -> Unit)? = null
    var onFinishOutLine: ((List<PointF>) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (mode) {
                Mode.DRAW_IN -> {
                    if (line_in.size < 2) {
                        line_in.add(PointF(event.x, event.y))
                        invalidate()
                        if (line_in.size == 2) {
                            mode = Mode.NONE
                            onFinishInLine?.invoke(line_in.toList())
                        }
                        return true
                    }
                }
                Mode.DRAW_OUT -> {
                    if (line_out.size < 2) {
                        line_out.add(PointF(event.x, event.y))
                        invalidate()
                        if (line_out.size == 2) {
                            mode = Mode.NONE
                            onFinishOutLine?.invoke(line_out.toList())
                        }
                        return true
                    }
                }
                else -> {}
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw "in" line
        if (line_in.size == 2) {
            val (p1, p2) = line_in
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, lineInPaint)
            canvas.drawCircle(p1.x, p1.y, 12f, circlePaint)
            canvas.drawCircle(p2.x, p2.y, 12f, circlePaint)
        } else {
            for (p in line_in) canvas.drawCircle(p.x, p.y, 12f, circlePaint)
            if (line_in.size == 2) {
                canvas.drawLine(line_in[0].x, line_in[0].y, line_in[1].x, line_in[1].y, lineInPaint)
            }
        }
        // Draw "out" line
        if (line_out.size == 2) {
            val (p1, p2) = line_out
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, lineOutPaint)
            canvas.drawCircle(p1.x, p1.y, 12f, circlePaint)
            canvas.drawCircle(p2.x, p2.y, 12f, circlePaint)
        } else {
            for (p in line_out) canvas.drawCircle(p.x, p.y, 12f, circlePaint)
            if (line_out.size == 2) {
                canvas.drawLine(line_out[0].x, line_out[0].y, line_out[1].x, line_out[1].y, lineOutPaint)
            }
        }
    }

    fun startInLineSelection() {
        line_in.clear()
        invalidate()
        mode = Mode.DRAW_IN
    }

    fun startOutLineSelection() {
        line_out.clear()
        invalidate()
        mode = Mode.DRAW_OUT
    }

    fun clearLines() {
        line_in.clear()
        line_out.clear()
        invalidate()
        mode = Mode.NONE
    }

    // Optional: show already defined lines
    fun showLines(inPoints: List<PointF>, outPoints: List<PointF>) {
        line_in.clear()
        line_in.addAll(inPoints)
        line_out.clear()
        line_out.addAll(outPoints)
        invalidate()
        mode = Mode.NONE
    }
}
