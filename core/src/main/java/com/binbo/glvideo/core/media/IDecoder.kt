package com.binbo.glvideo.core.media

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

interface IDecoder : Runnable {

    /**
     * 暂停解码
     */
    fun pause()

    /**
     * 继续解码
     */
    fun goOn()

    /**
     * 跳转到指定位置
     * 并返回实际帧的时间
     *
     * @param pos: 毫秒
     * @return 实际时间戳，单位：毫秒
     */
    fun seekTo(pos: Long): Long

    /**
     * 跳转到指定位置,并播放
     * 并返回实际帧的时间
     *
     * @param pos: 毫秒
     * @return 实际时间戳，单位：毫秒
     */
    fun seekAndPlay(pos: Long, mode: Int = MediaExtractor.SEEK_TO_PREVIOUS_SYNC): Long

    fun beginFlush()

    fun endFlush()

    fun reset()

    /**
     * 停止解码
     */
    fun stop()

    /**
     * 是否正在解码
     */
    fun isDecoding(): Boolean

    /**
     * 是否正在快进
     */
    fun isSeeking(): Boolean

    /**
     * 是否停止解码
     */
    fun isStop(): Boolean

    /**
     * 设置初始偏移
     */
    fun setStartPos(startPos: Long)

    /**
     * 设置尺寸监听器
     */
    fun setSizeListener(l: IDecoderProgress)

    /**
     * 设置状态监听器
     */
    fun setStateListener(l: IDecoderStateListener?)

    /**
     * 获取视频宽
     */
    fun getWidth(): Int

    /**
     * 获取视频高
     */
    fun getHeight(): Int

    /**
     * 获取视频长度
     */
    fun getDuration(): Long

    /**
     * 当前帧时间，单位：ms
     */
    fun getCurTimeStamp(): Long

    /**
     * 获取视频旋转角度
     */
    fun getRotationAngle(): Int

    /**
     * 获取音视频对应的格式参数
     */
    fun getMediaFormat(): MediaFormat?

    /**
     * 获取音视频对应的媒体轨道
     */
    fun getTrack(): Int

    /**
     * 获取解码的文件路径
     */
    fun getFileUri(): Uri

    /**
     * 无需音视频同步
     */
    fun withoutSync(): IDecoder
}