package com.kiayee_motiondetectapplication.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.kiayee_motiondetectapplication.R
import com.kiayee_motiondetectapplication.databinding.FragmentAnalyticBinding

class AnalyticFragment : Fragment() {

    private lateinit var binding: FragmentAnalyticBinding
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnalyticBinding.inflate(inflater, container, false)

        val labelMap = mapOf(
            3 to "9a",
            6 to "12p",
            9 to "3p",
            12 to "6p",
            15 to "9p"
        )

        val days =
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.daySelector.adapter = adapter

        binding.simpleBarChart.setData(weeklyData["Monday"] ?: emptyList(), labelMap)

        binding.daySelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedDay = days[position]
                val values = weeklyData[selectedDay] ?: emptyList()
                binding.simpleBarChart.setData(values, labelMap)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return binding.root
    }
}