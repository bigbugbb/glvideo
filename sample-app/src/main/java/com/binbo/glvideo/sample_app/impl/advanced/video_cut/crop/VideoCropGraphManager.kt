package com.binbo.glvideo.sample_app.impl.advanced.video_cut.crop

import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.core.os.bundleOf
import com.binbo.glvideo.core.ext.tryUntilTimeout
import com.binbo.glvideo.core.graph.EndOfStream
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.base.DirType
import com.binbo.glvideo.core.graph.component.FrameRecorder
import com.binbo.glvideo.core.graph.component.VideoConvertPostProcessor
import com.binbo.glvideo.core.graph.component.VideoSource
import com.binbo.glvideo.core.graph.event.*
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor
import com.binbo.glvideo.core.graph.interfaces.IMediaPostProcessor.Companion.ARG_DST_FILE_PATH
import com.binbo.glvideo.core.graph.interfaces.MResults
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.media.Frame
import com.binbo.glvideo.core.media.encoder.MediaVideoEncoder
import com.binbo.glvideo.core.media.ext.setupEncoderSurfaceRender
import com.binbo.glvideo.core.media.recorder.GLRecorderConfig
import com.binbo.glvideo.core.media.recorder.TextureToRecord
import com.binbo.glvideo.core.media.utils.VideoMetaData
import com.binbo.glvideo.core.opengl.drawer.Drawer
import com.binbo.glvideo.core.opengl.objects.Rectangle
import com.binbo.glvideo.core.opengl.program.TextureShaderProgram
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.event.CreateVideoCutFileSuccess
import com.binbo.glvideo.sample_app.impl.advanced.video_cut.VideoCutConfig
import com.binbo.glvideo.sample_app.utils.FileToolUtils
import com.binbo.glvideo.sample_app.utils.FileUseCase
import com.binbo.glvideo.sample_app.utils.rxbus.RxBus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.min

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

    override suspend fun waitUntilDone() {
        recordingCompleted.receive()
        RxBus.getDefault().send(CreateVideoCutFileSuccess(webpFilePath))
    }
}

class VideoCropSource(videoUri: Uri, timeline: Range<Long>) :
    VideoSource(videoUri, startPos = timeline.lower, clippingEnabled = true, clippingTimeline = timeline) {

    override val textureWidth: Int
        get() {
            var n = 1
            while (min(videoWidth, videoHeight) / n >= 800) {
                n *= 2
            }
            return videoWidth / n
        }

    override val textureHeight: Int
        get() {
            var n = 1
            while (min(videoWidth, videoHeight) / n >= 800) {
                n *= 2
            }
            return videoHeight / n
        }

    override val timeIntervalUsPerFrame: Long
        get() = 1000L / maxFrameRate * 1000L

    override val stopAfterWindowFilled: Boolean
        get() = true

    override val frameWindowSize: Int
        get() {
            /**
             * 需要限制帧率，因为手机拍的视频可能远超过30fps，而最终要裁剪生成的视频不需要那么高的帧率，且帧率过高会浪费显存。
             * 原则上app里其他graph的video source也应该限制帧率，但由于其他地方的源视频来源于我们自己生成的视频，所以目前没什么问题。
             * 这里的视频可以来自于手机相册，所以必须加限制。同时需要设置timeIntervalUsPerFrame，表示相邻采集帧的pts间隔必须大于这个值，从而控制帧采集的数量。
             */
            var frameRate = videoMetaData.frameRate.coerceAtMost(maxFrameRate)
            return frameRate * 6
        }

    override fun getFramePresentationTimeUs(frame: Frame, frameIndex: Int): Long {
        return frame.bufferInfo.presentationTimeUs
    }

    companion object {
        const val maxFrameRate = 30
    }
}

class VideoCropRenderingObject(val viewportSize: Size, val videoRotation: Int = 0) : SimpleMediaObject() {

    private var renderer: VideoCropRenderer? = null

    @Volatile
    private var meta: VideoMetaData? = null

    private var totalFrames: Int = Int.MAX_VALUE

    override suspend fun onPrepare() {
        super.onPrepare()
        renderer = VideoCropRenderer(this).apply {
            addDrawer(CropFrameDrawer())
        }
    }

    override suspend fun onStart() {
        super.onStart()
        renderer?.run {
            setUseCustomRenderThread(true, graph?.eglResource?.sharedContext)
            setSurface(null, viewportSize.width, viewportSize.height) // use offscreen rendering with PBuffer
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        }
    }

    override suspend fun onStop() {
        super.onRelease()
        renderer?.stop()
        renderer = null
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        when (event) {
            is VideoMetaDataRetrieved -> meta = event.meta
            is VideoDecodingCompleted -> totalFrames = event.totalFrames
        }
    }

    inner class VideoCropRenderer(private val renderingObject: VideoCropRenderingObject) : DefaultGLRenderer() {

        private val encoder: MediaVideoEncoder?
            get() = (renderingObject.outputQueues.elementAtOrNull(0)?.to as? FrameRecorder?)?.recorder?.getVideoEncoder()

        private val textureQueue: BaseMediaQueue<MediaData>
            get() = renderingObject.inputQueues[0]

        private val frameRate: Int
            get() = (meta?.frameRate ?: 24).coerceAtMost(30)

        private val videoWidth: Int
            get() = meta?.videoWidth ?: viewportSize.width

        private val videoHeight: Int
            get() = meta?.videoHeight ?: viewportSize.height

        private val frameBuffers = IntArray(1)
        private val frameBufferTextures = IntArray(1)

        private var frames: Int = 0
        private val ptsDelta = 1000000000L / frameRate

        override var impl: RenderImpl = object : RenderImpl {

            override fun onSurfaceChange(width: Int, height: Int) {
                super.onSurfaceChange(width, height)
                OpenGLUtils.createFBO(frameBuffers, frameBufferTextures, width, height)
                setupEncoderSurfaceRender(encoder)
            }

            override fun onSurfaceDestroy() {
                OpenGLUtils.deleteFBO(frameBuffers, frameBufferTextures)
            }

            override fun onDrawFrame() {
                kotlin.runCatching {
                    while (!endOfStream) {
                        textureQueue.poll(33, TimeUnit.MILLISECONDS)?.let { mediaData ->
                            when (mediaData) {
                                is DecodedVideoFrame -> {
                                    // val bitmap1 = OpenGLUtils.captureRenderBitmap(decodedVideoFrame.textureId, videoWidth, videoHeight)

                                    OpenGLUtils.drawWithFBO(frameBuffers[0], frameBufferTextures[0]) {
                                        GLES20.glViewport(0, 0, width, height)

                                        (drawers[CropFrameDrawer::class.java] as? CropFrameDrawer?)?.run {
                                            updateFrameSize(videoWidth, videoHeight)
                                            val windowSize = min(videoWidth, videoHeight)
                                            updateCropWindowSize((videoWidth - windowSize) / 2, (videoHeight - windowSize) / 2, windowSize, windowSize)
                                            setRotation(videoRotation)
                                            setTextureID(mediaData.textureId)
                                            draw()
                                        }

                                        val bitmap = OpenGLUtils.savePixels(0, 0, width, height)
                                        GLES20.glFinish()
                                    }

                                    encoder?.let { encoder ->
                                        drawFrameUntilSucceed(encoder, TextureToRecord(frameBufferTextures[0], frames++ * ptsDelta))
                                    }
                                }
                                is EndOfStream -> {
                                    runBlocking { broadcast(RenderingCompleted()) }
                                    endOfStream = true
                                }
                                else -> error("Unknown media data type")
                            }
                        }
                    }
                }.getOrElse {
                    it.printStackTrace()
                }
            }

            inline fun drawFrameUntilSucceed(encoder: MediaVideoEncoder, textureToRecord: TextureToRecord, timeout: Long = 3000) {
                tryUntilTimeout(timeout) {
                    Log.d("testpts", "frameAvailableSoon ${textureToRecord.pts / 1000}")
                    encoder.frameAvailableSoon(textureToRecord, surfaceSize)
                }
            }

            inline fun saveVideoFrame(bitmap: Bitmap) {
//                MemoryImageUtil.saveBitmap2File(bitmap, fileOfFirstFrame.absolutePath, 100)
            }
        }
    }

    private class CropFrameDrawer : Drawer() {

        private var textureProgram: TextureShaderProgram? = null

        private var frame: Rectangle? = null

        private var textureId = 0

        private var frameWidth = 200
        private var frameHeight = 200

        private var windowX = 0
        private var windowY = 0
        private var windowWidth = frameWidth
        private var windowHeight = frameHeight

        private var rotation = 0f

        init {
            Matrix.setIdentityM(projectionMatrix, 0)
        }

        // 原始frame尺寸
        fun updateFrameSize(width: Int, height: Int) {
            frameWidth = width
            frameHeight = height
        }

        // 裁剪窗口尺寸
        fun updateCropWindowSize(x: Int, y: Int, width: Int, height: Int) {
            windowX = x
            windowY = y
            windowWidth = width
            windowHeight = height
        }

        fun setRotation(rotation: Int) {
            this.rotation = 360 - rotation.toFloat() // 画到fbo的纹理是上下颠倒的，所以这里修正一下转向
        }

        override fun onWorldCreated() {
            textureProgram = TextureShaderProgram(context)
            frame = Rectangle()
        }

        override fun release() {
            textureProgram?.deleteProgram()
        }

        override fun draw() {
            super.draw()

            Matrix.setIdentityM(projectionMatrix, 0)
            Matrix.rotateM(projectionMatrix, 0, rotation, 0f, 0f, 1f)

            textureProgram?.useProgram()
            textureProgram?.setUniforms(projectionMatrix, textureId, 1f)
            frame?.updateTextureCoordinates(
                windowX.toFloat() / frameWidth, windowY.toFloat() / frameHeight,
                windowWidth.toFloat() / frameWidth, windowHeight.toFloat() / frameHeight
            )
            frame?.bindData(textureProgram)
            frame?.draw()

            // 解绑
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }

        override fun setTextureID(textureId: Int) {
            super.setTextureID(textureId)
            this.textureId = textureId
        }
    }
}