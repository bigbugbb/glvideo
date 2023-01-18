package com.binbo.glvideo.core.graph.interfaces

import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.base.DirType

/**
 * @author bigbug
 * @project lobby
 * @date 2022/11/23
 * @time 14:52
 */
interface IMediaQueueOwner<Q, D> where Q : BaseMediaQueue<D>, D : IMediaData {
    val inputQueues: MutableList<Q>
    val outputQueues: MutableList<Q>

    fun attachQueue(queue: Q, @DirType dirType: Int)
    fun detachQueue(queue: Q, @DirType dirType: Int)
    fun findQueue(uuid: String): Q?

    fun findInputQueue(uuid: String): Q?
    fun findOutputQueue(uuid: String): Q?
}