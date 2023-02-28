package com.binbo.glvideo.sample_app.utils.player

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import com.binbo.glvideo.sample_app.utils.rxbus.RxBus
import com.kk.taurus.playerbase.assist.RelationAssist
import com.kk.taurus.playerbase.entity.DataSource
import com.kk.taurus.playerbase.event.OnErrorEventListener
import com.kk.taurus.playerbase.event.OnErrorEventListener.*
import com.kk.taurus.playerbase.event.OnPlayerEventListener
import com.kk.taurus.playerbase.event.OnPlayerEventListener.*
import com.kk.taurus.playerbase.render.AspectRatio
import java.util.concurrent.atomic.AtomicBoolean

data class VideoPlaybackCompleted(val videoUri: Uri?)
data class VideoPlaybackStarted(val videoUri: Uri?)
data class VideoPlaybackLoaded(val videoUri: Uri?)
data class VideoPlaybackError(val videoUri: Uri?, val eventCode: Int, val bundle: Bundle?)

interface VideoPlayerAction {
    fun setDataSource(uri: Uri)
    fun playVideo(container: FrameLayout)
    fun attachContainer(container: FrameLayout)
    fun setVideoRotation(rotation: Int)
    fun getCurrentPosition(): Int
    fun getDuration(): Int
    fun getState(): Int
    fun resume()
    fun pause()
    fun reset()
    fun destroy()
    fun seekTo(msc: Int)
}

open class VideoPlayerDelegate(
    val assist: RelationAssist
) : VideoPlayerAction, OnPlayerEventListener, OnErrorEventListener {

    protected var videoUri: Uri? = null

    protected val playerEventListener = CompositeOnPlayerEventListener()

    private val looping = AtomicBoolean(false)

    var isLoopingEnabled: Boolean
        get() = looping.get()
        set(value) {
            looping.set(value)
        }

    init {
        playerEventListener.addListener(this)

        assist.setOnErrorEventListener(this)
        assist.setOnPlayerEventListener(playerEventListener)
        assist.setAspectRatio(AspectRatio.AspectRatio_FILL_PARENT)
    }

    override fun setDataSource(uri: Uri) {
        videoUri = uri
        assist.setDataSource(DataSource().apply {
            this.uri = uri
        })
    }

    override fun playVideo(container: FrameLayout) {
        RxBus.getDefault().send(VideoPlaybackLoaded(videoUri))
        assist.attachContainer(container, true)
        assist.play()
    }

    override fun attachContainer(container: FrameLayout) {
        if (assist.superContainer.parent !== container) {
            assist.attachContainer(container)
        }
    }

    override fun setVideoRotation(degree: Int) {
        assist.render.setVideoRotation(degree)
    }

    override fun getCurrentPosition(): Int = assist.currentPosition

    override fun getDuration(): Int = assist.duration

    override fun getState(): Int = assist.state

    override fun resume() {
        assist.resume()
    }

    override fun pause() {
        assist.pause()
    }

    override fun reset() {
        assist.reset()
    }

    override fun destroy() {
        assist.destroy()
    }

    override fun seekTo(msc: Int) {
        assist.seekTo(msc)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //   PlayerEventObserver
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onPlayerEvent(eventCode: Int, bundle: Bundle?) {
        when (eventCode) {
            PLAYER_EVENT_ON_DATA_SOURCE_SET -> Log.d(TAG, "PLAYER_EVENT_ON_DATA_SOURCE_SET")
            PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE -> Log.d(TAG, "PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE")
            PLAYER_EVENT_ON_SURFACE_UPDATE -> Log.d(TAG, "PLAYER_EVENT_ON_SURFACE_UPDATE")
            PLAYER_EVENT_ON_PAUSE -> Log.d(TAG, "PLAYER_EVENT_ON_PAUSE")
            PLAYER_EVENT_ON_RESUME -> Log.d(TAG, "PLAYER_EVENT_ON_RESUME")
            PLAYER_EVENT_ON_STOP -> Log.d(TAG, "PLAYER_EVENT_ON_STOP")
            PLAYER_EVENT_ON_RESET -> Log.d(TAG, "PLAYER_EVENT_ON_RESET")
            PLAYER_EVENT_ON_DESTROY -> Log.d(TAG, "PLAYER_EVENT_ON_DESTROY")
            PLAYER_EVENT_ON_SEEK_COMPLETE -> Log.d(TAG, "PLAYER_EVENT_ON_SEEK_COMPLETE")
            PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE -> Log.d(TAG, "PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE")
            PLAYER_EVENT_ON_VIDEO_ROTATION_CHANGED -> Log.d(TAG, "PLAYER_EVENT_ON_VIDEO_ROTATION_CHANGED")
            PLAYER_EVENT_ON_AUDIO_RENDER_START -> Log.d(TAG, "PLAYER_EVENT_ON_AUDIO_RENDER_START")
            PLAYER_EVENT_ON_AUDIO_DECODER_START -> Log.d(TAG, "PLAYER_EVENT_ON_AUDIO_DECODER_START")
            PLAYER_EVENT_ON_AUDIO_SEEK_RENDERING_START -> Log.d(TAG, "PLAYER_EVENT_ON_AUDIO_SEEK_RENDERING_START")
            PLAYER_EVENT_ON_NETWORK_BANDWIDTH -> Log.d(TAG, "PLAYER_EVENT_ON_NETWORK_BANDWIDTH")
            PLAYER_EVENT_ON_BAD_INTERLEAVING -> Log.d(TAG, "PLAYER_EVENT_ON_BAD_INTERLEAVING")
            PLAYER_EVENT_ON_NOT_SEEK_ABLE -> Log.d(TAG, "PLAYER_EVENT_ON_NOT_SEEK_ABLE")
            PLAYER_EVENT_ON_METADATA_UPDATE -> Log.d(TAG, "PLAYER_EVENT_ON_METADATA_UPDATE")
            PLAYER_EVENT_ON_TIMED_TEXT_ERROR -> Log.d(TAG, "PLAYER_EVENT_ON_TIMED_TEXT_ERROR")
            PLAYER_EVENT_ON_UNSUPPORTED_SUBTITLE -> Log.d(TAG, "PLAYER_EVENT_ON_UNSUPPORTED_SUBTITLE")
            PLAYER_EVENT_ON_SUBTITLE_TIMED_OUT -> Log.d(TAG, "PLAYER_EVENT_ON_SUBTITLE_TIMED_OUT")
            PLAYER_EVENT_ON_STATUS_CHANGE -> Log.d(TAG, "PLAYER_EVENT_ON_STATUS_CHANGE")
            PLAYER_EVENT_ON_PROVIDER_DATA_START -> Log.d(TAG, "PLAYER_EVENT_ON_PROVIDER_DATA_START")
            PLAYER_EVENT_ON_PROVIDER_DATA_SUCCESS -> Log.d(TAG, "PLAYER_EVENT_ON_PROVIDER_DATA_SUCCESS")
            PLAYER_EVENT_ON_PREPARED -> Log.d(TAG, "PLAYER_EVENT_ON_PREPARED")
            PLAYER_EVENT_ON_START -> {
                Log.d(TAG, "PLAYER_EVENT_ON_START")
                onPlayStart()
            }
            PLAYER_EVENT_ON_VIDEO_RENDER_START -> {
                Log.d(TAG, "PLAYER_EVENT_ON_VIDEO_RENDER_START")
            }
            PLAYER_EVENT_ON_BUFFERING_START -> {
                Log.d(TAG, "PLAYER_EVENT_ON_BUFFERING_START")
            }
            PLAYER_EVENT_ON_BUFFERING_END -> {
                Log.d(TAG, "PLAYER_EVENT_ON_BUFFERING_END")
            }
            PLAYER_EVENT_ON_PLAY_COMPLETE -> {
                Log.d(TAG, "PLAYER_EVENT_ON_PLAY_COMPLETE")
                onPlayComplete()
            }
            PLAYER_EVENT_ON_PROVIDER_DATA_ERROR -> {
                Log.d(TAG, "PLAYER_EVENT_ON_PROVIDER_DATA_ERROR")
                onProviderDataError()
            }
            PLAYER_EVENT_ON_TIMER_UPDATE -> {
//                Log.d(TAG, "PLAYER_EVENT_ON_TIMER_UPDATE")
            }
        }
    }

    open fun onPlayComplete() {
        if (isLoopingEnabled) {
            seekTo(0)
            assist.play()
        }
    }

    open fun onPlayStart() {
    }

    open fun onProviderDataError() {}

    override fun onErrorEvent(eventCode: Int, bundle: Bundle?) {
        when (eventCode) {
            ERROR_EVENT_DATA_PROVIDER_ERROR -> Log.d(TAG, "ERROR_EVENT_DATA_PROVIDER_ERROR")
            ERROR_EVENT_COMMON -> Log.d(TAG, "ERROR_EVENT_COMMON")
            ERROR_EVENT_UNKNOWN -> Log.d(TAG, "ERROR_EVENT_UNKNOWN")
            ERROR_EVENT_SERVER_DIED -> Log.d(TAG, "ERROR_EVENT_SERVER_DIED")
            ERROR_EVENT_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d(TAG, "ERROR_EVENT_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK")
            ERROR_EVENT_IO -> Log.d(TAG, "ERROR_EVENT_IO")
            ERROR_EVENT_MALFORMED -> Log.d(TAG, "ERROR_EVENT_MALFORMED")
            ERROR_EVENT_UNSUPPORTED -> Log.d(TAG, "ERROR_EVENT_UNSUPPORTED")
            ERROR_EVENT_TIMED_OUT -> Log.d(TAG, "ERROR_EVENT_TIMED_OUT")
        }
    }

    companion object {
        const val TAG = "VideoPlayerDelegate"
    }
}
