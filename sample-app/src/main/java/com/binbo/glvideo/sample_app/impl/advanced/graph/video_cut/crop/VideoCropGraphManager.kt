package com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.crop

import android.net.Uri
import android.os.Bundle
import android.util.Range
import androidx.core.os.bundleOf
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.base.DirType
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.component.VideoConvertPostProcessor
import com.binbo.glvideo.core.graph.event.RecordingCompleted
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor.Companion.ARG_DST_FILE_PATH
import com.binbo.glvideo.core.graph.interfaces.MResults
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import com.binbo.glvideo.core.utils.FileToolUtils
import com.binbo.glvideo.core.utils.FileUseCase
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.event.CreateVideoCutFileSuccess
import com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.VideoCutConfig
import com.binbo.glvideo.sample_app.utils.RxBus
import kotlinx.coroutines.channels.Channel
import java.io.File

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/8
 * @time 19:20
 */
class VideoCropGraphManager(val videoUri: Uri, val timeline: Range<Long> = Range(0L, Long.MAX_VALUE), val videoRotation: Int = 0) : BaseGraphManager() {

    private val recordingCompleted = Channel<Boolean>()

    private val recorderConfig: GLRecorderConfig
        get() = GLRecorderConfig.build {
            width(VideoCutConfig.cropVideoSize.width)
            height(VideoCutConfig.cropVideoSize.height)
            videoBitRate(600000)
            videoFrameRate(30)
            targetFileDir(FileToolUtils.getFile(FileUseCase.VIDEO_CUT))
            targetFilename("video_cut")
            clearFiles(false)
        }

    val webpFilePath: String
        get() = recorderConfig.targetFileDir.absolutePath + File.separator + recorderConfig.targetFilename + ".webp"

    init {
        FileToolUtils.getFile(FileUseCase.VIDEO_CUT).deleteRecursively()
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = VideoCropSource(videoUri, timeline).apply { mediaGraph.addObject(this) }
                val mediaObject = VideoCropRenderingObject(VideoCutConfig.cropVideoSize, videoRotation).apply { mediaGraph.addObject(this) }
                val mediaSink = FrameRecorder(context, recorderConfig).apply { mediaGraph.addObject(this) }

                mediaSource to mediaObject to mediaSink

                val args = bundleOf(ARG_DST_FILE_PATH to webpFilePath)
                mediaSink.setPostProcessor(object : VideoConvertPostProcessor(args) {
                    override suspend fun process(args: Bundle): MResults<String> {
                        val srcFilePath = args.getString(IMediaPostProcessor.ARG_SRC_FILE_PATH, "")
                        val dstFilePath = args.getString(ARG_DST_FILE_PATH, "")

                        val srcFile = File(srcFilePath)
                        val dstFile = File(dstFilePath)

                        return if (srcFile.exists()) {
                            kotlin.runCatching {
                                val result = convertMp4ToWebP(srcFile, dstFile)
                                MResults.success(result)
                            }.getOrElse {
                                MResults.failure(it)
                            }
                        } else {
                            MResults.failure(IllegalArgumentException("srcFile($srcFile) does NOT exist"))
                        }
                    }
                })
            }
        }
        return mediaGraph.apply { create() }
    }

    override fun destroyMediaGraph() {
        mediaGraph.destroy()
    }

    override suspend fun prepare(@DirType dirType: Int) {
        super.prepare(dirType)
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is RecordingCompleted -> {
                recordingCompleted.send(true)
            }
        }
    }

    suspend fun waitUntilDone() {
        recordingCompleted.receive()
        RxBus.getDefault().send(CreateVideoCutFileSuccess(webpFilePath))
    }
}