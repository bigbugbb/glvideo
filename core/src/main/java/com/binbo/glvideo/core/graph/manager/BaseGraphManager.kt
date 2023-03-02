package com.binbo.glvideo.core.graph.manager

import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.base.DirType
import com.binbo.glvideo.core.graph.interfaces.IGraphManager

/**
 * The manager class used to manager the lifecycle of media graph.
 *
 * @author bigbug
 * @project lobby
 * @date 2022/7/26
 * @time 14:05
 */
abstract class BaseGraphManager : IGraphManager<MediaData> {

    override lateinit var mediaGraph: BaseMediaGraph<MediaData>

    abstract override fun createMediaGraph(): BaseMediaGraph<MediaData>

    abstract override fun destroyMediaGraph()

    open suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {}

    override suspend fun prepare(@DirType dirType: Int) {
        mediaGraph.prepare(dirType)
    }

    override suspend fun start(@DirType dirType: Int) {
        mediaGraph.start(dirType)
    }

    override suspend fun beginFlush(dirType: Int) {
        mediaGraph.beginFlush(dirType)
    }

    override suspend fun endFlush(dirType: Int) {
        mediaGraph.endFlush(dirType)
    }

    override suspend fun stop(@DirType dirType: Int) {
        mediaGraph.stop(dirType)
    }

    override suspend fun release(@DirType dirType: Int) {
        mediaGraph.release(dirType)
    }

    override suspend fun waitUntilDone() {}
}