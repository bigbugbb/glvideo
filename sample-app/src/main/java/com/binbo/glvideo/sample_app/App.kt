package com.binbo.glvideo.sample_app

import android.app.Application
import android.content.Context
import com.binbo.glvideo.core.GLVideo

class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        context = this
    }

    override fun onCreate() {
        super.onCreate()
        GLVideo.initialize(this)
    }

    companion object {
        lateinit var context: Context
            private set
    }
}