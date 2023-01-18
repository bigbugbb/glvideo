package com.binbo.glvideo.core.graph.interfaces

import com.binbo.glvideo.core.exception.MediaException
import com.binbo.glvideo.core.graph.base.Command
import com.binbo.glvideo.core.graph.base.DirType

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 17:24
 */
interface IGraphVisitor {
    @Throws(MediaException::class)
    suspend fun visit(@DirType dirType: Int, @Command command: Int)
}