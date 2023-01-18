package com.binbo.glvideo.core.graph.base

import com.binbo.glvideo.core.graph.interfaces.IMediaData
import com.binbo.glvideo.core.graph.interfaces.IMediaQueueOwner
import java.util.*

/**
 *
 * @author bigbug
 * @project lobby
 * @date 2022/7/19
 * @time 15:30
 */
class BaseMediaQueueOwner<Q, D> : IMediaQueueOwner<Q, D> where Q : BaseMediaQueue<D>, D : IMediaData {

    override val inputQueues = LinkedList<Q>()
    override val outputQueues = LinkedList<Q>()

    override fun attachQueue(queue: Q, @DirType dirType: Int) {
        when (dirType) {
            DirType.TYPE_INPUT -> if (!inputQueues.contains(queue)) {
                inputQueues += queue
            }
            DirType.TYPE_OUTPUT -> if (!outputQueues.contains(queue)) {
                outputQueues += queue
            }
        }
    }

    override fun detachQueue(queue: Q, @DirType dirType: Int) {
        inputQueues.remove(queue)
        outputQueues.remove(queue)
    }

    override fun findQueue(uuid: String): Q? {
        return findInputQueue(uuid) ?: findOutputQueue(uuid)
    }

    override fun findInputQueue(uuid: String): Q? {
        return inputQueues.find { it.uuid == uuid }
    }

    override fun findOutputQueue(uuid: String): Q? {
        return outputQueues.find { it.uuid == uuid }
    }
}