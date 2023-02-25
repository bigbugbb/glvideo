package com.binbo.glvideo.sample_app.impl.video.graph.gif_to_mp4

import android.util.Size
import android.widget.Toast
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.component.GifSource
import com.binbo.glvideo.core.graph.event.RecordingCompleted
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import com.binbo.glvideo.core.utils.FileToolUtils
import com.binbo.glvideo.core.utils.FileUseCase
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.AppConsts
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

class GifToMp4GraphManager(
    val videoFilename: String,
    val bmpWidth: Int,
    val bmpHeight: Int,
    val provider: GifSource.GifFrameProvider,
) : BaseGraphManager() {

    private var recordingCompleted = Channel<Boolean>()

    val recorderConfig: GLRecorderConfig
        get() = GLRecorderConfig.build {
            width(AppConsts.recordVideoSize.width)
            height(AppConsts.recordVideoSize.height)
            videoFrameRate(AppConsts.frameRate)
            targetFileDir(FileToolUtils.getFile(FileUseCase.GIF_TO_MP4))
            targetFilename(videoFilename)
            targetFileExt(AppConsts.recordVideoExt)
        }

    init {
        FileToolUtils.getFile(FileUseCase.GIF_TO_MP4).deleteRecursively()
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = GifSource(bmpWidth, bmpHeight, provider).apply { mediaGraph.addObject(this) }
                val mediaObject = GifToMp4RenderingObject(Size(bmpWidth, bmpHeight)).apply { mediaGraph.addObject(this) }
                val mediaSink = FrameRecorder(context, recorderConfig).apply { mediaGraph.addObject(this) }

                mediaSource to mediaObject to mediaSink
            }
        }
        return mediaGraph.apply { create() }
    }

    override fun destroyMediaGraph() {
        mediaGraph?.destroy()
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is RecordingCompleted -> recordingCompleted.send(true)
        }
    }

    suspend fun waitUntilDone() {
        recordingCompleted.receive()
        FileToolUtils.writeVideoToGallery(FileToolUtils.getFile(FileUseCase.GIF_TO_MP4, videoFilename + AppConsts.recordVideoExt), "video/mp4")
        withContext(Dispatchers.Main) {
            context.toast(context.getString(R.string.video_recording_successful_message), Toast.LENGTH_SHORT)
        }
    }
}