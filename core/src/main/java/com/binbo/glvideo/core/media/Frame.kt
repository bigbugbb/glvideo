package com.binbo.glvideo.core.media

import android.media.MediaCodec
import java.nio.ByteBuffer

class Frame {
    var buffer: ByteBuffer? = null

    var bufferInfo = MediaCodec.BufferInfo()
        private set

    fun setBufferInfo(info: MediaCodec.BufferInfo) {
        bufferInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags)
    }
}