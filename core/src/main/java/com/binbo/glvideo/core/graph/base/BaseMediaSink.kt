package com.binbo.glvideo.core.graph.base

import com.binbo.glvideo.core.graph.interfaces.IMediaData
import com.binbo.glvideo.core.graph.interfaces.IMediaSink

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 18:22
 */
abstract class BaseMediaSink<D : IMediaData> : BaseMediaObject<D>(), IMediaSink<D> {
    override fun createDefaultQueue(dirType: Int): BaseMediaQueue<D> {
        error("MediaSink should NOT have forward link")
    }
}