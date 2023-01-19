package com.binbo.glvideo.sample_app.ui.advanced

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AdvancedViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is advanced Fragment"
    }
    val text: LiveData<String> = _text
}