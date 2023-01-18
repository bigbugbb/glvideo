package com.binbo.glvideo.core.graph

import com.binbo.glvideo.core.graph.interfaces.IMediaData

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/22
 * @time 17:49
 */
open class MediaData : IMediaData {

    override var textureId: Int = 0
    override var mediaWidth: Int = 0
    override var mediaHeight: Int = 0
    override var timestampUs: Long = 0
    override var keyframe: Boolean = false
    override var flag: Int = 0

}

class EndOfStream : MediaData() {
    init {
        flag = IMediaData.FLAG_END_OF_STREAM
    }
}