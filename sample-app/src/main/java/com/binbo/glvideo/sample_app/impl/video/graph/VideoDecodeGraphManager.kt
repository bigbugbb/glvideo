package com.binbo.glvideo.sample_app.impl.video.graph

import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.SurfaceView
import com.binbo.glvideo.core.graph.EndOfStream
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.base.GraphState
import com.binbo.glvideo.core.graph.component.VideoSource
import com.binbo.glvideo.core.graph.event.DecodedVideoFrame
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.graph.simple.SimpleSinkObject
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class VideoDecodeGraphManager(
    val videoUri: Uri,
    val videoRawId: Int,
    surfaceView: SurfaceView
) : BaseGraphManager() {

    private val surfaceViewRef: WeakReference<SurfaceView>

    init {
        surfaceViewRef = WeakReference(surfaceView)
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = VideoDecodeSource(videoUri, videoRawId).apply { mediaGraph.addObject(this) }
                val mediaObject = VideoDecodeRenderingObject(surfaceViewRef).apply { mediaGraph.addObject(this) }
                val mediaSink = SimpleSinkObject().apply { mediaGraph.addObject(this) }

                mediaSource to mediaObject to mediaSink
            }
        }
        return mediaGraph.apply { create() }
    }

    override fun destroyMediaGraph() {
        mediaGraph?.destroy()
    }
}

class VideoDecodeSource(videoUri: Uri, videoRawId: Int) : VideoSource(videoUri, videoRawId) {

    override val withSync: Boolean
        get() = true
}

class VideoDecodeRenderingObject(
    private val surfaceViewRef: WeakReference<SurfaceView>
) : SimpleMediaObject() {

    var renderer: VideoDecodeRenderer? = null
        private set

    override suspend fun onPrepare() {
        super.onPrepare()
        renderer = VideoDecodeRenderer(this).apply {
            addDrawer(FrameDrawer())
        }
    }

    override suspend fun onStart() {
        super.onStart()
        renderer?.run {
            setUseCustomRenderThread(true, graph?.eglResource?.sharedContext)
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
    }
}

class VideoDecodeRenderer(private val renderingObject: VideoDecodeRenderingObject) : DefaultGLRenderer() {

    private val textureQueue: BaseMediaQueue<MediaData>
        get() = renderingObject.inputQueues[0]

    private val frameDrawer: FrameDrawer?
        get() = drawers[FrameDrawer::class.java] as? FrameDrawer?

    override var impl: RenderImpl = object : RenderImpl {

        override fun onDrawFrame() {
            var frames = 1
            var endOfStream = false

            // 处理从decoder出来的帧
            kotlin.runCatching {
                if (renderingObject.state == GraphState.STARTED) {
                    textureQueue.poll(100, TimeUnit.MILLISECONDS)?.let { mediaData ->
                        when (mediaData) {
                            is DecodedVideoFrame -> {
                                val bitmap = OpenGLUtils.captureRenderBitmap(mediaData.textureId, mediaData.mediaWidth, mediaData.mediaHeight)
                                val viewportWidth = width
                                val viewportHeight = (viewportWidth.toFloat() * mediaData.mediaHeight / mediaData.mediaWidth).toInt()

                                GLES20.glViewport(0, (height - viewportHeight) / 2, viewportWidth, viewportHeight)

                                frameDrawer?.setTextureID(mediaData.textureId)
                                frameDrawer?.draw()

                                Log.d(TAG, "frames: ${frames++}")

                                mediaData.sharedTexture.close()  // free the used shared textures so they can be re-used by the video decoder
                            }
                            is EndOfStream -> endOfStream = true
                            else -> Log.d(TAG, "Unknown media data type")
                        }
                    }
                }
            }.getOrElse {
                it.printStackTrace()
            }
        }
    }

    companion object {
        private const val TAG = "VideoDecodeRenderer"
    }
}