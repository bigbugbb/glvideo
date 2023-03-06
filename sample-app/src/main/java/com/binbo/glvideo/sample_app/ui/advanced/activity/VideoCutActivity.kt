package com.binbo.glvideo.sample_app.ui.advanced.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.binbo.glvideo.sample_app.App.ArgKey.ARG_SELECTED_VIDEO_KEY
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.advanced.fragment.VideoCutFragment
import com.binbo.glvideo.sample_app.ui.base.BaseActivity
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class VideoCutActivity : BaseActivity() {

    private val arguments: Bundle
        get() = intent?.getBundleExtra(ARG_SELECTED_VIDEO_KEY) ?: bundleOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_container)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, VideoCutFragment::class.java, arguments)
    }
}