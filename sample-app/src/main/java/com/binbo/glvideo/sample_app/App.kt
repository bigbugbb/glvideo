package com.binbo.glvideo.sample_app

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Size
import com.binbo.glvideo.core.GLVideo
import com.kk.taurus.playerbase.config.PlayerLibrary
import java.io.File

class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        context = this
    }

    override fun onCreate() {
        super.onCreate()
        GLVideo.init(this)
        PlayerLibrary.init(this)
    }

    object Const {
        const val frameRate = 25
        const val recordVideoExt = ".mp4"
        val recordVideoSize = Size(540, 960)  // 录制生成的视频size

        val fileMainPath: String
            get() = Environment.getExternalStorageDirectory().toString() + File.separator + context.packageName

        val sampleVideoPath = ContentResolver.SCHEME_ANDROID_RESOURCE + File.pathSeparator + File.separator + File.separator +
                context.packageName + File.separator + R.raw.sample_video
        val sampleVideoUri = Uri.parse(sampleVideoPath)
    }

    object ArgKey {
        const val ARG_SELECTED_VIDEO_KEY = "arg_selected_video"
        const val ARG_VIDEO_PATH_KEY = "arg_video_path"
    }

    companion object {
        lateinit var context: Context
            private set
    }
}