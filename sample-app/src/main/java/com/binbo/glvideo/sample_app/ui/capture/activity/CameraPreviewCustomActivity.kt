package com.binbo.glvideo.sample_app.ui.capture.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.base.BaseActivity
import com.binbo.glvideo.sample_app.ui.capture.fragment.CameraPreviewCustomFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class CameraPreviewCustomActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_common_container)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, CameraPreviewCustomFragment::class.java)
    }
}