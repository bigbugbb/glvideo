package com.binbo.glvideo.core.graph.interfaces

import android.opengl.EGLContext
import com.binbo.glvideo.core.graph.utils.MediaTexturePool

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/5
 * @time 18:49
 */
interface IGraphEGLResource {
    val sharedContext: EGLContext

    fun create()

    fun destroy()

    suspend fun prepare()

    suspend fun getMediaTextures(
        width: Int,
        height: Int,
        countToGet: Int,
        maxAllowedCount: Int = 300,
        waitTimeout: Long = 5000
    ): List<MediaTexturePool.SharedMediaTexture>

    suspend fun createMediaTextures(width: Int, height: Int, count: Int): List<MediaTexturePool.SharedMediaTexture>

    suspend fun release()
}