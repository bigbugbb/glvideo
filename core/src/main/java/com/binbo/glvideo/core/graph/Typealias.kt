package com.binbo.glvideo.core.graph

import com.binbo.glvideo.core.exception.MediaException

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/19
 * @time 20:57
 */

typealias VisitGraphSuccess = (Int, Int) -> Unit
typealias VisitGraphError = (MediaException) -> Unit
typealias VisitGraphComplete = (Int, Int) -> Unit