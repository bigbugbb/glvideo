package com.binbo.glvideo.core.utils

import android.util.ArrayMap
import android.util.Log
import com.binbo.glvideo.core.BuildConfig

object TimeTraceUtil {
    private val timeMap by lazy { ArrayMap<String, Long>() }

    @JvmStatic
    fun beginTrace(key: String) {
        if (BuildConfig.DEBUG) {
            timeMap[key] = System.currentTimeMillis()
        }
    }

    @JvmStatic
    fun endTrace(key: String) {
        if (BuildConfig.DEBUG) {
            val time = System.currentTimeMillis() - (timeMap[key] ?: 0)
            Log.d("TimeTrace", "$key cost time: ${time}ms")
        }
    }
}