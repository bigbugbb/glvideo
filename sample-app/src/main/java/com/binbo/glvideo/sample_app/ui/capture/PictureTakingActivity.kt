package com.binbo.glvideo.sample_app.ui.capture

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.os.bundleOf
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ext.replaceViewWithFragment

class PictureTakingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_taking)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, PictureTakingFragment::class.java)
    }
}