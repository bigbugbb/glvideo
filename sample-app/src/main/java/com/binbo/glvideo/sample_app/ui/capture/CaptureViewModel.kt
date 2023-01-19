package com.binbo.glvideo.sample_app.ui.capture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CaptureViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is capture Fragment"
    }
    val text: LiveData<String> = _text
}