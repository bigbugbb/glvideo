package com.binbo.glvideo.sample_app.ui.widget

import android.content.Context
import android.util.AttributeSet
import com.binbo.glvideo.core.opengl.drawer.BlurDrawer
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.core.opengl.renderer.DefaultGLSurfaceView
import com.binbo.glvideo.sample_app.impl.capture.SimpleCameraRenderer


class SimpleCameraView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
) : DefaultGLSurfaceView(context, attributes) {

    private val cameraRender = SimpleCameraRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(cameraRender.apply {
            addDrawer(CameraDrawer())
            addDrawer(BlurDrawer())
            addDrawer(FrameDrawer())
        })
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    /**
     * 确保在renderer的onSurfaceCreated运行前调用该方法
     */
    fun setSurfaceTextureAvailableListener(listener: SurfaceTextureAvailableListener) {
        (cameraRender.getDrawer(CameraDrawer::class.java) as CameraDrawer?)?.run {
            setSurfaceTextureAvailableListener(listener)
        }
    }

    companion object {
        const val TAG = "GLCameraView"
    }
}