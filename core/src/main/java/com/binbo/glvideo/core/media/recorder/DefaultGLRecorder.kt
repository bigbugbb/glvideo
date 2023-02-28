package com.binbo.glvideo.core.media.recorder

import android.content.Context
import android.util.Log
import com.binbo.glvideo.core.ext.getTempFile
import com.binbo.glvideo.core.media.BaseMediaEncoder
import com.binbo.glvideo.core.media.config.MediaEncoderConfig
import com.binbo.glvideo.core.media.encoder.MediaAudioEncoder
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder
import com.binbo.glvideo.core.media.muxer.MediaMuxerManager
import java.io.File


class DefaultGLRecorder(val context: Context) {

    private var mediaMuxerManager: MediaMuxerManager? = null

    /**
     * 开始录制，这里放在了主线程运行(实际应该放在异步线程中运行)
     */
    fun startRecording(
        config: GLRecorderConfig,
        encoderListener: BaseMediaEncoder.MediaEncoderListener? = null,
    ) {
        Log.d("mm_leak", "startRecording $this")
        kotlin.runCatching {
            if (config.clearFiles) {
                config.targetFileDir.deleteRecursively()
            }

            if (!config.targetFileDir.exists()) {
                config.targetFileDir.mkdirs()
            }

            val mediaEncoderListener = encoderListener ?: defaultMediaEncoderListener

            // if you record audio only, ".m4a" is also OK.
            mediaMuxerManager = MediaMuxerManager(context, config.targetFileDir, config.targetFilename, config.targetFileExt)
            // 开始视频录制
            if (config.enableVideo) {
                val encoderConfig = MediaEncoderConfig(
                    config.width, config.height, frameRate = config.videoFrameRate, videoBitRate = config.videoBitRate, encoderName = config.videoEncoderName
                )
                MediaVideoEncoder(mediaMuxerManager, mediaEncoderListener, encoderConfig)
            }
            // 开启音频录制
            if (config.enableAudio) {
                val encoderConfig = MediaEncoderConfig(encoderName = config.audioEncoderName)
                MediaAudioEncoder(mediaMuxerManager, mediaEncoderListener, encoderConfig)
            }
            // 视频，音频 录制初始化
            mediaMuxerManager?.prepare()
            // 视频，音频 开始录制
            mediaMuxerManager?.startRecording()
        }.getOrElse {
            it.printStackTrace()
        }
    }

    /**
     * request stop recording
     * 开始录制
     */
    fun stopRecording() {
        Log.d("mm_leak", "stopRecording $this")
        mediaMuxerManager?.stopRecording()
        mediaMuxerManager = null
    }

    fun getVideoEncoder(): MediaVideoEncoder? {
        return mediaMuxerManager?.videoEncoder as? MediaVideoEncoder?
    }

    fun getAudioEncoder(): MediaAudioEncoder? {
        return mediaMuxerManager?.audioEncoder as? MediaAudioEncoder?
    }

    /**
     * 视频、音频 开始与结束录制的回调
     */
    private val defaultMediaEncoderListener = DefaultMediaEncoderListener()

    open class DefaultMediaEncoderListener : BaseMediaEncoder.MediaEncoderListener {
        /**
         * 目前由MediaVideoEncoderRunnable在主线程调用
         * @param encoder
         */
        override fun onPrepared(encoder: BaseMediaEncoder) {

        }

        /**
         * 目前在异步线程中调用
         * @param encoder
         */
        override fun onStopped(encoder: BaseMediaEncoder) {

        }
    }
}

data class GLRecorderConfig private constructor(
    val width: Int,
    val height: Int,
    val enableVideo: Boolean,
    val enableAudio: Boolean,
    val videoBitRate: Int,
    val audioBitRate: Int,
    val videoFrameRate: Int,
    val targetFileDir: File,
    val targetFilename: String,
    val targetFileExt: String,
    val videoEncoderName: String,
    val audioEncoderName: String,
    val clearFiles: Boolean
) {
    val targetFilePath: String
        get() = targetFileDir.absolutePath + File.separator + targetFilename + targetFileExt

    data class Builder(
        private var width: Int = 360,
        private var height: Int = 640,
        private var enableVideo: Boolean = true,
        private var enableAudio: Boolean = false,
        private var videoBitRate: Int = 800000,
        private var audioBitRate: Int = 64000,
        private var videoFrameRate: Int = 25,
        private var targetFileDir: File = getTempFile(""),
        private var targetFilename: String = "test_file",
        private var targetFileExt: String = ".mp4",
        private var videoEncoderName: String = defaultVideoEncoderName,
        private var audioEncoderName: String = defaultAudioEncoderName,
        private var clearFiles: Boolean = true
    ) {
        fun width(width: Int) = apply { this.width = width }
        fun height(height: Int) = apply { this.height = height }
        fun enableVideo(enableVideo: Boolean) = apply { this.enableVideo = enableVideo }
        fun enableAudio(enableAudio: Boolean) = apply { this.enableAudio = enableAudio }
        fun videoBitRate(videoBitRate: Int) = apply { this.videoBitRate = videoBitRate }
        fun audioBitRate(audioBitRate: Int) = apply { this.audioBitRate = audioBitRate }
        fun videoFrameRate(videoFrameRate: Int) = apply { this.videoFrameRate = videoFrameRate }
        fun targetFileDir(targetFileDir: File) = apply { this.targetFileDir = targetFileDir }
        fun targetFilename(targetFilename: String) = apply { this.targetFilename = targetFilename }
        fun targetFileExt(targetFileExt: String) = apply { this.targetFileExt = targetFileExt }
        fun videoEncoderName(videoEncoderName: String) = apply { this.videoEncoderName = videoEncoderName }
        fun audioEncoderName(audioEncoderName: String) = apply { this.audioEncoderName = audioEncoderName }
        fun clearFiles(clearFiles: Boolean) = apply { this.clearFiles = clearFiles }

        fun build() = GLRecorderConfig(
            width,
            height,
            enableVideo,
            enableAudio,
            videoBitRate,
            audioBitRate,
            videoFrameRate,
            targetFileDir,
            targetFilename,
            targetFileExt,
            videoEncoderName,
            audioEncoderName,
            clearFiles
        )
    }

    companion object {
        const val defaultVideoEncoderName = "default_video_encoder"
        const val defaultAudioEncoderName = "default_audio_encoder"

        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}