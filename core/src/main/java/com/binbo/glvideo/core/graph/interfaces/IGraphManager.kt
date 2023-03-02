package com.binbo.glvideo.core.graph.interfaces

import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.base.DirType
import com.binbo.glvideo.core.graph.base.DirType.Companion.TYPE_OUTPUT
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.ExecutorService

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/26
 * @time 13:59
 */
interface IGraphManager<D> where D : IMediaData {
    var mediaGraph: BaseMediaGraph<MediaData>

    fun createMediaGraph(): BaseMediaGraph<D>
    fun destroyMediaGraph()

    suspend fun waitUntilDone() {}

    suspend fun prepare(@DirType dirType: Int = TYPE_OUTPUT)
    suspend fun start(@DirType dirType: Int = TYPE_OUTPUT)
    suspend fun beginFlush(@DirType dirType: Int = TYPE_OUTPUT)
    suspend fun endFlush(@DirType dirType: Int = TYPE_OUTPUT)
    suspend fun stop(@DirType dirType: Int = TYPE_OUTPUT)
    suspend fun release(@DirType dirType: Int = TYPE_OUTPUT)
}