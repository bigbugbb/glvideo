package com.binbo.glvideo.core.graph

import com.binbo.glvideo.core.graph.base.BaseMediaObject

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 17:54
 */
abstract class MediaObject : BaseMediaObject<MediaData>() {
    override fun setEndOfStream() {
        if (!endOfStream) {
            endOfStream = true
            outputQueues.forEach {
                it.offer(EndOfStream())
            }
        }
    }
}