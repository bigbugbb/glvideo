package com.binbo.glvideo.core.graph.component

import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import com.binbo.glvideo.core.GLVideo.Core.tagOfGraph
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaSink
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.event.RecordingCompleted
import com.binbo.glvideo.core.graph.event.RenderingCompleted
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor.Companion.ARG_SRC_FILE_PATH
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor.Companion.ARG_VIDEO_FRAME_RATE
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor.Companion.ARG_VIDEO_HEIGHT
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor.Companion.ARG_VIDEO_WIDTH
import com.binbo.glvideo.core.media.BaseMediaEncoder
import com.binbo.glvideo.core.media.recorder.DefaultGLRecorder
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/4
 * @time 21:04
 */
class FrameRecorder(val context: Context, val recorderConfig: GLRecorderConfig) : MediaSink() {

    var recorder: DefaultGLRecorder? = null
        private set

    private var postProcessor: IMediaPostProcessor<String>? = null

    private var encoderPrepared: CountDownLatch? = null

    private val recordingCompleted = AtomicBoolean(false)
    private val renderingCompleted = AtomicBoolean(false)

    private val mediaEncoderListener = object : BaseMediaEncoder.MediaEncoderListener {

        override fun onPrepared(encoder: BaseMediaEncoder?) {
            encoderPrepared?.countDown()
        }

        override fun onStopped(encoder: BaseMediaEncoder?) {
            recordingCompleted.set(true)
            if (renderingCompleted.get()) {
                GraphExecutor.coroutineScope.launch {
                    postProcessor?.run {
                        // merge arguments
                        arguments.putAll(
                            bundleOf(
                                ARG_SRC_FILE_PATH to recorderConfig.targetFilePath,
                                ARG_VIDEO_WIDTH to recorderConfig.width,
                                ARG_VIDEO_HEIGHT to recorderConfig.height,
                                ARG_VIDEO_FRAME_RATE to recorderConfig.videoFrameRate
                            )
                        )

                        // do post processing
                        process(arguments)
                    }
                    broadcast(RecordingCompleted(recorderConfig.targetFilePath))
                }
            }
        }
    }

    override suspend fun onPrepare() {
        super.onPrepare()
        encoderPrepared = CountDownLatch(1)
        recorder = DefaultGLRecorder(context)
        recorder?.startRecording(recorderConfig, mediaEncoderListener)
        encoderPrepared?.await(8, TimeUnit.SECONDS)
    }

    override suspend fun onStart() {
        super.onStart()
    }

    override suspend fun onStop() {
        super.onStop()
        recorder?.stopRecording()
        recorder = null
    }

    override suspend fun onRelease() {
        super.onRelease()
        encoderPrepared?.countDown()
        encoderPrepared = null
        recorder?.stopRecording()
        recorder = null
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is RenderingCompleted -> {
                renderingCompleted.set(true)
                recorder?.stopRecording() // 触发上面的onStopped
            }
            is RecordingCompleted -> {
                Log.d(tagOfGraph, "${event.targetFilePath} is created")
            }
        }
    }

    open fun setPostProcessor(postProcessor: IMediaPostProcessor<String>) {
        this.postProcessor = postProcessor
    }
}
