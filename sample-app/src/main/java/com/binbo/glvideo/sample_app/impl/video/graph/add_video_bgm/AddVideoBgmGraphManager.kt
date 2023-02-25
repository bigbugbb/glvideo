package com.binbo.glvideo.sample_app.impl.video.graph.add_video_bgm

import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.core.os.bundleOf
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.base.DirType
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.component.GifSource
import com.binbo.glvideo.core.graph.component.VideoBgmPostProcessor
import com.binbo.glvideo.core.graph.event.RecordingCompleted
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor
import com.binbo.glvideo.core.graph.interfaces.MResults
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import com.binbo.glvideo.core.utils.FileToolUtils
import com.binbo.glvideo.core.utils.FileToolUtils.getFile
import com.binbo.glvideo.core.utils.FileUseCase
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.AppConsts
import com.binbo.glvideo.sample_app.R
import com.binbo.glvideo.sample_app.impl.video.graph.gif_to_mp4.GifToMp4RenderingObject
import com.binbo.glvideo.sample_app.utils.FileUtil
import com.binbo.glvideo.sample_app.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.File

class AddVideoBgmGraphManager(
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
            targetFileDir(getFile(FileUseCase.VIDEO_WITHOUT_BGM))
            targetFilename(videoFilename)
            targetFileExt(AppConsts.recordVideoExt)
        }

    val videoBgmFile: File
        get() = getFile(FileUseCase.VIDEO_BGM, "bgm.aac")

    init {
        getFile(FileUseCase.VIDEO_WITHOUT_BGM).deleteRecursively()
        getFile(FileUseCase.VIDEO_WITH_BGM).deleteRecursively()
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = GifSource(bmpWidth, bmpHeight, provider).apply { mediaGraph.addObject(this) }
                val mediaObject = GifToMp4RenderingObject(Size(bmpWidth, bmpHeight)).apply { mediaGraph.addObject(this) }
                val mediaSink = FrameRecorder(context, recorderConfig).apply { mediaGraph.addObject(this) }

                mediaSink.setPostProcessor(
                    createVideoBgmPostProcessor(
                        bundleOf(
                            IMediaPostProcessor.ARG_DST_FILE_PATH to getFile(FileUseCase.VIDEO_WITH_BGM, "${videoFilename}.mp4").absolutePath,
                            "video_bgm_file_path" to videoBgmFile.absolutePath
                        )
                    )
                )

                mediaSource to mediaObject to mediaSink
            }
        }
        return mediaGraph.apply { create() }
    }

    override fun destroyMediaGraph() {
        mediaGraph?.destroy()
    }

    override suspend fun prepare(@DirType dirType: Int) {
        super.prepare(dirType)
        val bgmFolder = getFile(FileUseCase.VIDEO_BGM).apply { deleteRecursively() }
        FileUtil.copyAssets(context, "bgm.aac", "bgm", bgmFolder.absolutePath, null)
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is RecordingCompleted -> recordingCompleted.send(true)
        }
    }

    suspend fun waitUntilDone() {
        recordingCompleted.receive()
        FileToolUtils.writeVideoToGallery(getFile(FileUseCase.VIDEO_WITH_BGM, videoFilename + AppConsts.recordVideoExt), "video/mp4")
        withContext(Dispatchers.Main) {
            context.toast(context.getString(R.string.video_recording_successful_message), Toast.LENGTH_SHORT)
        }
    }

    private fun createVideoBgmPostProcessor(arguments: Bundle) = object : VideoBgmPostProcessor(arguments) {
        override suspend fun process(args: Bundle): MResults<String> {
            val srcFilePath = args.getString(IMediaPostProcessor.ARG_SRC_FILE_PATH, "")
            val dstFilePath = args.getString(IMediaPostProcessor.ARG_DST_FILE_PATH, "")
            val bgmFilePath = args.getString("video_bgm_file_path", "")

            val srcFile = File(srcFilePath)
            val dstFile = File(dstFilePath)
            val bgmFile = File(bgmFilePath)

            return if (srcFile.exists()) {
                kotlin.runCatching {
                    val result = addBgmToVideo(srcFile, bgmFile, dstFile, false)
                    MResults.success(result)
                }.getOrElse {
                    MResults.failure(it)
                }
            } else {
                MResults.failure(IllegalArgumentException("srcFile($srcFile) does NOT exist"))
            }
        }
    }
}