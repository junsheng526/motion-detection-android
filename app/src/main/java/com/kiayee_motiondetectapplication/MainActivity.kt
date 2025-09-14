package com.kiayee_motiondetectapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.AlertDialog
import com.kiayee_motiondetectapplication.view.AdminActivity
import com.kiayee_motiondetectapplication.view.UserActivity

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_CODE = 101
    val app_name = "Crowd Monitoring App"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // ← MOVE THIS UP!

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }

        val titleText = findViewById<TextView>(R.id.Appname)
        titleText.text = app_name

        val adminButton = findViewById<Button>(R.id.adminButton)
        val userButton = findViewById<Button>(R.id.userButton)

        adminButton.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        userButton.setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied - check if "Don't ask again" is checked
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // User checked "Don't ask again"
                    AlertDialog.Builder(this)
                        .setTitle("Camera Permission Required")
                        .setMessage("Camera access has been permanently denied. Please enable it in app settings.")
                        .setCancelable(false)
                        .setPositiveButton("Go to Settings") { dialog, _ ->
                            dialog.dismiss()
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = android.net.Uri.parse("package:$packageName")
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Exit") { dialog, _ ->
                            dialog.dismiss()
                            finish()
                        }
                        .show()
                } else {
                    // Regular denial, can ask again
                    AlertDialog.Builder(this)
                        .setTitle("Camera Permission Required")
                        .setMessage("This app requires camera permission to function. Please allow camera access to use the app.")
                        .setCancelable(false)
                        .setPositiveButton("Retry") { dialog, _ ->
                            dialog.dismiss()
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
                        }
                        .setNegativeButton("Exit") { dialog, _ ->
                            dialog.dismiss()
                            finish()
                        }
                        .show()
                }
            }
        }
    }
}