package com.binbo.glvideo.sample_app.ui.video.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.video.fragment.AddVideoBgmFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class AddVideoBgmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_video_bgm)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, AddVideoBgmFragment::class.java)
    }
}