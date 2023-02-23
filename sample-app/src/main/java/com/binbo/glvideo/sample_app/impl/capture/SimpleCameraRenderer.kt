package com.binbo.glvideo.sample_app.impl.capture

import android.opengl.GLES20
import android.util.ArrayMap
import android.util.Log
import com.binbo.glvideo.core.GLVideo.Core.tagOfCamera
import com.binbo.glvideo.core.opengl.drawer.BlurDrawer
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.bindFBO
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.createFBO
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.deleteFBO
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.unbindFBO
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class SimpleCameraRenderer : DefaultGLRenderer() {

    override var impl: RenderImpl = DefaultRenderImpl(this)

    override fun onSurfaceCreated(glUnused: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(glUnused, config)
        Log.d(tagOfCamera, "onSurfaceCreated")
    }

    override fun onSurfaceChanged(glUnused: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(glUnused, width, height)
        Log.d(tagOfCamera, "onSurfaceChanged $width $height")
    }

    override fun onDrawFrame(glUnused: GL10?) {
        super.onDrawFrame(glUnused)
    }

    override fun onSurfaceDestroyed() {
        super.onSurfaceDestroyed()
        Log.d(tagOfCamera, "onSurfaceDestroyed")
    }

    open class DefaultRenderImpl(val renderer: DefaultGLRenderer) : RenderImpl {
        private val frameBuffers = IntArray(1)
        private val frameBufferTextures = IntArray(1)

        private val verticalBlurBuffers = IntArray(1)
        private val verticalBlurBufferTextures = IntArray(1)

        private val horizontalBlurBuffers = IntArray(1)
        private val horizontalBlurBufferTextures = IntArray(1)

        protected val drawers: ArrayMap<Class<out Drawer>, Drawer>
            get() = renderer.drawers

        private val cameraDrawer: CameraDrawer?
            get() = drawers[CameraDrawer::class.java] as? CameraDrawer?

        private val frameDrawer: FrameDrawer?
            get() = drawers[FrameDrawer::class.java] as? FrameDrawer?

        override fun onSurfaceCreate() {}

        override fun onSurfaceChange(width: Int, height: Int) {
            createFBO(frameBuffers, frameBufferTextures, width, height)
            createFBO(verticalBlurBuffers, verticalBlurBufferTextures, width,height)
            createFBO(horizontalBlurBuffers, horizontalBlurBufferTextures,width,height)
        }

        override fun onSurfaceDestroy() {
            deleteFBO(frameBuffers, frameBufferTextures)
            deleteFBO(verticalBlurBuffers, verticalBlurBufferTextures)
            deleteFBO(horizontalBlurBuffers, horizontalBlurBuffers)
        }

        override fun onDrawFrame() {
            kotlin.runCatching {
                renderCameraTexture()
                drawFrameToScreen()
            }.getOrElse {
                unbindFBO()
            }
        }

        private fun renderCameraTexture() {
            bindFBO(frameBuffers[0], frameBufferTextures[0])
            configFboViewport(renderer.width, renderer.height)
            cameraDrawer?.draw()
//            val bitmap = OpenGLUtils.savePixels(0, 0, renderer.width, renderer.height)
            unbindFBO()
            configDefViewport()
        }

        open fun drawFrameToScreen() {
            frameDrawer?.setTextureID(frameBufferTextures[0])
            frameDrawer?.draw()
        }

        /**
         * 配置FBO窗口
         */
        private fun configFboViewport(width: Int, height: Int) {
            // 设置颠倒的顶点坐标
            GLES20.glViewport(0, 0, width, height)
            // 设置一个颜色状态
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            // 使能颜色状态的值来清屏
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }

        /**
         * 配置默认显示的窗口
         */
        private fun configDefViewport() {
            // 恢复窗口
            GLES20.glViewport(0, 0, renderer.width, renderer.height)
        }
    }
}