package com.binbo.glvideo.core.opengl.renderer

import android.opengl.GLES20
import android.util.ArrayMap
import android.util.Log
import com.binbo.glvideo.core.GLVideo.Core.tagOfCamera
import com.binbo.glvideo.core.GLVideo.Core.tagOfFace
import com.binbo.glvideo.core.camera.face.Face
import com.binbo.glvideo.core.camera.face.FaceTrackObserver
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.bindFBO
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.createFBO
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.deleteFBO
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils.unbindFBO
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


open class DefaultCameraRenderer : DefaultGLRenderer(), FaceTrackObserver {

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

    override var face: Face? = null

    override fun onFaceDetected(face: Face) {
        Log.d(tagOfFace, "onFaceDetected $face")
        this.face = face
    }

    open class DefaultRenderImpl(val renderer: DefaultGLRenderer) : RenderImpl {

        protected open val frameBuffers = IntArray(1)
        protected open val frameBufferTextures = IntArray(1)

        protected val drawers: ArrayMap<Class<out Drawer>, Drawer>
            get() = renderer.drawers

        override fun onSurfaceCreate() {}

        override fun onSurfaceChange(width: Int, height: Int) {
            createFBO(frameBuffers, frameBufferTextures, width, height)
        }

        override fun onSurfaceDestroy() {
            deleteFBO(frameBuffers, frameBufferTextures)
        }

        override fun onDrawFrame() {
            kotlin.runCatching {
                renderCameraTexture()
                drawFrameToScreen()
            }.getOrElse {
                unbindFBO()
            }
        }

        open fun renderCameraTexture() {
            bindFBO(frameBuffers[0], frameBufferTextures[0])
            configFboViewport(renderer.width, renderer.height)
            drawers[CameraDrawer::class.java]?.draw()
//            val bitmap = OpenGLUtils.savePixels(0, 0, renderer.width, renderer.height)
            configDefViewport()
            unbindFBO()
        }

        open fun drawFrameToScreen() {
            drawers[FrameDrawer::class.java]?.setTextureID(frameBufferTextures[0])
            drawers[FrameDrawer::class.java]?.draw()
        }

        /**
         * 配置FBO窗口
         */
        protected fun configFboViewport(width: Int, height: Int) {
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
        protected fun configDefViewport() {
            // 恢复窗口
            GLES20.glViewport(0, 0, renderer.width, renderer.height)
        }
    }
}
