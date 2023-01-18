package com.binbo.glvideo.core.media

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

interface IExtractor {

    fun getFormat(): MediaFormat?

    fun selectSourceTrack()

    /**
     * 读取音视频数据
     */
    fun readBuffer(byteBuffer: ByteBuffer): Int

    /**
     * 获取当前帧时间
     */
    fun getCurrentTimestamp(): Long

    fun getSampleFlag(): Int

    /**
     * Seek到指定位置，并返回实际帧的时间戳
     */
    fun seek(pos: Long, mode: Int = MediaExtractor.SEEK_TO_PREVIOUS_SYNC): Long

    fun setStartPos(pos: Long)

    fun getStartPos(): Long

    /**
     * 停止读取数据
     */
    fun stop()
}