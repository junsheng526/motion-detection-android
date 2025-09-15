package com.kiayee_motiondetectapplication.data.model

import com.kiayee_motiondetectapplication.view.DefineInoutActivity
import com.kiayee_motiondetectapplication.view.DefineROIActivity

data class SettingsPackage(
    val roi: DefineROIActivity.ROI?,        // Your region of interest
    val threshold: Int?,                  // Your threshold
    val lines: DefineInoutActivity.InOutLines? // Your in/out lines
)