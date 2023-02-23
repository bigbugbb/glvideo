package com.binbo.glvideo.sample_app.impl.video.graph.video_decode

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Size
import android.view.SurfaceView
import com.binbo.glvideo.core.graph.EndOfStream
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaQueue
import com.binbo.glvideo.core.graph.base.GraphState
import com.binbo.glvideo.core.graph.event.DecodedVideoFrame
import com.binbo.glvideo.core.graph.simple.SimpleMediaObject
import com.binbo.glvideo.core.graph.simple.SimpleMediaQueue
import com.binbo.glvideo.core.opengl.drawer.FrameDrawer
import com.binbo.glvideo.core.opengl.renderer.DefaultGLRenderer
import com.binbo.glvideo.core.opengl.renderer.RenderImpl
import com.binbo.glvideo.core.opengl.utils.OpenGLUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

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

    override fun createDefaultQueue(dirType: Int): BaseMediaQueue<MediaData> {
        return SimpleMediaQueue()
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
//                                val bitmap = OpenGLUtils.captureRenderBitmap(mediaData.textureId, mediaData.mediaWidth, mediaData.mediaHeight)
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