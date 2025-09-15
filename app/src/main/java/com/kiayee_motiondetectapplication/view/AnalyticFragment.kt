package com.kiayee_motiondetectapplication.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.kiayee_motiondetectapplication.R
import com.kiayee_motiondetectapplication.databinding.FragmentAnalyticBinding
import com.kiayee_motiondetectapplication.viewmodel.AnalyticViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticFragment : Fragment() {

    private lateinit var binding: FragmentAnalyticBinding
    private val vm: AnalyticViewModel by activityViewModels()
    private val labelMap = mapOf(
        3 to "9a",
        6 to "12p",
        9 to "3p",
        12 to "6p",
        15 to "9p"
    )
    private val days = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnalyticBinding.inflate(inflater, container, false)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.daySelector.adapter = adapter

        fetchAndRender("Monday")

//        lifecycleScope.launch {
//            vm.seedMockData()
//        }

        binding.btnPickDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, y, m, d ->
                calendar.set(y, m, d)
                updateSelectedText()
                vm.setMockDateTime(calendar.clone() as Calendar)
            }, year, month, day).show()
        }

        // Time picker
        binding.btnPickTime.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(requireContext(), { _, h, m ->
                calendar.set(Calendar.HOUR_OF_DAY, h)
                calendar.set(Calendar.MINUTE, m)
                updateSelectedText()
                vm.setMockDateTime(calendar.clone() as Calendar)
            }, hour, minute, true).show()
        }


        binding.daySelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedDay = days[position]
                fetchAndRender(selectedDay)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return binding.root
    }

    private fun fetchAndRender(day: String) {
        lifecycleScope.launch {
            val hourly = vm.getHourlyAverageForDay(day)
            println("DEBUG: hourly data for $day = $hourly")
            binding.simpleBarChart.setData(
                if (hourly.isEmpty()) List(19) { 0f } else hourly,
                labelMap
            )
        }
    }

    private fun updateSelectedText() {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        binding.tvSelectedDateTime.text = "Selected: ${fmt.format(calendar.time)}"
    }
}