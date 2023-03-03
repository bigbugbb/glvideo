package com.binbo.glvideo.sample_app.ui.advanced.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.advanced.fragment.NameCardWithVideoFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class NameCardWithVideoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_container)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, NameCardWithVideoFragment::class.java)
    }
}