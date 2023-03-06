package com.binbo.glvideo.core.graph.base

import androidx.annotation.IntDef
import com.binbo.glvideo.core.graph.base.DirType.Companion.TYPE_INPUT
import com.binbo.glvideo.core.graph.base.DirType.Companion.TYPE_OUTPUT
import com.binbo.glvideo.core.graph.interfaces.*
import java.util.*

/**
 *
 * @author bigbug
 * @project lobby
 * @date 2022/7/12
 * @time 17:35
 */

@IntDef(TYPE_INPUT, TYPE_OUTPUT)
@Retention(AnnotationRetention.SOURCE)
annotation class DirType {
    companion object {
        const val TYPE_INPUT = 0
        const val TYPE_OUTPUT = 1
    }
}

abstract class BaseMediaObject<D : IMediaData> : IMediaObject<D>, IMediaQueueOwner<BaseMediaQueue<D>, D> by BaseMediaQueueOwner() {

    override val uuid: String = UUID.randomUUID().toString()

    override val name: String = ""

    override val priority: Int = 0

    override var endOfStream: Boolean = false

    override var graph: BaseMediaGraph<D>? = null

    override val state: Int
        get() = graph?.state ?: GraphState.IDLE

    val eglResource: IGraphEGLResource?
        get() = graph?.eglResource

    override infix fun to(other: BaseMediaObject<D>): BaseMediaObject<D> {
        if (outputQueues.isEmpty()) {
            attachQueue(createDefaultQueue(TYPE_OUTPUT), TYPE_OUTPUT)
        }

        if (outputQueues.any { it.link(this, other) }) {
            return other
        } else {
            error("Fail to link $this to $other")
        }
    }

    override infix fun to(queue: BaseMediaQueue<D>): BaseMediaObject<D> {
        attachQueue(queue, TYPE_OUTPUT)
        return this
    }

    override infix fun to(queues: List<BaseMediaQueue<D>>): BaseMediaObject<D> {
        queues.forEach {
            attachQueue(it, TYPE_OUTPUT)
        }
        return this
    }

    override fun onAddedToGraph(graph: BaseMediaGraph<D>) {
        this.graph = graph
    }

    override fun onRemovedFromGraph(graph: BaseMediaGraph<D>) {
        this.graph = null
    }

    override fun getConnectedObjects(@DirType dirType: Int): List<IMediaObject<D>> {
        return when (dirType) {
            TYPE_INPUT -> inputQueues.mapNotNull { it.from }
            TYPE_OUTPUT -> outputQueues.mapNotNull { it.to }
            else -> error("Unknown dirType $dirType")
        }
    }

    override suspend fun broadcast(event: BaseGraphEvent<D>) {
        graph?.broadcast(event)
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<D>) {

    }

    abstract fun createDefaultQueue(@DirType dirType: Int): BaseMediaQueue<D>
}