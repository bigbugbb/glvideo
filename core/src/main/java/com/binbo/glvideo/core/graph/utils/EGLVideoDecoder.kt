package com.binbo.glvideo.core.graph.utils

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.net.Uri
import android.opengl.GLES20
import android.util.Log
import android.util.Range
import android.view.Surface
import com.binbo.glvideo.core.GLVideo.Core.tagOfGraph
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.component.VideoSource
import com.binbo.glvideo.core.graph.event.DecodedVideoFrame
import com.binbo.glvideo.core.graph.event.VideoDecodingCompleted
import com.binbo.glvideo.core.graph.interfaces.IGraphEGLResource
import com.binbo.glvideo.core.media.BaseDecoder
import com.binbo.glvideo.core.media.DefaultDecoderStateListener
import com.binbo.glvideo.core.media.Frame
import com.binbo.glvideo.core.media.IDecoderStateListener
import com.binbo.glvideo.core.media.decoder.VideoDecoder
import com.binbo.glvideo.core.media.utils.VideoMetaData
import com.binbo.glvideo.core.opengl.drawer.EGLVideoDrawer
import com.binbo.glvideo.core.opengl.egl.EGLContextProvider
import com.binbo.glvideo.core.opengl.egl.IEGLContextProvider
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import java.util.concurrent.TimeUnit
import kotlin.math.abs

interface VideoDecoderHelper : IDecoderStateListener {
    val viewportWidth: Int
    val viewportHeight: Int
    val timeIntervalUsPerFrame: Long
    val eglResource: IGraphEGLResource?
    val outputQueue: BaseMediaQueue<MediaData>
    fun sendEvent(event: BaseGraphEvent<MediaData>)
    fun getFramePresentationTimeUs(frame: Frame, frameIndex: Int): Long = 0L
    fun onDecodeFrameWindowCompleted() {}
    fun onDecodeClippingTimelineCompleted() {}
    fun onEndOfStream() {}
}

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/31
 * @time 16:58
 */
class EGLVideoDecoder(
    private val helper: VideoDecoderHelper,
    private val videoUri: Uri,
    private val videoMetaData: VideoMetaData,
    private val frameWindowSize: Int,
    private val startPos: Long,
    private val videoDrawerMode: Int,
    private val clippingEnabled: Boolean = false,
    private val clippingTimeline: Range<Long> = Range(0L, Long.MAX_VALUE)
) : VideoDecoder(videoUri, null, null), IEGLContextProvider by EGLContextProvider() {

    private val vw: Int = videoMetaData.videoWidth
    private val vh: Int = videoMetaData.videoHeight
    private val vpw: Int = helper.viewportWidth
    private val vph: Int = helper.viewportHeight

    var frames = 0
        private set

    private var lastCapturedFramePtsUs: Long = -1L

    private val eglResource: IGraphEGLResource?
        get() = helper.eglResource

    /**
     * the surface texture from which the video decoder output surface is created
     */
    private var surfaceTexture: SurfaceTexture? = null

    /**
     * texture where the video decoder output draw the decoded frame
     */
    private var textures = intArrayOf(0)
    private val frameBuffer = IntArray(1)
    private val videoDrawer = EGLVideoDrawer(vw, vh, vpw, vph, videoDrawerMode)
    private val stateListener = VideoSourceDecoderStateListener()

    init {
        withoutSync() // 以最快速度完成解码
        setStateListener(stateListener)
    }

    inner class VideoSourceDecoderStateListener : DefaultDecoderStateListener() {

        override fun onDecoderPrepare(decodeJob: BaseDecoder?) {
            /**
             * create EGL context for GL operations
             */
            attachEGLContext(eglResource!!.sharedContext, vw, vh)

            /**
             * create OES texture and create surface, surfaceTexture for decoder output data
             */
            GLES20.glGenTextures(1, textures, 0) // let the decoder allocate the actual graphics buffer size
            surfaceTexture = SurfaceTexture(textures[0])
            surface = Surface(surfaceTexture)

            /**
             * set texture for video drawer as the OES texture and the surfaceTexture
             */
            videoDrawer.textureId = textures[0]
            videoDrawer.surfaceTexture = surfaceTexture

            /**
             * create frame buffer object so that we can bind it to any shared texture.
             * With this, the decoded frame can be drawn to each shared texture which can be passed through the pin to the next component.
             */
            GLES20.glGenFramebuffers(1, frameBuffer, 0)

            setStartPos(startPos)

            helper.onDecoderPrepare(decodeJob)
        }

        override fun onDecoderReady(decodeJob: BaseDecoder?) {
            helper.onDecoderReady(decodeJob)
        }

        override fun onDecoderRunning(decodeJob: BaseDecoder?) {
            helper.onDecoderRunning(decodeJob)
        }

        override fun onDecoderPause(decodeJob: BaseDecoder?) {
            helper.onDecoderPause(decodeJob)
        }

        override fun onDecoderBeginFlush(decoder: BaseDecoder?) {
            super.onDecoderBeginFlush(decoder)
            helper.onDecoderBeginFlush(decoder)
        }

        override fun onDecoderEndFlush(decoder: BaseDecoder?) {
            super.onDecoderEndFlush(decoder)
            helper.onDecoderEndFlush(decoder)
            frames = 0
            lastCapturedFramePtsUs = -1
        }

        override suspend fun onPostConsumeDecodedFrame(decodeJob: BaseDecoder?, frame: Frame) {
            kotlin.runCatching {
                val flags = frame.bufferInfo.flags
                var keyFrame = false
                if ((flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                    keyFrame = true
                } else if ((flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    return
                } else if ((flags and MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) != 0) {
                    return
                } else if ((flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    helper.onEndOfStream()
                    return
                }

                if (frames >= frameWindowSize) {
                    Log.d(tagOfGraph, "onDecodeFrameWindowCompleted")
                    helper.onDecodeFrameWindowCompleted()
                    return
                }

                if (clippingEnabled) {
                    if (frame.bufferInfo.presentationTimeUs < clippingTimeline.lower) {
                        return
                    }
                    if (frame.bufferInfo.presentationTimeUs > clippingTimeline.upper) {
                        Log.d(tagOfGraph, "onDecodeClippingTimelineCompleted")
                        helper.onDecodeClippingTimelineCompleted()
                        return
                    }
                }

                val sharedTexture = eglResource!!.getMediaTextures(vpw, vph, 1, frameWindowSize).first()

                OpenGLUtils.drawWithFBO(frameBuffer[0], sharedTexture.textureId) {
                    GLES20.glViewport(0, 0, vpw, vph)
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    videoDrawer?.setRotation(videoMetaData.rotation)
                    videoDrawer?.draw()
                    GLES20.glFinish()

//                        val bitmap = OpenGLUtils.savePixels(0, 0, vpw, vph)
//                        LogUtil.d(tagOfGraph, "")
                }

                /**
                 * 通过这部操作来控制每秒采集的频率，两帧的pts差距大于minPtsIntervalUs时才采集。
                 */
                if (lastCapturedFramePtsUs < 0 || abs(frame.bufferInfo.presentationTimeUs - lastCapturedFramePtsUs) > helper.timeIntervalUsPerFrame) {
                    val mediaData = DecodedVideoFrame(videoUri, videoMetaData, sharedTexture).apply {
                        this.textureId = sharedTexture.textureId
                        this.mediaWidth = sharedTexture.width
                        this.mediaHeight = sharedTexture.height
                        this.timestampUs = helper.getFramePresentationTimeUs(frame, frames)
                        this.keyframe = keyFrame
                    }

                    if (!helper.outputQueue.offer(mediaData, 5, TimeUnit.SECONDS)) {
                        sharedTexture.close()
                    }
                    Log.d("debug", "decoder offer frame $frames for $videoUri, ts ${mediaData.timestampUs}")

                    lastCapturedFramePtsUs = frame.bufferInfo.presentationTimeUs

                    frames++
                } else {
                    sharedTexture.close()
                }
            }.getOrElse {
                Log.d(VideoSource.TAG, "decodeOneFrame with error: $it")
            }

            helper.onPostConsumeDecodedFrame(decodeJob, frame)
        }

        override fun onDecoderFinish(decodeJob: BaseDecoder?) {
            Log.i(VideoSource.TAG, "onDecoderFinish")
            helper.sendEvent(VideoDecodingCompleted(frames))
            helper.onDecoderFinish(decodeJob)
        }

        override fun onDecoderDestroy(decodeJob: BaseDecoder?) {
            videoDrawer?.release()
            surface?.release()
            surfaceTexture?.release()
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
            GLES20.glDeleteFramebuffers(1, frameBuffer, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glDeleteTextures(1, textures, 0)
            detachEGLContext()
            helper.onDecoderDestroy(decodeJob)
        }

        override fun onDecoderError(decodeJob: BaseDecoder?, msg: String) {
            helper.onDecoderError(decodeJob, msg)
        }
    }
}