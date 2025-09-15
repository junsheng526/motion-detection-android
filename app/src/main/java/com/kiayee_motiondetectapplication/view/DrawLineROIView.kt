package com.kiayee_motiondetectapplication.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.kiayee_motiondetectapplication.data.model.DetectedPerson

class DrawLineROIView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var roiPoints: DefineROIActivity.ROI? = null
    private var inLine: List<PointF>? = null
    private var outLine: List<PointF>? = null
    private var people: List<DetectedPerson> = emptyList()

    private val roiPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

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

    fun setadminsetting(roi: DefineROIActivity.ROI?, inLine: List<PointF>?, outLine: List<PointF>?) {
        this.roiPoints = roi
        this.inLine = inLine
        this.outLine = outLine
        invalidate()
    }

    fun updatePeople(people: List<DetectedPerson>) {
        this.people = people
        invalidate() // Triggers a redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw ROI polygon
        roiPoints?.points?.let { points ->
            if (points.size == 4) {
                val path = Path().apply {
                    moveTo(points[0].x.toFloat(), points[0].y.toFloat())
                    for (i in 1..3) lineTo(points[i].x.toFloat(), points[i].y.toFloat())
                    close()
                }
                canvas.drawPath(path, roiPaint)
            }
        }
        // Draw "in" line
        inLine?.takeIf { it.size == 2 }?.let {
            val (p1, p2) = it
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, lineInPaint)
            canvas.drawCircle(p1.x, p1.y, 12f, circlePaint)
            canvas.drawCircle(p2.x, p2.y, 12f, circlePaint)
        }

        // Draw "out" line
        outLine?.takeIf { it.size == 2 }?.let {
            val (p1, p2) = it
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, lineOutPaint)
            canvas.drawCircle(p1.x, p1.y, 12f, circlePaint)
            canvas.drawCircle(p2.x, p2.y, 12f, circlePaint)
        }

        for (person in people) {
            // Draw box
            if (person.box.size == 4) {
                val (x1, y1, x2, y2) = person.box
                val paint = Paint().apply {
                    color = Color.argb(128, 255, 255, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                canvas.drawRect(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), paint)
            }
            // Draw circle (center)
            if (person.circle.size == 2) {
                val (cx, cy) = person.circle
                val paint = Paint().apply {
                    color = Color.MAGENTA
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx.toFloat(), cy.toFloat(), 10f, paint)
            }
            // Draw ID
            val textPaint = Paint().apply {
                color = Color.BLUE
                textSize = 32f
                isFakeBoldText = true
            }
            canvas.drawText("ID:${person.id}", person.box[0].toFloat(), person.box[1].toFloat() - 10f, textPaint)
        }
    }
}
