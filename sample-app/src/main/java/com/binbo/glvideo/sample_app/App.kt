package com.binbo.glvideo.sample_app

import android.app.Application
import android.content.Context
import com.binbo.glvideo.core.GLVideo
import com.binbo.glvideo.sample_app.utils.HeartBeatManager
import com.binbo.glvideo.sample_app.utils.player.CustomExoMediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        context = this
        HeartBeatManager.bootstrap()
        ioScope.launch { CustomExoMediaPlayer.init(context) }
    }

    override fun onCreate() {
        super.onCreate()
        GLVideo.initialize(this)
    }

    companion object {
        lateinit var context: Context
            private set

        val ioScope by lazy { CoroutineScope(Dispatchers.IO) }
    }
}