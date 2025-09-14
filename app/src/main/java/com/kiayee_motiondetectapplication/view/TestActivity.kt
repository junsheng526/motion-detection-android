package com.kiayee_motiondetectapplication.view

import android.R
import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlin.math.max
import com.kiayee_motiondetectapplication.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding

    // Example data for each day (19 values = 6a → 12a midnight)
    private val weeklyData = mapOf(
        "Monday" to listOf(
            0f,
            0f,
            0f,
            4f,
            5f,
            6f,
            8f,
            7f,
            6f,
            5f,
            9f,
            8f,
            6f,
            4f,
            7f,
            5f,
            3f,
            2f,
            1f
        ),
        "Tuesday" to listOf(
            2f,
            1f,
            3f,
            5f,
            6f,
            4f,
            7f,
            9f,
            8f,
            6f,
            7f,
            5f,
            10f,
            9f,
            6f,
            4f,
            3f,
            2f,
            1f
        ),
        "Wednesday" to List(18) { (1..10).random().toFloat() },
        "Thursday" to List(18) { (1..10).random().toFloat() },
        "Friday" to List(18) { (1..10).random().toFloat() },
        "Saturday" to List(18) { (1..10).random().toFloat() },
        "Sunday" to List(18) { (1..10).random().toFloat() }
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Labels at major points only
        val labelMap = mapOf(
            3 to "9a",   // before 9am bar
            6 to "12p",  // before 12pm bar
            9 to "3p",   // before 3pm bar
            12 to "6p",  // before 6pm bar
            15 to "9p"   // before 9pm bar
        )

        val days =
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.daySelector.adapter = adapter

        binding.simpleBarChart.setData(weeklyData["Monday"] ?: emptyList(), labelMap)

        binding.daySelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedDay = days[position]
                val values = weeklyData[selectedDay] ?: emptyList()
                binding.simpleBarChart.setData(values, labelMap)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }
}


class SimpleBarChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F51B5")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 4f
    }

    private val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val tooltipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private var barValues: List<Float> = emptyList()
    private var labelMap: Map<Int, String> = emptyMap()
    private var selectedIndex: Int? = null

    // Cache for layouting bars (index → rect)
    private val barRects = mutableListOf<RectF>()

    fun setData(values: List<Float>, labels: Map<Int, String>) {
        this.barValues = values
        this.labelMap = labels
        selectedIndex = null
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (barValues.isEmpty()) return

        barRects.clear()

        val totalSpacingFactor = 1.7f
        val totalUnits = barValues.size * totalSpacingFactor
        val widthPerBar = width / totalUnits

        val maxBarHeight = height * 0.6f
        val bottom = height * 0.75f
        val radius = 40f

        val maxValue = max(1f, barValues.maxOrNull() ?: 1f)

        var currentX = widthPerBar // start offset

        barValues.forEachIndexed { i, value ->
            // Label & tick
            labelMap[i]?.let { label ->
                val tickX = currentX - widthPerBar / 2
                val labelY = bottom + 70f
                val tickTop = bottom + 10f
                val tickBottom = bottom + 30f
                canvas.drawLine(tickX, tickTop, tickX, tickBottom, tickPaint)
                canvas.drawText(label, tickX, labelY, labelPaint)
            }

            // Bar
            val barHeight = (value / maxValue) * maxBarHeight
            val left = currentX - widthPerBar / 2
            val right = currentX + widthPerBar / 2
            val top = bottom - barHeight
            val rectF = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rectF, radius, radius, barPaint)

            barRects.add(rectF)

            currentX += widthPerBar * totalSpacingFactor
        }

        // Last label
        labelMap[barValues.size]?.let { label ->
            val tickX = currentX - widthPerBar / 2
            canvas.drawLine(tickX, bottom + 10f, tickX, bottom + 30f, tickPaint)
            canvas.drawText(label, tickX, bottom + 70f, labelPaint)
        }

        // Draw tooltip if bar is selected
        selectedIndex?.let { index ->
            if (index in barRects.indices) {
                val rect = barRects[index]
                val barTopX = rect.centerX()
                val barTopY = rect.top

                // Draw connector line
                val lineHeight = 80f
                canvas.drawLine(
                    barTopX, barTopY,
                    barTopX, barTopY - lineHeight,
                    tickPaint
                )

                // Tooltip box
                // Tooltip box
                val tooltipText = getTooltipText(index, barValues[index])
                val padding = 20f
                val textWidth = tooltipTextPaint.measureText(tooltipText)
                val fm = tooltipTextPaint.fontMetrics
                val textHeight = fm.bottom - fm.top

                // Default position (above the bar)
                var boxLeft = barTopX - textWidth / 2 - padding
                var boxRight = barTopX + textWidth / 2 + padding
                var boxBottom = barTopY - lineHeight - 10f
                var boxTop = boxBottom - textHeight - 2 * padding

                // --- Clamp horizontally ---
                if (boxLeft < 0) {
                    val shift = -boxLeft
                    boxLeft += shift
                    boxRight += shift
                } else if (boxRight > width) {
                    val shift = boxRight - width
                    boxLeft -= shift
                    boxRight -= shift
                }

                // --- Clamp vertically (so it doesn’t go off top) ---
                if (boxTop < 0) {
                    val shift = -boxTop
                    boxTop += shift
                    boxBottom += shift
                }

                val rectF = RectF(boxLeft, boxTop, boxRight, boxBottom)
                canvas.drawRoundRect(rectF, 20f, 20f, tooltipPaint)

                // Correct baseline for text inside box
                val textBaseline = boxTop + padding - fm.top
                canvas.drawText(tooltipText, rectF.centerX(), textBaseline, tooltipTextPaint)

            }
        }
    }

    private fun getTooltipText(index: Int, value: Float): String {
        // Example: convert index to time
        val hour = (6 + index) % 24
        val ampm = if (hour < 12) "am" else "pm"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour $ampm: $value"
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                selectedIndex = barRects.indexOfFirst { it.contains(x, y) }
                if (selectedIndex != -1) {
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}

