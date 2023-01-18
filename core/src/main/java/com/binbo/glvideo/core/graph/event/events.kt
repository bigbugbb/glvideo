package com.binbo.glvideo.core.graph.event

import android.net.Uri
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.utils.MediaTexturePool
import com.binbo.glvideo.core.media.utils.VideoMetaData

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/31
 * @time 15:15
 */

class StartFrameBuffering : BaseGraphEvent<MediaData>()
class StopFrameBuffering : BaseGraphEvent<MediaData>()
class RenderingCompleted : BaseGraphEvent<MediaData>()
data class RecordingCompleted(val targetFilePath: String) : BaseGraphEvent<MediaData>()
data class VideoDecodingCompleted(val totalFrames: Int) : BaseGraphEvent<MediaData>()
data class VideoMetaDataRetrieved(val meta: VideoMetaData) : BaseGraphEvent<MediaData>()
data class VideosMetaDataRetrieved(val metas: List<VideoMetaData>) : BaseGraphEvent<MediaData>()
data class DecodedVideoFrame(val videoUri: Uri, val videoMetaData: VideoMetaData, val sharedTexture: MediaTexturePool.SharedMediaTexture) : MediaData(), Comparable<DecodedVideoFrame> {
    override fun compareTo(other: DecodedVideoFrame): Int {
        val x = timestampUs - other.timestampUs
        return when {
            x > 0L -> 1
            x == 0L -> 0
            else -> -1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedVideoFrame) return false
        if (timestampUs != other?.timestampUs) return false
        return true
    }
}

data class DecodeMoreFrames(val startPos: Long) : BaseGraphEvent<MediaData>()