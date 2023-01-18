package com.binbo.glvideo.core.graph

import com.binbo.glvideo.core.graph.base.BaseGraphVisitor

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 18:08
 */
class MediaGraphVisitor(val mediaGraph: MediaGraph) : BaseGraphVisitor<MediaData>(mediaGraph)