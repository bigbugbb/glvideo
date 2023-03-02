package com.binbo.glvideo.core.graph.component

import android.media.MediaExtractor
import android.net.Uri
import android.util.Log
import android.util.Range
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaSource
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.event.DecodeMoreFrames
import com.binbo.glvideo.core.graph.event.StartFrameBuffering
import com.binbo.glvideo.core.graph.event.StopFrameBuffering
import com.binbo.glvideo.core.graph.event.VideoMetaDataRetrieved
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.core.graph.simple.SimpleMediaQueue
import com.binbo.glvideo.core.graph.utils.EGLVideoDecoder
import com.binbo.glvideo.core.graph.utils.VideoDecoderHelper
import com.binbo.glvideo.core.media.BaseDecoder
import com.binbo.glvideo.core.media.DefaultDecoderStateListener
import com.binbo.glvideo.core.media.Frame
import com.binbo.glvideo.core.media.IDecoderStateListener
import com.binbo.glvideo.core.media.utils.VideoMetaData
import com.binbo.glvideo.core.media.utils.VideoMetaDataProvider
import com.binbo.glvideo.core.media.utils.VideoMetaDataProviderDelegate
import com.binbo.glvideo.core.utils.DefaultPoolExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext


/**
 * @author bigbug
 * @project lobby
 * @date 2022/7/25
 * @time 20:43
 */
open class VideoSource(
    val videoUri: Uri,
    val videoRawId: Int = 0,
    val startPos: Long = 0L,
    val clippingEnabled: Boolean = false,
    val clippingTimeline: Range<Long> = Range(0L, Long.MAX_VALUE)
) : MediaSource(), VideoDecoderHelper, IDecoderStateListener by DefaultDecoderStateListener() {

    private var videoMetaDataProvider: VideoMetaDataProvider = VideoMetaDataProviderDelegate()

    private var videoDecoder: EGLVideoDecoder? = null
    private var flushed: Semaphore = Semaphore(1, 1)

    protected var videoMetaData = VideoMetaData()

    protected val videoWidth: Int
        get() = videoMetaData.videoWidth

    protected val videoHeight: Int
        get() = videoMetaData.videoHeight

    protected val frameRate: Int
        get() = videoMetaData.frameRate

    protected val rotation: Int
        get() = videoMetaData.rotation

    protected open val frameWindowSize: Int
        get() = videoMetaData.frameRate * 2

    override val timeIntervalUsPerFrame: Long
        get() = 1L

    override val outputQueue: BaseMediaQueue<MediaData>
        get() = outputQueues[0]

    protected open val withSync: Boolean
        get() = false

    protected open val stopAfterWindowFilled: Boolean
        get() = false

    protected open val videoDrawerMode: Int
        get() = 0

    protected open val textureCount: Int
        get() = frameWindowSize

    protected open val textureWidth: Int
        get() {
            var n = 1
            while (kotlin.math.min(videoWidth, videoHeight) / n >= 1080) {
                n *= 2
            }
            return videoWidth / n
        }

    protected open val textureHeight: Int
        get() {
            var n = 1
            while (kotlin.math.min(videoWidth, videoHeight) / n >= 1080) {
                n *= 2
            }
            return videoHeight / n
        }

    override val viewportWidth: Int
        get() = textureWidth

    override val viewportHeight: Int
        get() = textureHeight

    override fun sendEvent(event: BaseGraphEvent<MediaData>) {
        runBlocking { broadcast(event) }
    }
    override suspend fun onPrepare() {
        super.onPrepare()
        videoMetaData = if (videoRawId != 0) {
            videoMetaDataProvider.getVideoMetaData(videoRawId)
        } else {
            videoMetaDataProvider.getVideoMetaData(videoUri.path ?: "")
        }
        Log.d(TAG, "video fps: ${videoMetaData.frameRate}")
        videoDecoder = EGLVideoDecoder(this, videoUri, videoMetaData, frameWindowSize, startPos, withSync, stopAfterWindowFilled, videoDrawerMode, clippingEnabled, clippingTimeline)
        broadcast(VideoMetaDataRetrieved(videoMetaData)) // 其他组件需要metadata信息，这里发个广播告诉其他组件，使它们能够有足够的初始化信息
        // pre allocate shared texture to use
        eglResource?.createMediaTextures(textureWidth, textureHeight, textureCount)
    }

    override fun createDefaultQueue(dirType: Int): BaseMediaQueue<MediaData> {
        return SimpleMediaQueue()
    }

    override suspend fun onStart() {
        super.onStart()
        DefaultPoolExecutor.getInstance().execute(videoDecoder)
        videoDecoder?.goOn()
    }

    override suspend fun onBeginFlush() {
        super.onBeginFlush()
        videoDecoder?.beginFlush()
        flushed.acquire()
    }

    override suspend fun onEndFlush() {
        super.onEndFlush()
        videoDecoder?.endFlush()
    }

    override suspend fun onStop() {
        super.onStop()
        videoDecoder?.stop()
    }

    override suspend fun onRelease() {
        super.onRelease()
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is DecodeMoreFrames -> {
                withContext(GraphExecutor.dispatchers) {
                    graph?.beginFlush()
                    videoDecoder?.seekAndPlay(event.startPos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    graph?.endFlush()
                }
            }
        }
    }

    override fun getFramePresentationTimeUs(frame: Frame, frameIndex: Int): Long {
        return (1000000L / frameRate) * frameIndex
    }

    override fun onDecodeFrameWindowCompleted() {
        setEndOfStream()
    }

    override fun onDecodeClippingTimelineCompleted() {
        setEndOfStream()
    }

    override fun onEndOfStream() {
        setEndOfStream()
    }

    override fun onDecoderBeginFlush(decoder: BaseDecoder?) {
        flushed.release()
    }

    override suspend fun onPostConsumeDecodedFrame(decodeJob: BaseDecoder?, frame: Frame) {
        val frames = videoDecoder?.frames ?: 0
        when {
            frames <= 1 -> broadcast(StartFrameBuffering())
            frames >= frameWindowSize -> broadcast(StopFrameBuffering())
            endOfStream -> broadcast(StopFrameBuffering())
        }
    }

    companion object {
        const val TAG = "VideoDecodeSource"
    }
}

