package com.binbo.glvideo.sample_app.ui.capture.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VideoRecordingViewModel : ViewModel() {
    val resetRequested: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
}