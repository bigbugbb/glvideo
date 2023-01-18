package com.binbo.glvideo.core.graph.simple

import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaObject
import com.binbo.glvideo.core.graph.base.BaseMediaQueue

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/25
 * @time 14:02
 */
open class SimpleMediaObject : MediaObject() {
    override fun createDefaultQueue(dirType: Int): BaseMediaQueue<MediaData> {
        return SimpleMediaQueue()
    }
}