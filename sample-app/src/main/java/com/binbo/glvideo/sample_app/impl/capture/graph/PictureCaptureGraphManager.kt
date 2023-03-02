package com.binbo.glvideo.sample_app.impl.capture.graph

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.view.SurfaceView
import android.widget.Toast
import com.binbo.glvideo.core.ext.writeToGallery
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.graph.simple.SimpleSinkObject
import com.binbo.glvideo.core.graph.simple.SimpleSourceObject
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.core.opengl.renderer.DefaultCameraRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.event.TakePictureCompleteEvent
import com.binbo.glvideo.sample_app.event.TakePictureEvent
import com.binbo.glvideo.sample_app.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

class PictureCaptureGraphManager(
    surfaceView: SurfaceView,
    textureAvailableListener: SurfaceTextureAvailableListener
) : BaseGraphManager(), SurfaceTexture.OnFrameAvailableListener {

    private val surfaceViewRef: WeakReference<SurfaceView>
    private val textureAvailableListenerRef: WeakReference<SurfaceTextureAvailableListener>

    private var renderingObject: PictureCaptureRenderingObject? = null

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
                val mediaObject = PictureCaptureRenderingObject(surfaceViewRef, textureAvailableListenerRef).apply { mediaGraph.addObject(this) }
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
            is TakePictureCompleteEvent -> withContext(Dispatchers.Main) {
                context.toast(context.getString(R.string.picture_taking_successful_message), Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        renderingObject?.renderer?.notifySwap(SystemClock.uptimeMillis() * 1000)
    }
}

class PictureCaptureRenderingObject(
    private val surfaceViewRef: WeakReference<SurfaceView>,
    private val textureAvailableListener: WeakReference<SurfaceTextureAvailableListener>
) : SimpleMediaObject() {

    var renderer: PictureCaptureRenderer? = null
        private set

    override suspend fun onPrepare() {
        super.onPrepare()
        renderer = PictureCaptureRenderer(this).apply {
            addDrawer(CameraDrawer().apply { setSurfaceTextureAvailableListener(textureAvailableListener.get()) })
            addDrawer(FrameDrawer())
        }
    }

    override suspend fun onStart() {
        super.onStart()
        renderer?.run {
            setUseCustomRenderThread(true)
            setSurface(surfaceViewRef.get()!!)
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
            setRenderWaitingTime(10)
        }
    }

    override suspend fun onRelease() {
        super.onRelease()
        renderer?.stop()
        renderer = null
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        super.onReceiveEvent(event)
        when (event) {
            is TakePictureEvent -> renderer?.takePicture()
        }
    }
}

class PictureCaptureRenderer(private val renderingObject: PictureCaptureRenderingObject) : DefaultCameraRenderer() {

    private var takePictureCompleted = AtomicInteger(0)

    private val onPictureTakenSuccess: (String) -> Unit = {
        runBlocking { renderingObject.broadcast(TakePictureCompleteEvent(it)) }
    }

    fun takePicture() {
        takePictureCompleted.set(1)
    }

    override var impl: RenderImpl = object : DefaultRenderImpl(this) {

        override fun renderCameraTexture() {
            OpenGLUtils.bindFBO(frameBuffers[0], frameBufferTextures[0])
            configFboViewport(renderer.width, renderer.height)
            drawers[CameraDrawer::class.java]?.draw()
            if (takePictureCompleted.compareAndSet(1, 2)) {
                val bitmap = OpenGLUtils.savePixels(0, 0, renderer.width, renderer.height)
                bitmap.writeToGallery(Bitmap.CompressFormat.JPEG, "image/jpeg", onSuccess = onPictureTakenSuccess)
            }
            OpenGLUtils.unbindFBO()
            configDefViewport()
        }
    }

}