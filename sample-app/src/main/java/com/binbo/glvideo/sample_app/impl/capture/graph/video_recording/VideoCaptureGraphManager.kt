package com.binbo.glvideo.sample_app.impl.capture.graph.video_recording

import android.graphics.SurfaceTexture
import android.os.SystemClock
import android.view.SurfaceView
import android.widget.Toast
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.event.RecordingCompleted
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleSourceObject
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import com.binbo.glvideo.core.opengl.drawer.SurfaceTextureAvailableListener
import com.binbo.glvideo.core.utils.FileToolUtils
import com.binbo.glvideo.core.utils.FileUseCase
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.AppConsts
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.event.RecordVideoEvent
import com.binbo.glvideo.sample_app.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

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
            width(AppConsts.recordVideoSize.width)
            height(AppConsts.recordVideoSize.height)
            videoFrameRate(AppConsts.frameRate)
            targetFileDir(FileToolUtils.getFile(FileUseCase.VIDEO_RECORDING))
            targetFilename(capturedFilename)
            targetFileExt(AppConsts.recordVideoExt)
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
        FileToolUtils.writeVideoToGallery(FileToolUtils.getFile(FileUseCase.VIDEO_RECORDING, capturedFilename + AppConsts.recordVideoExt), "video/mp4")
        withContext(Dispatchers.Main) {
            context.toast(context.getString(R.string.video_recording_successful_message), Toast.LENGTH_SHORT)
        }
    }
}