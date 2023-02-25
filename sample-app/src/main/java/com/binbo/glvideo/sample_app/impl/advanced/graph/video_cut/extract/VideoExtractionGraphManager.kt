package com.binbo.glvideo.sample_app.impl.advanced.graph.video_cut.extract

import android.net.Uri
import android.util.Log
import android.util.Range
import android.view.Choreographer
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.binbo.glvideo.core.ext.debounce
import com.binbo.glvideo.core.graph.MediaData
import com.binbo.glvideo.core.graph.MediaGraph
import com.binbo.glvideo.core.graph.base.BaseGraphEvent
import com.binbo.glvideo.core.graph.base.BaseMediaGraph
import com.binbo.glvideo.core.graph.event.StartFrameBuffering
import com.binbo.glvideo.core.graph.event.StopFrameBuffering
import com.binbo.glvideo.core.graph.event.VideoDecodingCompleted
import com.binbo.glvideo.core.graph.executor.GraphExecutor
import com.binbo.glvideo.core.graph.manager.BaseGraphManager
import com.binbo.glvideo.core.graph.simple.SimpleSinkObject
import com.binbo.glvideo.core.widget.VideoExtractionSurfaceView
import com.binbo.glvideo.sample_app.App.Companion.context
import com.binbo.glvideo.sample_app.event.TimelineUpdatedEvent
import com.binbo.glvideo.sample_app.utils.RxBus
import com.binbo.glvideo.sample_app.utils.VideoPlayerDelegate
import com.kk.taurus.playerbase.assist.RelationAssist
import com.kk.taurus.playerbase.player.IPlayer.STATE_STARTED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

/**
 * @author bigbug
 * @project lobby
 * @date 2022/12/13
 * @time 20:05
 */

const val tagOfExtract = "tag_of_extract"

class VideoExtractionGraphManager(
    val videoUri: Uri,
    viewVideoContainer: FrameLayout,
    surfaceView: VideoExtractionSurfaceView
) : BaseGraphManager(), Choreographer.FrameCallback, DefaultLifecycleObserver {

    internal var player: VideoPlayerDelegate

    var timeline: Range<Long> = Range(1000000L, 6000000L)
        private set

    val visibleTimeRange: Range<Long>
        get() = renderingObject?.visibleTimeRange ?: Range(0L, 10000000L)

    private val surfaceViewRef: WeakReference<VideoExtractionSurfaceView>

    private var renderingObject: VideoExtractionRenderingObject? = null

    private val mainScope by lazy { CoroutineScope(Dispatchers.Main) }

    init {
        surfaceViewRef = WeakReference(surfaceView)

        player = object : VideoPlayerDelegate(RelationAssist(context)) {
            /**
             * 需要处理选择框范围内循环播放
             */
            override fun onPlayComplete() {
                if (isLoopingEnabled) {
                    seekTo((timeline.lower / 1000L).toInt())
                    assist.play()
                }
            }
        }.apply {
            isLoopingEnabled = true
            setDataSource(videoUri)
            attachContainer(viewVideoContainer)
            playVideo(viewVideoContainer)
        }
    }

    override fun createMediaGraph(): BaseMediaGraph<MediaData> {
        mediaGraph = object : MediaGraph(this) {
            override fun onCreate() {
                super.onCreate()

                val mediaSource = VideoExtractionSource(videoUri).apply { mediaGraph.addObject(this) }
                val mediaObject = VideoExtractionRenderingObject(surfaceViewRef).apply { mediaGraph.addObject(this) }
                val mediaSink = SimpleSinkObject().apply { mediaGraph.addObject(this) }

                mediaSource to mediaObject to mediaSink

                renderingObject = mediaObject
            }
        }
        return mediaGraph.apply { create() }
    }

    override suspend fun start(dirType: Int) {
        super.start(dirType)
        Choreographer.getInstance().postFrameCallback(this)
    }

    override suspend fun stop(dirType: Int) {
        super.stop(dirType)
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun destroyMediaGraph() {
        mediaGraph.destroy()
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override suspend fun onReceiveEvent(event: BaseGraphEvent<MediaData>) {
        super.onReceiveEvent(event)
        when (event) {
            is StartFrameBuffering -> onStartFrameBuffering(event)
            is StopFrameBuffering -> onStopFrameBuffering(event)
            is VideoDecodingCompleted -> onStopFrameBuffering(StopFrameBuffering())
            is TimelineUpdatedEvent -> onTimelineUpdated(event)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        player?.let {
            if (it.getState() == STATE_STARTED) {
                val position = it.getCurrentPosition() * 1000L
                if (position > timeline.upper) {
//                        Log.d(tagOfExtract, "video playing seek to lower")
                    it.seekTo((timeline.lower / 1000L).toInt())
                }
//                    Log.d(tagOfExtract, "update video playing pos $position")
                mediaGraph.eventCoroutineScope.launch {
                    mediaGraph.broadcast(VideoPlayingPosUpdated(position))
                }
            }
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun setVideoRotation(degree: Int) {
        player.setVideoRotation(degree)
    }

    private fun onStartFrameBuffering(event: StartFrameBuffering) {
        RxBus.getDefault().send(event)
    }

    private fun onStopFrameBuffering(event: StopFrameBuffering) {
        RxBus.getDefault().send(event)
    }

    private fun onTimelineUpdated(event: TimelineUpdatedEvent) {
        timeline = event.timeline
        if (!event.suspendFrameLoading) {
            debounce(250, mainScope) { pos: Int ->
                player.seekTo(pos)
            }((timeline.lower / 1000f).toInt())
        }
        RxBus.getDefault().send(event)
        Log.d(tagOfExtract, "timeline updated to $timeline")
    }

    // LifecycleObserver
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        runBlocking {
            createMediaGraph()
            prepare()
            start()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onPause(owner)
        player.resume()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onResume(owner)
        player.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        runBlocking {
            stop()
            release()
            destroyMediaGraph()
        }
        player.reset()
        player.destroy()
    }
}