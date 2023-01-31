package com.binbo.glvideo.sample_app

import android.app.Application
import android.content.Context
import com.binbo.glvideo.core.GLVideo

class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        ContextUtil.init(this)
    }

    override fun onCreate() {
        super.onCreate()
        GLVideo.initialize(this)
    }
}