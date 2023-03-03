package com.binbo.glvideo.sample_app.ui.widget

import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import com.binbo.glvideo.core.GLVideo.Core.isGlEs3Supported
import com.binbo.glvideo.core.media.utils.VideoDecoderHolder
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.core.opengl.renderer.DefaultGLSurfaceView
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.NameCardConfig.cardVideoUri
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.drawer.*
import com.binbo.glvideo.sample_app.impl.advanced.namecard_with_video.renderer.NameCardRenderer


class NameCardSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : DefaultGLSurfaceView(context, attrs), Choreographer.FrameCallback {

    private var decoderHolder = VideoDecoderHolder()

    init {
        setEGLContextClientVersion(if (isGlEs3Supported) 3 else 2)
        setRenderer(NameCardRenderer().apply {
            addDrawer(VideoFrameDrawer().apply {
                setSurfaceTextureAvailableListener(object : SurfaceTextureAvailableListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture) {
                        decoderHolder.startVideoDecoderWithSurfaceTexture(cardVideoUri, st) {
                            startVideoDecoder(cardVideoUri)
                        }
                    }
                })
            })
            addDrawer(VideoVerticalClippedFrameDrawer())
            addDrawer(VideoHorizontalClippedFrameDrawer())
            addDrawer(NameCardDrawer())
            addDrawer(BlurredElementDrawer())
            addDrawer(FrameDrawer())
        })
        renderMode = RENDERMODE_WHEN_DIRTY
        preserveEGLContextOnPause = false
    }

    private fun startVideoDecoder(uri: Uri) {
        post {
            Log.d(TAG, "startVideoDecoder $uri")
            decoderHolder.startVideoDecoder(uri) {
                startVideoDecoder(it)
            }
        }
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
        private val TAG = NameCardSurfaceView::class.java.simpleName
    }
}