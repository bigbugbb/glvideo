package com.binbo.glvideo.sample_app.ui.capture

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ui.capture.fragment.CameraPreviewCustomFragment
import com.binbo.glvideo.sample_app.ui.capture.fragment.CameraPreviewEffectsFragment
import com.binbo.glvideo.sample_app.utils.replaceViewWithFragment

class CameraPreviewEffectsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview_effects)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        replaceViewWithFragment(R.id.viewContainer, CameraPreviewEffectsFragment::class.java)
    }
}