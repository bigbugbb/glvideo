package com.binbo.glvideo.core.graph.simple

import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaSource
import com.binbo.glvideo.core.graph.base.BaseMediaQueue

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/25
 * @time 14:12
 */
open class SimpleSourceObject : MediaSource() {

    override fun createDefaultQueue(dirType: Int): BaseMediaQueue<MediaData> {
        return SimpleMediaQueue()
    }
}