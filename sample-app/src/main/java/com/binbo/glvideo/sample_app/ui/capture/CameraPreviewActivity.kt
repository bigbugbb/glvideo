package com.binbo.glvideo.sample_app.ui.capture

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.os.bundleOf
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.ext.replaceViewWithFragment

class CameraPreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)
        replaceViewWithFragment(R.id.viewContainer, CameraPreviewFragment::class.java, bundleOf(), "CameraPreviewFragment")
    }
}