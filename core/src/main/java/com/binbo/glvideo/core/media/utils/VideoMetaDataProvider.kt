package com.binbo.glvideo.core.media.utils

import android.media.MediaExtractor
import android.media.MediaFormat
import com.binbo.glvideo.core.GLVideo.Core.context

/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/26
 * @time 19:32
 */
data class VideoMetaData(
    val videoFilePath: String = "",
    val actualVideoWidth: Int = 540,
    val actualVideoHeight: Int = 960,
    val frameRate: Int = 25,
    val totalDurationUs: Long = 0,
    val rotation: Int = 0
) {
    /**
     * 手机拍摄的视频会加上rotation参数，需要转换一下
     */
    val videoWidth: Int
        get() = when (rotation) {
            0, 180 -> actualVideoWidth
            90, 270 -> actualVideoHeight
            else -> actualVideoWidth
        }

    val videoHeight: Int
        get() = when (rotation) {
            0, 180 -> actualVideoHeight
            90, 270 -> actualVideoWidth
            else -> actualVideoHeight
        }
}

interface VideoMetaDataProvider {
    fun getVideoMetaData(rawId: Int): VideoMetaData
    fun getVideoMetaData(videoUri: String): VideoMetaData
}

open class VideoMetaDataProviderDelegate : VideoMetaDataProvider {

    companion object {
        val TAG by lazy { this::class.java.canonicalName }
    }

    override fun getVideoMetaData(rawId: Int): VideoMetaData = MediaExtractor().run {
        setDataSource(context.resources.openRawResourceFd(rawId))
        parseVideoMetaData(this)
    }

    override fun getVideoMetaData(videoFilePath: String): VideoMetaData = MediaExtractor().run {
        setDataSource(videoFilePath)
        parseVideoMetaData(this, videoFilePath)
    }

    private fun parseVideoMetaData(extractor: MediaExtractor, videoFilePath: String = ""): VideoMetaData {
        var width = 0
        var height = 0
        var frameRate = 25
        var duration = 0L
        var rotation = 0
        try {
            (0 until extractor.trackCount).forEach { i ->
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                        width = format.getInteger(MediaFormat.KEY_WIDTH)
                    }
                    if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                        height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    }
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        duration = format.getLong(MediaFormat.KEY_DURATION)
                    }
                    if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                        rotation = format.getInteger(MediaFormat.KEY_ROTATION)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor.release()
        }

        return VideoMetaData(videoFilePath, width, height, frameRate, duration, rotation)
    }
}