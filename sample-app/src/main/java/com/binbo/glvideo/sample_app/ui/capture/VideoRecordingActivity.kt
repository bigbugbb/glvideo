package com.binbo.glvideo.sample_app.ui.capture

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.capture.fragment.VideoRecordingFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class VideoRecordingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_recording)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, VideoRecordingFragment::class.java)
    }
}