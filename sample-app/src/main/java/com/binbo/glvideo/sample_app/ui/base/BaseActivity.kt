package com.binbo.glvideo.sample_app.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.binbo.glvideo.sample_app.R

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.activity_bottom_to_center_anim, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.activity_center_to_bottom_anim)
    }
}