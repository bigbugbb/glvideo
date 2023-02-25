package com.binbo.glvideo.sample_app.impl.video.graph.gif_to_mp4

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Size
import com.binbo.glvideo.core.ext.tryUntilTimeout
import com.binbo.glvideo.core.graph.EndOfStream
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.base.GraphState
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.event.DecodedGifFrame
import com.binbo.glvideo.core.graph.event.RenderingCompleted
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder
import com.binbo.glvideo.core.media.ext.setupEncoderSurfaceRender
import com.binbo.glvideo.core.media.recorder.TextureToRecord
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class GifToMp4RenderingObject(val viewportSize: Size) : SimpleMediaObject() {

    private var renderer: GifToMp4Renderer? = null

    override suspend fun onPrepare() {
        super.onPrepare()
        renderer = GifToMp4Renderer(this).apply {
            addDrawer(FrameDrawer().apply {
                rotateByX(180f) // 通过FBO绘制到纹理会上下颠倒，需要修正一下
            })
        }
    }

    override suspend fun onStart() {
        super.onStart()
        renderer?.run {
            setUseCustomRenderThread(true, graph?.eglResource?.sharedContext)
            setSurface(null, viewportSize.width, viewportSize.height) // use offscreen rendering with PBuffer
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
            setRenderWaitingTime(1L)
        }
    }

    override suspend fun onRelease() {
        super.onRelease()
        renderer?.stop()
        renderer = null
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        super.onReceiveEvent(event)
    }
}

private class GifToMp4Renderer(val renderingObject: GifToMp4RenderingObject) : DefaultGLRenderer() {

    override var impl: RenderImpl = GifToMp4RendererRenderImpl()

    private val textureQueue: BaseMediaQueue<MediaData>
        get() = renderingObject.inputQueues[0]

    private val frameDrawer: FrameDrawer?
        get() = drawers[FrameDrawer::class.java] as? FrameDrawer?

    private val encoder: MediaVideoEncoder?
        get() = (renderingObject.outputQueues.elementAtOrNull(0)?.to as? FrameRecorder?)?.recorder?.getVideoEncoder()

    inner class GifToMp4RendererRenderImpl : RenderImpl {
        private val frameBuffers = IntArray(64)
        private val frameBufferTextures = IntArray(64)

        private var frames = 0

        override fun onSurfaceCreate() {}

        override fun onSurfaceChange(width: Int, height: Int) {
            /**
             * video decoder默认分配的texture高度始终跟surface的高度一致，所以这里的fbo尺寸直接取surface的尺寸即可
             */
            OpenGLUtils.createFBO(frameBuffers, frameBufferTextures, width, height)

            setupEncoderSurfaceRender(encoder)
        }

        override fun onSurfaceDestroy() {
            OpenGLUtils.deleteFBO(frameBuffers, frameBufferTextures)
        }

        override fun onDrawFrame() {
            kotlin.runCatching {
                while (renderingObject.state == GraphState.STARTED) {
                    textureQueue.poll(100, TimeUnit.MILLISECONDS)?.let { mediaData ->
                        when (mediaData) {
                            is DecodedGifFrame -> {
//                                val bitmap = OpenGLUtils.captureRenderBitmap(mediaData.textureId, mediaData.mediaWidth, mediaData.mediaHeight)
                                val i = frames++ % frameBuffers.size

                                OpenGLUtils.bindFBO(frameBuffers[i], frameBufferTextures[i])
                                configFboViewport(width, height)
                                frameDrawer?.setTextureID(mediaData.textureId)
                                frameDrawer?.draw()
                                GLES20.glFinish()
//                                val bitmap2 = OpenGLUtils.savePixels(0, 0, width, height)
                                configDefViewport()
                                OpenGLUtils.unbindFBO()

                                mediaData.sharedTexture.close()  // free the used shared textures so they can be re-used by the video decoder

                                encoder?.let { drawFrameUntilSucceed(it, TextureToRecord(frameBufferTextures[i], mediaData.timestampUs)) }
                            }
                            is EndOfStream -> runBlocking {
                                renderingObject.broadcast(RenderingCompleted()) // 触发recorder的stopRecording
                            }
                            else -> Log.d(TAG, "Unknown media data type")
                        }
                    }
                }
            }.getOrElse {
                OpenGLUtils.unbindFBO()
            }
        }

        inline fun drawFrameUntilSucceed(encoder: MediaVideoEncoder, textureToRecord: TextureToRecord, timeout: Long = 3000) {
            tryUntilTimeout(timeout) {
                encoder.frameAvailableSoon(textureToRecord, surfaceSize)
            }
        }

        /**
         * 配置FBO窗口
         */
        private fun configFboViewport(width: Int, height: Int) {
            // 设置颠倒的顶点坐标
            GLES20.glViewport(0, 0, width, height)
            // 设置一个颜色状态
            GLES20.glClearColor(0.06f, 0.06f, 0.06f, 1.0f)
            // 使能颜色状态的值来清屏
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }

        /**
         * 配置默认显示的窗口
         */
        private fun configDefViewport() {
            // 恢复窗口
            GLES20.glViewport(0, 0, width, height)
        }
    }

    companion object {
        const val TAG = "GifToMp4Renderer"
    }
}