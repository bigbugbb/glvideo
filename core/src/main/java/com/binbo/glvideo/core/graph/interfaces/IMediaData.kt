package com.binbo.glvideo.core.graph.interfaces

/**
 *
 * @author bigbug
 * @project lobby
 * @date 2022/7/12
 * @time 19:30
 */

interface IMediaData {
    var textureId: Int
    var mediaWidth: Int
    var mediaHeight: Int
    var timestampUs: Long
    var keyframe: Boolean
    var flag: Int

    companion object {
        const val FLAG_END_OF_STREAM = -1
    }
}

