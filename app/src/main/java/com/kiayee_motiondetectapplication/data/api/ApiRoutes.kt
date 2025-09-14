package com.kiayee_motiondetectapplication.data.api

import com.kiayee_motiondetectapplication.BuildConfig

object ApiRoutes {
    private const val BASE_URL = BuildConfig.BASE_URL

    const val SEND_FRAME = "${BASE_URL}upload_frame_from_phone"
    const val SEND_SETTING = "${BASE_URL}upload_setting_from_phone"
    const val RECEIVE_RESULT = "${BASE_URL}get_people_count"
}