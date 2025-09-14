package com.kiayee_motiondetectapplication.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawROIView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var roiSelectionMode: Boolean = false           // When true, allow touch to select ROI
    val roiPoints = mutableListOf<PointF>()         // The 4 ROI corner points

    // For drawing
    private val paintROI = Paint().apply {
        color = Color.argb(180, 0, 255, 0)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val circlePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    var onFinishROI: ((List<PointF>) -> Unit)? = null // Optional: callback when ROI is finished

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (roiSelectionMode && event.action == MotionEvent.ACTION_DOWN && roiPoints.size < 4) {
            roiPoints.add(PointF(event.x, event.y))
            invalidate()
            if (roiPoints.size == 4) {
                roiSelectionMode = false // disable further touch
                // Notify that ROI is done
                onFinishROI?.invoke(roiPoints.toList())
            }
            return true
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw lines between each ROI point
        for (i in 0 until roiPoints.size - 1) {
            val p1 = roiPoints[i]
            val p2 = roiPoints[i + 1]
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintROI)
        }
        // Close the polygon if we have 4 points
        if (roiPoints.size == 4) {
            val p1 = roiPoints[3]
            val p2 = roiPoints[0]
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintROI)
        }
        // Draw circles for the points
        for (p in roiPoints) canvas.drawCircle(p.x, p.y, 10f, circlePaint)
    }

    fun clearPoints() {
        roiPoints.clear()
        invalidate()
        roiSelectionMode = false
    }

    fun startRoiSelection() {
        clearPoints()
        roiSelectionMode = true
    }

    fun showROI(points: List<PointF>) {
        roiPoints.clear()
        roiPoints.addAll(points)
        roiSelectionMode = false  // Disable selection mode when showing ROI
        invalidate()
    }

    fun clearROI() {
        roiPoints.clear()
        invalidate()
    }

}