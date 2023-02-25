package com.binbo.glvideo.sample_app.ui.advanced.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.advanced.fragment.VideoCutSelectFragment
import com.binbo.glvideo.sample_app.ui.video.fragment.VideoDecodeFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class VideoCutSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_cut_select)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, VideoCutSelectFragment::class.java)
    }
}