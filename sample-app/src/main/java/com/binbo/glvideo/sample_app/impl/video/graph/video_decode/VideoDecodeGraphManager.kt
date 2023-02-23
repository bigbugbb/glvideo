package com.binbo.glvideo.sample_app.impl.video.graph.video_decode

import android.net.Uri
import android.view.SurfaceView
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleSinkObject
import java.lang.ref.WeakReference


class VideoDecodeGraphManager(
    val videoUri: Uri,
    surfaceView: SurfaceView
) : BaseGraphManager() {

    private val surfaceViewRef: WeakReference<SurfaceView>

    private var renderingObject: VideoDecodeRenderingObject? = null

    init {
        surfaceViewRef = WeakReference(surfaceView)
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = VideoDecodeSource(videoUri).apply { mediaGraph.addObject(this) }
                val mediaObject = VideoDecodeRenderingObject(surfaceViewRef).apply { mediaGraph.addObject(this) }
                val mediaSink = SimpleSinkObject().apply { mediaGraph.addObject(this) }

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
//        when (event) {
//            is RecordingCompleted -> recordingCompleted.send(true)
//        }
    }
}