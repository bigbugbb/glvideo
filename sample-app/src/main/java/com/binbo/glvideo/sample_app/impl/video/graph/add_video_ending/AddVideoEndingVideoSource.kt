package com.binbo.glvideo.sample_app.impl.video.graph.add_video_ending

import android.net.Uri
import com.binbo.glvideo.core.graph.component.VideoSource

class AddVideoEndingVideoSource(videoUri: Uri, videoRawId: Int) : VideoSource(videoUri, videoRawId) {

    override val stopAfterWindowFilled: Boolean
        get() = false
}