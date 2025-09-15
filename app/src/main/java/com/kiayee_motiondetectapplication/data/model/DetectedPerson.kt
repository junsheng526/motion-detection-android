package com.kiayee_motiondetectapplication.data.model

data class DetectedPerson(
    val id: Int,
    val box: IntArray,
    val circle: IntArray
)