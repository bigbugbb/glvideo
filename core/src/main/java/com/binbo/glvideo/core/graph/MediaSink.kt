package com.binbo.glvideo.core.graph

import com.binbo.glvideo.core.graph.base.BaseMediaSink

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 18:15
 */
abstract class MediaSink : BaseMediaSink<MediaData>() {
    override fun setEndOfStream() {
        endOfStream = true
    }
}