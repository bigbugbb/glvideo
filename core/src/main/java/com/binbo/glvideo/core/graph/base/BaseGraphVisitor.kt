package com.binbo.glvideo.core.graph.base

import com.binbo.glvideo.core.graph.interfaces.IGraphVisitor
import com.binbo.glvideo.core.graph.interfaces.IMediaData
import com.binbo.glvideo.core.graph.interfaces.IMediaGraph
import com.binbo.glvideo.core.graph.interfaces.IMediaObject
import java.util.*

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/19
 * @time 21:02
 */
open class BaseGraphVisitor<D : IMediaData>(private val mediaGraph: IMediaGraph<D>) : IGraphVisitor {

    override suspend fun visit(@DirType dirType: Int, @Command command: Int) {
        val seen = HashSet<String>()
        val deque = LinkedList<IMediaObject<D>>()
        val nextLayer = PriorityQueue<IMediaObject<D>>(mediaGraph.mediaObjects.size) { o1, o2 -> o1.priority - o2.priority }

        deque += when (dirType) {
            DirType.TYPE_INPUT -> mediaGraph.mediaSink
            DirType.TYPE_OUTPUT -> mediaGraph.mediaSource
            else -> error("Invalid dirType $dirType")
        }

        while (deque.isNotEmpty()) {
            val mediaObject = deque.pollFirst()
            mediaGraph.execCommand(mediaObject, command)
            seen.add(mediaObject.uuid)
            mediaObject.getConnectedObjects(dirType).forEach {
                if (!seen.contains(it.uuid)) {
                    nextLayer.add(it)
                }
            }

            /**
             * All of the media objects from the current layer have been visited,
             * it's time to push the objects of the next layer to the queue.
             */
            if (deque.isEmpty()) {
                while (nextLayer.isNotEmpty()) {
                    deque.offerLast(nextLayer.poll())
                }
            }
        }
    }
}