package com.binbo.glvideo.core.graph.interfaces

import com.binbo.glvideo.core.exception.MediaException
import com.binbo.glvideo.core.graph.base.*

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 17:25
 */
interface IMediaGraph<D> where D : IMediaData {

    @GraphState var state: Int

    var mediaSource: BaseMediaSource<D>
    var mediaSink: BaseMediaSink<D>

    val mediaObjects: Map<String, IMediaObject<D>>

    val visitor: IGraphVisitor
    val eglResource: IGraphEGLResource

    fun create()
    fun destroy()

    fun onCreate()
    fun onDestroy()

    fun onVisitGraphSuccess(@DirType dirType: Int, @Command command: Int)
    fun onVisitGraphError(e: MediaException)
    fun onVisitGraphComplete(@DirType dirType: Int, @Command command: Int)

    fun addObject(mediaObject: BaseMediaObject<D>)
    fun removeObject(mediaObject: BaseMediaObject<D>)
    fun findObject(uuid: String): BaseMediaObject<D>?

    suspend fun prepare(@DirType dirType: Int = DirType.TYPE_OUTPUT)
    suspend fun start(@DirType dirType: Int = DirType.TYPE_OUTPUT)
    suspend fun beginFlush(@DirType dirType: Int = DirType.TYPE_OUTPUT)
    suspend fun endFlush(@DirType dirType: Int = DirType.TYPE_OUTPUT)
    suspend fun stop(@DirType dirType: Int = DirType.TYPE_OUTPUT)
    suspend fun release(@DirType dirType: Int = DirType.TYPE_OUTPUT)
    suspend fun broadcast(event: BaseGraphEvent<D>, @DirType dirType: Int = DirType.TYPE_OUTPUT)

    suspend fun execCommand(mediaObject: IMediaObject<D>, @Command command: Int)
}