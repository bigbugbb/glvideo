package com.binbo.glvideo.sample_app

import android.content.ContentResolver
import android.net.Uri
import android.util.Size
import java.io.File

object AppConsts {
    const val frameRate = 25
    const val recordVideoExt = ".mp4"
    val recordVideoSize = Size(540, 960)  // 录制生成的视频size

    val sampleVideoPath = ContentResolver.SCHEME_ANDROID_RESOURCE + File.pathSeparator + File.separator + File.separator +
            App.context.packageName + File.separator + R.raw.sample_video
    val sampleVideoUri = Uri.parse(sampleVideoPath)
}