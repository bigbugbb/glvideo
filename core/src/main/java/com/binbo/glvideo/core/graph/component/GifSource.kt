package com.binbo.glvideo.core.graph.component

import android.graphics.Bitmap
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.binbo.glvideo.core.GLVideo
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaSource
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.event.DecodedGifFrame
import com.binbo.glvideo.core.graph.simple.SimpleMediaQueue
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.egl.EGLContextProvider
import com.binbo.glvideo.core.opengl.egl.IEGLContextProvider
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class GifSource(
    private val bmpWidth: Int,
    private val bmpHeight: Int,
    gifFrameProvider: GifFrameProvider
) : MediaSource(), IEGLContextProvider by EGLContextProvider() {

    private val maxTextureCount = 60

    private var gifFrameProviderRef: WeakReference<GifFrameProvider> = WeakReference(gifFrameProvider)

    private val frameBuffer = IntArray(1)
    private val frameDrawer = FrameDrawer()
    private var frames: Int = 0
    private var lastFramePresentationTimeUs: Long = 0L

    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var handlerDispatcher: CoroutineDispatcher

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val outputQueue: BaseMediaQueue<MediaData>
        get() = outputQueues[0]

    override suspend fun onPrepare() {
        super.onPrepare()
        // pre allocate shared texture to use
        eglResource?.createMediaTextures(bmpWidth, bmpHeight, maxTextureCount)

        handlerThread = HandlerThread("[bitmap-provider]").apply { start() }
        handler = Handler(handlerThread.looper)
        handlerDispatcher = handler.asCoroutineDispatcher()
    }

    override suspend fun onStart() {
        super.onStart()

        val vpw = bmpWidth
        val vph = bmpHeight

        withContext(handlerDispatcher) {
            /**
             * create EGL context for GL operations
             */
            attachEGLContext(eglResource!!.sharedContext, bmpWidth, bmpHeight)

            /**
             * create frame buffer object so that we can bind it to any shared texture.
             * With this, the decoded frame can be drawn to each shared texture which can be passed through the pin to the next component.
             */
            GLES20.glGenFramebuffers(1, frameBuffer, 0)

            frameDrawer.onWorldCreated()
            frameDrawer.setViewportSize(vpw, vph)
        }

        ioScope.launch {
            frames = 0
            lastFramePresentationTimeUs = 0L

            gifFrameProviderRef.get()?.let { gifFrameProvider ->
                gifFrameProvider.getFrames()
                    .map { frame ->
                        val textureId = OpenGLUtils.loadTexture(GLVideo.context, frame.bitmap)
                        val sharedTexture = eglResource!!.getMediaTextures(vpw, vph, 1, maxTextureCount).first()

                        OpenGLUtils.drawWithFBO(frameBuffer[0], sharedTexture.textureId) {
                            GLES20.glViewport(0, 0, bmpWidth, bmpHeight)
                            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                            frameDrawer?.setTextureID(textureId)
                            frameDrawer?.draw()
                            GLES20.glFinish()
                        }

                        val mediaData = DecodedGifFrame(sharedTexture).apply {
                            this.textureId = sharedTexture.textureId
                            this.mediaWidth = sharedTexture.width
                            this.mediaHeight = sharedTexture.height
                            this.timestampUs = lastFramePresentationTimeUs
                        }
                        Log.i(TAG, "frames: $frames, pts: $lastFramePresentationTimeUs")

                        if (!outputQueue.offer(mediaData, 5, TimeUnit.SECONDS)) {
                            sharedTexture.close()
                        }

                        frames++
                        lastFramePresentationTimeUs += frame.delay * 1000L
                    }
                    .flowOn(handlerDispatcher) // switch to handler thread so we can use the shared textures
                    .onCompletion { setEndOfStream() }
                    .catch { setEndOfStream() }
                    .collect()
            }
        }
    }

    override suspend fun onStop() {
        super.onStop()
        withContext(handlerDispatcher) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
            GLES20.glDeleteFramebuffers(1, frameBuffer, 0)
            detachEGLContext()
        }
    }

    override suspend fun onRelease() {
        super.onRelease()
        handler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()
    }

    override fun createDefaultQueue(dirType: Int): BaseMediaQueue<MediaData> {
        return SimpleMediaQueue()
    }

    data class GifFrame(val bitmap: Bitmap, val delay: Int, val eos: Boolean = false)

    interface GifFrameProvider {
        fun getFrames(): Flow<GifFrame>
    }

    companion object {
        const val TAG = "BitmapSource"
    }
}



