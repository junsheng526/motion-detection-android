package com.kiayee_motiondetectapplication.view

import android.graphics.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kiayee_motiondetectapplication.R
import com.kiayee_motiondetectapplication.utils.CameraConfig


class DefineROIActivity : AppCompatActivity() {

    /*this is the Part you can play with your own variable*/
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // select

    //View
    private lateinit var previewView: PreviewView

    data class ROI(val name: String, val points: List<PointF>)
    lateinit var roiAdapter: android.widget.ArrayAdapter<String>
    lateinit var listView: android.widget.ListView
    private lateinit var drawROIbutton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var roiOverlay: DrawROIView
    private val roiList = mutableListOf<ROI>()
    private val roiNames = mutableListOf<String>()

    private val PREFS_NAME = "roi_prefs"
    private val ROI_LIST_KEY = "roi_list"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_define_roi)

        roiAdapter = android.widget.ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            roiNames
        )
        listView = findViewById(R.id.listViewRoiPoints)
        listView.adapter = roiAdapter
        drawROIbutton = findViewById(R.id.addROI)
        toolbar = findViewById(R.id.toolbar)
        roiOverlay = findViewById(R.id.roiOverlay)
        previewView = findViewById(R.id.previewView)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Admin: Define ROI"

        loadROIList()

        startCamera()
        drawROIbutton.setOnClickListener {
            roiOverlay.startRoiSelection()
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedName = roiNames[position]
            android.app.AlertDialog.Builder(this)
                .setTitle("Remove ROI")
                .setMessage("Do you want to remove \"$selectedName\"?")
                .setPositiveButton("Yes") { dialog, _ ->
                    roiList.removeAt(position)
                    roiNames.removeAt(position)
                    roiAdapter.notifyDataSetChanged()
                    expandlistview(listView)
                    saveROIList()
                    roiOverlay.clearROI()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            true  // important: this tells the ListView the long click was handled
        }

        roiOverlay.onFinishROI = { pointsList ->
            // Show an AlertDialog with an EditText
            val editText = android.widget.EditText(this)
            editText.hint = "Enter ROI name"
            android.app.AlertDialog.Builder(this)
                .setTitle("Name this ROI")
                .setView(editText)
                .setPositiveButton("Save") { dialog, _ ->
                    val roiName = editText.text.toString().trim()
                    if (roiName.isNotEmpty()) {
                        roiList.add(ROI(roiName, pointsList))
                        roiNames.add(roiName)       // Add to the display list
                        roiAdapter.notifyDataSetChanged() // Refresh ListView!
                        expandlistview(listView)
                        saveROIList()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } // end of setup the save dialog to list view
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedROI = roiList[position]
            roiOverlay.showROI(selectedROI.points)
        }
    }

    //Toolbar usage
    override fun onSupportNavigateUp(): Boolean {
        finish()  // Go back when the back arrow is pressed
        return true
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetResolution(CameraConfig.DEFAULT_RESOLUTION)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            try {
                cameraProvider.unbindAll()  // clear previous use cases
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }// end startcamera function

    fun expandlistview(listView: android.widget.ListView) {
        val listAdapter = listView.adapter ?: return
        var totalHeight = 0
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(listView.width, android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            totalHeight += listItem.measuredHeight
        }
        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (listAdapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }

    fun saveROIList() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(roiList)
        editor.putString(ROI_LIST_KEY, json)
        editor.apply()
    }

    fun loadROIList() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(ROI_LIST_KEY, null)
        val list: MutableList<ROI> = if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<MutableList<ROI>>() {}.type
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf() // fallback if parsing fails
            }
        } else {
            mutableListOf() // return empty if nothing saved
        }

        roiList.clear()
        roiList.addAll(list)
        roiNames.clear()
        roiNames.addAll(roiList.map { it.name })
        roiAdapter.notifyDataSetChanged()
        expandlistview(listView)
    }

}