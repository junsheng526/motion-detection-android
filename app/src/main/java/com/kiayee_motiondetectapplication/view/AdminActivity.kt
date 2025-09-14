package com.kiayee_motiondetectapplication.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import android.content.Intent
import com.kiayee_motiondetectapplication.R

class AdminActivity : AppCompatActivity() {
    private val PREFS_NAME = "threshold_prefs"
    private val KEY_THRESHOLD = "threshold"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Admin"

        val roiButton = findViewById<Button>(R.id.roiButton)
        roiButton.setOnClickListener {
            val intent = Intent(this, DefineROIActivity::class.java)
            startActivity(intent)
        }

        val lineButton = findViewById<Button>(R.id.inoutButton)
        lineButton.setOnClickListener {
            val intent = Intent(this, DefineInoutActivity::class.java)
            startActivity(intent)
        }

        val thresholdButton = findViewById<Button>(R.id.thresholdButton)
        thresholdButton.setOnClickListener {
            showThresholdDialog()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()  // Go back when the back arrow is pressed
        return true
    }

    fun saveThreshold(value: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putInt(KEY_THRESHOLD, value).apply()
    }

    fun loadThreshold(): Int? {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return if (prefs.contains(KEY_THRESHOLD)) prefs.getInt(KEY_THRESHOLD, 0) else null
    }

    private fun showThresholdDialog() {
        val currentThreshold = loadThreshold()
        val editText = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentThreshold?.toString() ?: "")
            // Don't rely on hint for current value!
        }
        val messageText = if (currentThreshold == null)
            "Please input a new threshold."
        else
            "Current: $currentThreshold"

        val builder = android.app.AlertDialog.Builder(this)
            .setTitle("Set Threshold")
            .setMessage(messageText)
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val inputStr = editText.text.toString()
                val inputNumber = inputStr.toIntOrNull()
                if (inputNumber != null) {
                    saveThreshold(inputNumber)
                } else {
                    editText.error = "Enter a valid number"
                    return@setPositiveButton
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }


}