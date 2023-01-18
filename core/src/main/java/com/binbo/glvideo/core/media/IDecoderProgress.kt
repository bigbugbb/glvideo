package com.binbo.glvideo.core.media

interface IDecoderProgress {
    /**
     * 视频宽高回调
     */
    fun videoSizeChange(width: Int, height: Int, rotationAngle: Int)

    /**
     * 视频播放进度回调
     */
    fun videoProgressChange(pos: Long)
}