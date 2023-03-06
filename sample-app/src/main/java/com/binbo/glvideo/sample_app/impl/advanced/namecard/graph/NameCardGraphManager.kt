package com.binbo.glvideo.sample_app.impl.advanced.namecard.graph

import android.util.Size
import android.widget.Toast
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.event.RecordingCompleted
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleSourceObject
import com.binbo.glvideo.core.media.muxer.MediaMuxerManager
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import com.binbo.glvideo.core.utils.DeviceUtil
import com.binbo.glvideo.sample_app.App
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.App.Const.frameRate
import com.binbo.glvideo.sample_app.App.Const.recordVideoExt
import com.binbo.glvideo.sample_app.App.Const.recordVideoSize
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.impl.advanced.namecard.NameCardConfig
import com.binbo.glvideo.sample_app.utils.FileToolUtils
import com.binbo.glvideo.sample_app.utils.FileToolUtils.getFile
import com.binbo.glvideo.sample_app.utils.FileUseCase.Companion.NAME_CARD
import com.binbo.glvideo.sample_app.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/5
 * @time 19:26
 */
class NameCardGraphManager : BaseGraphManager() {

    private var recordingCompleted = Channel<Boolean>()

    private val recorderConfig: GLRecorderConfig
        get() = GLRecorderConfig.build {
            width(recordVideoSize.width)
            height(recordVideoSize.height)
            videoFrameRate(frameRate)
            targetFileDir(getFile(NAME_CARD))
            targetFilename("name_card")
        }

    private val viewportSize: Size
        get() = Size(DeviceUtil.getScreenWidth(context), DeviceUtil.getScreenHeight(context))

    init {
        getFile(NAME_CARD).deleteRecursively()
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = SimpleSourceObject().apply { mediaGraph.addObject(this) }
                val mediaObject = CardRenderingObject(viewportSize).apply { mediaGraph.addObject(this) }
                val mediaSink = FrameRecorder(context, recorderConfig).apply { mediaGraph.addObject(this) }

                mediaSource to mediaObject to mediaSink
            }
        }
        mediaGraph?.create()
        return mediaGraph!!
    }

    override fun destroyMediaGraph() {
        mediaGraph?.destroy()
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is RecordingCompleted -> recordingCompleted.send(true)
        }
    }

    override suspend fun waitUntilDone() {
        recordingCompleted.receive()
        val videoFile = getFile(NAME_CARD, recorderConfig.targetFilename + App.Const.recordVideoExt)
        FileToolUtils.writeVideoToGallery(videoFile, "video/mp4")
        withContext(Dispatchers.Main) {
            context.toast(context.getString(R.string.video_recording_successful_message), Toast.LENGTH_SHORT)
        }
    }
}