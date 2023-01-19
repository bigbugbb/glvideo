package com.binbo.glvideo.core

import android.content.Context
import android.os.Build
import android.os.Environment
import com.binbo.glvideo.core.ext.activityManager
import java.io.File

interface GLVideo {

    companion object Core {
        internal val tagOfCamera = "glv_camera"
        internal val tagOfCapture = "glv_capture"
        internal val tagOfFace = "glv_face"
        internal val tagOfGraph = "glv_graph"

        internal val fileMainPath: String
            get() = Environment.getExternalStorageDirectory().toString() + File.separator + context.packageName

        internal lateinit var context: Context

        fun initialize(ctx: Context) {
            context = ctx.applicationContext
        }

        val isGlEs3Supported: Boolean
            get() {
                return activityManager.deviceConfigurationInfo.reqGlEsVersion >= 0x30000
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            }
    }
}