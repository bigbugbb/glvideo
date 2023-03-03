package com.binbo.glvideo.sample_app.impl.advanced.namecard.renderer

import android.opengl.GLES20
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.createFBO
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.deleteFBO
import com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer.BackgroundDrawer
import com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer.NameCardDrawer


class NameCardRenderer : DefaultGLRenderer() {

    override var impl: RenderImpl = MissionCardRenderImpl()

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

    inner class MissionCardRenderImpl : RenderImpl {
        private val frameBuffers = IntArray(1)
        private val frameBufferTextures = IntArray(1)

        override fun onSurfaceCreate() {}

        override fun onSurfaceChange(width: Int, height: Int) {
            createFBO(frameBuffers, frameBufferTextures, width, height)
        }

        override fun onSurfaceDestroy() {
            deleteFBO(frameBuffers, frameBufferTextures)
        }

        override fun onDrawFrame() {
            kotlin.runCatching {
                renderFrameBufferTexture()
                drawFrameToScreen()
            }.getOrElse {
                OpenGLUtils.unbindFBO()
            }
        }

        fun renderFrameBufferTexture() {
            OpenGLUtils.bindFBO(frameBuffers[0], frameBufferTextures[0])
            configFboViewport(width, height)
            drawers[BackgroundDrawer::class.java]?.draw()
            drawers[NameCardDrawer::class.java]?.draw()
//            val bitmap = OpenGLUtils.savePixels(0, 0, width, height)
            OpenGLUtils.unbindFBO()
            configDefViewport()
        }

        fun drawFrameToScreen() {
            drawers[FrameDrawer::class.java]?.setTextureID(frameBufferTextures[0])
            drawers[FrameDrawer::class.java]?.draw()
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