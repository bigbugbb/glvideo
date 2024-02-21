package com.binbo.glvideo.sample_app.impl.advanced.namecard.graph

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Size
import com.binbo.glvideo.core.GLVideo
import com.binbo.glvideo.core.ext.tryUntilTimeout
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaObject
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.event.RenderingCompleted
import com.binbo.glvideo.core.graph.simple.SimpleMediaQueue
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder
import com.binbo.glvideo.core.media.ext.setupEncoderSurfaceRender
import com.binbo.glvideo.core.media.recorder.TextureToRecord
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.sample_app.App.Const.frameRate
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig
import com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer.BackgroundDrawer
import com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer.OffscreenNameCardDrawer
import com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer.WatermarkDrawer
import kotlinx.coroutines.runBlocking

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/5
 * @time 20:23
 */
class CardRenderingObject(val viewportSize: Size) : MediaObject() {

    private var renderer: CardOffscreenRenderer? = null

    override suspend fun onPrepare() {
        super.onPrepare()
        renderer = CardOffscreenRenderer(this).apply {
            addDrawer(BackgroundDrawer())
            addDrawer(OffscreenNameCardDrawer())
            addDrawer(WatermarkDrawer())
        }
    }

    override suspend fun onStart() {
        super.onStart()
        renderer?.run {
            setUseCustomRenderThread(true)
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

    override fun createDefaultQueue(dirType: Int): BaseMediaQueue<MediaData> {
        return SimpleMediaQueue()
    }
}

private class CardOffscreenRenderer(val renderingObject: CardRenderingObject) : DefaultGLRenderer() {

    override var impl: RenderImpl = CustomRenderImpl()

    private val encoder: MediaVideoEncoder?
        get() = (renderingObject.outputQueues.elementAtOrNull(0)?.to as? FrameRecorder?)?.recorder?.getVideoEncoder()

    inner class CustomRenderImpl : RenderImpl {
        private val frameBuffers = IntArray(1)
        private val frameBufferTextures = IntArray(1)

        private var frames = 0
        private val ptsDelta = 1000000000L / frameRate
        private var fenceSyncObject = 0L

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
                if (frames < frameRate * 8) {
                    OpenGLUtils.bindFBO(frameBuffers[0], frameBufferTextures[0])
                    configFboViewport(width, height)
                    drawers[BackgroundDrawer::class.java]?.draw()
                    drawers[OffscreenNameCardDrawer::class.java]?.draw()
                    drawers[WatermarkDrawer::class.java]?.draw()
                    OpenGLUtils.unbindFBO()
                    configDefViewport()

                    if (GLVideo.isGlEs3Supported) {
                        fenceSyncObject = GLES30.glFenceSync(GLES30.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
                    } else {
                        GLES20.glFinish() // consider about using glFenceSync
                    }

                    Log.d(TAG, "card offscreen frames = $frames")

                    // notify to capturing thread that the frame is available.
                    encoder?.let {
                        drawFrameUntilSucceed(it, TextureToRecord(frameBufferTextures[0], ptsDelta * frames++, fenceSyncObject))
                    }

                    if (frames == frameRate * 8) {
                        runBlocking { renderingObject.broadcast(RenderingCompleted()) }
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
        const val TAG = "CardOffscreenRenderer"
    }
}