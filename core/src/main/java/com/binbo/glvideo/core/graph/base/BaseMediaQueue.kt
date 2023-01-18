package com.binbo.glvideo.core.graph.base

import android.util.Log
import com.binbo.glvideo.core.GLVideo.Core.tagOfGraph
import com.binbo.glvideo.core.graph.interfaces.IMediaData
import com.binbo.glvideo.core.graph.interfaces.IMediaQueue
import java.util.*
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque

/**
 *
 * @author bigbug
 * @project lobby
 * @date 2022/7/12
 * @time 17:36
 */
abstract class BaseMediaQueue<D : IMediaData> : IMediaQueue<D>, BlockingDeque<D> by LinkedBlockingDeque(300) {

    override val uuid: String by lazy { UUID.randomUUID().toString() }

    override var from: BaseMediaObject<D>? = null
    override var to: BaseMediaObject<D>? = null

    override fun link(from: BaseMediaObject<D>, to: BaseMediaObject<D>): Boolean {
        if (from === to) {
            Log.e(tagOfGraph, "link to itself is not allowed!")
            return false
        }

        if (!isLinkable()) {
            Log.e(tagOfGraph, "$from can NOT link to $to through $this")
            return false
        }

        this.from = from
        from.attachQueue(this, DirType.TYPE_OUTPUT)

        this.to = to
        to.attachQueue(this, DirType.TYPE_INPUT)

        return true
    }
}
