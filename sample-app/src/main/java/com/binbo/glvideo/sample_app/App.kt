package com.binbo.glvideo.sample_app

import android.app.Application
import com.binbo.glvideo.core.GLVideo

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        GLVideo.initialize(this)
    }
}