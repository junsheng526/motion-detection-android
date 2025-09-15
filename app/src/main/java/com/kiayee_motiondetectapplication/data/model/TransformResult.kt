package com.kiayee_motiondetectapplication.data.model

data class TransformResult(
    val imageWidth: Float,
    val imageHeight: Float,
    val previewWidth: Float,
    val previewHeight: Float,
    val scale: Float,
    val dx: Float,
    val dy: Float
)