package com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.renderer

import android.opengl.EGL14
import android.opengl.GLES20
import android.os.SystemClock
import com.binbo.glvideo.core.media.config.EncoderSurfaceRenderConfig
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder
import com.binbo.glvideo.core.media.recorder.TextureToRecord
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.createFBO
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.deleteFBO
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.drawer.*


class NameCardRenderer : DefaultGLRenderer() {

    override var impl: RenderImpl = NameCardRenderImpl()

    var encoder: MediaVideoEncoder? = null

    override fun onTouchPress(normalizedX: Float, normalizedY: Float) {
        drawers.values.forEach {
            it.handleTouchPress(normalizedX, normalizedY)
        }
    }

    override fun onTouchDragged(normalizedX: Float, normalizedY: Float) {
        drawers.values.forEach {
            it.handleTouchDragged(normalizedX, normalizedY)
        }
    }

    override fun onTouchRelease(normalizedX: Float, normalizedY: Float) {
        drawers.values.forEach {
            it.handleTouchRelease(normalizedX, normalizedY)
        }
    }

    inner class NameCardRenderImpl : RenderImpl {
        private val frameBuffers = IntArray(1)
        private val frameBufferTextures = IntArray(1)

        private val originalVideoFrameBuffers = IntArray(1)
        private val originalVideoFrameBufferTextures = IntArray(1)

        private val verticalClippedVideoFrameBuffers = IntArray(1)
        private val verticalClippedVideoFrameBufferTextures = IntArray(1)

        private val horizontalClippedVideoFrameBuffers = IntArray(1)
        private val horizontalClippedVideoFrameBufferTextures = IntArray(1)

        private val videoVerticalClippedFrameDrawer: VideoVerticalClippedFrameDrawer?
            get() = drawers[VideoVerticalClippedFrameDrawer::class.java] as? VideoVerticalClippedFrameDrawer?

        private val videoHorizontalClippedFrameDrawer: VideoHorizontalClippedFrameDrawer?
            get() = drawers[VideoHorizontalClippedFrameDrawer::class.java] as? VideoHorizontalClippedFrameDrawer?

        private val verticalClippedWidth: Int
            get() = videoVerticalClippedFrameDrawer?.clippedWidth ?: width

        private val verticalClippedHeight: Int
            get() = videoVerticalClippedFrameDrawer?.clippedHeight ?: height

        private val horizontalClippedWidth: Int
            get() = videoHorizontalClippedFrameDrawer?.clippedWidth ?: width

        private val horizontalClippedHeight: Int
            get() = videoHorizontalClippedFrameDrawer?.clippedHeight ?: height

        private var lastRecordTimestamp = SystemClock.uptimeMillis()

        override fun onSurfaceCreate() {}

        override fun onSurfaceChange(width: Int, height: Int) {
            /**
             * video decoder默认分配的texture高度始终跟surface的高度一致，所以这里的fbo尺寸直接取surface的尺寸即可
             */
            createFBO(frameBuffers, frameBufferTextures, width, height)
            createFBO(originalVideoFrameBuffers, originalVideoFrameBufferTextures, width, height)
            /**
             * we need a frame buffer to store the clipped video frame
             * (针对NameCard，目前只支持宽大于高的情况，如果视频的高大于宽，则需要进行调整)
             */
            createFBO(verticalClippedVideoFrameBuffers, verticalClippedVideoFrameBufferTextures, verticalClippedWidth, verticalClippedHeight)
            createFBO(horizontalClippedVideoFrameBuffers, horizontalClippedVideoFrameBufferTextures, horizontalClippedWidth, horizontalClippedHeight)
        }

        override fun onSurfaceDestroy() {
            deleteFBO(frameBuffers, frameBufferTextures)
            deleteFBO(originalVideoFrameBuffers, originalVideoFrameBufferTextures)
            deleteFBO(verticalClippedVideoFrameBuffers, verticalClippedVideoFrameBufferTextures)
            deleteFBO(horizontalClippedVideoFrameBuffers, horizontalClippedVideoFrameBufferTextures)
        }

        /**
         * NameCard上有一个video，并且这个video要从左到右平移，移到最右边后要从右往左移动回来，反复循环。
         * 先用renderVideoTexture把视频解码出的纹理画到缓冲中，
         * 再用clipVideoTextureVertically和clipVideoTextureHorizontally对纹理做垂直和水平方向的裁剪，
         * 最后通过renderFrameBufferTexture把裁剪后的纹理画到最后的帧缓冲中。
         * 通过drawFrameToScreen把缓冲的frame画到屏幕上，
         * 通过drawFrameToEncoder把缓冲的frame传给encoder进行编码。
         * encoder编码线程和当前SurfaceView的GLThread共享GLContext，所以可以共享纹理。
         * clipVideoTextureVertically和clipVideoTextureHorizontally可以放在一起做，以后可以优化。
         */
        override fun onDrawFrame() {
            kotlin.runCatching {
                renderVideoTexture()
                clipVideoTextureVertically()
                clipVideoTextureHorizontally()
                renderFrameBufferTexture()
                drawFrameToScreen()
                drawFrameToEncoder()
            }.getOrElse {
                OpenGLUtils.unbindFBO()
            }
        }

        fun renderVideoTexture() {
            OpenGLUtils.bindFBO(originalVideoFrameBuffers[0], originalVideoFrameBufferTextures[0])
            configFboViewport(width, height)
            drawers[VideoFrameDrawer::class.java]?.draw()
//            val bitmap = OpenGLUtils.savePixels(0, 0, width, height)
            OpenGLUtils.unbindFBO()
            configDefViewport()
        }

        fun clipVideoTextureVertically() {
            OpenGLUtils.bindFBO(verticalClippedVideoFrameBuffers[0], verticalClippedVideoFrameBufferTextures[0])
            configFboViewport(verticalClippedWidth, verticalClippedHeight)
            videoVerticalClippedFrameDrawer?.setTextureID(originalVideoFrameBufferTextures[0])
            videoVerticalClippedFrameDrawer?.draw()
//            val bitmap = OpenGLUtils.savePixels(0, 0, verticalClippedWidth, verticalClippedHeight)
            OpenGLUtils.unbindFBO()
            configDefViewport()
        }

        fun clipVideoTextureHorizontally() {
            OpenGLUtils.bindFBO(horizontalClippedVideoFrameBuffers[0], horizontalClippedVideoFrameBufferTextures[0])
            configFboViewport(horizontalClippedWidth, horizontalClippedHeight)
            videoHorizontalClippedFrameDrawer?.setTextureID(verticalClippedVideoFrameBufferTextures[0])
            videoHorizontalClippedFrameDrawer?.draw()
//            val bitmap = OpenGLUtils.savePixels(0, 0, horizontalClippedWidth, horizontalClippedHeight)
            OpenGLUtils.unbindFBO()
            configDefViewport()
        }

        fun renderFrameBufferTexture() {
            OpenGLUtils.bindFBO(frameBuffers[0], frameBufferTextures[0])
            configFboViewport(width, height)
            drawers[BlurredElementDrawer::class.java]?.draw()
            drawers[NameCardDrawer::class.java]?.setTextureID(horizontalClippedVideoFrameBufferTextures[0])
            drawers[NameCardDrawer::class.java]?.draw()
//            val bitmap = OpenGLUtils.savePixels(0, 0, width, height)
            OpenGLUtils.unbindFBO()
            configDefViewport()
        }

        fun drawFrameToScreen() {
            drawers[FrameDrawer::class.java]?.setTextureID(frameBufferTextures[0])
            drawers[FrameDrawer::class.java]?.draw()
        }

        fun drawFrameToEncoder() {
            val now = SystemClock.uptimeMillis()
            if (now - lastRecordTimestamp > 30) {    // ~30fps
                synchronized(this@NameCardRenderer) {
                    // notify to capturing thread that the frame is available.
                    encoder?.let {
                        EGL14.eglGetCurrentContext()?.let { eglContext ->
                            if (it.recorderGLRender.eglContext != eglContext) {
                                it.setupSurfaceRender(EncoderSurfaceRenderConfig(eglContext))
                            }
                            it.frameAvailableSoon(TextureToRecord(frameBufferTextures[0]), surfaceSize)
                        }
                    }
                }
                lastRecordTimestamp = now
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
        const val TAG = "NameCardRenderer"
    }
}