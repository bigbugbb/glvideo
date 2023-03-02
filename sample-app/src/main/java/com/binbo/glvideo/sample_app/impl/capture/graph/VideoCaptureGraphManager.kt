package com.binbo.glvideo.sample_app.impl.capture.graph

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import com.binbo.glvideo.core.ext.tryUntilTimeout
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.event.RecordingCompleted
import com.binbo.glvideo.core.graph.event.RenderingCompleted
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.graph.simple.SimpleMediaQueue
import com.binbo.glvideo.core.graph.simple.SimpleSourceObject
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder
import com.binbo.glvideo.core.media.ext.setupEncoderSurfaceRender
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import com.binbo.glvideo.core.media.recorder.TextureToRecord
import com.binbo.glvideo.core.opengl.drawer.CameraDrawer
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.core.opengl.renderer.DefaultCameraRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.core.utils.Constants
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.App.Const.frameRate
import com.binbo.glvideo.sample_app.App.Const.recordVideoExt
import com.binbo.glvideo.sample_app.App.Const.recordVideoSize
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.event.RecordVideoEvent
import com.binbo.glvideo.sample_app.utils.FileToolUtils
import com.binbo.glvideo.sample_app.utils.FileUseCase
import com.binbo.glvideo.sample_app.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class VideoCaptureGraphManager(
    private val capturedFilename: String,
    surfaceView: SurfaceView,
    textureAvailableListener: SurfaceTextureAvailableListener
) : BaseGraphManager(), SurfaceTexture.OnFrameAvailableListener {

    private val surfaceViewRef: WeakReference<SurfaceView>
    private val textureAvailableListenerRef: WeakReference<SurfaceTextureAvailableListener>

    private var recordingCompleted = Channel<Boolean>()
    private var recording = AtomicBoolean(false)

    val isRecording: Boolean
        get() = recording.get()

    val recorderConfig: GLRecorderConfig
        get() = GLRecorderConfig.build {
            width(recordVideoSize.width)
            height(recordVideoSize.height)
            videoFrameRate(frameRate)
            targetFileDir(FileToolUtils.getFile(FileUseCase.VIDEO_RECORDING))
            targetFilename(capturedFilename)
            targetFileExt(recordVideoExt)
        }

    private var renderingObject: VideoCaptureRenderingObject? = null

    init {
        surfaceViewRef = WeakReference(surfaceView)
        textureAvailableListenerRef = WeakReference(textureAvailableListener)
        FileToolUtils.getFile(FileUseCase.VIDEO_RECORDING).deleteRecursively()
    }

    suspend fun recordVideo(recording: Boolean) {
        mediaGraph.broadcast(RecordVideoEvent(recording))
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = SimpleSourceObject().apply { mediaGraph.addObject(this) }
                val mediaObject = VideoCaptureRenderingObject(surfaceViewRef, textureAvailableListenerRef).apply { mediaGraph.addObject(this) }
                val mediaSink = FrameRecorder(context, recorderConfig).apply { mediaGraph.addObject(this) }

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
            is RecordVideoEvent -> recording.set(event.recording)
            is RecordingCompleted -> recordingCompleted.send(true)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        renderingObject?.renderer?.notifySwap(SystemClock.uptimeMillis() * 1000)
    }

    suspend fun waitUntilDone() {
        recordingCompleted.receive()
        FileToolUtils.writeVideoToGallery(FileToolUtils.getFile(FileUseCase.VIDEO_RECORDING, capturedFilename + recordVideoExt), "video/mp4")
        withContext(Dispatchers.Main) {
            context.toast(context.getString(R.string.video_recording_successful_message), Toast.LENGTH_SHORT)
        }
    }
}

class VideoCaptureRenderingObject(
    private val surfaceViewRef: WeakReference<SurfaceView>,
    private val textureAvailableListener: WeakReference<SurfaceTextureAvailableListener>
) : SimpleMediaObject() {

    var renderer: VideoCaptureRenderer? = null
        private set

    var recording: Boolean = false
        private set

    override suspend fun onPrepare() {
        super.onPrepare()
        renderer = VideoCaptureRenderer(this).apply {
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

class VideoCaptureRenderer(private val renderingObject: VideoCaptureRenderingObject) : DefaultCameraRenderer() {

    private val encoder: MediaVideoEncoder?
        get() = (renderingObject.outputQueues.elementAtOrNull(0)?.to as? FrameRecorder?)?.recorder?.getVideoEncoder()

    override var impl: RenderImpl = object : DefaultRenderImpl(this) {

        private var lastCaptureTime = 0L

        private val recordingFrameBuffers = IntArray(Constants.RECORDER_INPUT_QUEUE_SIZE * 2)
        private val recordingFrameBufferTextures = IntArray(Constants.RECORDER_INPUT_QUEUE_SIZE * 2)

        private var frames = 0
        private val ptsDelta = 1000000000L / frameRate

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
                    if (abs(SystemClock.uptimeMillis() - lastCaptureTime) >= 1000 / frameRate) {
                        Log.d("capture video frames", "onDrawFrame capture frames: $frames")
                        lastCaptureTime = SystemClock.uptimeMillis()
                        val i = frames % recordingFrameBuffers.size
                        drawFrameToFrameBuffers(recordingFrameBuffers, recordingFrameBufferTextures, i)
                        GLES20.glFinish()
                        encoder?.let { drawFrameUntilSucceed(it, TextureToRecord(recordingFrameBufferTextures[i], frames * ptsDelta)) }
                        frames++
                    }
                } else {
                    if (frames > 0) {
                        runBlocking {
                            renderingObject.broadcast(RenderingCompleted()) // 触发recorder的stopRecording
                        }
                    }
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