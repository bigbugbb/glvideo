package com.binbo.glvideo.core.graph.simple

import com.binbo.glvideo.core.graph.MediaQueue

/**
 * @author bigbug
 * @project lobby
 * @date 2022/11/30
 * @time 17:38
 */
open class SimpleMediaQueue : MediaQueue() {
    override fun isLinkable() = true
}