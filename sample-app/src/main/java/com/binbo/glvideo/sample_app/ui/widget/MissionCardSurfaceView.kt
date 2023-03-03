package com.binbo.glvideo.sample_app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import com.binbo.glvideo.core.GLVideo.Core.isGlEs3Supported
import com.binbo.glvideo.core.media.utils.VideoDecoderHolder
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultGLSurfaceView
import com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer.BackgroundDrawer
import com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer.NameCardDrawer
import com.binbo.glvideo.sample_app.impl.advanced.namecard.drawer.WatermarkDrawer
import com.binbo.glvideo.sample_app.impl.advanced.namecard.renderer.NameCardRenderer


class MissionCardSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : DefaultGLSurfaceView(context, attrs), Choreographer.FrameCallback {

    private var decoderHolder = VideoDecoderHolder()

    init {
        setEGLContextClientVersion(if (isGlEs3Supported) 3 else 2)
        setRenderer(NameCardRenderer().apply {
            addDrawer(BackgroundDrawer())
            addDrawer(NameCardDrawer())
            addDrawer(WatermarkDrawer())
            addDrawer(FrameDrawer())
        })
        renderMode = RENDERMODE_WHEN_DIRTY
        preserveEGLContextOnPause = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        Log.d(TAG, "surfaceCreated")
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        super.surfaceChanged(holder, format, w, h)
        Log.d(TAG, "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        Log.d(TAG, "surfaceDestroyed")
        queueEvent { renderer.onSurfaceDestroyed() }
        decoderHolder.release()
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (renderer.isReady) {
            requestRender()
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    companion object {
        private val TAG = MissionCardSurfaceView::class.java.simpleName
    }
}