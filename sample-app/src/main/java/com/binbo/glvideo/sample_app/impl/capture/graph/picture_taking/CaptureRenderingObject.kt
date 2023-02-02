package com.binbo.glvideo.sample_app.impl.capture.graph.picture_taking

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.view.SurfaceView
import com.binbo.glvideo.core.ext.writeToGallery
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.core.opengl.renderer.DefaultCameraRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.sample_app.event.TakePictureCompleteEvent
import com.binbo.glvideo.sample_app.event.TakePictureEvent
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class CaptureRenderingObject(
    private val surfaceViewRef: WeakReference<SurfaceView>,
    private val textureAvailableListener: WeakReference<SurfaceTextureAvailableListener>
) : SimpleMediaObject() {

    var renderer: CaptureCameraRenderer? = null
        private set

    override suspend fun onPrepare() {
        super.onPrepare()
        renderer = CaptureCameraRenderer(this).apply {
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

class CaptureCameraRenderer(private val renderingObject: CaptureRenderingObject) : DefaultCameraRenderer() {

    private var takePictureCompleted = AtomicBoolean(false)

    private val onPictureTakenSuccess: (String) -> Unit = {
        runBlocking { renderingObject.broadcast(TakePictureCompleteEvent(it)) }
    }

    fun takePicture() {
        takePictureCompleted.set(false)
    }

    override var impl: RenderImpl = object : DefaultRenderImpl(this) {

        override fun renderCameraTexture() {
            OpenGLUtils.bindFBO(frameBuffers[0], frameBufferTextures[0])
            configFboViewport(renderer.width, renderer.height)
            drawers[CameraDrawer::class.java]?.draw()
            if (takePictureCompleted.compareAndSet(false, true)) {
                val bitmap = OpenGLUtils.savePixels(0, 0, renderer.width, renderer.height)
                bitmap.writeToGallery(Bitmap.CompressFormat.JPEG, "image/jpeg", onSuccess = onPictureTakenSuccess)
            }
            OpenGLUtils.unbindFBO()
            configDefViewport()
        }
    }

}