package com.binbo.glvideo.core.graph.interfaces

import com.binbo.glvideo.core.exception.MediaException
import com.binbo.glvideo.core.graph.base.*

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 17:26
 */
interface IMediaObject<D : IMediaData> {
    val uuid: String
    val name: String
    val priority: Int

    var endOfStream: Boolean

    @GraphState val state: Int

    var graph: BaseMediaGraph<D>?

    suspend fun onIdle() {}
    suspend fun onPrepare() {}
    suspend fun onStart() {}
    suspend fun onBeginFlush() {}
    suspend fun onEndFlush() {}
    suspend fun onStop() {}
    suspend fun onRelease() {}
    suspend fun onError(e: MediaException) {}

    suspend fun transform(inputData: D): D = inputData

    suspend fun broadcast(event: BaseGraphEvent<D>)

    suspend fun onReceiveEvent(event: BaseGraphEvent<D>)

    infix fun to(queue: BaseMediaQueue<D>): BaseMediaObject<D>
    infix fun to(other: BaseMediaObject<D>): BaseMediaObject<D>
    infix fun to(queues: List<BaseMediaQueue<D>>): BaseMediaObject<D>

    fun setEndOfStream()

    fun onAddedToGraph(graph: BaseMediaGraph<D>)
    fun onRemovedFromGraph(graph: BaseMediaGraph<D>)

    fun getConnectedObjects(@DirType dirType: Int): List<IMediaObject<D>>
}