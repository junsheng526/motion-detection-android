package com.kiayee_motiondetectapplication.view

import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kiayee_motiondetectapplication.R

class UserActivity : AppCompatActivity() {

    private val PREFS_NAME = "roi_prefs"
    private val ROI_LIST_KEY = "roi_list"
    var selectedROI: ROI? = null

    data class ROI(val name: String, val points: List<PointF>)

    class ROIAdapter(
        context: android.content.Context,
        private val items: List<String>
    ) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, items) {
        var selectedPosition: Int = -1
        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
            val view = super.getView(position, convertView, parent)
            view.setBackgroundColor(
                if (position == selectedPosition) android.graphics.Color.LTGRAY
                else android.graphics.Color.TRANSPARENT
            )
            return view
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "User"

        val title = findViewById<TextView>(R.id.listofROI)
        val roilistCalled = findViewById<ListView>(R.id.listViewRoi)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        val roiList = loadROIList()
        val roiNames = roiList.map { it.name }

        val roiAdapter = ROIAdapter(this, roiNames)
        roilistCalled.adapter = roiAdapter

        roilistCalled.setOnItemClickListener { _, _, position, _ ->
            roiAdapter.selectedPosition = position
            roiAdapter.notifyDataSetChanged()
            selectedROI = roiList[position]
        }

        btnConfirm.setOnClickListener {
            val intent = Intent(this, DashboardLiveActivity::class.java)
            val json = Gson().toJson(selectedROI)
            intent.putExtra("selected_roi_json", json)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadROIList(): List<ROI> {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(ROI_LIST_KEY, null)
        return if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<ROI>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
