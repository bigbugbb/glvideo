package com.binbo.glvideo.sample_app.impl.video.graph.video_decode

import android.net.Uri
import com.binbo.glvideo.core.graph.component.VideoSource

class VideoDecodeSource(videoUri: Uri, videoRawId: Int) : VideoSource(videoUri, videoRawId) {

    override val withSync: Boolean
        get() = true
}