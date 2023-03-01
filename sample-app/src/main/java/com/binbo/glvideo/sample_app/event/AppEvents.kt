package com.binbo.glvideo.sample_app.event

import android.util.Range
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import java.io.File

class TakePictureEvent : BaseGraphEvent<MediaData>()
class TakePictureCompleteEvent(url: String) : BaseGraphEvent<MediaData>()
class RecordVideoEvent(val recording: Boolean) : BaseGraphEvent<MediaData>()

data class VideoFileCreated(val videoFile: File) {
    val videoPath: String
        get() = videoFile.absolutePath
}

data class CreateVideoCutFileSuccess(val webpPath: String)
data class CreateVideoCutFileFailed(val throwable: Throwable? = null)
data class TimelineUpdatedEvent(val timeline: Range<Long>, val videoDurationTs: Long, val suspendFrameLoading: Boolean) : BaseGraphEvent<MediaData>()