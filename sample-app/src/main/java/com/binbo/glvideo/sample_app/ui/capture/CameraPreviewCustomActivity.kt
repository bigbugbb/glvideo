package com.binbo.glvideo.sample_app.ui.capture

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class CameraPreviewCustomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview_custom)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, CameraPreviewCustomFragment::class.java)
    }
}