package com.binbo.glvideo.core.graph

import com.binbo.glvideo.core.graph.base.BaseMediaSource

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 18:13
 */
abstract class MediaSource : BaseMediaSource<MediaData>() {

    override suspend fun onPrepare() {
    }

    override suspend fun transform(inputData: MediaData): MediaData {
        return inputData
    }

    override fun setEndOfStream() {
        if (!endOfStream) {
            endOfStream = true
            outputQueues.forEach { it.offer(EndOfStream()) }
        }
    }
}