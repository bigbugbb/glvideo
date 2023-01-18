package com.binbo.glvideo.core.graph.interfaces

import com.binbo.glvideo.core.graph.base.BaseMediaObject
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import java.util.concurrent.BlockingDeque

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 17:26
 */
interface IMediaQueue<D> : BlockingDeque<D> where D : IMediaData {
    val uuid: String
    var from: BaseMediaObject<D>?
    var to: BaseMediaObject<D>?

    fun link(from: BaseMediaObject<D>, to: BaseMediaObject<D>): Boolean
    fun isLinkable(): Boolean
}