package com.binbo.glvideo.sample_app.ui.advanced.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.advanced.fragment.VideoCutFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class VideoCutActivity : AppCompatActivity() {

    private val arguments: Bundle
        get() = intent?.getBundleExtra(ARG_SELECT_VIDEO_KEY) ?: bundleOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_cut)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, VideoCutFragment::class.java, arguments)
    }

    companion object {
        const val ARG_SELECT_VIDEO_KEY = "arg_select_video"
    }
}