package com.binbo.glvideo.sample_app.ui.video.activity

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.capture.fragment.CameraPreviewFragment
import com.binbo.glvideo.sample_app.ui.video.fragment.VideoDecodeFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class VideoDecodeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_decode)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, VideoDecodeFragment::class.java)
    }
}