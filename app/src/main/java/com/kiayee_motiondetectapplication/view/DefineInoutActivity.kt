package com.kiayee_motiondetectapplication.view

import android.graphics.PointF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kiayee_motiondetectapplication.R
import com.kiayee_motiondetectapplication.utils.CameraConfig


class DefineInoutActivity:AppCompatActivity()  {

    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // select
    data class Points(val x: Float, val y: Float)
    data class InOutLines(val inLine: List<Points>, val outLine: List<Points>)
    //View
    private lateinit var previewView: PreviewView
    private lateinit var lineOverlay: DrawInOutView

    private val PREFS_NAME = "inout_prefs"
    private val LINES_KEY = "lines"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_define_inout)

        val btn_in = findViewById<Button>(R.id.select_in_line)
        val btn_out = findViewById<Button>(R.id.select_out_line)
        val btn_confirm_line = findViewById<Button>(R.id.confirm_line)

        previewView = findViewById(R.id.inoutpreviewView)
        lineOverlay = findViewById(R.id.lineOverlayview)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Admin: Select in out lines"

        loadLines()

        startCamera()

        btn_in.setOnClickListener { lineOverlay.startInLineSelection() }
        btn_out.setOnClickListener { lineOverlay.startOutLineSelection() }
        btn_confirm_line.setOnClickListener {
             saveLines()
        }

        lineOverlay.onFinishInLine = { points ->
            // Notify user or update UI if needed
        }
        lineOverlay.onFinishOutLine = { points ->
            // Notify user or update UI if needed
        }
    }

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


    private fun saveLines() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        val gson = Gson()
        val inPoints = lineOverlay.line_in.map { Points(it.x, it.y) }
        val outPoints = lineOverlay.line_out.map { Points(it.x, it.y) }
        val lines = InOutLines(inPoints, outPoints)
        val json = gson.toJson(lines)
        editor.putString(LINES_KEY, json)
        editor.apply()
    }

    private fun loadLines() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(LINES_KEY, null)
        if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<InOutLines>() {}.type
            val lines: InOutLines = gson.fromJson(json, type)
            lineOverlay.line_in.clear()
            lineOverlay.line_in.addAll(lines.inLine.map { PointF(it.x, it.y) })
            lineOverlay.line_out.clear()
            lineOverlay.line_out.addAll(lines.outLine.map { PointF(it.x, it.y) })
            lineOverlay.invalidate()
        }
    }

}