package com.binbo.glvideo.sample_app.impl.capture.graph.video_recording

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceView
import com.binbo.glvideo.core.ext.tryUntilTimeout
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.graph.simple.SimpleMediaQueue
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder
import com.binbo.glvideo.core.media.ext.setupEncoderSurfaceRender
import com.binbo.glvideo.core.media.recorder.TextureToRecord
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.core.opengl.renderer.DefaultCameraRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.core.utils.Constants.RECORDER_INPUT_QUEUE_SIZE
import com.binbo.glvideo.sample_app.AppConsts
import com.binbo.glvideo.sample_app.event.RecordVideoEvent
import java.lang.ref.WeakReference
import kotlin.math.abs

class VideoCaptureRenderingObject(
    private val surfaceViewRef: WeakReference<SurfaceView>,
    private val textureAvailableListener: WeakReference<SurfaceTextureAvailableListener>
) : SimpleMediaObject() {

    var renderer: CaptureCameraRenderer? = null
        private set

    var recording: Boolean = false
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
            setRenderWaitingTime(8)
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
            is RecordVideoEvent -> recording = event.recording
        }
    }

    override fun createDefaultQueue(dirType: Int): BaseMediaQueue<MediaData> {
        return SimpleMediaQueue()
    }
}

class CaptureCameraRenderer(private val renderingObject: VideoCaptureRenderingObject) : DefaultCameraRenderer() {

    private val encoder: MediaVideoEncoder?
        get() = (renderingObject.outputQueues.elementAtOrNull(0)?.to as? FrameRecorder?)?.recorder?.getVideoEncoder()

    override var impl: RenderImpl = object : DefaultRenderImpl(this) {

        private var lastCaptureTime = 0L

        private val recordingFrameBuffers = IntArray(RECORDER_INPUT_QUEUE_SIZE * 2)
        private val recordingFrameBufferTextures = IntArray(RECORDER_INPUT_QUEUE_SIZE * 2)

        private var frames = 0
        private val ptsDelta = 1000000000L / AppConsts.frameRate

        override fun onSurfaceChange(width: Int, height: Int) {
            super.onSurfaceChange(width, height)
            OpenGLUtils.createFBO(recordingFrameBuffers, recordingFrameBufferTextures, width, height)
            setupEncoderSurfaceRender(encoder)
        }

        override fun onSurfaceDestroy() {
            super.onSurfaceDestroy()
            OpenGLUtils.deleteFBO(recordingFrameBuffers, recordingFrameBufferTextures)
        }

        override fun onDrawFrame() {
            kotlin.runCatching {
                renderCameraTexture()
                drawFrameToScreen()

                if (renderingObject.recording) {
                    if (abs(SystemClock.uptimeMillis() - lastCaptureTime) >= 1000 / AppConsts.frameRate) {
                        Log.d("capture video frames", "onDrawFrame capture frames: $frames")
                        lastCaptureTime = SystemClock.uptimeMillis()
                        val i = frames % recordingFrameBuffers.size
                        drawFrameToFrameBuffers(recordingFrameBuffers, recordingFrameBufferTextures, i)
                        GLES20.glFinish()
                        encoder?.let { drawFrameUntilSucceed(it, TextureToRecord(recordingFrameBufferTextures[i], frames * ptsDelta)) }
                        frames++
                    }
                } else {
                    frames = 0
                    lastCaptureTime = 0L
                }
            }.getOrElse {
                OpenGLUtils.unbindFBO()
            }
        }

        private fun drawFrameToFrameBuffers(fbo: IntArray, fboTextures: IntArray, i: Int) {
            OpenGLUtils.bindFBO(fbo[i], fboTextures[i])
            configFboViewport(renderer.width, renderer.height)
            drawers[FrameDrawer::class.java]?.setTextureID(frameBufferTextures[0])
            drawers[FrameDrawer::class.java]?.draw()
//            val bitmap = OpenGLUtils.savePixels(0, 0, renderer.width, renderer.height)
            configDefViewport()
            OpenGLUtils.unbindFBO()
        }

        inline fun drawFrameUntilSucceed(encoder: MediaVideoEncoder, textureToRecord: TextureToRecord, timeout: Long = 3000) {
            tryUntilTimeout(timeout) {
                encoder.frameAvailableSoon(textureToRecord, surfaceSize)
            }
        }
    }

}