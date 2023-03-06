package com.binbo.glvideo.sample_app.ui.video.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.binbo.glvideo.sample_app.App.ArgKey.ARG_SELECTED_VIDEO_KEY
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.base.BaseActivity
import com.binbo.glvideo.sample_app.ui.video.fragment.VideoPreviewFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class VideoPreviewActivity : BaseActivity() {

    private val arguments: Bundle
        get() = intent?.getBundleExtra(ARG_SELECTED_VIDEO_KEY) ?: bundleOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_container)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, VideoPreviewFragment::class.java, arguments)
        overridePendingTransition(R.anim.activity_right_to_screen_anim, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.activity_screen_to_right_anim)
    }
}