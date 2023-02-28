package com.binbo.glvideo.core

import android.content.Context
import android.os.Build
import com.binbo.glvideo.core.ext.activityManager

interface GLVideo {

    companion object Core {
        val tagOfCamera = "glv_camera"
        val tagOfCapture = "glv_capture"
        val tagOfFace = "glv_face"
        val tagOfGraph = "glv_graph"

        internal lateinit var context: Context

        fun init(ctx: Context) {
            context = ctx.applicationContext
        }

        val isGlEs3Supported: Boolean
            get() {
                return activityManager.deviceConfigurationInfo.reqGlEsVersion >= 0x30000
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            }
    }
}