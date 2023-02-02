package com.binbo.glvideo.sample_app.impl.capture.graph.picture_taking

import android.graphics.SurfaceTexture
import android.os.SystemClock
import android.view.SurfaceView
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleSinkObject
import com.binbo.glvideo.core.graph.simple.SimpleSourceObject
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.event.TakePictureCompleteEvent
import com.binbo.glvideo.sample_app.event.TakePictureEvent
import com.binbo.glvideo.sample_app.ext.toast
import java.lang.ref.WeakReference

class PictureCaptureGraphManager(
    surfaceView: SurfaceView,
    textureAvailableListener: SurfaceTextureAvailableListener
) : BaseGraphManager(), SurfaceTexture.OnFrameAvailableListener {

    private val surfaceViewRef: WeakReference<SurfaceView>
    private val textureAvailableListenerRef: WeakReference<SurfaceTextureAvailableListener>

    private var renderingObject: CaptureRenderingObject? = null

    init {
        surfaceViewRef = WeakReference(surfaceView)
        textureAvailableListenerRef = WeakReference(textureAvailableListener)
    }

    suspend fun takePicture() {
        mediaGraph.broadcast(TakePictureEvent())
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = SimpleSourceObject().apply { mediaGraph.addObject(this) }
                val mediaObject = CaptureRenderingObject(surfaceViewRef, textureAvailableListenerRef).apply { mediaGraph.addObject(this) }
                val mediaSink = SimpleSinkObject()

                mediaSource to mediaObject to mediaSink

                renderingObject = mediaObject
            }
        }
        return mediaGraph.apply { create() }
    }

    override fun destroyMediaGraph() {
        mediaGraph?.destroy()
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is TakePictureCompleteEvent -> {
                App.context.toast("Picture has saved to gallery")
            }
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        renderingObject?.renderer?.notifySwap(SystemClock.uptimeMillis() * 1000)
    }
}