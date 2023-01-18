package com.binbo.glvideo.core.graph

import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import kotlinx.coroutines.withContext

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 18:06
 */
open class MediaGraph(graphManager: BaseGraphManager) : BaseMediaGraph<MediaData>(graphManager) {

    override suspend fun broadcast(event: BaseGraphEvent<MediaData>, dirType: Int) {
        withContext(eventDispatcher) {
            graphManager.onReceiveEvent(event)
            mediaObjects.values.forEach { it.onReceiveEvent(event) }
        }
    }
}
